package com.example.dream11india

import android.content.ClipData
import android.content.ClipboardManager
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────
// PAYMENT MODE
// ─────────────────────────────────────────────

private enum class PaymentMode { AUTO, MANUAL }

private enum class ScreenState {
    AMOUNT_SELECT,
    PROCESSING,
    PAYMENT_OPENED,
    VERIFYING,
    SUCCESS,
    FAILED,
    PENDING,
    MANUAL_QR
}

// ─────────────────────────────────────────────
// MAIN PAYMENT SCREEN
// ─────────────────────────────────────────────

@Composable
fun PaymentScreen(
    userData: UserData = UserData(),
    onBack:   () -> Unit = {}
) {
    val context           = LocalContext.current
    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val db  = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ── State ──
    var screenState   by remember { mutableStateOf(ScreenState.AMOUNT_SELECT) }
    var selectedAmt   by remember { mutableStateOf(0) }
    var customAmt     by remember { mutableStateOf("") }
    var couponCode    by remember { mutableStateOf("") }
    var couponApplied by remember { mutableStateOf(false) }
    var currentOrderId by remember { mutableStateOf("") }
    var utrNumber     by remember { mutableStateOf("") }
    var errorMsg      by remember { mutableStateOf("") }
    var pollCount     by remember { mutableStateOf(0) }

    val finalAmt = customAmt.toIntOrNull()?.takeIf { it > 0 } ?: selectedAmt

    // ── Create order + open payment ──
    fun startPayment() {
        if (finalAmt < 10) { errorMsg = "Minimum deposit ₹10"; return }
        errorMsg = ""
        screenState = ScreenState.PROCESSING
        scope.launch {
            val orderId = TrustopeRepository.generateOrderId()
            currentOrderId = orderId
            when (val result = TrustopeRepository.createOrder(finalAmt, userData.phone.ifEmpty { "9999999999" }, orderId)) {
                is PaymentResult.Success -> {
                    screenState = ScreenState.PAYMENT_OPENED
                    // Open Custom Tab
                    try {
                        val tabIntent = CustomTabsIntent.Builder().setShowTitle(true).build()
                        tabIntent.launchUrl(context, Uri.parse(result.paymentUrl))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.paymentUrl)))
                    }
                }
                is PaymentResult.Error -> {
                    errorMsg = result.message
                    screenState = ScreenState.MANUAL_QR   // fallback
                }
            }
        }
    }

    // ── Verify payment ──
    fun verifyPayment() {
        if (currentOrderId.isEmpty()) return
        screenState = ScreenState.VERIFYING
        pollCount = 0
        scope.launch {
            // Poll up to 3 times
            repeat(3) { attempt ->
                pollCount = attempt + 1
                when (val result = TrustopeRepository.checkOrderStatus(currentOrderId)) {
                    StatusResult.Success -> {
                        val snap   = db.collection("paymentOrders").document(currentOrderId).get().await()
                        val amount = snap.getLong("amount")?.toInt() ?: finalAmt
                        TrustopeRepository.creditWalletSafe(currentOrderId, amount)
                        screenState = ScreenState.SUCCESS
                        return@launch
                    }
                    StatusResult.Failed -> {
                        screenState = ScreenState.FAILED
                        return@launch
                    }
                    StatusResult.Pending -> {
                        if (attempt < 2) delay(3000L)
                        else screenState = ScreenState.PENDING
                    }
                    is StatusResult.Error -> {
                        screenState = ScreenState.PENDING
                        return@launch
                    }
                }
            }
        }
    }

    // ── Manual QR submit ──
    fun submitManual() {
        if (utrNumber.length < 6) { errorMsg = "Enter valid UTR number"; return }
        errorMsg = ""
        scope.launch {
            try {
                db.collection("payment_requests").add(mapOf(
                    "userId"    to uid,
                    "userName"  to userData.name,
                    "userPhone" to userData.phone,
                    "amount"    to finalAmt,
                    "utrNumber" to utrNumber,
                    "status"    to "pending",
                    "timestamp" to System.currentTimeMillis()
                )).await()
                screenState = ScreenState.SUCCESS
            } catch (e: Exception) {
                errorMsg = "Submit failed: ${e.message}"
            }
        }
    }

    Scaffold(
        snackbarHost = {
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

        // ── FULL SCREEN STATES ──
        when (screenState) {

            ScreenState.SUCCESS -> PaymentSuccessScreen(
                isManual  = utrNumber.isNotEmpty(),
                amount    = finalAmt,
                orderId   = currentOrderId,
                utr       = utrNumber,
                onBack    = onBack
            )

            ScreenState.FAILED -> PaymentFailedScreen(
                amount    = finalAmt,
                onRetry   = { screenState = ScreenState.AMOUNT_SELECT },
                onBack    = onBack
            )

            ScreenState.PENDING -> PaymentPendingScreen(
                orderId   = currentOrderId,
                onCheck   = { verifyPayment() },
                onBack    = onBack
            )

            ScreenState.VERIFYING -> PaymentVerifyingScreen(pollCount = pollCount)

            ScreenState.PROCESSING -> PaymentProcessingScreen()

            ScreenState.PAYMENT_OPENED -> PaymentOpenedScreen(
                amount    = finalAmt,
                onVerify  = { verifyPayment() },
                onManual  = { screenState = ScreenState.MANUAL_QR },
                onBack    = { screenState = ScreenState.AMOUNT_SELECT }
            )

            ScreenState.MANUAL_QR -> ManualQRScreen(
                amount    = finalAmt,
                utrNumber = utrNumber,
                errorMsg  = errorMsg,
                onUtrChange = { utrNumber = it },
                onSubmit  = { submitManual() },
                onBack    = { screenState = ScreenState.AMOUNT_SELECT }
            )

            ScreenState.AMOUNT_SELECT -> AmountSelectScreen(
                userData      = userData,
                selectedAmt   = selectedAmt,
                customAmt     = customAmt,
                couponCode    = couponCode,
                couponApplied = couponApplied,
                errorMsg      = errorMsg,
                onAmtSelect   = { selectedAmt = it; customAmt = "" },
                onCustomChange= { customAmt = it; selectedAmt = 0 },
                onCouponChange= { couponCode = it },
                onApplyCoupon = {
                    if (couponCode.isNotEmpty()) {
                        couponApplied = true
                        scope.launch { snackbarHostState.showSnackbar("Coupon applied!") }
                    }
                },
                onPay         = { startPayment() },
                onManualQR    = { screenState = ScreenState.MANUAL_QR },
                onBack        = onBack
            )
        }
    }
}

// ─────────────────────────────────────────────
// AMOUNT SELECT SCREEN
// ─────────────────────────────────────────────

@Composable
private fun AmountSelectScreen(
    userData:       UserData,
    selectedAmt:    Int,
    customAmt:      String,
    couponCode:     String,
    couponApplied:  Boolean,
    errorMsg:       String,
    onAmtSelect:    (Int) -> Unit,
    onCustomChange: (String) -> Unit,
    onCouponChange: (String) -> Unit,
    onApplyCoupon:  () -> Unit,
    onPay:          () -> Unit,
    onManualQR:     () -> Unit,
    onBack:         () -> Unit
) {
    val finalAmt = customAmt.toIntOrNull()?.takeIf { it > 0 } ?: selectedAmt

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF8B0000), Color(0xFFCC0000))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                        Column {
                            Text("Add Money", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Lock, null, tint = Color(0xAAFFFFFF), modifier = Modifier.size(10.dp))
                                Text("100% Secure Payment", color = Color(0xAAFFFFFF), fontSize = 10.sp)
                            }
                        }
                    }
                    // Balance chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("₹${userData.balance}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Quick amounts
            item {
                Text("Select Amount", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                val chips = listOf(
                    Triple(50,    null,      null),
                    Triple(100,   "Popular", null),
                    Triple(200,   null,      null),
                    Triple(500,   "Best",    "₹50 Bonus"),
                    Triple(1000,  null,      null),
                    Triple(2000,  "Hot",     null),
                    Triple(5000,  null,      "₹500 Bonus"),
                    Triple(10000, null,      null)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (amt, badge, cashback) ->
                                AmountChip(
                                    amount   = amt,
                                    badge    = badge,
                                    cashback = cashback,
                                    selected = selectedAmt == amt && customAmt.isEmpty(),
                                    onClick  = { onAmtSelect(amt) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty slots
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // Custom amount
            item {
                OutlinedTextField(
                    value         = customAmt,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) onCustomChange(it) },
                    placeholder   = { Text("Enter custom amount", color = Color(0xFF888888)) },
                    label         = { Text("Or enter amount", color = Color(0xFF888888)) },
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
            }

            // Coupon code
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = couponCode,
                        onValueChange = onCouponChange,
                        placeholder   = { Text("Promo / Coupon code", color = Color(0xFF888888)) },
                        modifier      = Modifier.weight(1f),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = if (couponApplied) D11Green else D11Red,
                            unfocusedBorderColor    = Color(0xFF333333),
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            cursorColor             = D11Red,
                            focusedContainerColor   = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1A1A1A)
                        ),
                        shape      = RoundedCornerShape(10.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (couponApplied) Icon(Icons.Default.CheckCircle, null, tint = D11Green, modifier = Modifier.size(18.dp))
                        }
                    )
                    Button(
                        onClick  = onApplyCoupon,
                        colors   = ButtonDefaults.buttonColors(if (couponApplied) D11Green else D11Red),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(if (couponApplied) "Applied" else "Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Payment summary
            if (finalAmt > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(Color(0xFF1A1A1A)),
                        shape  = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF333333))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Payment Summary", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = Color(0xFF2A2A2A))
                            SummaryRow("Amount",       "₹$finalAmt",       Color.White)
                            if (couponApplied) SummaryRow("Discount", "- ₹0", D11Green)
                            HorizontalDivider(color = Color(0xFF2A2A2A))
                            SummaryRow("Total Payable","₹$finalAmt",       D11Yellow)
                            SummaryRow("Wallet Credit","₹$finalAmt",       D11Green)
                        }
                    }
                }
            }

            // Error
            if (errorMsg.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(Color(0xFF2A0A0A)), shape = RoundedCornerShape(8.dp)) {
                        Text("⚠️ $errorMsg", color = D11Red, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // Pay button
            item {
                Button(
                    onClick  = onPay,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors   = ButtonDefaults.buttonColors(if (finalAmt >= 10) D11Red else Color(0xFF333333)),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = finalAmt >= 10
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            if (finalAmt >= 10) "Pay ₹$finalAmt Securely" else "Select Amount to Continue",
                            color = if (finalAmt >= 10) Color.White else Color(0xFF888888),
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                        )
                    }
                }
            }

            // Manual fallback
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Having trouble? ", color = Color(0xFF888888), fontSize = 12.sp)
                    Text(
                        "Pay via QR code",
                        color      = D11Red,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.clickable { onManualQR() }
                    )
                }
            }

            // Trust badges
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrustBadge("🔒", "SSL Secure")
                    TrustBadge("⚡", "Instant Credit")
                    TrustBadge("🏦", "UPI / Cards")
                    TrustBadge("✅", "Verified")
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun AmountChip(
    amount:   Int,
    badge:    String?,
    cashback: String?,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale.value)
                .clickable {
                    scope.launch {
                        scale.animateTo(1.1f, tween(60))
                        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                    }
                    onClick()
                },
            colors = CardDefaults.cardColors(if (selected) Color(0xFF330000) else Color(0xFF1A1A1A)),
            shape  = RoundedCornerShape(10.dp),
            border = BorderStroke(if (selected) 1.5.dp else 0.5.dp, if (selected) D11Red else Color(0xFF333333))
        ) {
            Column(
                modifier            = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("₹$amount", color = if (selected) Color.White else Color(0xCCFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                if (cashback != null) {
                    Text(cashback, color = D11Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // Badge
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(D11Red)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(badge, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrustBadge(icon: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 16.sp)
        Text(label, color = Color(0xFF666666), fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────
// PAYMENT OPENED SCREEN
// ─────────────────────────────────────────────

@Composable
private fun PaymentOpenedScreen(
    amount:   Int,
    onVerify: () -> Unit,
    onManual: () -> Unit,
    onBack:   () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("💳", fontSize = 52.sp)
            Text("Payment Page Opened", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text("Complete your payment of ₹$amount in the browser. Once done, tap Verify below.", color = Color(0xFF888888), fontSize = 13.sp, textAlign = TextAlign.Center)

            Card(colors = CardDefaults.cardColors(Color(0xFF1A1A00)), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ℹ️ Steps:", color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("1. Complete payment in browser", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                    Text("2. Come back to this screen", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                    Text("3. Tap 'I've Paid - Verify' button", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                }
            }

            Button(onClick = onVerify, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(D11Green), shape = RoundedCornerShape(12.dp)) {
                Text("I've Paid — Verify Now", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().height(46.dp), border = BorderStroke(1.dp, Color(0xFF444444)), shape = RoundedCornerShape(12.dp)) {
                Text("Pay via QR instead", color = Color(0xFF888888), fontSize = 13.sp)
            }
            Text("← Go Back", color = D11Red, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() })
        }
    }
}

// ─────────────────────────────────────────────
// PROCESSING SCREEN
// ─────────────────────────────────────────────

@Composable
private fun PaymentProcessingScreen() {
    val inf   = rememberInfiniteTransition(label = "spin")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "angle")

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = D11Red, modifier = Modifier.size(52.dp), strokeWidth = 4.dp)
            Text("Creating Payment Order…", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Please wait", color = Color(0xFF888888), fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────
// VERIFYING SCREEN
// ─────────────────────────────────────────────

@Composable
private fun PaymentVerifyingScreen(pollCount: Int) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = D11Green, modifier = Modifier.size(52.dp), strokeWidth = 4.dp)
            Text("Verifying Payment…", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Attempt $pollCount / 3", color = Color(0xFF888888), fontSize = 12.sp)
        }
    }
}

// ─────────────────────────────────────────────
// SUCCESS SCREEN
// ─────────────────────────────────────────────

@Composable
private fun PaymentSuccessScreen(
    isManual: Boolean,
    amount:   Int,
    orderId:  String,
    utr:      String,
    onBack:   () -> Unit
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), Alignment.Center) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(90.dp).scale(scale.value).clip(CircleShape).background(
                    Brush.radialGradient(listOf(Color(0xFF004400), D11Green))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            }

            Text(
                if (isManual) "Request Submitted!" else "Payment Successful!",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (isManual) "Admin will verify within 30 minutes." else "₹$amount added to your wallet!",
                color = Color(0xFF888888), fontSize = 13.sp, textAlign = TextAlign.Center
            )

            Card(colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isManual) {
                        SummaryRow("Amount Credited", "₹$amount", D11Green)
                        SummaryRow("Order ID", orderId.takeLast(16), Color(0xFF888888))
                    } else {
                        SummaryRow("Amount", "₹$amount", D11Yellow)
                        SummaryRow("UTR Number", utr, Color(0xFF888888))
                        SummaryRow("Status", "Under Review", D11Yellow)
                    }
                }
            }

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(12.dp)) {
                Text("Back to Wallet", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────
// FAILED SCREEN
// ─────────────────────────────────────────────

@Composable
private fun PaymentFailedScreen(amount: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), Alignment.Center) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF2A0000)),
                contentAlignment = Alignment.Center
            ) { Text("✕", color = D11Red, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold) }
            Text("Payment Failed", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your payment of ₹$amount could not be processed.", color = Color(0xFF888888), fontSize = 13.sp, textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(12.dp)) {
                Text("Try Again", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(46.dp), border = BorderStroke(1.dp, Color(0xFF444444)), shape = RoundedCornerShape(12.dp)) {
                Text("Back to Wallet", color = Color(0xFF888888))
            }
        }
    }
}

// ─────────────────────────────────────────────
// PENDING SCREEN
// ─────────────────────────────────────────────

@Composable
private fun PaymentPendingScreen(orderId: String, onCheck: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), Alignment.Center) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("⏳", fontSize = 52.sp)
            Text("Payment Pending", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your payment is being processed. Check status after a few minutes.", color = Color(0xFF888888), fontSize = 13.sp, textAlign = TextAlign.Center)
            Card(colors = CardDefaults.cardColors(Color(0xFF1A1A00)), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Order ID: ${orderId.takeLast(16)}", color = D11Yellow, fontSize = 11.sp)
                }
            }
            Button(onClick = onCheck, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(D11Green), shape = RoundedCornerShape(12.dp)) {
                Text("Check Status Again", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(46.dp), border = BorderStroke(1.dp, Color(0xFF444444)), shape = RoundedCornerShape(12.dp)) {
                Text("Back to Wallet", color = Color(0xFF888888))
            }
        }
    }
}

// ─────────────────────────────────────────────
// MANUAL QR SCREEN (Fallback)
// ─────────────────────────────────────────────

@Composable
private fun ManualQRScreen(
    amount:       Int,
    utrNumber:    String,
    errorMsg:     String,
    onUtrChange:  (String) -> Unit,
    onSubmit:     () -> Unit,
    onBack:       () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF8B0000), Color(0xFFCC0000))))) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(17.dp))
                }
                Column {
                    Text("Pay via QR", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Manual payment mode", color = Color(0xAAFFFFFF), fontSize = 10.sp)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            item {
                Card(colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Scan & Pay ₹$amount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)

                        // QR Placeholder
                        Box(
                            modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(3.dp, D11Red, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("QR CODE", color = Color(0xFF111111), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(6.dp))
                                Text("dream11india@upi", color = D11Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("₹$amount", color = Color(0xFF111111), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // UPI copy row
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("UPI ID", color = Color(0xFF888888), fontSize = 10.sp)
                                Text("dream11india@upi", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(D11Red).clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("UPI", "dream11india@upi"))
                                }.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Copy", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value         = utrNumber,
                    onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) onUtrChange(it) },
                    placeholder   = { Text("e.g. 123456789012", color = Color(0xFF888888)) },
                    label         = { Text("UTR / Transaction ID", color = Color(0xFF888888)) },
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
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("⚠️ $errorMsg", color = D11Red, fontSize = 12.sp)
                }
            }

            item {
                Button(
                    onClick  = onSubmit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(if (utrNumber.length >= 6) D11Green else Color(0xFF333333)),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = utrNumber.length >= 6
                ) {
                    Text("Submit Payment Request", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}