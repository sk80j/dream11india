package com.example.dream11india

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// EXTENDED TRANSACTION MODEL (add orderId)
// ─────────────────────────────────────────────

// ─────────────────────────────────────────────
// TRANSACTION FILTER TABS
// ─────────────────────────────────────────────

enum class TxnFilter(val label: String) {
    ALL("All"), DEPOSIT("Deposits"),
    WITHDRAW("Withdrawals"), WINNINGS("Winnings"), BONUS("Bonus")
}

// ─────────────────────────────────────────────
// MAIN WALLET SCREEN
// ─────────────────────────────────────────────

@Composable
fun WalletScreen(
    userData:   UserData  = UserData(),
    onBack:     () -> Unit = {},
    onAddMoney: () -> Unit = {}
) {
    val vm: WalletViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab      by remember { mutableStateOf(TxnFilter.ALL) }
    var showDepositSheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Handle payment state changes ──
    LaunchedEffect(state.paymentState) {
        when (val ps = state.paymentState) {
            is PaymentState.AwaitingPayment -> {
                // Open in Custom Tab
                openCustomTab(context, ps.url)
            }
            is PaymentState.Success -> {
                scope.launch { snackbarHostState.showSnackbar("✅ Payment successful! Wallet updated.") }
                vm.resetPayment()
            }
            is PaymentState.Failed -> {
                scope.launch { snackbarHostState.showSnackbar("❌ Payment failed. Please try again.") }
            }
            is PaymentState.Pending -> {
                scope.launch { snackbarHostState.showSnackbar("⏳ Payment pending. We'll update you soon.") }
            }
            is PaymentState.Error -> {
                scope.launch { snackbarHostState.showSnackbar("⚠️ ${ps.message}") }
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost    = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier       = Modifier.padding(14.dp),
                    containerColor = Color(0xFF1C1C1C),
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(10.dp)
                ) { Text(data.visuals.message, fontWeight = FontWeight.SemiBold) }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { pad ->

        Column(modifier = Modifier.fillMaxSize().padding(pad)) {

            // ── TOP HEADER ──
            WalletHeader(
                balance      = state.balance,
                winnings     = state.winnings,
                bonusBalance = state.bonusBalance,
                onBack       = onBack,
                onAddMoney   = { showDepositSheet = true },
                onWithdraw   = { showWithdrawSheet = true }
            )

            // ── FILTER TABS ──
            TxnFilterTabs(selected = selectedTab, onSelect = { selectedTab = it })

            // ── TRANSACTION LIST ──
            val filtered = when (selectedTab) {
                TxnFilter.ALL      -> state.transactions
                TxnFilter.DEPOSIT  -> state.transactions.filter { it.type == "credit" }
                TxnFilter.WITHDRAW -> state.transactions.filter { it.type == "debit" }
                TxnFilter.WINNINGS -> state.transactions.filter { it.type == "winning" }
                TxnFilter.BONUS    -> state.transactions.filter { it.type == "bonus" }
            }

            if (state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = D11Red)
                }
            } else if (filtered.isEmpty()) {
                WalletEmptyState(modifier = Modifier.weight(1f), filter = selectedTab)
            } else {
                LazyColumn(
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { txn ->
                        TransactionCard(txn = txn)
                    }
                }
            }
        }

        // ── DEPOSIT SHEET ──
        if (showDepositSheet) {
            DepositBottomSheet(
                phone         = userData.phone,
                paymentState  = state.paymentState,
                onCreateOrder = { amount -> vm.createOrder(amount, userData.phone) },
                onVerify      = { orderId -> vm.verifyPayment(orderId) },
                onDismiss     = {
                    showDepositSheet = false
                    vm.resetPayment()
                }
            )
        }

        // ── WITHDRAW SHEET ──
        if (showWithdrawSheet) {
            WithdrawBottomSheet(
                winnings  = state.winnings,
                userId    = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                onDismiss = { showWithdrawSheet = false },
                onSuccess = { msg ->
                    showWithdrawSheet = false
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                onError   = { err ->
                    scope.launch { snackbarHostState.showSnackbar(err) }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────
// WALLET HEADER
// ─────────────────────────────────────────────

@Composable
private fun WalletHeader(
    balance:      Int,
    winnings:     Int,
    bonusBalance: Int,
    onBack:       () -> Unit,
    onAddMoney:   () -> Unit,
    onWithdraw:   () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF8B0000), Color(0xFFCC0000)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Top row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick  = onBack,
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                    Text("My Wallet", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0x88FFFFFF), modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Balance cards row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WalletBalanceCard(
                    label   = "Total Balance",
                    amount  = balance,
                    color   = Color.White,
                    icon    = "💰",
                    modifier= Modifier.weight(1f)
                )
                WalletBalanceCard(
                    label   = "Winnings",
                    amount  = winnings,
                    color   = D11Yellow,
                    icon    = "🏆",
                    modifier= Modifier.weight(1f)
                )
                WalletBalanceCard(
                    label   = "Bonus",
                    amount  = bonusBalance,
                    color   = Color(0xFF69F0AE),
                    icon    = "🎁",
                    modifier= Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Action buttons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick  = onAddMoney,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Add Cash", color = D11Red, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                Button(
                    onClick  = onWithdraw,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, Color(0x66FFFFFF))
                ) {
                    Text("Withdraw", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun WalletBalanceCard(
    label:    String,
    amount:   Int,
    color:    Color,
    icon:     String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(Color(0x33FFFFFF)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.height(3.dp))
            Text("₹$amount", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color(0xBBFFFFFF), fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────
// FILTER TABS
// ─────────────────────────────────────────────

@Composable
private fun TxnFilterTabs(selected: TxnFilter, onSelect: (TxnFilter) -> Unit) {
    LazyRow(
        modifier        = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111)),
        contentPadding  = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TxnFilter.values()) { filter ->
            val active = selected == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) D11Red else Color(0xFF1E1E1E))
                    .border(1.dp, if (active) D11Red else Color(0xFF333333), RoundedCornerShape(20.dp))
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    filter.label,
                    color      = if (active) Color.White else Color(0xFF888888),
                    fontSize   = 12.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// TRANSACTION CARD
// ─────────────────────────────────────────────

@Composable
fun TransactionCard(txn: Transaction) {
    val isCredit  = txn.type in listOf("credit", "winning", "bonus")
    val (icon, bg, fg) = when (txn.type) {
        "credit"  -> Triple("💳", Color(0xFF0A2A0A), D11Green)
        "winning" -> Triple("🏆", Color(0xFF2A2A00), D11Yellow)
        "bonus"   -> Triple("🎁", Color(0xFF0A1A2A), Color(0xFF82B1FF))
        "debit"   -> Triple("🏧", Color(0xFF2A0A0A), D11Red)
        else      -> Triple("💸", Color(0xFF1A1A1A), Color.White)
    }
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color(0xFF2A2A2A))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Box(
                    modifier         = Modifier.size(44.dp).clip(CircleShape).background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 18.sp)
                }
                Column {
                    Text(
                        txn.description.ifEmpty {
                            when (txn.type) {
                                "credit"  -> "Deposit"
                                "winning" -> "Contest Winning"
                                "bonus"   -> "Bonus Credit"
                                "debit"   -> "Withdrawal"
                                else      -> "Transaction"
                            }
                        },
                        color    = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        sdf.format(Date(txn.timestamp)),
                        color    = Color(0xFF888888),
                        fontSize = 10.sp
                    )
                    if (txn.orderId.isNotEmpty()) {
                        Text(
                            "ID: ${txn.orderId.takeLast(12)}",
                            color    = Color(0xFF555555),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isCredit) "+" else "−"}₹${txn.amount}",
                    color      = fg,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (txn.status) {
                                "completed" -> Color(0xFF004400)
                                "pending"   -> Color(0xFF444400)
                                "failed"    -> Color(0xFF440000)
                                else        -> Color(0xFF222222)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        txn.status.uppercase(),
                        color = when (txn.status) {
                            "completed" -> D11Green
                            "pending"   -> D11Yellow
                            "failed"    -> D11Red
                            else        -> Color(0xFF888888)
                        },
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// DEPOSIT BOTTOM SHEET
// ─────────────────────────────────────────────

@Composable
fun DepositBottomSheet(
    phone:         String,
    paymentState:  PaymentState,
    onCreateOrder: (Int) -> Unit,
    onVerify:      (String) -> Unit,
    onDismiss:     () -> Unit
) {
    var amount    by remember { mutableStateOf("") }
    var lastOrder by remember { mutableStateOf("") }

    // Store orderId when AwaitingPayment
    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.AwaitingPayment) {
            lastOrder = paymentState.orderId
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {},
            colors = CardDefaults.cardColors(Color(0xFF111111)),
            shape  = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Handle
                Box(
                    modifier = Modifier.width(40.dp).height(4.dp)
                        .clip(CircleShape).background(Color(0xFF444444))
                        .align(Alignment.CenterHorizontally)
                )

                Text("Add Money", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)

                // Quick chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("100", "200", "500", "1000", "5000").forEach { chip ->
                        val active = amount == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) D11Red else Color(0xFF1E1E1E))
                                .border(1.dp, if (active) D11Red else Color(0xFF333333), RoundedCornerShape(8.dp))
                                .clickable { amount = chip }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "₹$chip",
                                color      = if (active) Color.White else Color(0xFF888888),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Amount input
                OutlinedTextField(
                    value         = amount,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) amount = it },
                    placeholder   = { Text("Enter amount", color = Color(0xFF888888)) },
                    prefix        = { Text("₹ ", color = Color.White, fontWeight = FontWeight.Bold) },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = D11Red,
                        unfocusedBorderColor    = Color(0xFF333333),
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White,
                        cursorColor             = D11Red,
                        focusedContainerColor   = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape           = RoundedCornerShape(10.dp),
                    singleLine      = true
                )

                // State-based UI
                when (paymentState) {
                    is PaymentState.Idle, is PaymentState.Error -> {
                        if (paymentState is PaymentState.Error) {
                            Text(
                                "⚠️ ${paymentState.message}",
                                color    = D11Red,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick  = {
                                val amt = amount.toIntOrNull() ?: 0
                                if (amt < 10) return@Button
                                onCreateOrder(amt)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if ((amount.toIntOrNull() ?: 0) >= 10) D11Red else Color(0xFF333333)
                            ),
                            shape    = RoundedCornerShape(10.dp),
                            enabled  = (amount.toIntOrNull() ?: 0) >= 10
                        ) {
                            Text("Proceed to Pay ₹${amount.ifEmpty { "0" }}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }

                    is PaymentState.Creating -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = D11Red, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Text("Creating payment order…", color = Color(0xFF888888), fontSize = 13.sp)
                            }
                        }
                    }

                    is PaymentState.AwaitingPayment -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(colors = CardDefaults.cardColors(Color(0xFF1A1A00)), shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("ℹ️", fontSize = 18.sp)
                                    Text("Payment page opened. Complete payment then tap Verify below.", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick  = { onVerify(lastOrder) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors   = ButtonDefaults.buttonColors(D11Green),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Text("Verify Payment", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }

                    is PaymentState.Verifying -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = D11Green, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Text("Verifying payment…", color = Color(0xFF888888), fontSize = 13.sp)
                            }
                        }
                    }

                    is PaymentState.Success -> {
                        PaymentSuccessView(onDone = onDismiss)
                    }

                    is PaymentState.Failed -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(colors = CardDefaults.cardColors(Color(0xFF2A0A0A)), shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("❌", fontSize = 18.sp)
                                    Text("Payment failed. Please try again.", color = D11Red, fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick  = { onCreateOrder(amount.toIntOrNull() ?: 0) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors   = ButtonDefaults.buttonColors(D11Red),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Text("Retry Payment", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }

                    is PaymentState.Pending -> {
                        Card(colors = CardDefaults.cardColors(Color(0xFF1A1A00)), shape = RoundedCornerShape(10.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("⏳", fontSize = 18.sp)
                                Column {
                                    Text("Payment Pending", color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("We'll update your wallet once confirmed.", color = Color(0xFF888888), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Info row
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoChip("🔒", "Secure")
                    InfoChip("⚡", "Instant")
                    InfoChip("🏦", "UPI/Cards")
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun InfoChip(icon: String, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 12.sp)
        Text(label, color = Color(0xFF888888), fontSize = 11.sp)
    }
}

// ─────────────────────────────────────────────
// PAYMENT SUCCESS VIEW
// ─────────────────────────────────────────────

@Composable
private fun PaymentSuccessView(onDone: () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
    }
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("✅", fontSize = 52.sp, modifier = Modifier.scale(scale.value))
        Text("Payment Successful!", color = D11Green, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text("Your wallet has been updated.", color = Color(0xFF888888), fontSize = 13.sp)
        Button(
            onClick  = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(D11Green),
            shape    = RoundedCornerShape(10.dp)
        ) {
            Text("Done", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

// ─────────────────────────────────────────────
// WITHDRAW BOTTOM SHEET
// ─────────────────────────────────────────────

@Composable
fun WithdrawBottomSheet(
    winnings:  Int,
    userId:    String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    onError:   (String) -> Unit
) {
    var amount    by remember { mutableStateOf("") }
    var upiId     by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val db    = FirebaseFirestore.getInstance()

    fun validateUpi(upi: String) = upi.contains("@") && upi.length > 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {},
            colors = CardDefaults.cardColors(Color(0xFF111111)),
            shape  = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFF444444)).align(Alignment.CenterHorizontally))

                Text("Withdraw Winnings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)

                // Winnings balance
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1A1A00))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Winnings Balance", color = Color(0xFF888888), fontSize = 13.sp)
                    Text("₹$winnings", color = D11Yellow, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Quick amounts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("100", "500", "1000", "2000").forEach { chip ->
                        val active = amount == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) D11Red else Color(0xFF1E1E1E))
                                .border(1.dp, if (active) D11Red else Color(0xFF333333), RoundedCornerShape(8.dp))
                                .clickable { amount = chip }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text("₹$chip", color = if (active) Color.White else Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Amount input
                OutlinedTextField(
                    value         = amount,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) amount = it },
                    placeholder   = { Text("Min ₹100", color = Color(0xFF888888)) },
                    prefix        = { Text("₹ ", color = Color.White, fontWeight = FontWeight.Bold) },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = D11Red,
                        unfocusedBorderColor    = Color(0xFF333333),
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White,
                        cursorColor             = D11Red,
                        focusedContainerColor   = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape           = RoundedCornerShape(10.dp),
                    singleLine      = true
                )

                // UPI input
                OutlinedTextField(
                    value         = upiId,
                    onValueChange = { upiId = it },
                    placeholder   = { Text("yourname@upi", color = Color(0xFF888888)) },
                    label         = { Text("UPI ID", color = Color(0xFF888888)) },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = D11Red,
                        unfocusedBorderColor    = Color(0xFF333333),
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White,
                        cursorColor             = D11Red,
                        focusedContainerColor   = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    shape      = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Info box
                Card(colors = CardDefaults.cardColors(Color(0xFF1A1A00)), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("ℹ️ Withdrawal Info", color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("• Min withdrawal: ₹100", color = Color(0xFF888888), fontSize = 11.sp)
                        Text("• Processing: 24–48 hours", color = Color(0xFF888888), fontSize = 11.sp)
                        Text("• GST (28%) deducted by admin", color = Color(0xFF888888), fontSize = 11.sp)
                        Text("• UPI transfers only", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                }

                // Submit
                Button(
                    onClick = {
                        val amt = amount.toIntOrNull() ?: 0
                        when {
                            amt < 100          -> onError("Minimum withdraw ₹100!")
                            amt > winnings     -> onError("Insufficient winnings balance!")
                            !validateUpi(upiId)-> onError("Enter valid UPI ID!")
                            else -> {
                                isLoading = true
                                scope.launch {
                                    try {
                                        db.collection("withdrawRequests").add(mapOf(
                                            "userId"    to userId,
                                            "amount"    to amt,
                                            "upiId"     to upiId,
                                            "status"    to "pending",
                                            "createdAt" to System.currentTimeMillis()
                                        )).await()
                                        isLoading = false
                                        onSuccess("Withdraw request submitted! Processing in 24–48 hours.")
                                    } catch (e: Exception) {
                                        isLoading = false
                                        onError("Failed: ${e.message}")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (amount.isNotEmpty() && upiId.isNotEmpty()) D11Red else Color(0xFF333333)
                    ),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = !isLoading && amount.isNotEmpty() && upiId.isNotEmpty()
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Submit Withdraw Request", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────

@Composable
private fun WalletEmptyState(modifier: Modifier, filter: TxnFilter) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📭", fontSize = 40.sp)
            Text(
                when (filter) {
                    TxnFilter.DEPOSIT  -> "No deposits yet"
                    TxnFilter.WITHDRAW -> "No withdrawals yet"
                    TxnFilter.WINNINGS -> "No winnings yet"
                    TxnFilter.BONUS    -> "No bonuses yet"
                    else               -> "No transactions yet"
                },
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
            Text("Your history will appear here", color = Color(0xFF888888), fontSize = 12.sp)
        }
    }
}

// ─────────────────────────────────────────────
// WITHDRAW REQUEST CARD (for WalletScreen tabs)
// ─────────────────────────────────────────────

@Composable
fun WithdrawRequestCard(req: WithdrawRequest) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (req.status) {
                "approved" -> Color(0xFF0A2A0A)
                "rejected" -> Color(0xFF2A0A0A)
                else       -> Color(0xFF1A1A1A)
            }
        ),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color(0xFF2A2A2A))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Withdrawal Request", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("UPI: ${req.upiId}", color = Color(0xFF888888), fontSize = 11.sp)
                Text(sdf.format(Date(req.createdAt)), color = Color(0xFF666666), fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${req.amount}", color = D11Yellow, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(when (req.status) {
                            "approved" -> Color(0xFF004400)
                            "rejected" -> Color(0xFF440000)
                            else       -> Color(0xFF444400)
                        })
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        req.status.uppercase(),
                        color = when (req.status) {
                            "approved" -> D11Green
                            "rejected" -> D11Red
                            else       -> D11Yellow
                        },
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// CUSTOM TAB HELPER
// ─────────────────────────────────────────────

private fun openCustomTab(context: Context, url: String) {
    try {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        intent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        // Fallback to browser
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
