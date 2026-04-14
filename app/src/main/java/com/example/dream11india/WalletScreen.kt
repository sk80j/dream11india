package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Transaction(
    val id: String = "",
    val type: String = "",
    val amount: Int = 0,
    val description: String = "",
    val timestamp: Long = 0L,
    val status: String = "completed"
)

data class WithdrawRequest(
    val id: String = "",
    val userId: String = "",
    val amount: Int = 0,
    val upiId: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
)

@Composable
fun WalletScreen(
    userData: UserData = UserData(),
    onBack: () -> Unit = {},
    onAddMoney: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("balance") }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var withdrawRequests by remember { mutableStateOf<List<WithdrawRequest>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            // Load transactions
            db.collection("transactions")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener { snap, _ ->
                    snap?.let {
                        transactions = it.documents.map { doc ->
                            Transaction(
                                id = doc.id,
                                type = doc.getString("type") ?: "",
                                amount = doc.getLong("amount")?.toInt() ?: 0,
                                description = doc.getString("description") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                status = doc.getString("status") ?: "completed"
                            )
                        }
                    }
                }
            // Load withdraw requests
            db.collection("withdrawRequests")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    snap?.let {
                        withdrawRequests = it.documents.map { doc ->
                            WithdrawRequest(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                amount = doc.getLong("amount")?.toInt() ?: 0,
                                upiId = doc.getString("upiId") ?: "",
                                status = doc.getString("status") ?: "pending",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        }
                    }
                }
        }
    }

    if (showWithdrawSheet) {
        WithdrawBottomSheet(
            balance = userData.balance,
            userId = uid,
            onDismiss = { showWithdrawSheet = false },
            onSuccess = { msg ->
                showWithdrawSheet = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            onError = { err ->
                scope.launch { snackbarHostState.showSnackbar(err) }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize()
            .background(Color(0xFF1A1A1A)).padding(padding)) {

            // TOP BAR
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(D11Red, D11DarkRed)))
                .statusBarsPadding()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("<", color = D11White, fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onBack() })
                            Image(painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = null, modifier = Modifier.size(28.dp))
                            Text("My Wallet", color = D11White, fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Balance cards
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Total balance
                        Card(modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Balance", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                                Text("Rs.${userData.balance}", color = D11White,
                                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        // Winnings
                        Card(modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Winnings", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                                Text("Rs.${userData.winnings}", color = D11Yellow,
                                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onAddMoney,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = D11White),
                            shape = RoundedCornerShape(10.dp)) {
                            Text("+ Add Cash", color = D11Red,
                                fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { showWithdrawSheet = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userData.balance >= 100)
                                    D11Green else Color(0xFF444444)),
                            shape = RoundedCornerShape(10.dp),
                            enabled = userData.balance >= 100
                        ) {
                            Text("Withdraw", color = D11White,
                                fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // TABS
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("balance" to "Transactions",
                    "withdraw" to "Withdrawals").forEach { (tab, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = tab }
                            .padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(label,
                            color = if (selectedTab == tab) D11Red else D11Gray,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold
                            else FontWeight.Normal)
                        if (selectedTab == tab) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Red))
                        }
                    }
                }
            }

            HorizontalDivider(color = D11Border)

            when (selectedTab) {
                "balance" -> TransactionsTab(transactions)
                "withdraw" -> WithdrawalsTab(withdrawRequests)
            }
        }
    }
}

@Composable
fun TransactionsTab(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No transactions yet", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Text("Add money to get started", color = D11Gray, fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transactions) { txn ->
                TransactionCard(txn)
            }
        }
    }
}

@Composable
fun TransactionCard(txn: Transaction) {
    val isCredit = txn.type == "credit"
    Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
        shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(if (isCredit) Color(0xFF1A3A1A) else Color(0xFF2A0000)),
                    contentAlignment = Alignment.Center) {
                    Text(if (isCredit) "+" else "-", color = if (isCredit) D11Green else D11Red,
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text(txn.description.ifEmpty { if (isCredit) "Money Added" else "Money Deducted" },
                        color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(java.text.SimpleDateFormat("dd MMM, hh:mm a",
                        java.util.Locale.getDefault()).format(java.util.Date(txn.timestamp)),
                        color = D11Gray, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (isCredit) "+" else "-"}Rs.${txn.amount}",
                    color = if (isCredit) D11Green else D11Red,
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(txn.status.uppercase(), color = D11Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun WithdrawalsTab(requests: List<WithdrawRequest>) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No withdrawal requests", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Text("Withdraw your winnings!", color = D11Gray, fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(requests) { req ->
                WithdrawRequestCard(req)
            }
        }
    }
}

@Composable
fun WithdrawRequestCard(req: WithdrawRequest) {
    Card(colors = CardDefaults.cardColors(
        containerColor = when(req.status) {
            "approved" -> Color(0xFF0A2A0A)
            "rejected" -> Color(0xFF2A0A0A)
            else -> D11CardBg
        }),
        shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Withdraw Request", color = D11White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Text("UPI: ${req.upiId}", color = D11Gray, fontSize = 12.sp)
                    Text(java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",
                        java.util.Locale.getDefault()).format(java.util.Date(req.createdAt)),
                        color = D11Gray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Rs.${req.amount}", color = D11Yellow,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(when(req.status) {
                            "approved" -> Color(0xFF004400)
                            "rejected" -> Color(0xFF440000)
                            else -> Color(0xFF444400)
                        }).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(req.status.uppercase(),
                            color = when(req.status) {
                                "approved" -> D11Green
                                "rejected" -> D11Red
                                else -> D11Yellow
                            }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WithdrawBottomSheet(
    balance: Int,
    userId: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    fun isValidUpi(upi: String): Boolean {
        return upi.contains("@") && upi.length > 4
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Color(0x88000000))
        .clickable { onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Handle bar
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(D11Gray).align(Alignment.CenterHorizontally))

                Text("Withdraw Money", color = D11White, fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold)

                // Balance info
                Row(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(D11LightGray)
                    .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Available Balance", color = D11Gray, fontSize = 13.sp)
                    Text("Rs.$balance", color = D11Green, fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold)
                }

                // Amount input
                Column {
                    Text("Enter Amount", color = D11Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.length <= 6) amount = it },
                        placeholder = { Text("Min Rs.100", color = D11Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = D11Red,
                            unfocusedBorderColor = D11Border,
                            focusedTextColor = D11White,
                            unfocusedTextColor = D11White,
                            cursorColor = D11Red,
                            focusedContainerColor = D11LightGray,
                            unfocusedContainerColor = D11LightGray),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        prefix = { Text("Rs. ", color = D11White, fontWeight = FontWeight.Bold) },
                        singleLine = true
                    )
                }

                // Quick amounts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("100","500","1000","2000").forEach { amt ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (amount == amt) D11Red else D11LightGray)
                            .clickable { amount = amt }
                            .padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Rs.$amt",
                                color = if (amount == amt) D11White else D11Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // UPI ID input
                Column {
                    Text("UPI ID", color = D11Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        placeholder = { Text("yourname@upi", color = D11Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = D11Red,
                            unfocusedBorderColor = D11Border,
                            focusedTextColor = D11White,
                            unfocusedTextColor = D11White,
                            cursorColor = D11Red,
                            focusedContainerColor = D11LightGray,
                            unfocusedContainerColor = D11LightGray),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Info box
                Box(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A00))
                    .padding(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Important:", color = D11Yellow, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                        Text("Minimum withdraw: Rs.100", color = D11Gray, fontSize = 12.sp)
                        Text("Processing time: 24-48 hours", color = D11Gray, fontSize = 12.sp)
                        Text("Payment via UPI only", color = D11Gray, fontSize = 12.sp)
                    }
                }

                // Submit button
                Button(
                    onClick = {
                        val amt = amount.toIntOrNull() ?: 0
                        when {
                            amt < 100 -> onError("Minimum withdraw amount is Rs.100!")
                            amt > balance -> onError("Insufficient balance!")
                            !isValidUpi(upiId) -> onError("Please enter valid UPI ID!")
                            else -> {
                                isLoading = true
                                scope.launch {
                                    try {
                                        db.collection("withdrawRequests").add(mapOf(
                                            "userId" to userId,
                                            "amount" to amt,
                                            "upiId" to upiId,
                                            "status" to "pending",
                                            "createdAt" to System.currentTimeMillis()
                                        )).await()
                                        isLoading = false
                                        onSuccess("Withdraw request submitted! Processing in 24-48 hours.")
                                    } catch (e: Exception) {
                                        isLoading = false
                                        onError("Failed: ${e.message}")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (amount.isNotEmpty() && upiId.isNotEmpty())
                            D11Red else Color(0xFF444444)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && amount.isNotEmpty() && upiId.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = D11White,
                            modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Submit Withdraw Request", color = D11White,
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
