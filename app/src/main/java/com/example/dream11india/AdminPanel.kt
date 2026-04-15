package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

data class PaymentRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val amount: Int = 0,
    val utrNumber: String = "",
    val status: String = "pending",
    val timestamp: Long = 0L
)

data class AdminStats(
    val totalUsers: Int = 0,
    val totalDeposit: Long = 0L,
    val totalWithdraw: Long = 0L,
    val totalWinnings: Long = 0L,
    val runningContests: Int = 0,
    val pendingPayments: Int = 0,
    val pendingKyc: Int = 0
)

@Composable
fun AdminPanelScreen(onBack: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf("dashboard") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        // TOP BAR
        Row(modifier = Modifier.fillMaxWidth().background(D11Red)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("<", color = D11White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(8.dp))
            Image(painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Admin Panel", color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }

        // TABS scroll
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)),
            contentPadding = PaddingValues(horizontal = 8.dp)) {
            val tabs = listOf(
                "dashboard" to "Dashboard",
                "payments" to "Payments",
                "withdraw" to "Withdraw",
                "kyc" to "KYC",
                "users" to "Users",
                "contests" to "Contests",
                "notif" to "Notify",
                "gst" to "GST",
                "logs" to "Logs",
                "stats" to "Stats"
            )
            items(tabs) { (tab, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedTab = tab }
                        .padding(horizontal = 10.dp, vertical = 10.dp)) {
                    Text(label,
                        color = if (selectedTab == tab) D11Red else D11Gray,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold
                        else FontWeight.Normal)
                    if (selectedTab == tab) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.width(35.dp).height(2.dp).background(D11Red))
                    }
                }
            }
        }

        HorizontalDivider(color = D11Border)

        when (selectedTab) {
            "dashboard" -> AdminDashboard()
            "payments" -> AdminPaymentPanel(onBack = {})
            "withdraw" -> AdminWithdrawTab()
            "kyc" -> AdminKYCTab()
            "users" -> AdminUsersTab()
            "contests" -> AdminContestsTab()
            "notif" -> AdminNotifyTab()
            "gst" -> AdminGSTTab()
            "logs" -> AdminLogsTab()
            "stats" -> AdminStatsTab()
        }
    }
}

// ===== DASHBOARD =====
@Composable
fun AdminDashboard() {
    val db = FirebaseFirestore.getInstance()
    var stats by remember { mutableStateOf(AdminStats()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Total users
        db.collection("users").addSnapshotListener { snap, _ ->
            val users = snap?.size() ?: 0
            // Total deposits
            db.collection("transactions")
                .whereEqualTo("type", "credit")
                .whereEqualTo("status", "completed")
                .get().addOnSuccessListener { txns ->
                    val totalDeposit = txns.documents.sumOf { it.getLong("amount") ?: 0L }
                    db.collection("transactions")
                        .whereEqualTo("type", "debit")
                        .whereEqualTo("description", "Withdrawal approved")
                        .get().addOnSuccessListener { wTxns ->
                            val totalWithdraw = wTxns.documents.sumOf { it.getLong("amount") ?: 0L }
                            db.collection("withdrawRequests")
                                .whereEqualTo("status", "pending")
                                .get().addOnSuccessListener { pSnap ->
                                    db.collection("kyc_requests")
                                        .whereEqualTo("status", "pending")
                                        .get().addOnSuccessListener { kSnap ->
                                            stats = AdminStats(
                                                totalUsers = users,
                                                totalDeposit = totalDeposit,
                                                totalWithdraw = totalWithdraw,
                                                pendingPayments = pSnap.size(),
                                                pendingKyc = kSnap.size()
                                            )
                                            isLoading = false
                                        }
                                }
                        }
                }
        }
    }

    LazyColumn(contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Live Dashboard", color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold)
        }

        // Stats grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashCard("Total Users", "${stats.totalUsers}", D11White, Modifier.weight(1f))
                    DashCard("Pending KYC", "${stats.pendingKyc}", D11Yellow, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashCard("Total Deposit", "Rs.${stats.totalDeposit}", D11Green, Modifier.weight(1f))
                    DashCard("Total Withdraw", "Rs.${stats.totalWithdraw}", D11Red, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashCard("Net Profit",
                        "Rs.${stats.totalDeposit - stats.totalWithdraw - stats.totalWinnings}",
                        D11Yellow, Modifier.weight(1f))
                    DashCard("Pending Pay", "${stats.pendingPayments}", D11Red, Modifier.weight(1f))
                }
            }
        }

        // Quick actions
        item {
            Text("Quick Actions", color = D11Gray, fontSize = 13.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Send Match Reminder" to { InAppNotificationManager.sendMatchReminder("IPL 2026", 10) },
                    "Send Contest Alert" to { InAppNotificationManager.sendContestFillingAlert("Mega Contest", 95) }
                ).forEach { (label, action) ->
                    Button(onClick = { action() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = D11LightGray),
                        shape = RoundedCornerShape(8.dp)) {
                        Text(label, color = D11White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = D11CardBg),
        shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = D11Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

// ===== PAYMENTS =====
@Composable
fun AdminPaymentPanel(onBack: () -> Unit = {}) {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<PaymentRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("payment_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { snap ->
                    requests = snap.documents.map { doc ->
                        PaymentRequest(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            userPhone = doc.getString("userPhone") ?: "",
                            amount = doc.getLong("amount")?.toInt() ?: 0,
                            utrNumber = doc.getString("utrNumber") ?: "",
                            status = doc.getString("status") ?: "pending",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }
                    isLoading = false
                }
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = D11Red)
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Total" to requests.size.toString() to D11White,
                    "Pending" to requests.count { it.status == "pending" }.toString() to D11Yellow,
                    "Approved" to requests.count { it.status == "approved" }.toString() to D11Green,
                    "Rejected" to requests.count { it.status == "rejected" }.toString() to D11Red
                ).forEach { (lv, color) ->
                    val (label, value) = lv
                    Card(modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = D11CardBg),
                        shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(value, color = color, fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text(label, color = D11Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        items(requests) { request ->
            PaymentRequestCard(
                request = request,
                onApprove = {
                    db.collection("payment_requests").document(request.id)
                        .update("status", "approved")
                    db.collection("users").document(request.userId)
                        .get().addOnSuccessListener { doc ->
                            val currentBalance = doc.getLong("balance")?.toInt() ?: 0
                            db.collection("users").document(request.userId)
                                .update("balance", currentBalance + request.amount)
                            db.collection("transactions").add(mapOf(
                                "userId" to request.userId,
                                "type" to "credit",
                                "amount" to request.amount,
                                "description" to "Deposit approved",
                                "status" to "completed",
                                "timestamp" to System.currentTimeMillis()
                            ))
                            logAdminAction("approve_payment",
                                "Approved Rs.${request.amount} for ${request.userId}")
                        }
                },
                onReject = {
                    db.collection("payment_requests").document(request.id)
                        .update("status", "rejected")
                    logAdminAction("reject_payment",
                        "Rejected payment for ${request.userId}")
                }
            )
        }
    }
}

@Composable
fun PaymentRequestCard(
    request: PaymentRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when(request.status) {
                "approved" -> Color(0xFF0A2A0A)
                "rejected" -> Color(0xFF2A0A0A)
                else -> D11CardBg
            }),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(D11LightGray),
                        contentAlignment = Alignment.Center) {
                        Text(request.userName.take(2).uppercase(), color = D11White,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(request.userName.ifEmpty { "User" }, color = D11White,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(request.userPhone, color = D11Gray, fontSize = 12.sp)
                    }
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(when(request.status) {
                        "approved" -> Color(0xFF004400)
                        "rejected" -> Color(0xFF440000)
                        else -> Color(0xFF444400)
                    }).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(request.status.uppercase(),
                        color = when(request.status) {
                            "approved" -> D11Green
                            "rejected" -> D11Red
                            else -> D11Yellow
                        }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Amount", color = D11Gray, fontSize = 11.sp)
                    Text("Rs.${request.amount}", color = D11Yellow, fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UTR", color = D11Gray, fontSize = 11.sp)
                    Text(request.utrNumber.ifEmpty { "N/A" }, color = D11White,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (request.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onReject, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF330000)),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Reject", color = D11Red, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onApprove, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004400)),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Approve", color = D11Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===== USERS =====
@Composable
fun AdminUsersTab() {
    val db = FirebaseFirestore.getInstance()
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showUserDetail by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("users").limit(100).addSnapshotListener { snap, _ ->
            snap?.let {
                users = it.documents.map { doc ->
                    val data = doc.data ?: mutableMapOf()
                    data.toMutableMap().apply { put("__id", doc.id) }
                }
            }
        }
    }

    if (showUserDetail && selectedUser != null) {
        AdminUserDetail(user = selectedUser!!, onBack = { showUserDetail = false })
        return
    }

    LazyColumn(contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or phone...", color = D11Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = D11Red, unfocusedBorderColor = D11Border,
                    focusedTextColor = D11White, unfocusedTextColor = D11White,
                    cursorColor = D11Red, focusedContainerColor = D11LightGray,
                    unfocusedContainerColor = D11LightGray),
                shape = RoundedCornerShape(10.dp), singleLine = true)
        }
        item {
            Text("Total Users: ${users.size}", color = D11Gray, fontSize = 13.sp)
        }
        val filtered = if (searchQuery.isEmpty()) users
        else users.filter {
            (it["name"] as? String)?.contains(searchQuery, true) == true ||
                    (it["phone"] as? String)?.contains(searchQuery) == true
        }
        items(filtered) { user ->
            val isBlocked = user["isBlocked"] as? Boolean ?: false
            Card(modifier = Modifier.fillMaxWidth().clickable {
                selectedUser = user; showUserDetail = true },
                colors = CardDefaults.cardColors(
                    containerColor = if (isBlocked) Color(0xFF2A0000) else D11CardBg),
                shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (isBlocked) Color(0xFF440000) else D11Red),
                            contentAlignment = Alignment.Center) {
                            Text((user["name"] as? String)?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                color = D11White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(user["name"] as? String ?: "User", color = D11White,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(user["phone"] as? String ?: "", color = D11Gray, fontSize = 12.sp)
                            Text("KYC: ${user["kycStatus"] as? String ?: "none"}",
                                color = when(user["kycStatus"] as? String) {
                                    "approved" -> D11Green
                                    "pending" -> D11Yellow
                                    else -> D11Gray
                                }, fontSize = 11.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rs.${user["balance"] ?: 0}", color = D11Green,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (isBlocked) Text("BLOCKED", color = D11Red,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(">", color = D11Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserDetail(user: Map<String, Any>, onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userId = user["__id"] as? String ?: ""
    var balanceInput by remember { mutableStateOf("") }
    var isBlocked by remember { mutableStateOf(user["isBlocked"] as? Boolean ?: false) }
    var snackMsg by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("<", color = D11White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBack() })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("User Detail", color = D11White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Name" to (user["name"] as? String ?: "-"),
                            "Phone" to (user["phone"] as? String ?: "-"),
                            "Balance" to "Rs.${user["balance"] ?: 0}",
                            "Winnings" to "Rs.${user["winnings"] ?: 0}",
                            "KYC Status" to (user["kycStatus"] as? String ?: "none"),
                            "PAN" to (user["panNumber"] as? String ?: "Not submitted"),
                            "UPI" to (user["upiId"] as? String ?: "Not added"),
                            "UID" to userId
                        ).forEach { (label, value) ->
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = D11Gray, fontSize = 13.sp)
                                Text(value, color = D11White, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = D11Border, thickness = 0.3.dp)
                        }
                    }
                }
            }
            // Balance edit
            item {
                Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Edit Balance", color = D11White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = balanceInput,
                            onValueChange = { balanceInput = it },
                            placeholder = { Text("Enter amount (+/-)", color = D11Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = D11Red, unfocusedBorderColor = D11Border,
                                focusedTextColor = D11White, unfocusedTextColor = D11White,
                                cursorColor = D11Red, focusedContainerColor = D11LightGray,
                                unfocusedContainerColor = D11LightGray),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val amt = balanceInput.toIntOrNull() ?: return@Button
                                db.runTransaction { tx ->
                                    val ref = db.collection("users").document(userId)
                                    val snap = tx.get(ref)
                                    val bal = snap.getLong("balance")?.toInt() ?: 0
                                    tx.update(ref, "balance", bal + amt)
                                }.addOnSuccessListener {
                                    db.collection("transactions").add(mapOf(
                                        "userId" to userId,
                                        "type" to if (amt > 0) "credit" else "debit",
                                        "amount" to kotlin.math.abs(amt),
                                        "description" to "Admin balance adjustment",
                                        "status" to "completed",
                                        "timestamp" to System.currentTimeMillis()
                                    ))
                                    logAdminAction("edit_balance",
                                        "Adjusted Rs.$amt for $userId")
                                    snackMsg = "Balance updated!"
                                    balanceInput = ""
                                }
                            }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                shape = RoundedCornerShape(8.dp)) {
                                Text("Add/Remove", color = D11White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            // Block/Unblock
            item {
                Button(onClick = {
                    val newBlock = !isBlocked
                    db.collection("users").document(userId)
                        .update("isBlocked", newBlock)
                        .addOnSuccessListener {
                            isBlocked = newBlock
                            logAdminAction(
                                if (newBlock) "block_user" else "unblock_user",
                                "$userId")
                            snackMsg = if (newBlock) "User blocked!" else "User unblocked!"
                        }
                }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) D11Green else Color(0xFF440000)),
                    shape = RoundedCornerShape(10.dp)) {
                    Text(if (isBlocked) "Unblock User" else "Block User",
                        color = D11White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===== CONTESTS =====
@Composable
fun AdminContestsTab() {
    val db = FirebaseFirestore.getInstance()
    var contests by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var entryFee by remember { mutableStateOf("") }
    var prizePool by remember { mutableStateOf("") }
    var totalSpots by remember { mutableStateOf("") }
    var fillPercent by remember { mutableStateOf("75") }
    var snackMsg by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    LaunchedEffect(Unit) {
        db.collection("contests").addSnapshotListener { snap, _ ->
            snap?.let {
                contests = it.documents.map { doc ->
                    val data = doc.data ?: mutableMapOf()
                    data.toMutableMap().apply { put("__id", doc.id) }
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding)) {
            item {
                Button(onClick = { showCreate = !showCreate },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                    shape = RoundedCornerShape(8.dp)) {
                    Text(if (showCreate) "Cancel" else "+ Create Contest",
                        color = D11White, fontWeight = FontWeight.Bold)
                }
            }
            if (showCreate) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                        shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Create New Contest", color = D11White,
                                fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            listOf(
                                Triple("Contest Name", name, { v: String -> name = v }),
                                Triple("Entry Fee (Rs)", entryFee, { v: String -> entryFee = v }),
                                Triple("Prize Pool", prizePool, { v: String -> prizePool = v }),
                                Triple("Total Spots", totalSpots, { v: String -> totalSpots = v }),
                                Triple("Fill % (0-100)", fillPercent, { v: String -> fillPercent = v })
                            ).forEach { (label, value, onChange) ->
                                OutlinedTextField(value = value,
                                    onValueChange = onChange,
                                    label = { Text(label, color = D11Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = D11Red, unfocusedBorderColor = D11Border,
                                        focusedTextColor = D11White, unfocusedTextColor = D11White,
                                        cursorColor = D11Red, focusedContainerColor = D11LightGray,
                                        unfocusedContainerColor = D11LightGray),
                                    shape = RoundedCornerShape(8.dp), singleLine = true)
                            }
                            Button(onClick = {
                                if (name.isEmpty()) { snackMsg = "Enter contest name!"; return@Button }
                                val fee = entryFee.toIntOrNull() ?: 0
                                val spots = totalSpots.toIntOrNull() ?: 1000
                                val fill = fillPercent.toIntOrNull() ?: 75
                                val joined = (spots * fill / 100)
                                db.collection("contests").add(mapOf(
                                    "name" to name,
                                    "entryFee" to fee,
                                    "prizePool" to prizePool,
                                    "totalSpots" to spots,
                                    "joinedCount" to joined,
                                    "fillPercent" to fill,
                                    "status" to "open",
                                    "isGuaranteed" to true,
                                    "isHot" to (fee > 0),
                                    "isFree" to (fee == 0),
                                    "firstPrize" to "Rs.${(prizePool.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) / 10}",
                                    "winners" to (spots / 10).toString(),
                                    "isMatchEnded" to false,
                                    "isDistributed" to false,
                                    "createdAt" to System.currentTimeMillis()
                                )).addOnSuccessListener {
                                    snackMsg = "Contest created!"
                                    name = ""; entryFee = ""; prizePool = ""
                                    totalSpots = ""; showCreate = false
                                    logAdminAction("create_contest", "Created: $name")
                                }
                            }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                shape = RoundedCornerShape(8.dp)) {
                                Text("Create", color = D11White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            items(contests) { contest ->
                val contestId = contest["__id"] as? String ?: ""
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(contest["name"] as? String ?: "Contest", color = D11White,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Entry: Rs.${contest["entryFee"] ?: 0} | " +
                                        "Prize: ${contest["prizePool"] ?: "N/A"}",
                                    color = D11Gray, fontSize = 12.sp)
                                Text("Spots: ${contest["joinedCount"] ?: 0}/${contest["totalSpots"] ?: 0}",
                                    color = D11Green, fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(when(contest["status"]) {
                                    "open" -> Color(0xFF004400)
                                    "closed" -> Color(0xFF440000)
                                    else -> Color(0xFF333333)
                                }).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("${contest["status"] ?: "open"}".uppercase(),
                                    color = when(contest["status"]) {
                                        "open" -> D11Green
                                        "closed" -> D11Red
                                        else -> D11Gray
                                    }, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                db.collection("contests").document(contestId)
                                    .update("status", "closed")
                                snackMsg = "Contest closed!"
                            }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF440000)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)) {
                                Text("Close", color = D11Red, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = {
                                PrizeDistributor.distributeContestPrizes(contestId)
                                snackMsg = "Prize distribution started!"
                                logAdminAction("distribute_prizes", "Contest: $contestId")
                            }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)) {
                                Text("Distribute", color = D11White, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = {
                                db.collection("contests").document(contestId).delete()
                                snackMsg = "Contest deleted!"
                            }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF330000)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)) {
                                Text("Delete", color = D11Red, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== GST TAB =====
@Composable
fun AdminGSTTab() {
    val db = FirebaseFirestore.getInstance()
    var gstPercent by remember { mutableStateOf("28") }
    var minWithdraw by remember { mutableStateOf("100") }
    var platformFee by remember { mutableStateOf("15") }
    var isLoading by remember { mutableStateOf(true) }
    var snackMsg by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    LaunchedEffect(Unit) {
        db.collection("app_config").document("gst")
            .get().addOnSuccessListener { doc ->
                gstPercent = doc.getLong("gstPercent")?.toString() ?: "28"
                minWithdraw = doc.getLong("minWithdraw")?.toString() ?: "100"
                platformFee = doc.getLong("platformFee")?.toString() ?: "15"
                isLoading = false
            }.addOnFailureListener { isLoading = false }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(padding)) {

            item {
                Text("GST & Fee Control", color = D11White, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold)
            }

            // GST Info
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A00)),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("GST Rules (India)", color = D11Yellow,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        listOf(
                            "28% GST on net winnings (winnings - entry fee)",
                            "TDS 30% on winnings above Rs.10,000",
                            "GST collected before withdrawal",
                            "Platform fee deducted from prize pool"
                        ).forEach { rule ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("*", color = D11Red, fontSize = 12.sp)
                                Text(rule, color = D11Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // GST Calculator
            item {
                Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("GST Configuration", color = D11White,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        listOf(
                            Triple("GST Percent (%)", gstPercent, { v: String -> gstPercent = v }),
                            Triple("Min Withdrawal (Rs)", minWithdraw, { v: String -> minWithdraw = v }),
                            Triple("Platform Fee (%)", platformFee, { v: String -> platformFee = v })
                        ).forEach { (label, value, onChange) ->
                            Column {
                                Text(label, color = D11Gray, fontSize = 12.sp)
                                OutlinedTextField(value = value, onValueChange = onChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = D11Red,
                                        unfocusedBorderColor = D11Border,
                                        focusedTextColor = D11White,
                                        unfocusedTextColor = D11White,
                                        cursorColor = D11Red,
                                        focusedContainerColor = D11LightGray,
                                        unfocusedContainerColor = D11LightGray),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp), singleLine = true)
                            }
                        }

                        // GST Preview
                        val gst = gstPercent.toIntOrNull() ?: 28
                        val minW = minWithdraw.toIntOrNull() ?: 100
                        val pFee = platformFee.toIntOrNull() ?: 15
                        val sampleWin = 1000
                        val gstAmt = (sampleWin * gst / 100)
                        val platformAmt = (sampleWin * pFee / 100)
                        val netPayout = sampleWin - gstAmt - platformAmt

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                            shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Example: Rs.1000 Winning", color = D11White,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("GST ($gst%):", color = D11Gray, fontSize = 12.sp)
                                    Text("- Rs.$gstAmt", color = D11Red, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Platform Fee ($pFee%):", color = D11Gray, fontSize = 12.sp)
                                    Text("- Rs.$platformAmt", color = D11Red, fontSize = 12.sp)
                                }
                                HorizontalDivider(color = D11Border)
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Payout:", color = D11White,
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Rs.$netPayout", color = D11Green,
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Min Withdraw: Rs.$minW", color = D11Gray, fontSize = 11.sp)
                            }
                        }

                        Button(onClick = {
                            db.collection("app_config").document("gst").set(mapOf(
                                "gstPercent" to (gstPercent.toLongOrNull() ?: 28L),
                                "minWithdraw" to (minWithdraw.toLongOrNull() ?: 100L),
                                "platformFee" to (platformFee.toLongOrNull() ?: 15L),
                                "updatedAt" to System.currentTimeMillis()
                            )).addOnSuccessListener {
                                snackMsg = "GST config saved!"
                                logAdminAction("update_gst",
                                    "GST: $gstPercent%, Platform: $platformFee%")
                            }
                        }, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                            shape = RoundedCornerShape(10.dp)) {
                            Text("Save GST Config", color = D11White,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Withdraw GST logic info
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2A0A)),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("How GST Works on Withdrawal", color = D11Green,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("1. User requests withdrawal\n" +
                                "2. System calculates GST on net winnings\n" +
                                "3. Admin sends notification: 'Pay Rs.X GST to proceed'\n" +
                                "4. User deposits GST amount\n" +
                                "5. Admin approves withdrawal after GST payment\n" +
                                "6. Net amount transferred to UPI",
                            color = D11Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ===== LOGS =====
@Composable
fun AdminLogsTab() {
    val db = FirebaseFirestore.getInstance()
    var logs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("admin_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    logs = it.documents.map { doc -> doc.data ?: emptyMap() }
                }
            }
    }

    LazyColumn(contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("Admin Action Logs (Last 50)", color = D11White,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        if (logs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No logs yet", color = D11Gray, fontSize = 14.sp)
                }
            }
        }
        items(logs) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log["action"] as? String ?: "", color = D11White,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(log["details"] as? String ?: "", color = D11Gray, fontSize = 11.sp)
                    }
                    Text(java.text.SimpleDateFormat("dd/MM HH:mm",
                        java.util.Locale.getDefault())
                        .format(java.util.Date(log["timestamp"] as? Long ?: 0L)),
                        color = D11Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

// ===== STATS =====
@Composable
fun AdminStatsTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("App Statistics", color = D11White, fontSize = 16.sp,
            fontWeight = FontWeight.Bold) }
        items(listOf(
            "Total Users" to "Loading...",
            "Total Matches" to "6",
            "Total Contests" to "5",
            "Total Revenue" to "Rs.0",
            "Pending Payments" to "0",
            "Active Users Today" to "0"
        )) { (label, value) ->
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = D11Gray, fontSize = 14.sp)
                    Text(value, color = D11Yellow, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===== MATCHES =====
@Composable
fun AdminMatchesTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Match Management", color = D11White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
        }
        items(listOf("RR vs MI - LIVE", "CSK vs RCB - Today 7:30 PM",
            "KKR vs DC - Tomorrow")) { match ->
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(match, color = D11White, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Edit", color = D11Yellow, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                        Text("Delete", color = D11Red, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===== WITHDRAW TAB =====
@Composable
fun AdminWithdrawTab() {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<WithdrawRequest>>(emptyList()) }
    var snackMsg by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    LaunchedEffect(Unit) {
        db.collection("withdrawRequests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    requests = it.documents.map { doc ->
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Total" to requests.size.toString() to D11White,
                        "Pending" to requests.count { it.status == "pending" }.toString() to D11Yellow,
                        "Approved" to requests.count { it.status == "approved" }.toString() to D11Green,
                        "Rejected" to requests.count { it.status == "rejected" }.toString() to D11Red
                    ).forEach { (lv, color) ->
                        val (label, value) = lv
                        Card(modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, color = color, fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(label, color = D11Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            items(requests) { req ->
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when(req.status) {
                            "approved" -> Color(0xFF0A2A0A)
                            "rejected" -> Color(0xFF2A0A0A)
                            else -> D11CardBg
                        }),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("User: ${req.userId.take(10)}...", color = D11White,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("UPI: ${req.upiId}", color = D11Gray, fontSize = 12.sp)
                            }
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
                        Spacer(modifier = Modifier.height(8.dp))

                        // GST calculation
                        val gstRate = 28
                        val gstAmt = req.amount * gstRate / 100
                        val netPayout = req.amount - gstAmt

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                            shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Requested:", color = D11Gray, fontSize = 12.sp)
                                    Text("Rs.${req.amount}", color = D11White,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("GST ($gstRate%):", color = D11Gray, fontSize = 12.sp)
                                    Text("- Rs.$gstAmt", color = D11Red, fontSize = 12.sp)
                                }
                                HorizontalDivider(color = D11Border, thickness = 0.5.dp)
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Payout:", color = D11White,
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Rs.$netPayout", color = D11Green,
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (req.status == "pending") {
                            Spacer(modifier = Modifier.height(10.dp))
                            // Send GST notification
                            Button(onClick = {
                                InAppNotificationManager.sendNotification(
                                    target = req.userId,
                                    title = "GST Payment Required",
                                    body = "To withdraw Rs.${req.amount}, please deposit Rs.$gstAmt as GST (28%). After payment, your Rs.$netPayout will be transferred.",
                                    type = "wallet"
                                )
                                snackMsg = "GST notification sent to user!"
                            }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A00)),
                                shape = RoundedCornerShape(8.dp)) {
                                Text("Send GST Notification (Rs.$gstAmt)",
                                    color = D11Yellow, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    db.collection("withdrawRequests").document(req.id)
                                        .update("status", "rejected")
                                        .addOnSuccessListener {
                                            logAdminAction("reject_withdraw",
                                                "Rejected Rs.${req.amount} for ${req.userId}")
                                            snackMsg = "Rejected!"
                                        }
                                }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF330000)),
                                    shape = RoundedCornerShape(8.dp)) {
                                    Text("Reject", color = D11Red, fontWeight = FontWeight.Bold)
                                }
                                Button(onClick = {
                                    db.collection("users").document(req.userId)
                                        .get().addOnSuccessListener { userSnap ->
                                            val bal = userSnap.getLong("balance")?.toInt() ?: 0
                                            if (bal >= req.amount) {
                                                db.runTransaction { tx ->
                                                    val ref = db.collection("users")
                                                        .document(req.userId)
                                                    val snap = tx.get(ref)
                                                    val b = snap.getLong("balance")?.toInt() ?: 0
                                                    tx.update(ref, "balance", b - req.amount)
                                                }.addOnSuccessListener {
                                                    db.collection("transactions").add(mapOf(
                                                        "userId" to req.userId,
                                                        "type" to "debit",
                                                        "amount" to req.amount,
                                                        "gstDeducted" to gstAmt,
                                                        "netPayout" to netPayout,
                                                        "description" to "Withdrawal approved (GST: Rs.$gstAmt)",
                                                        "timestamp" to System.currentTimeMillis()
                                                    ))
                                                    db.collection("withdrawRequests").document(req.id)
                                                        .update("status", "approved")
                                                    InAppNotificationManager.sendWithdrawApproved(
                                                        req.userId, netPayout)
                                                    logAdminAction("approve_withdraw",
                                                        "Approved Rs.$netPayout (GST: Rs.$gstAmt) for ${req.userId}")
                                                    snackMsg = "Approved! Net: Rs.$netPayout"
                                                }
                                            } else {
                                                snackMsg = "Insufficient balance!"
                                            }
                                        }
                                }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF004400)),
                                    shape = RoundedCornerShape(8.dp)) {
                                    Text("Approve", color = D11Green,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== KYC TAB =====
@Composable
fun AdminKYCTab() {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<KycData>>(emptyList()) }
    var snackMsg by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    LaunchedEffect(Unit) {
        db.collection("kyc_requests")
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    requests = it.documents.map { doc ->
                        KycData(
                            userId = doc.getString("userId") ?: "",
                            name = doc.getString("name") ?: "",
                            panNumber = doc.getString("panNumber") ?: "",
                            upiId = doc.getString("upiId") ?: "",
                            status = doc.getString("status") ?: "pending",
                            submittedAt = doc.getLong("submittedAt") ?: 0L
                        )
                    }
                }
            }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Total" to requests.size.toString() to D11White,
                        "Pending" to requests.count { it.status == "pending" }.toString() to D11Yellow,
                        "Approved" to requests.count { it.status == "approved" }.toString() to D11Green,
                        "Rejected" to requests.count { it.status == "rejected" }.toString() to D11Red
                    ).forEach { (lv, color) ->
                        val (label, value) = lv
                        Card(modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, color = color, fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(label, color = D11Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            items(requests) { kyc ->
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when(kyc.status) {
                            "approved" -> Color(0xFF0A2A0A)
                            "rejected" -> Color(0xFF2A0A0A)
                            else -> D11CardBg
                        }),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(kyc.name, color = D11White, fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold)
                                Text("PAN: ${kyc.panNumber}", color = D11Gray, fontSize = 12.sp)
                                Text("UPI: ${kyc.upiId}", color = D11Gray, fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(when(kyc.status) {
                                    "approved" -> Color(0xFF004400)
                                    "rejected" -> Color(0xFF440000)
                                    else -> Color(0xFF444400)
                                }).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(kyc.status.uppercase(),
                                    color = when(kyc.status) {
                                        "approved" -> D11Green
                                        "rejected" -> D11Red
                                        else -> D11Yellow
                                    }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (kyc.status == "pending") {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    db.collection("kyc_requests").document(kyc.userId)
                                        .update("status", "rejected")
                                    db.collection("users").document(kyc.userId)
                                        .update(mapOf(
                                            "kycStatus" to "rejected",
                                            "kycRejectionReason" to "Invalid documents"
                                        ))
                                    InAppNotificationManager.sendNotification(
                                        kyc.userId, "KYC Rejected",
                                        "Please resubmit with valid documents.", "wallet")
                                    logAdminAction("reject_kyc", kyc.userId)
                                    snackMsg = "KYC Rejected!"
                                }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF330000)),
                                    shape = RoundedCornerShape(8.dp)) {
                                    Text("Reject", color = D11Red, fontWeight = FontWeight.Bold)
                                }
                                Button(onClick = {
                                    db.collection("kyc_requests").document(kyc.userId)
                                        .update("status", "approved")
                                    db.collection("users").document(kyc.userId)
                                        .update(mapOf(
                                            "kycStatus" to "approved",
                                            "kycVerifiedAt" to System.currentTimeMillis()
                                        ))
                                    InAppNotificationManager.sendNotification(
                                        kyc.userId, "KYC Approved!",
                                        "You can now withdraw your winnings!", "wallet")
                                    logAdminAction("approve_kyc", kyc.userId)
                                    snackMsg = "KYC Approved!"
                                }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF004400)),
                                    shape = RoundedCornerShape(8.dp)) {
                                    Text("Approve", color = D11Green, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== NOTIFY TAB =====
@Composable
fun AdminNotifyTab() {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var notifType by remember { mutableStateOf("general") }
    var isSending by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf("") }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)) {
            item { Text("Send Notification", color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold) }
            item {
                Text("Quick Send", color = D11Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    Triple("Match Reminder", "Match starting in 10 minutes!", "match"),
                    Triple("Contest Alert", "Mega Contest 95% full - Join now!", "contest"),
                    Triple("Result Out", "Match results are out! Check leaderboard", "general"),
                    Triple("Free Contest", "Free contest available! Join now!", "contest")
                ).forEach { (t, b, tp) ->
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        InAppNotificationManager.sendNotification("all", t, b, tp)
                        snackMsg = "Notification sent!"
                        logAdminAction("send_notification", "Quick: $t")
                    }, colors = CardDefaults.cardColors(containerColor = D11LightGray),
                        shape = RoundedCornerShape(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t, color = D11White, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold)
                                Text(b, color = D11Gray, fontSize = 11.sp, maxLines = 1)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(D11Red)
                                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("Send", color = D11White, fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            item { HorizontalDivider(color = D11Border) }
            item {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title", color = D11Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = D11Red, unfocusedBorderColor = D11Border,
                        focusedTextColor = D11White, unfocusedTextColor = D11White,
                        cursorColor = D11Red, focusedContainerColor = D11LightGray,
                        unfocusedContainerColor = D11LightGray),
                    shape = RoundedCornerShape(10.dp), singleLine = true)
            }
            item {
                OutlinedTextField(value = body, onValueChange = { body = it },
                    label = { Text("Message", color = D11Gray) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = D11Red, unfocusedBorderColor = D11Border,
                        focusedTextColor = D11White, unfocusedTextColor = D11White,
                        cursorColor = D11Red, focusedContainerColor = D11LightGray,
                        unfocusedContainerColor = D11LightGray),
                    shape = RoundedCornerShape(10.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("general","match","contest","wallet").forEach { type ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (notifType == type) D11Red else D11LightGray)
                            .clickable { notifType = type }
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(type.replaceFirstChar { it.uppercase() },
                                color = D11White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                LoadingButton(text = "Send Notification", isLoading = isSending,
                    onClick = {
                        if (title.isEmpty() || body.isEmpty()) {
                            snackMsg = "Enter title and message!"
                            return@LoadingButton
                        }
                        isSending = true
                        InAppNotificationManager.sendNotification("all", title, body, notifType)
                        logAdminAction("send_notification", "Custom: $title")
                        title = ""; body = ""
                        isSending = false
                        snackMsg = "Notification sent!"
                    }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ===== HELPER =====
fun logAdminAction(action: String, details: String) {
    val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    FirebaseFirestore.getInstance().collection("admin_logs").add(mapOf(
        "action" to action,
        "details" to details,
        "adminId" to adminId,
        "timestamp" to System.currentTimeMillis()
    ))
}

