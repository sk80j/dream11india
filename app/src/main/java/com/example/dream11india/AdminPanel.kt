package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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

data class AdminContestItem(
    val id: String = "",
    val name: String = "",
    val prize: String = "",
    val entryFee: Int = 0,
    val totalSpots: Int = 0,
    val filledSpots: Int = 0
)

@Composable
fun AdminPanelScreen(onBack: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf("payments") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        // TOP BAR
        Row(modifier = Modifier.fillMaxWidth().background(D11Red)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = D11White, fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(8.dp))
            Image(painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo", modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Admin Panel", color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }

        // TABS
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                "payments" to "Payments",
                "users" to "Users",
                "matches" to "Matches",
                "stats" to "Stats"
            ).forEach { (tab, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedTab = tab }
                        .padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(label,
                        color = if (selectedTab == tab) D11Red else D11Gray,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold
                        else FontWeight.Normal)
                    if (selectedTab == tab) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Red))
                    }
                }
            }
        }

        HorizontalDivider(color = D11Border)

        when (selectedTab) {
            "payments" -> AdminPaymentPanel(onBack = {})
            "users" -> AdminUsersTab()
            "matches" -> AdminMatchesTab()
            "stats" -> AdminStatsTab()
        }
    }
}

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

    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No payment requests", color = D11Gray, fontSize = 16.sp)
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Stats
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Total" to requests.size.toString() to D11White,
                    "Pending" to requests.count { it.status == "pending" }.toString() to D11Yellow,
                    "Approved" to requests.count { it.status == "approved" }.toString() to D11Green,
                    "Rejected" to requests.count { it.status == "rejected" }.toString() to D11Red
                ).forEach { (labelValue, color) ->
                    val (label, value) = labelValue
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
                        }
                },
                onReject = {
                    db.collection("payment_requests").document(request.id)
                        .update("status", "rejected")
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
            containerColor = when (request.status) {
                "approved" -> Color(0xFF0A2A0A)
                "rejected" -> Color(0xFF2A0A0A)
                else -> D11CardBg
            }
        ),
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
                    .background(when (request.status) {
                        "approved" -> Color(0xFF004400)
                        "rejected" -> Color(0xFF440000)
                        else -> Color(0xFF444400)
                    }).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(request.status.uppercase(),
                        color = when (request.status) {
                            "approved" -> D11Green
                            "rejected" -> D11Red
                            else -> D11Yellow
                        },
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = D11Border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Amount", color = D11Gray, fontSize = 11.sp)
                    Text("Rs.${request.amount}", color = D11Yellow, fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UTR Number", color = D11Gray, fontSize = 11.sp)
                    Text(request.utrNumber.ifEmpty { "N/A" }, color = D11White,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (request.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF330000)),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Reject", color = D11Red, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004400)),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Approve", color = D11Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab() {
    val db = FirebaseFirestore.getInstance()
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snap, _ ->
            snap?.let {
                users = it.documents.map { doc -> doc.data ?: emptyMap() }
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Total Users: ${users.size}", color = D11White,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        items(users) { user ->
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(D11Red),
                            contentAlignment = Alignment.Center) {
                            Text(
                                (user["name"] as? String)?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                color = D11White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(user["name"] as? String ?: "User",
                                color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(user["phone"] as? String ?: "",
                                color = D11Gray, fontSize = 12.sp)
                        }
                    }
                    Text("Rs.${user["balance"] ?: 0}", color = D11Green,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminMatchesTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Match Management", color = D11White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(8.dp)) {
                Text("+ Add New Match", color = D11White, fontWeight = FontWeight.Bold)
            }
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

@Composable
fun AdminStatsTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("App Statistics", color = D11White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
        }
        items(listOf(
            "Total Users" to "0",
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
@Composable
fun AdminWithdrawTab() {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<WithdrawRequest>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("withdrawRequests")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
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

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
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
                    ).forEach { (labelValue, color) ->
                        val (label, value) = labelValue
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
                                Text("User: ${req.userId.take(10)}...",
                                    color = D11White, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold)
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
                        Text("Rs.${req.amount}", color = D11Yellow,
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        if (req.status == "pending") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    db.collection("withdrawRequests")
                                        .document(req.id)
                                        .update("status", "rejected")
                                        .addOnSuccessListener { snackMsg = "Rejected!" }
                                }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF330000)),
                                    shape = RoundedCornerShape(8.dp)) {
                                    Text("Reject", color = D11Red, fontWeight = FontWeight.Bold)
                                }
                                Button(onClick = {
                                    db.collection("users").document(req.userId)
                                        .get()
                                        .addOnSuccessListener { userSnap ->
                                            val bal = userSnap.getLong("balance")?.toInt() ?: 0
                                            if (bal >= req.amount) {
                                                db.collection("users").document(req.userId)
                                                    .update("balance", bal - req.amount)
                                                db.collection("transactions").add(mapOf(
                                                    "userId" to req.userId,
                                                    "type" to "debit",
                                                    "amount" to req.amount,
                                                    "description" to "Withdrawal approved",
                                                    "timestamp" to System.currentTimeMillis()
                                                ))
                                                db.collection("withdrawRequests")
                                                    .document(req.id)
                                                    .update("status", "approved")
                                                snackMsg = "Approved! Rs.${req.amount} deducted."
                                            } else {
                                                snackMsg = "User has insufficient balance!"
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
