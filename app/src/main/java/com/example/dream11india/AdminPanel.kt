package com.example.dream11india

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────

data class PaymentRequest(
    val id: String = "", val userId: String = "", val userName: String = "",
    val userPhone: String = "", val amount: Int = 0, val utrNumber: String = "",
    val status: String = "pending", val timestamp: Long = 0L
)

data class AdminStats(
    val totalUsers: Int = 0, val totalDeposit: Long = 0L,
    val totalWithdraw: Long = 0L, val totalWinnings: Long = 0L,
    val runningContests: Int = 0, val pendingPayments: Int = 0,
    val pendingKyc: Int = 0, val pendingWithdraws: Int = 0
)

data class PromoCode(
    val id: String = "", val code: String = "", val type: String = "flat",
    val value: Int = 0, val maxUses: Int = 100, val usedCount: Int = 0,
    val expiryDate: Long = 0L, val isActive: Boolean = true
)

data class SupportTicket(
    val id: String = "", val userId: String = "", val userName: String = "",
    val subject: String = "", val message: String = "",
    val status: String = "open", val createdAt: Long = 0L,
    val reply: String = ""
)

// ─────────────────────────────────────────────
// ADMIN TABS ENUM
// ─────────────────────────────────────────────

enum class AdminTab(val label: String) {
    DASHBOARD("Dashboard"),
    USERS("Users"),
    PAYMENTS("Payments"),
    WITHDRAW("Withdraw"),
    KYC("KYC"),
    CONTESTS("Contests"),
    MATCHES("Matches"),
    NOTIFY("Notify"),
    PROMO("Promo"),
    WALLET("Wallet"),
    RISK("Risk"),
    CONFIG("Config"),
    SUPPORT("Support"),
    LOGS("Logs")
}

// ─────────────────────────────────────────────
// MAIN ADMIN PANEL
// ─────────────────────────────────────────────

@Composable
fun AdminPanelScreen(onBack: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(AdminTab.DASHBOARD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF8B0000), D11Red))
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text("Admin Panel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Dream11 India", color = Color(0xCCFFFFFF), fontSize = 10.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("OWNER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // ── Tab Row ──
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111)),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(AdminTab.values()) { tab ->
                val active = selectedTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text(
                        tab.label,
                        color = if (active) D11Red else Color(0xFF888888),
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal
                    )
                    if (active) {
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.width(30.dp).height(2.dp).background(D11Red))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF222222))

        // ── Content ──
        when (selectedTab) {
            AdminTab.DASHBOARD -> AdminDashboard()
            AdminTab.USERS     -> AdminUsersTab()
            AdminTab.PAYMENTS  -> AdminPaymentPanel()
            AdminTab.WITHDRAW  -> AdminWithdrawTab()
            AdminTab.KYC       -> AdminKYCTab()
            AdminTab.CONTESTS  -> AdminContestsTab()
            AdminTab.MATCHES   -> AdminMatchesTab()
            AdminTab.NOTIFY    -> AdminNotifyTab()
            AdminTab.PROMO     -> AdminPromoTab()
            AdminTab.WALLET    -> AdminWalletControlTab()
            AdminTab.RISK      -> AdminRiskTab()
            AdminTab.CONFIG    -> AdminConfigTab()
            AdminTab.SUPPORT   -> AdminSupportTab()
            AdminTab.LOGS      -> AdminLogsTab()
        }
    }
}

// ─────────────────────────────────────────────
// DASHBOARD
// ─────────────────────────────────────────────

@Composable
fun AdminDashboard() {
    val db = FirebaseFirestore.getInstance()
    var stats by remember { mutableStateOf(AdminStats()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snap, _ ->
            val users = snap?.size() ?: 0
            db.collection("transactions").whereEqualTo("type", "credit")
                .whereEqualTo("status", "completed").get().addOnSuccessListener { t ->
                    val dep = t.documents.sumOf { it.getLong("amount") ?: 0L }
                    db.collection("transactions").whereEqualTo("type", "debit")
                        .get().addOnSuccessListener { w ->
                            val with = w.documents.sumOf { it.getLong("amount") ?: 0L }
                            db.collection("withdrawRequests").whereEqualTo("status", "pending")
                                .get().addOnSuccessListener { pw ->
                                    db.collection("kyc_requests").whereEqualTo("status", "pending")
                                        .get().addOnSuccessListener { pk ->
                                            db.collection("payment_requests").whereEqualTo("status", "pending")
                                                .get().addOnSuccessListener { pp ->
                                                    stats = AdminStats(
                                                        totalUsers = users,
                                                        totalDeposit = dep,
                                                        totalWithdraw = with,
                                                        pendingPayments = pp.size(),
                                                        pendingKyc = pk.size(),
                                                        pendingWithdraws = pw.size()
                                                    )
                                                    isLoading = false
                                                }
                                        }
                                }
                        }
                }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Live Dashboard", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }

        // Stats grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashCard("Total Users",       "${stats.totalUsers}",         Color(0xFF82B1FF), Modifier.weight(1f))
                    DashCard("Pending KYC",       "${stats.pendingKyc}",         D11Yellow,         Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashCard("Total Deposit",     "₹${stats.totalDeposit}",      D11Green,          Modifier.weight(1f))
                    DashCard("Total Withdraw",    "₹${stats.totalWithdraw}",     D11Red,            Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashCard("Net Profit",
                        "₹${stats.totalDeposit - stats.totalWithdraw}",
                        D11Yellow, Modifier.weight(1f))
                    DashCard("Pending Pays",      "${stats.pendingPayments}",    D11Red,            Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashCard("Pending Withdraws", "${stats.pendingWithdraws}",   Color(0xFFFF8A80), Modifier.weight(1f))
                    DashCard("Winnings Paid",     "₹${stats.totalWinnings}",     Color(0xFF69F0AE), Modifier.weight(1f))
                }
            }
        }

        // Quick actions
        item {
            Text("Quick Actions", color = Color(0xFF888888), fontSize = 13.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Match Reminder" to { InAppNotificationManager.sendMatchReminder("IPL 2026", 10) },
                    "Contest Alert"  to { InAppNotificationManager.sendContestFillingAlert("Mega Contest", 95) }
                ).forEach { (label, action) ->
                    Button(
                        onClick  = { action() },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape    = RoundedCornerShape(8.dp)
                    ) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier             = Modifier.padding(12.dp),
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color(0xFF888888), fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────
// PAYMENTS
// ─────────────────────────────────────────────

@Composable
fun AdminPaymentPanel() {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<PaymentRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("payment_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    requests = it.documents.map { doc ->
                        PaymentRequest(
                            id = doc.id,
                            userId    = doc.getString("userId") ?: "",
                            userName  = doc.getString("userName") ?: "",
                            userPhone = doc.getString("userPhone") ?: "",
                            amount    = doc.getLong("amount")?.toInt() ?: 0,
                            utrNumber = doc.getString("utrNumber") ?: "",
                            status    = doc.getString("status") ?: "pending",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }
                    isLoading = false
                }
            }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { pad ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = D11Red)
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad)
        ) {
            // Summary row
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple("Total",    requests.size,                                       Color.White),
                        Triple("Pending",  requests.count { it.status == "pending" },           D11Yellow),
                        Triple("Approved", requests.count { it.status == "approved" },          D11Green),
                        Triple("Rejected", requests.count { it.status == "rejected" },          D11Red)
                    ).forEach { (label, count, color) ->
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$count", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(label, color = Color(0xFF888888), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            items(requests, key = { it.id }) { req ->
                PaymentRequestCard(
                    request   = req,
                    onApprove = {
                        db.collection("payment_requests").document(req.id).update("status", "approved")
                        db.collection("users").document(req.userId).get()
                            .addOnSuccessListener { doc ->
                                val bal = doc.getLong("balance")?.toInt() ?: 0
                                db.collection("users").document(req.userId).update("balance", bal + req.amount)
                                db.collection("transactions").add(mapOf(
                                    "userId" to req.userId, "type" to "credit",
                                    "amount" to req.amount, "description" to "Deposit approved",
                                    "status" to "completed", "timestamp" to System.currentTimeMillis()
                                ))
                                logAdminAction("approve_payment", "₹${req.amount} for ${req.userId}")
                                scope.launch { snackbarHostState.showSnackbar("Approved ₹${req.amount}!") }
                            }
                    },
                    onReject  = {
                        db.collection("payment_requests").document(req.id).update("status", "rejected")
                        logAdminAction("reject_payment", "Rejected ₹${req.amount} for ${req.userId}")
                        scope.launch { snackbarHostState.showSnackbar("Rejected!") }
                    }
                )
            }
        }
    }
}

@Composable
fun PaymentRequestCard(request: PaymentRequest, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = when (request.status) {
                "approved" -> Color(0xFF0A2A0A); "rejected" -> Color(0xFF2A0A0A); else -> Color(0xFF1A1A1A)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2A2A2A)), Alignment.Center) {
                        Text(request.userName.take(2).uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(request.userName.ifEmpty { "User" }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(request.userPhone, color = Color(0xFF888888), fontSize = 11.sp)
                    }
                }
                StatusBadge(request.status)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Amount", color = Color(0xFF888888), fontSize = 10.sp)
                    Text("₹${request.amount}", color = D11Yellow, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UTR", color = Color(0xFF888888), fontSize = 10.sp)
                    Text(request.utrNumber.ifEmpty { "N/A" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (request.status == "pending") {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onReject, Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(Color(0xFF330000)), shape = RoundedCornerShape(8.dp)) {
                        Text("Reject", color = D11Red, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onApprove, Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(Color(0xFF004400)), shape = RoundedCornerShape(8.dp)) {
                        Text("Approve", color = D11Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// WITHDRAW TAB
// ─────────────────────────────────────────────

@Composable
fun AdminWithdrawTab() {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<WithdrawRequest>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("withdrawRequests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    requests = it.documents.map { doc ->
                        WithdrawRequest(
                            id        = doc.id,
                            userId    = doc.getString("userId") ?: "",
                            amount    = doc.getLong("amount")?.toInt() ?: 0,
                            upiId     = doc.getString("upiId") ?: "",
                            status    = doc.getString("status") ?: "pending",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }
            }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple("Total",    requests.size,                                Color.White),
                        Triple("Pending",  requests.count { it.status == "pending" },    D11Yellow),
                        Triple("Approved", requests.count { it.status == "approved" },   D11Green),
                        Triple("Rejected", requests.count { it.status == "rejected" },   D11Red)
                    ).forEach { (label, count, color) ->
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$count", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(label, color = Color(0xFF888888), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            items(requests, key = { it.id }) { req ->
                val gstAmt  = req.amount * 28 / 100
                val netPay  = req.amount - gstAmt
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (req.status) {
                            "approved" -> Color(0xFF0A2A0A); "rejected" -> Color(0xFF2A0A0A); else -> Color(0xFF1A1A1A)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column {
                                Text("UID: ${req.userId.take(12)}…", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("UPI: ${req.upiId}", color = Color(0xFF888888), fontSize = 11.sp)
                            }
                            StatusBadge(req.status)
                        }
                        Spacer(Modifier.height(8.dp))
                        // GST breakdown
                        Card(colors = CardDefaults.cardColors(Color(0xFF111111)), shape = RoundedCornerShape(8.dp)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                GstRow("Requested", "₹${req.amount}", Color.White)
                                GstRow("GST (28%)", "- ₹$gstAmt", D11Red)
                                HorizontalDivider(color = Color(0xFF333333))
                                GstRow("Net Payout", "₹$netPay", D11Green)
                            }
                        }
                        if (req.status == "pending") {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    InAppNotificationManager.sendNotification(
                                        req.userId, "GST Payment Required",
                                        "Pay ₹$gstAmt GST to process your ₹${req.amount} withdrawal. Net payout: ₹$netPay", "wallet"
                                    )
                                    scope.launch { snackbarHostState.showSnackbar("GST notification sent!") }
                                },
                                Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(Color(0xFF1A1A00)),
                                shape  = RoundedCornerShape(8.dp)
                            ) {
                                Text("Send GST Notice (₹$gstAmt)", color = D11Yellow, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        db.collection("withdrawRequests").document(req.id).update("status", "rejected")
                                        logAdminAction("reject_withdraw", "₹${req.amount} for ${req.userId}")
                                        scope.launch { snackbarHostState.showSnackbar("Rejected!") }
                                    },
                                    Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(Color(0xFF330000)),
                                    shape  = RoundedCornerShape(8.dp)
                                ) { Text("Reject", color = D11Red, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = {
                                        db.runTransaction { tx ->
                                            val ref  = db.collection("users").document(req.userId)
                                            val snap = tx.get(ref)
                                            val bal  = snap.getLong("balance")?.toInt() ?: 0
                                            if (bal >= req.amount) tx.update(ref, "balance", bal - req.amount)
                                        }.addOnSuccessListener {
                                            db.collection("transactions").add(mapOf(
                                                "userId" to req.userId, "type" to "debit",
                                                "amount" to req.amount, "gstDeducted" to gstAmt,
                                                "netPayout" to netPay,
                                                "description" to "Withdrawal approved (GST: ₹$gstAmt)",
                                                "timestamp" to System.currentTimeMillis()
                                            ))
                                            db.collection("withdrawRequests").document(req.id).update("status", "approved")
                                            InAppNotificationManager.sendWithdrawApproved(req.userId, netPay)
                                            logAdminAction("approve_withdraw", "₹$netPay (GST ₹$gstAmt) for ${req.userId}")
                                            scope.launch { snackbarHostState.showSnackbar("Approved! Net: ₹$netPay") }
                                        }
                                    },
                                    Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(Color(0xFF004400)),
                                    shape  = RoundedCornerShape(8.dp)
                                ) { Text("Approve", color = D11Green, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GstRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────
// USERS TAB
// ─────────────────────────────────────────────

@Composable
fun AdminUsersTab() {
    val db = FirebaseFirestore.getInstance()
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        db.collection("users").limit(100).addSnapshotListener { snap, _ ->
            snap?.let {
                users = it.documents.map { doc ->
                    (doc.data ?: mutableMapOf()).toMutableMap().apply { put("__id", doc.id) }
                }
            }
        }
    }

    if (selectedUser != null) {
        AdminUserDetail(user = selectedUser!!, onBack = { selectedUser = null })
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) Text("Search name or phone...", color = Color(0xFF888888), fontSize = 13.sp)
                    inner()
                }
            )
        }

        val filtered = if (searchQuery.isEmpty()) users else users.filter {
            (it["name"] as? String)?.contains(searchQuery, true) == true ||
                    (it["phone"] as? String)?.contains(searchQuery) == true
        }

        Text(
            "Users: ${filtered.size}",
            color = Color(0xFF888888), fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it["__id"] as? String ?: "" }) { user ->
                val isBlocked = user["isBlocked"] as? Boolean ?: false
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedUser = user },
                    colors   = CardDefaults.cardColors(if (isBlocked) Color(0xFF2A0000) else Color(0xFF1A1A1A)),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (isBlocked) Color(0xFF440000) else D11Red), Alignment.Center) {
                                Text((user["name"] as? String)?.firstOrNull()?.uppercase() ?: "U", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(user["name"] as? String ?: "User", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(user["phone"] as? String ?: "", color = Color(0xFF888888), fontSize = 11.sp)
                                Text("KYC: ${user["kycStatus"] as? String ?: "none"}",
                                    color = when (user["kycStatus"] as? String) {
                                        "approved" -> D11Green; "pending" -> D11Yellow; else -> Color(0xFF888888)
                                    }, fontSize = 10.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${user["balance"] ?: 0}", color = D11Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (isBlocked) Text("BLOCKED", color = D11Red, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
                        }
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
    var balInput   by remember { mutableStateOf("") }
    var bonusInput by remember { mutableStateOf("") }
    var isBlocked  by remember { mutableStateOf(user["isBlocked"] as? Boolean ?: false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding      = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(pad)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack, Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF1E1E1E))) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("User Detail", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            item {
                AdminCard {
                    listOf(
                        "Name"    to (user["name"] as? String ?: "—"),
                        "Phone"   to (user["phone"] as? String ?: "—"),
                        "Balance" to "₹${user["balance"] ?: 0}",
                        "Winning" to "₹${user["winnings"] ?: 0}",
                        "KYC"     to (user["kycStatus"] as? String ?: "none"),
                        "PAN"     to (user["panNumber"] as? String ?: "Not submitted"),
                        "UPI"     to (user["upiId"] as? String ?: "Not added"),
                        "UID"     to userId
                    ).forEach { (l, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), Arrangement.SpaceBetween) {
                            Text(l, color = Color(0xFF888888), fontSize = 12.sp)
                            Text(v, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
                    }
                }
            }

            // Balance edit
            item {
                AdminCard {
                    Text("Edit Balance", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AdminTextField(value = balInput, onValueChange = { balInput = it }, placeholder = "Enter amount (+/-)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val amt = balInput.toIntOrNull() ?: return@Button
                            db.runTransaction { tx ->
                                val ref  = db.collection("users").document(userId)
                                val snap = tx.get(ref)
                                val bal  = snap.getLong("balance")?.toInt() ?: 0
                                tx.update(ref, "balance", bal + amt)
                            }.addOnSuccessListener {
                                db.collection("transactions").add(mapOf(
                                    "userId" to userId, "type" to if (amt > 0) "credit" else "debit",
                                    "amount" to kotlin.math.abs(amt),
                                    "description" to "Admin balance adjustment",
                                    "status" to "completed", "timestamp" to System.currentTimeMillis()
                                ))
                                logAdminAction("edit_balance", "₹$amt for $userId")
                                scope.launch { snackbarHostState.showSnackbar("Balance updated!") }
                                balInput = ""
                            }
                        },
                        Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(D11Green),
                        shape  = RoundedCornerShape(8.dp)
                    ) { Text("Apply Balance Change", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            // Bonus wallet
            item {
                AdminCard {
                    Text("Add Bonus", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AdminTextField(value = bonusInput, onValueChange = { bonusInput = it }, placeholder = "Bonus amount", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val amt = bonusInput.toIntOrNull() ?: return@Button
                            db.collection("users").document(userId).get()
                                .addOnSuccessListener { doc ->
                                    val curBonus = doc.getLong("bonusBalance")?.toInt() ?: 0
                                    db.collection("users").document(userId).update("bonusBalance", curBonus + amt)
                                    db.collection("transactions").add(mapOf(
                                        "userId" to userId, "type" to "bonus",
                                        "amount" to amt, "description" to "Admin bonus credit",
                                        "timestamp" to System.currentTimeMillis()
                                    ))
                                    logAdminAction("add_bonus", "₹$amt bonus for $userId")
                                    scope.launch { snackbarHostState.showSnackbar("Bonus ₹$amt added!") }
                                    bonusInput = ""
                                }
                        },
                        Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(Color(0xFF1A4A00)),
                        shape  = RoundedCornerShape(8.dp)
                    ) { Text("Add Bonus", color = D11Green, fontWeight = FontWeight.Bold) }
                }
            }

            // Block/Unblock
            item {
                Button(
                    onClick = {
                        val newBlock = !isBlocked
                        db.collection("users").document(userId).update("isBlocked", newBlock)
                            .addOnSuccessListener {
                                isBlocked = newBlock
                                logAdminAction(if (newBlock) "block_user" else "unblock_user", userId)
                                scope.launch { snackbarHostState.showSnackbar(if (newBlock) "User blocked!" else "User unblocked!") }
                            }
                    },
                    Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(if (isBlocked) Color(0xFF004400) else Color(0xFF440000)),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isBlocked) "Unblock User" else "Block User", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// KYC TAB
// ─────────────────────────────────────────────

@Composable
fun AdminKYCTab() {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<KycData>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("kyc_requests").addSnapshotListener { snap, _ ->
            snap?.let {
                requests = it.documents.map { doc ->
                    KycData(
                        userId      = doc.getString("userId") ?: "",
                        name        = doc.getString("name") ?: "",
                        panNumber   = doc.getString("panNumber") ?: "",
                        upiId       = doc.getString("upiId") ?: "",
                        status      = doc.getString("status") ?: "pending",
                        submittedAt = doc.getLong("submittedAt") ?: 0L
                    )
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Total" to requests.size to Color.White,
                        "Pending" to requests.count{it.status=="pending"} to D11Yellow,
                        "Approved" to requests.count{it.status=="approved"} to D11Green,
                        "Rejected" to requests.count{it.status=="rejected"} to D11Red
                    ).forEach { (lv, color) ->
                        val (l, v) = lv
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$v", color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text(l, color = Color(0xFF888888), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
            items(requests, key = { it.userId }) { kyc ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(when (kyc.status) {
                        "approved" -> Color(0xFF0A2A0A); "rejected" -> Color(0xFF2A0A0A); else -> Color(0xFF1A1A1A)
                    }),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column {
                                Text(kyc.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("PAN: ${kyc.panNumber}", color = Color(0xFF888888), fontSize = 11.sp)
                                Text("UPI: ${kyc.upiId}", color = Color(0xFF888888), fontSize = 11.sp)
                            }
                            StatusBadge(kyc.status)
                        }
                        if (kyc.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    db.collection("kyc_requests").document(kyc.userId).update("status", "rejected")
                                    db.collection("users").document(kyc.userId).update(mapOf("kycStatus" to "rejected"))
                                    InAppNotificationManager.sendNotification(kyc.userId, "KYC Rejected", "Please resubmit valid documents.", "wallet")
                                    logAdminAction("reject_kyc", kyc.userId)
                                    scope.launch { snackbarHostState.showSnackbar("KYC Rejected!") }
                                }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(Color(0xFF330000)), shape = RoundedCornerShape(8.dp)) {
                                    Text("Reject", color = D11Red, fontWeight = FontWeight.Bold)
                                }
                                Button(onClick = {
                                    db.collection("kyc_requests").document(kyc.userId).update("status", "approved")
                                    db.collection("users").document(kyc.userId).update(mapOf(
                                        "kycStatus" to "approved",
                                        "kycVerifiedAt" to System.currentTimeMillis()
                                    ))
                                    InAppNotificationManager.sendNotification(kyc.userId, "KYC Approved!", "You can now withdraw!", "wallet")
                                    logAdminAction("approve_kyc", kyc.userId)
                                    scope.launch { snackbarHostState.showSnackbar("KYC Approved!") }
                                }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(Color(0xFF004400)), shape = RoundedCornerShape(8.dp)) {
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

// ─────────────────────────────────────────────
// CONTESTS TAB
// ─────────────────────────────────────────────

@Composable
fun AdminContestsTab() {
    val db = FirebaseFirestore.getInstance()
    var contests   by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var cName      by remember { mutableStateOf("") }
    var cFee       by remember { mutableStateOf("") }
    var cPrize     by remember { mutableStateOf("") }
    var cSpots     by remember { mutableStateOf("") }
    var cFill      by remember { mutableStateOf("75") }
    var cMaxTeams  by remember { mutableStateOf("1") }
    var cGuaranteed by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("contests").addSnapshotListener { snap, _ ->
            snap?.let {
                contests = it.documents.map { doc ->
                    (doc.data ?: mutableMapOf()).toMutableMap().apply { put("__id", doc.id) }
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad)
        ) {
            item {
                Button(onClick = { showCreate = !showCreate }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(8.dp)) {
                    Text(if (showCreate) "Cancel" else "+ Create Contest", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (showCreate) {
                item {
                    AdminCard {
                        Text("New Contest", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        AdminTextField(cName,   { cName = it },   "Contest Name")
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(cFee,    { cFee = it },    "Entry Fee (₹)", KeyboardType.Number)
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(cPrize,  { cPrize = it },  "Prize Pool (₹)", KeyboardType.Number)
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(cSpots,  { cSpots = it },  "Total Spots", KeyboardType.Number)
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(cFill,   { cFill = it },   "Fill % (0–100)", KeyboardType.Number)
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(cMaxTeams, { cMaxTeams = it }, "Max Teams/User", KeyboardType.Number)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = cGuaranteed, onCheckedChange = { cGuaranteed = it },
                                colors = CheckboxDefaults.colors(checkedColor = D11Green))
                            Text("Guaranteed Contest", color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (cName.isEmpty()) { scope.launch { snackbarHostState.showSnackbar("Enter contest name!") }; return@Button }
                                val fee   = cFee.toIntOrNull() ?: 0
                                val spots = cSpots.toIntOrNull() ?: 1000
                                val fill  = cFill.toIntOrNull() ?: 75
                                val prize = cPrize.toIntOrNull() ?: 0
                                db.collection("contests").add(mapOf(
                                    "name" to cName, "entryFee" to fee,
                                    "prizePool" to "₹$prize", "totalSpots" to spots,
                                    "joinedCount" to (spots * fill / 100),
                                    "fillPercent" to fill, "status" to "open",
                                    "isGuaranteed" to cGuaranteed,
                                    "isHot" to (fee > 0), "isFree" to (fee == 0),
                                    "maxTeamsPerUser" to (cMaxTeams.toIntOrNull() ?: 1),
                                    "firstPrize" to "₹${prize / 10}",
                                    "winners" to "${spots / 10}",
                                    "isMatchEnded" to false, "isDistributed" to false,
                                    "createdAt" to System.currentTimeMillis()
                                )).addOnSuccessListener {
                                    logAdminAction("create_contest", cName)
                                    scope.launch { snackbarHostState.showSnackbar("Contest created!") }
                                    cName = ""; cFee = ""; cPrize = ""; cSpots = ""; showCreate = false
                                }
                            },
                            Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(D11Green),
                            shape  = RoundedCornerShape(8.dp)
                        ) { Text("Create", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }

            items(contests, key = { it["__id"] as? String ?: "" }) { contest ->
                val cid = contest["__id"] as? String ?: ""
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column {
                                Text(contest["name"] as? String ?: "Contest", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Entry: ₹${contest["entryFee"] ?: 0}  |  Prize: ${contest["prizePool"] ?: "N/A"}", color = Color(0xFF888888), fontSize = 11.sp)
                                Text("Spots: ${contest["joinedCount"] ?: 0}/${contest["totalSpots"] ?: 0}", color = D11Green, fontSize = 10.sp)
                            }
                            StatusBadge(contest["status"] as? String ?: "open")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdminSmallBtn("Close",    Color(0xFF330000), D11Red) {
                                db.collection("contests").document(cid).update("status", "closed")
                                scope.launch { snackbarHostState.showSnackbar("Closed!") }
                            }
                            AdminSmallBtn("Prizes",   Color(0xFF003300), D11Green) {
                                PrizeDistributor.distributeContestPrizes(cid)
                                logAdminAction("distribute_prizes", cid)
                                scope.launch { snackbarHostState.showSnackbar("Distribution started!") }
                            }
                            AdminSmallBtn("Delete",   Color(0xFF330000), D11Red) {
                                db.collection("contests").document(cid).delete()
                                scope.launch { snackbarHostState.showSnackbar("Deleted!") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// MATCHES TAB
// ─────────────────────────────────────────────

@Composable
fun AdminMatchesTab() {
    val db = FirebaseFirestore.getInstance()
    var matches    by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var mTeam1     by remember { mutableStateOf("") }
    var mTeam2     by remember { mutableStateOf("") }
    var mVenue     by remember { mutableStateOf("") }
    var mDate      by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("matches").addSnapshotListener { snap, _ ->
            snap?.let {
                matches = it.documents.map { doc ->
                    (doc.data ?: mutableMapOf()).toMutableMap().apply { put("__id", doc.id) }
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad)
        ) {
            item {
                Button(onClick = { showCreate = !showCreate }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(8.dp)) {
                    Text(if (showCreate) "Cancel" else "+ Create Match", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (showCreate) {
                item {
                    AdminCard {
                        Text("New Match", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        AdminTextField(mTeam1, { mTeam1 = it }, "Team 1 (e.g. CSK)")
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(mTeam2, { mTeam2 = it }, "Team 2 (e.g. RCB)")
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(mVenue, { mVenue = it }, "Venue")
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(mDate,  { mDate = it },  "Date (DD/MM/YYYY HH:MM)")
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (mTeam1.isEmpty() || mTeam2.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("Enter both teams!") }
                                    return@Button
                                }
                                db.collection("matches").add(mapOf(
                                    "team1" to mTeam1.uppercase(), "team2" to mTeam2.uppercase(),
                                    "team1Full" to mTeam1, "team2Full" to mTeam2,
                                    "venue" to mVenue, "matchDate" to mDate,
                                    "status" to "upcoming", "isLive" to false,
                                    "createdAt" to System.currentTimeMillis()
                                )).addOnSuccessListener {
                                    logAdminAction("create_match", "$mTeam1 vs $mTeam2")
                                    scope.launch { snackbarHostState.showSnackbar("Match created!") }
                                    mTeam1 = ""; mTeam2 = ""; mVenue = ""; mDate = ""; showCreate = false
                                }
                            },
                            Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(D11Green),
                            shape  = RoundedCornerShape(8.dp)
                        ) { Text("Create Match", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
            items(matches, key = { it["__id"] as? String ?: "" }) { match ->
                val mid = match["__id"] as? String ?: ""
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column {
                                Text("${match["team1"]} vs ${match["team2"]}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(match["venue"] as? String ?: "", color = Color(0xFF888888), fontSize = 11.sp)
                                Text(match["matchDate"] as? String ?: "", color = Color(0xFF888888), fontSize = 10.sp)
                            }
                            StatusBadge(match["status"] as? String ?: "upcoming")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdminSmallBtn("Go Live",   Color(0xFF003300), D11Green) {
                                db.collection("matches").document(mid).update(mapOf("status" to "live", "isLive" to true))
                                logAdminAction("match_live", mid)
                            }
                            AdminSmallBtn("Complete",  Color(0xFF1A1A00), D11Yellow) {
                                db.collection("matches").document(mid).update(mapOf("status" to "completed", "isLive" to false))
                                logAdminAction("match_complete", mid)
                            }
                            AdminSmallBtn("Delete",    Color(0xFF330000), D11Red) {
                                db.collection("matches").document(mid).delete()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// NOTIFY TAB
// ─────────────────────────────────────────────

@Composable
fun AdminNotifyTab() {
    var title   by remember { mutableStateOf("") }
    var body    by remember { mutableStateOf("") }
    var nType   by remember { mutableStateOf("general") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(pad)
        ) {
            item { Text("Send Notification", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }

            // Quick templates
            item {
                Text("Quick Templates", color = Color(0xFF888888), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                listOf(
                    Triple("Match Reminder",  "Match starting in 10 minutes!", "match"),
                    Triple("Contest Alert",   "Mega Contest 95% full – Join now!", "contest"),
                    Triple("Result Out",      "Match results are out! Check leaderboard.", "general"),
                    Triple("Free Contest",    "Free contest available! Join now!", "contest"),
                    Triple("Deposit Bonus",   "Get 20% bonus on your next deposit!", "wallet")
                ).forEach { (t, b, tp) ->
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable {
                            InAppNotificationManager.sendNotification("all", t, b, tp)
                            logAdminAction("quick_notify", t)
                            scope.launch { snackbarHostState.showSnackbar("Sent: $t") }
                        },
                        colors = CardDefaults.cardColors(Color(0xFF1A1A1A)),
                        shape  = RoundedCornerShape(10.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(b, color = Color(0xFF888888), fontSize = 10.sp, maxLines = 1)
                            }
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(D11Red).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("Send", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(color = Color(0xFF222222)) }

            // Custom
            item {
                Text("Custom Notification", color = Color(0xFF888888), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                AdminTextField(title, { title = it }, "Title")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    placeholder = { Text("Message…", color = Color(0xFF888888)) },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    colors = adminTextFieldColors(),
                    shape  = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("general", "match", "contest", "wallet").forEach { type ->
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (nType == type) D11Red else Color(0xFF1E1E1E))
                                .clickable { nType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(type.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (title.isEmpty() || body.isEmpty()) { scope.launch { snackbarHostState.showSnackbar("Enter title and message!") }; return@Button }
                        InAppNotificationManager.sendNotification("all", title, body, nType)
                        logAdminAction("custom_notify", title)
                        scope.launch { snackbarHostState.showSnackbar("Sent!") }
                        title = ""; body = ""
                    },
                    Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(D11Red),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Send to All Users", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PROMO CODES TAB
// ─────────────────────────────────────────────

@Composable
fun AdminPromoTab() {
    val db = FirebaseFirestore.getInstance()
    var promos     by remember { mutableStateOf<List<PromoCode>>(emptyList()) }
    var showCreate by remember { mutableStateOf(false) }
    var pCode      by remember { mutableStateOf("") }
    var pType      by remember { mutableStateOf("flat") }
    var pValue     by remember { mutableStateOf("") }
    var pMaxUses   by remember { mutableStateOf("100") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("promo_codes").addSnapshotListener { snap, _ ->
            snap?.let {
                promos = it.documents.map { doc ->
                    PromoCode(
                        id       = doc.id,
                        code     = doc.getString("code") ?: "",
                        type     = doc.getString("type") ?: "flat",
                        value    = doc.getLong("value")?.toInt() ?: 0,
                        maxUses  = doc.getLong("maxUses")?.toInt() ?: 100,
                        usedCount= doc.getLong("usedCount")?.toInt() ?: 0,
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad)
        ) {
            item {
                Button(onClick = { showCreate = !showCreate }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(8.dp)) {
                    Text(if (showCreate) "Cancel" else "+ Create Promo Code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (showCreate) {
                item {
                    AdminCard {
                        Text("New Promo Code", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        AdminTextField(pCode,    { pCode = it.uppercase() }, "Code (e.g. WIN100)")
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("flat", "percent").forEach { t ->
                                Box(
                                    Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(if (pType == t) D11Red else Color(0xFF1E1E1E))
                                        .clickable { pType = t }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(if (t == "flat") "Flat ₹" else "Percent %", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(pValue,   { pValue = it },   if (pType == "flat") "Value (₹)" else "Value (%)", KeyboardType.Number)
                        Spacer(Modifier.height(6.dp))
                        AdminTextField(pMaxUses, { pMaxUses = it }, "Max Uses", KeyboardType.Number)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (pCode.isEmpty() || pValue.isEmpty()) { scope.launch { snackbarHostState.showSnackbar("Fill all fields!") }; return@Button }
                                db.collection("promo_codes").add(mapOf(
                                    "code" to pCode, "type" to pType,
                                    "value" to (pValue.toIntOrNull() ?: 0),
                                    "maxUses" to (pMaxUses.toIntOrNull() ?: 100),
                                    "usedCount" to 0, "isActive" to true,
                                    "createdAt" to System.currentTimeMillis()
                                )).addOnSuccessListener {
                                    logAdminAction("create_promo", pCode)
                                    scope.launch { snackbarHostState.showSnackbar("Promo $pCode created!") }
                                    pCode = ""; pValue = ""; pMaxUses = "100"; showCreate = false
                                }
                            },
                            Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(D11Green),
                            shape  = RoundedCornerShape(8.dp)
                        ) { Text("Create", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }

            items(promos, key = { it.id }) { promo ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text(promo.code, color = D11Yellow, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (promo.type == "flat") "₹${promo.value} flat off" else "${promo.value}% off",
                                color = Color(0xFF888888), fontSize = 11.sp
                            )
                            Text("Used: ${promo.usedCount}/${promo.maxUses}", color = D11Green, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(if (promo.isActive) Color(0xFF004400) else Color(0xFF330000))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (promo.isActive) "ACTIVE" else "OFF", color = if (promo.isActive) D11Green else D11Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = {
                                db.collection("promo_codes").document(promo.id).delete()
                                logAdminAction("delete_promo", promo.code)
                            }, Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, null, tint = D11Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// WALLET CONTROL TAB
// ─────────────────────────────────────────────

@Composable
fun AdminWalletControlTab() {
    var targetUid by remember { mutableStateOf("") }
    var amount    by remember { mutableStateOf("") }
    var action    by remember { mutableStateOf("add") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(pad)
        ) {
            item { Text("Wallet Control", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
            item {
                AdminCard {
                    Text("Manual Wallet Operation", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AdminTextField(targetUid, { targetUid = it }, "User UID")
                    Spacer(Modifier.height(6.dp))
                    AdminTextField(amount, { amount = it }, "Amount (₹)", KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("add" to "Add Cash", "remove" to "Remove", "bonus" to "Bonus", "freeze" to "Freeze").forEach { (a, label) ->
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (action == a) D11Red else Color(0xFF1E1E1E))
                                    .clickable { action = a }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (targetUid.isEmpty()) { scope.launch { snackbarHostState.showSnackbar("Enter UID!") }; return@Button }
                            val amt = amount.toIntOrNull() ?: 0
                            when (action) {
                                "add" -> db.runTransaction { tx ->
                                    val ref  = db.collection("users").document(targetUid)
                                    val snap = tx.get(ref)
                                    val bal  = snap.getLong("balance")?.toInt() ?: 0
                                    tx.update(ref, "balance", bal + amt)
                                }.addOnSuccessListener {
                                    logAdminAction("wallet_add", "₹$amt to $targetUid")
                                    scope.launch { snackbarHostState.showSnackbar("₹$amt added!") }
                                }
                                "remove" -> db.runTransaction { tx ->
                                    val ref  = db.collection("users").document(targetUid)
                                    val snap = tx.get(ref)
                                    val bal  = snap.getLong("balance")?.toInt() ?: 0
                                    if (bal >= amt) tx.update(ref, "balance", bal - amt)
                                }.addOnSuccessListener {
                                    logAdminAction("wallet_remove", "₹$amt from $targetUid")
                                    scope.launch { snackbarHostState.showSnackbar("₹$amt removed!") }
                                }
                                "bonus" -> db.collection("users").document(targetUid).get()
                                    .addOnSuccessListener { doc ->
                                        val cur = doc.getLong("bonusBalance")?.toInt() ?: 0
                                        db.collection("users").document(targetUid).update("bonusBalance", cur + amt)
                                        logAdminAction("wallet_bonus", "₹$amt bonus to $targetUid")
                                        scope.launch { snackbarHostState.showSnackbar("Bonus ₹$amt added!") }
                                    }
                                "freeze" -> {
                                    db.collection("users").document(targetUid).update("walletFrozen", true)
                                    logAdminAction("wallet_freeze", targetUid)
                                    scope.launch { snackbarHostState.showSnackbar("Wallet frozen!") }
                                }
                            }
                        },
                        Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(D11Red),
                        shape  = RoundedCornerShape(10.dp)
                    ) { Text("Execute Operation", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// RISK / FRAUD TAB
// ─────────────────────────────────────────────

@Composable
fun AdminRiskTab() {
    val db = FirebaseFirestore.getInstance()
    var flagged by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("riskFlag", true)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    flagged = it.documents.map { doc ->
                        (doc.data ?: mutableMapOf()).toMutableMap().apply { put("__id", doc.id) }
                    }
                }
            }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Risk & Fraud Monitor", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }

        item {
            AdminCard {
                Text("Auto-Detection Rules", color = Color(0xFF888888), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Multiple accounts from same device",
                    "Withdrawals > 5x deposit in 7 days",
                    "Unusual winning streak (>80% win rate)",
                    "Bonus abuse — redeem and withdraw"
                ).forEach { rule ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(D11Red))
                        Text(rule, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        if (flagged.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No flagged users", color = Color(0xFF888888), fontSize = 14.sp)
                    }
                }
            }
        } else {
            item { Text("Flagged Users (${flagged.size})", color = D11Red, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            items(flagged, key = { it["__id"] as? String ?: "" }) { user ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF2A0A0A)), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text(user["name"] as? String ?: "User", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Risk: ${user["riskReason"] as? String ?: "Flagged"}", color = D11Red, fontSize = 11.sp)
                        }
                        Text("Review →", color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// APP CONFIG TAB
// ─────────────────────────────────────────────

@Composable
fun AdminConfigTab() {
    val db = FirebaseFirestore.getInstance()
    var gst          by remember { mutableStateOf("28") }
    var platformFee  by remember { mutableStateOf("15") }
    var minDeposit   by remember { mutableStateOf("100") }
    var minWithdraw  by remember { mutableStateOf("200") }
    var maxTeams     by remember { mutableStateOf("10") }
    var maintenance  by remember { mutableStateOf(false) }
    var bannerText   by remember { mutableStateOf("") }
    var supportPhone by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("app_config").document("settings").get().addOnSuccessListener { doc ->
            gst         = doc.getLong("gstPercent")?.toString() ?: "28"
            platformFee = doc.getLong("platformFee")?.toString() ?: "15"
            minDeposit  = doc.getLong("minDeposit")?.toString() ?: "100"
            minWithdraw = doc.getLong("minWithdraw")?.toString() ?: "200"
            maxTeams    = doc.getLong("maxTeams")?.toString() ?: "10"
            maintenance = doc.getBoolean("maintenanceMode") ?: false
            bannerText  = doc.getString("bannerText") ?: ""
            supportPhone= doc.getString("supportPhone") ?: ""
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(pad)
        ) {
            item { Text("App Configuration", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }

            item {
                AdminCard {
                    Text("Financial Settings", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        Triple("GST %",           gst,         { v: String -> gst = v }),
                        Triple("Platform Fee %",  platformFee, { v: String -> platformFee = v }),
                        Triple("Min Deposit ₹",   minDeposit,  { v: String -> minDeposit = v }),
                        Triple("Min Withdrawal ₹",minWithdraw, { v: String -> minWithdraw = v }),
                        Triple("Max Teams/Match",  maxTeams,    { v: String -> maxTeams = v })
                    ).forEach { (label, value, change) ->
                        Spacer(Modifier.height(6.dp))
                        Text(label, color = Color(0xFF888888), fontSize = 11.sp)
                        AdminTextField(value, change, label, KeyboardType.Number)
                    }
                }
            }

            item {
                AdminCard {
                    Text("App Settings", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Maintenance Mode", color = Color.White, fontSize = 13.sp)
                        Switch(checked = maintenance, onCheckedChange = { maintenance = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = D11Red, checkedTrackColor = Color(0xFF440000)))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Banner Text", color = Color(0xFF888888), fontSize = 11.sp)
                    AdminTextField(bannerText, { bannerText = it }, "e.g. IPL Season is LIVE!")
                    Spacer(Modifier.height(6.dp))
                    Text("Support Phone", color = Color(0xFF888888), fontSize = 11.sp)
                    AdminTextField(supportPhone, { supportPhone = it }, "+91XXXXXXXXXX", KeyboardType.Phone)
                }
            }

            item {
                Button(
                    onClick = {
                        db.collection("app_config").document("settings").set(mapOf(
                            "gstPercent"      to (gst.toLongOrNull() ?: 28L),
                            "platformFee"     to (platformFee.toLongOrNull() ?: 15L),
                            "minDeposit"      to (minDeposit.toLongOrNull() ?: 100L),
                            "minWithdraw"     to (minWithdraw.toLongOrNull() ?: 200L),
                            "maxTeams"        to (maxTeams.toLongOrNull() ?: 10L),
                            "maintenanceMode" to maintenance,
                            "bannerText"      to bannerText,
                            "supportPhone"    to supportPhone,
                            "updatedAt"       to System.currentTimeMillis()
                        )).addOnSuccessListener {
                            logAdminAction("update_config", "GST:$gst PF:$platformFee Maint:$maintenance")
                            scope.launch { snackbarHostState.showSnackbar("Config saved!") }
                        }
                    },
                    Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(D11Red),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Save Configuration", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SUPPORT TICKETS TAB
// ─────────────────────────────────────────────

@Composable
fun AdminSupportTab() {
    val db = FirebaseFirestore.getInstance()
    var tickets  by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var selected by remember { mutableStateOf<SupportTicket?>(null) }
    var reply    by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("support_tickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    tickets = it.documents.map { doc ->
                        SupportTicket(
                            id        = doc.id,
                            userId    = doc.getString("userId") ?: "",
                            userName  = doc.getString("userName") ?: "",
                            subject   = doc.getString("subject") ?: "",
                            message   = doc.getString("message") ?: "",
                            status    = doc.getString("status") ?: "open",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            reply     = doc.getString("reply") ?: ""
                        )
                    }
                }
            }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0D)) { pad ->
        if (selected != null) {
            val ticket = selected!!
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(pad)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = { selected = null; reply = "" }, Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF1E1E1E))) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text("Ticket Detail", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                item {
                    AdminCard {
                        Text(ticket.subject, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("From: ${ticket.userName}", color = Color(0xFF888888), fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(ticket.message, color = Color.White, fontSize = 13.sp)
                        if (ticket.reply.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF222222))
                            Spacer(Modifier.height(8.dp))
                            Text("Reply sent:", color = D11Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(ticket.reply, color = Color(0xCCFFFFFF), fontSize = 12.sp)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = reply, onValueChange = { reply = it },
                        placeholder = { Text("Type reply…", color = Color(0xFF888888)) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = adminTextFieldColors(), shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (reply.isEmpty()) return@Button
                            db.collection("support_tickets").document(ticket.id).update(mapOf(
                                "reply" to reply, "status" to "resolved",
                                "repliedAt" to System.currentTimeMillis()
                            ))
                            InAppNotificationManager.sendNotification(ticket.userId, "Support Reply", reply, "general")
                            logAdminAction("support_reply", ticket.id)
                            scope.launch { snackbarHostState.showSnackbar("Reply sent!") }
                            selected = null; reply = ""
                        },
                        Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(D11Green),
                        shape  = RoundedCornerShape(10.dp)
                    ) { Text("Send Reply & Resolve", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(pad)
        ) {
            item { Text("Support Tickets (${tickets.size})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) }
            if (tickets.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No tickets", color = Color(0xFF888888), fontSize = 14.sp)
                    }
                }
            }
            items(tickets, key = { it.id }) { ticket ->
                Card(
                    Modifier.fillMaxWidth().clickable { selected = ticket },
                    colors = CardDefaults.cardColors(if (ticket.status == "open") Color(0xFF1A1000) else Color(0xFF1A1A1A)),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ticket.subject, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ticket.userName, color = Color(0xFF888888), fontSize = 11.sp)
                        }
                        StatusBadge(ticket.status)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// GST TAB
// ─────────────────────────────────────────────

@Composable
fun AdminGSTTab() {
    // Merged into AdminConfigTab — redirect
    AdminConfigTab()
}

// ─────────────────────────────────────────────
// LOGS TAB
// ─────────────────────────────────────────────

@Composable
fun AdminLogsTab() {
    val db = FirebaseFirestore.getInstance()
    var logs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val sdf  = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        db.collection("admin_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, _ ->
                snap?.let { logs = it.documents.map { doc -> doc.data ?: emptyMap() } }
            }
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { Text("Audit Logs (Last 100)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) }
        if (logs.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No logs yet", color = Color(0xFF888888), fontSize = 13.sp)
                }
            }
        }
        items(logs) { log ->
            Card(colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(log["action"] as? String ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(log["details"] as? String ?: "", color = Color(0xFF888888), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        sdf.format(Date(log["timestamp"] as? Long ?: 0L)),
                        color = Color(0xFF666666), fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// STATS TAB
// ─────────────────────────────────────────────

@Composable
fun AdminStatsTab() {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("App Statistics", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
        items(listOf(
            "Total Users" to "—", "Active Today" to "—",
            "Total Matches" to "—", "Total Contests" to "—",
            "Total Revenue" to "—", "Pending Payments" to "—"
        )) { (label, value) ->
            Card(colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween) {
                    Text(label, color = Color(0xFF888888), fontSize = 13.sp)
                    Text(value, color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SHARED HELPERS
// ─────────────────────────────────────────────

@Composable
fun AdminCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(Color(0xFF1A1A1A)),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun AdminTextField(
    value:        String,
    onValueChange:(String) -> Unit,
    placeholder:  String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(placeholder, color = Color(0xFF888888), fontSize = 13.sp) },
        modifier      = Modifier.fillMaxWidth(),
        colors        = adminTextFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape         = RoundedCornerShape(8.dp),
        singleLine    = true
    )
}

@Composable
fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = D11Red,
    unfocusedBorderColor  = Color(0xFF333333),
    focusedTextColor      = Color.White,
    unfocusedTextColor    = Color.White,
    cursorColor           = D11Red,
    focusedContainerColor = Color(0xFF1E1E1E),
    unfocusedContainerColor = Color(0xFF1A1A1A)
)

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "approved", "open", "active", "live"     -> Color(0xFF004400) to D11Green
        "rejected", "closed", "blocked", "frozen"-> Color(0xFF440000) to D11Red
        "pending", "upcoming"                    -> Color(0xFF444400) to D11Yellow
        "resolved"                               -> Color(0xFF003344) to Color(0xFF82B1FF)
        else                                     -> Color(0xFF333333) to Color(0xFF888888)
    }
    Box(
        Modifier.clip(RoundedCornerShape(5.dp)).background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status.uppercase(), color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun AdminSmallBtn(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier.height(34.dp),
        colors   = ButtonDefaults.buttonColors(bg),
        shape    = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────
// AUDIT LOG HELPER
// ─────────────────────────────────────────────

fun logAdminAction(action: String, details: String) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    FirebaseFirestore.getInstance().collection("admin_logs").add(mapOf(
        "action"    to action,
        "details"   to details,
        "adminId"   to uid,
        "timestamp" to System.currentTimeMillis()
    ))
}

@Composable
fun LoadingButton(text: String, isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick  = { if (!isLoading) onClick() },
        modifier = modifier.height(50.dp),
        colors   = ButtonDefaults.buttonColors(D11Red),
        shape    = RoundedCornerShape(10.dp),
        enabled  = !isLoading
    ) {
        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}