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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Transaction(
    val title: String,
    val amount: String,
    val date: String,
    val isCredit: Boolean
)

@Composable
fun WalletScreen(
    userData: UserData = UserData(),
    onBack: () -> Unit = {},
    onAddMoney: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("transactions")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    snap?.let {
                        transactions = it.documents.map { doc ->
                            Transaction(
                                title = doc.getString("title") ?: "Transaction",
                                amount = doc.getString("amount") ?: "Rs.0",
                                date = doc.getString("date") ?: "Today",
                                isCredit = doc.getBoolean("isCredit") ?: true
                            )
                        }
                        if (transactions.isEmpty()) {
                            transactions = listOf(
                                Transaction("Welcome Bonus", "+Rs.50", "Today", true)
                            )
                        }
                    }
                    isLoading = false
                }
        } else {
            transactions = listOf(Transaction("Welcome Bonus", "+Rs.50", "Today", true))
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = D11White, fontSize = 24.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.width(8.dp))
                Image(painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo", modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("My Balance", color = D11White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Balance", color = Color(0xFF666666), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Rs.${userData.balance}", color = D11Green,
                                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Button(
                                onClick = onAddMoney,
                                colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ADD CASH", color = D11White,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Amount Unutilised", color = Color(0xFF666666), fontSize = 12.sp)
                                Text("Rs.${userData.balance}", color = Color(0xFF333333),
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Winnings", color = Color(0xFF666666), fontSize = 12.sp)
                                Text("Rs.${userData.winnings}", color = Color(0xFF333333),
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, D11Green),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("WITHDRAW INSTANTLY", color = D11Green,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Menu Items
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        listOf(
                            "DreamCoins Balance",
                            "My Transactions",
                            "Manage Payments",
                            "My KYC Details",
                            "Invite & Collect"
                        ).forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        if (item == "Manage Payments" ||
                                            item == "My Transactions") onAddMoney()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item, color = Color(0xFF333333),
                                    fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(">", color = Color(0xFF666666), fontSize = 16.sp)
                            }
                            if (index < 4) {
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Transactions Header
            item {
                Text("My Transactions", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }

            // Transactions List
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = D11Red)
                    }
                }
            } else {
                items(transactions) { txn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = D11CardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                        .background(
                                            if (txn.isCredit) Color(0xFF1A3A1A)
                                            else Color(0xFF3A1A1A)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (txn.isCredit) "+" else "-",
                                        color = if (txn.isCredit) D11Green else D11Red,
                                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
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

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}