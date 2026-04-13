package com.example.dream11india

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// ===== ADMIN UID =====
const val ADMIN_UID = "1irz1sRJ3QNeEtUuN70OSWiUBdq2"

data class PaymentRequest(
    val id: String = "",
    val userId: String = "",
    val userPhone: String = "",
    val amount: Int = 0,
    val status: String = "pending",
    val timestamp: Long = 0L
)

data class AdminContestItem(
    val id: String = "",
    val name: String = "",
    val prize: String = "",
    val spots: String = "",
    val fillPercent: Int = 0,
    val entryFee: String = "",
    val isFree: Boolean = false,
    val isLocked: Boolean = false
)

@Composable
fun AdminPanelScreen(onBack: () -> Unit = {}) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isAdmin = currentUser?.uid == ADMIN_UID

    // Agar admin nahi hai to access denied
    if (!isAdmin) {
        Box(
            modifier = Modifier.fillMaxSize().background(D11Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ðŸš«", fontSize = 60.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Access Denied!", color = D11Red, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold)
                Text("Sirf Owner dekh sakta hai", color = D11Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Wapas Jao") }
            }
        }
        return
    }

    var selectedTab by remember { mutableStateOf("payments") }
    var showCreateContest by remember { mutableStateOf(false) }
    var contestName by remember { mutableStateOf("") }
    var prizePool by remember { mutableStateOf("") }
    var totalSpots by remember { mutableStateOf("") }
    var entryFee by remember { mutableStateOf("") }
    var fillPercent by remember { mutableStateOf(50f) }
    var isFree by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val contests = remember { mutableStateListOf<AdminContestItem>() }
    val paymentRequests = remember { mutableStateListOf<PaymentRequest>() }
    var totalUsers by remember { mutableStateOf(0) }
    var totalRevenue by remember { mutableStateOf(0) }

    // Load payment requests from Firebase
    LaunchedEffect(Unit) {
        db.collection("payments")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                paymentRequests.clear()
                snapshot?.documents?.forEach { doc ->
                    paymentRequests.add(
                        PaymentRequest(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userPhone = doc.getString("userPhone") ?: "",
                            amount = doc.getLong("amount")?.toInt() ?: 0,
                            status = doc.getString("status") ?: "pending",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    )
                }
            }

        // Load contests
        db.collection("contests").addSnapshotListener { snapshot, _ ->
            contests.clear()
            snapshot?.documents?.forEach { doc ->
                contests.add(
                    AdminContestItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        prize = doc.getString("prize") ?: "",
                        spots = doc.getString("spots") ?: "",
                        fillPercent = doc.getLong("fillPercent")?.toInt() ?: 0,
                        entryFee = doc.getString("entryFee") ?: "",
                        isFree = doc.getBoolean("isFree") ?: false,
                        isLocked = doc.getBoolean("isLocked") ?: false
                    )
                )
            }
        }

        // Load user count
        db.collection("users").get().addOnSuccessListener { snapshot ->
            totalUsers = snapshot.size()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1A0000))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("â†", color = D11White, fontSize = 24.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("âš™ï¸ ADMIN PANEL", color = D11Yellow, fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("Owner Only", color = D11Red, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier.clip(CircleShape).background(D11Red)
                    .padding(8.dp)
            ) {
                Text("ðŸ‘‘", fontSize = 16.sp)
            }
        }

        // Admin Tabs
        LazyColumn(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "payments" to "ðŸ’° Payments",
                        "contests" to "ðŸ† Contests",
                        "users" to "ðŸ‘¥ Users",
                        "stats" to "ðŸ“Š Stats"
                    ).forEach { (tab, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedTab = tab }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                color = if (selectedTab == tab) D11Yellow else D11Gray,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selectedTab == tab) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Yellow))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = D11Border)

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            when (selectedTab) {

                // ===== PAYMENTS TAB =====
                "payments" -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ðŸ’° Payment Requests", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier.clip(CircleShape).background(D11Red)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${paymentRequests.size} Pending",
                                    color = D11White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (paymentRequests.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("âœ…", fontSize = 40.sp)
                                    Text("Koi pending payment nahi!",
                                        color = D11Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    items(paymentRequests.size) { index ->
                        val payment = paymentRequests[index]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("ðŸ“± ${payment.userPhone}",
                                            color = D11White, fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold)
                                        Text("Amount: â‚¹${payment.amount}",
                                            color = D11Yellow, fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold)
                                        Text("Status: Pending â³",
                                            color = D11Gray, fontSize = 12.sp)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Approve Button
                                        Button(
                                            onClick = {
                                                // Approve payment
                                                db.collection("payments")
                                                    .document(payment.id)
                                                    .update("status", "approved")

                                                // Add balance to user
                                                db.collection("users")
                                                    .document(payment.userId)
                                                    .get()
                                                    .addOnSuccessListener { doc ->
                                                        val currentBalance = doc.getLong("balance")?.toInt() ?: 0
                                                        db.collection("users")
                                                            .document(payment.userId)
                                                            .update("balance", currentBalance + payment.amount)
                                                    }
                                                paymentRequests.removeAt(index)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("âœ… Approve", color = D11Black,
                                                fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        // Reject Button
                                        Button(
                                            onClick = {
                                                db.collection("payments")
                                                    .document(payment.id)
                                                    .update("status", "rejected")
                                                paymentRequests.removeAt(index)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("âŒ Reject", fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== CONTESTS TAB =====
                "contests" -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ðŸ† Contests", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(D11Red)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { showCreateContest = !showCreateContest }
                            ) {
                                Text("+ New", color = D11White, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Create Contest Form
                    if (showCreateContest) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A00)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("ðŸ†• New Contest", color = D11Yellow, fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    AdminTextField("Contest Name", contestName) { contestName = it }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AdminTextField("Prize Pool (â‚¹)", prizePool, KeyboardType.Number) { prizePool = it }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AdminTextField("Total Spots", totalSpots, KeyboardType.Number) { totalSpots = it }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AdminTextField("Entry Fee (â‚¹)", entryFee, KeyboardType.Number) { entryFee = it }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("Fill %: ${fillPercent.toInt()}%",
                                        color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = fillPercent,
                                        onValueChange = { fillPercent = it },
                                        valueRange = 0f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = D11Red,
                                            activeTrackColor = D11Red,
                                            inactiveTrackColor = D11LightGray
                                        )
                                    )

                                    // Progress Preview
                                    Box(modifier = Modifier.fillMaxWidth().height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)).background(D11LightGray)) {
                                        Box(modifier = Modifier.fillMaxWidth(fillPercent / 100f)
                                            .height(6.dp).clip(RoundedCornerShape(3.dp)).background(D11Green))
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("Free Contest", color = D11White, fontSize = 14.sp)
                                        Switch(checked = isFree, onCheckedChange = { isFree = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = D11Green,
                                                checkedTrackColor = Color(0xFF004D00)))
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("Lock Contest", color = D11White, fontSize = 14.sp)
                                        Switch(checked = isLocked, onCheckedChange = { isLocked = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = D11Red,
                                                checkedTrackColor = Color(0xFF4D0000)))
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (contestName.isNotEmpty()) {
                                                val contestData = hashMapOf(
                                                    "name" to contestName,
                                                    "prize" to "â‚¹$prizePool",
                                                    "spots" to totalSpots,
                                                    "fillPercent" to fillPercent.toInt(),
                                                    "entryFee" to if (isFree) "FREE" else "â‚¹$entryFee",
                                                    "isFree" to isFree,
                                                    "isLocked" to isLocked,
                                                    "createdAt" to System.currentTimeMillis()
                                                )
                                                db.collection("contests").add(contestData)
                                                contestName = ""; prizePool = ""
                                                totalSpots = ""; entryFee = ""
                                                fillPercent = 50f; showCreateContest = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("âœ… CREATE CONTEST", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }

                    // Contest List
                    items(contests.size) { index ->
                        val contest = contests[index]
                        var sliderValue by remember { mutableStateOf(contest.fillPercent.toFloat()) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(contest.name, color = D11White, fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold)
                                        Text("Prize: ${contest.prize} | Fee: ${contest.entryFee}",
                                            color = D11Gray, fontSize = 12.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Lock/Unlock
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                                .background(if (contest.isLocked) D11Red else D11Green)
                                                .padding(8.dp)
                                                .clickable {
                                                    db.collection("contests")
                                                        .document(contest.id)
                                                        .update("isLocked", !contest.isLocked)
                                                }
                                        ) {
                                            Text(if (contest.isLocked) "ðŸ”’" else "ðŸ”“", fontSize = 14.sp)
                                        }
                                        // Delete
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF330000))
                                                .padding(8.dp)
                                                .clickable {
                                                    db.collection("contests")
                                                        .document(contest.id).delete()
                                                }
                                        ) {
                                            Text("ðŸ—‘ï¸", fontSize = 14.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Fill %: ${sliderValue.toInt()}%",
                                    color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = {
                                        db.collection("contests").document(contest.id)
                                            .update("fillPercent", sliderValue.toInt())
                                    },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = D11Yellow,
                                        activeTrackColor = D11Green,
                                        inactiveTrackColor = D11LightGray
                                    )
                                )

                                Box(modifier = Modifier.fillMaxWidth().height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)).background(D11LightGray)) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth(sliderValue / 100f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(when {
                                            sliderValue > 80f -> D11Red
                                            sliderValue > 50f -> D11Yellow
                                            else -> D11Green
                                        }))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Fill Buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(25, 50, 75, 100).forEach { percent ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (sliderValue.toInt() == percent) D11Red else D11LightGray)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .clickable {
                                                    sliderValue = percent.toFloat()
                                                    db.collection("contests").document(contest.id)
                                                        .update("fillPercent", percent)
                                                }
                                        ) {
                                            Text("$percent%", color = D11White, fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== USERS TAB =====
                "users" -> {
                    item {
                        Text("ðŸ‘¥ All Users", color = D11White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatBox("Total Users", "$totalUsers", D11Yellow)
                                    StatBox("Active Today", "45", D11Green)
                                    StatBox("New Today", "12", D11Red)
                                }
                            }
                        }
                    }
                }

                // ===== STATS TAB =====
                "stats" -> {
                    item {
                        Text("ðŸ“Š Statistics", color = D11White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    StatBox("Total Revenue", "â‚¹45K", D11Yellow)
                                    StatBox("Contests", "${contests.size}", D11Green)
                                    StatBox("Pending Pay", "${paymentRequests.size}", D11Red)
                                }

                                HorizontalDivider(color = D11Border)

                                // Revenue Chart
                                Text("Weekly Revenue", color = D11White, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold)

                                Row(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    listOf(
                                        "Mon" to 0.4f,
                                        "Tue" to 0.6f,
                                        "Wed" to 0.8f,
                                        "Thu" to 0.5f,
                                        "Fri" to 0.9f,
                                        "Sat" to 0.7f,
                                        "Sun" to 1.0f
                                    ).forEach { (day, height) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(24.dp)
                                                    .height((80 * height).dp)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(D11Red)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(day, color = D11Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = D11Gray, fontSize = 11.sp)
    }
}

@Composable
fun AdminTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = D11Gray) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = D11Red,
            unfocusedBorderColor = D11Border,
            focusedTextColor = D11White,
            unfocusedTextColor = D11White,
            cursorColor = D11Red,
            focusedContainerColor = D11LightGray,
            unfocusedContainerColor = D11LightGray
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}
