package com.example.dream11india

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

data class Transaction(
    val title: String,
    val amount: String,
    val date: String,
    val isCredit: Boolean
)

@Composable
fun WalletScreen(
    userData: UserData = UserData(),
    onBack: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userPhone = currentUser?.phoneNumber ?: ""

    var addAmount by remember { mutableStateOf("") }
    var showAddCash by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    var showQR by remember { mutableStateOf(false) }
    var pendingAmount by remember { mutableStateOf(0) }
    var showPendingMsg by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadStatus by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var upiId by remember { mutableStateOf("dream11india@upi") }

    LaunchedEffect(Unit) {
        db.collection("settings").document("payment")
            .get().addOnSuccessListener { doc ->
                upiId = doc.getString("upiId") ?: "dream11india@upi"
            }
    }

    val transactions = remember { mutableStateListOf<Transaction>() }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("transactions")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, _ ->
                    transactions.clear()
                    snapshot?.documents?.forEach { doc ->
                        transactions.add(Transaction(
                            title = doc.getString("title") ?: "",
                            amount = doc.getString("amount") ?: "",
                            date = doc.getString("date") ?: "",
                            isCredit = doc.getBoolean("isCredit") ?: false
                        ))
                    }
                    if (transactions.isEmpty()) {
                        transactions.add(Transaction("Welcome Bonus", "+₹0", "Today", true))
                    }
                }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {

        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", color = D11White, fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Text("My Balance", color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Balance", color = Color(0xFF666666), fontSize = 13.sp)
                                Text("₹${userData.balance}", color = D11Green,
                                    fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Button(
                                onClick = { showAddCash = !showAddCash; showWithdraw = false },
                                colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("ADD CASH", fontWeight = FontWeight.ExtraBold, color = D11White) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Amount Unutilised", color = Color(0xFF666666), fontSize = 12.sp)
                                Text("₹0", color = Color(0xFF111111), fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Winnings", color = Color(0xFF666666), fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("₹${userData.winnings}", color = Color(0xFF111111),
                                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    if (userData.winnings > 0) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("WITHDRAW", color = D11Green,
                                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Withdraw Button
                        OutlinedButton(
                            onClick = { showWithdraw = !showWithdraw; showAddCash = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = D11Green),
                            border = androidx.compose.foundation.BorderStroke(1.dp, D11Green),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("WITHDRAW INSTANTLY", fontWeight = FontWeight.Bold, color = D11Green) }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(12.dp))

                        listOf("💎" to "DreamCoins Balance",
                            "📋" to "My Transactions",
                            "💳" to "Manage Payments",
                            "👤" to "My KYC Details",
                            "🎁" to "Invite & Collect"
                        ).forEach { (icon, title) ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {}
                                .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(icon, fontSize = 18.sp)
                                    Text(title, color = Color(0xFF111111), fontSize = 14.sp)
                                }
                                Text(">", color = Color(0xFF666666), fontSize = 16.sp)
                            }
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }

            // Add Cash Form
            if (showAddCash) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                        shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Add Cash", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Admin approval ke baad amount add hogi",
                                color = D11Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("100", "500", "1000", "2000").forEach { amt ->
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(if (addAmount == amt) D11Red else D11LightGray)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .clickable { addAmount = amt }) {
                                        Text("₹$amt", color = D11White, fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = addAmount,
                                onValueChange = { addAmount = it },
                                label = { Text("Enter Amount", color = D11Gray) },
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
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true)

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val amt = addAmount.toIntOrNull() ?: 0
                                    if (amt > 0) { pendingAmount = amt; showQR = true }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("📱 Show QR & Pay", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }

            // QR + Screenshot Upload
            if (showQR) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                        shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Scan & Pay", color = D11White, fontSize = 18.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Amount: ₹$pendingAmount", color = D11Yellow,
                                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(modifier = Modifier.size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(D11White).padding(16.dp),
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("█▀▀▀█ ▄ █▀▀▀█", color = D11Black,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("█ ▄ █ █ █ ▄ █", color = D11Black,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("█▄▄▄█ ▄ █▄▄▄█", color = D11Black,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("▄▄▄ ▀█▀ ▄▄▀▄▀", color = D11Black, fontSize = 14.sp)
                                    Text("▀▄▀█▄▄▀ █▀▀▀▄", color = D11Black, fontSize = 14.sp)
                                    Text("█▀▀▀█ ▀▄█▄▀▄▀", color = D11Black, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("UPI QR Code", color = D11Black, fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text(upiId, color = Color(0xFF555555), fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("UPI ID: $upiId", color = D11Gray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹$pendingAmount pay karo aur\nscreenshot upload karo",
                                color = D11Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Screenshot Upload
                            Box(modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(D11LightGray)
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .padding(16.dp),
                                contentAlignment = Alignment.Center) {
                                if (selectedImageUri != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Screenshot",
                                            modifier = Modifier.fillMaxWidth().height(200.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Fit)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("✅ Screenshot selected!",
                                            color = D11Green, fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold)
                                        Text("Change karne ke liye tap karo",
                                            color = D11Gray, fontSize = 11.sp)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📸", fontSize = 40.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Payment Screenshot Upload Karo",
                                            color = D11White, fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold)
                                        Text("Tap to select from gallery",
                                            color = D11Gray, fontSize = 12.sp)
                                    }
                                }
                            }

                            if (uploadStatus.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(uploadStatus,
                                    color = if (uploadStatus.contains("✅")) D11Green else D11Red,
                                    fontSize = 13.sp, textAlign = TextAlign.Center)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (selectedImageUri == null) {
                                        uploadStatus = "❌ Pehle screenshot select karo!"
                                        return@Button
                                    }
                                    isUploading = true
                                    uploadStatus = "⏳ Uploading..."
                                    val uid = currentUser?.uid ?: return@Button
                                    val fileName = "payments/${uid}_${System.currentTimeMillis()}.jpg"
                                    val storageRef = storage.reference.child(fileName)

                                    storageRef.putFile(selectedImageUri!!)
                                        .addOnSuccessListener {
                                            storageRef.downloadUrl.addOnSuccessListener { url ->
                                                db.collection("payments").add(mapOf(
                                                    "userId" to uid,
                                                    "userPhone" to userPhone,
                                                    "amount" to pendingAmount,
                                                    "screenshotUrl" to url.toString(),
                                                    "status" to "pending",
                                                    "timestamp" to System.currentTimeMillis()
                                                ))
                                                db.collection("transactions").add(mapOf(
                                                    "userId" to uid,
                                                    "title" to "Payment Pending",
                                                    "amount" to "₹$pendingAmount",
                                                    "date" to "Today",
                                                    "isCredit" to false
                                                ))
                                                uploadStatus = "✅ Submit ho gaya! Admin approve karega."
                                                isUploading = false
                                                showPendingMsg = true
                                                showQR = false
                                                showAddCash = false
                                            }
                                        }
                                        .addOnFailureListener {
                                            uploadStatus = "❌ Upload failed! Try again."
                                            isUploading = false
                                        }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedImageUri != null) D11Red
                                    else D11LightGray),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isUploading
                            ) {
                                if (isUploading) CircularProgressIndicator(color = D11White,
                                    modifier = Modifier.size(24.dp))
                                else Text("🚀 SUBMIT PAYMENT",
                                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Pending Message
            if (showPendingMsg) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3300)),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("⏳", fontSize = 32.sp)
                            Column {
                                Text("Payment Under Review", color = D11White,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Admin verify karega aur balance add karega.",
                                    color = D11Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Withdraw Form
            if (showWithdraw) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                        shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Withdraw Cash", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = withdrawAmount,
                                onValueChange = { withdrawAmount = it },
                                label = { Text("Enter Amount", color = D11Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = D11Green,
                                    unfocusedBorderColor = D11Border,
                                    focusedTextColor = D11White,
                                    unfocusedTextColor = D11White,
                                    cursorColor = D11Green,
                                    focusedContainerColor = D11LightGray,
                                    unfocusedContainerColor = D11LightGray),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val amt = withdrawAmount.toIntOrNull() ?: 0
                                    if (amt > 0 && amt <= userData.balance) {
                                        currentUser?.uid?.let { uid ->
                                            db.collection("users").document(uid)
                                                .update("balance", userData.balance - amt)
                                            db.collection("transactions").add(mapOf(
                                                "userId" to uid,
                                                "title" to "Withdrawal",
                                                "amount" to "-₹$amt",
                                                "date" to "Today",
                                                "isCredit" to false))
                                        }
                                        withdrawAmount = ""
                                        showWithdraw = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("WITHDRAW INSTANTLY",
                                fontWeight = FontWeight.ExtraBold, color = D11Black) }
                        }
                    }
                }
            }

            // Transaction History
            item {
                Text("My Transactions", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }

            items(transactions.size) { index ->
                val txn = transactions[index]
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (txn.isCredit) Color(0xFF004D00)
                                else Color(0xFF4D0000)),
                                contentAlignment = Alignment.Center) {
                                Text(if (txn.isCredit) "↑" else "↓",
                                    color = if (txn.isCredit) D11Green else D11Red,
                                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(txn.title, color = D11White, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold)
                                Text(txn.date, color = D11Gray, fontSize = 12.sp)
                            }
                        }
                        Text(txn.amount,
                            color = if (txn.isCredit) D11Green else D11Red,
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}