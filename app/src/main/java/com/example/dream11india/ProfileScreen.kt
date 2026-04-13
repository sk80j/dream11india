package com.example.dream11india

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    userData: UserData = UserData(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var showEditName by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf(userData.name) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", color = D11White) },
            text = { Text("Kya aap logout karna chahte hain?", color = D11Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = D11Red)
                ) { Text("Logout") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = D11White)
                }
            },
            containerColor = D11CardBg
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = D11White, fontSize = 24.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.width(12.dp))
                Text("My Profile", color = D11White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
            }
            Text("✏️ Edit", color = D11White, fontSize = 14.sp,
                modifier = Modifier.clickable { showEditName = !showEditName })
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                                .background(D11Red)
                                .border(3.dp, D11Yellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (userData.name.firstOrNull() ?: "P").toString().uppercase(),
                                color = D11White, fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (showEditName) {
                            OutlinedTextField(
                                value = editNameText,
                                onValueChange = { editNameText = it },
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
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                label = { Text("Your Name", color = D11Gray) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        currentUser?.uid?.let { uid ->
                                            db.collection("users").document(uid)
                                                .update("name", editNameText)
                                        }
                                        showEditName = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = D11Red),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Save") }
                                OutlinedButton(
                                    onClick = { showEditName = false },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, D11Border),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Cancel", color = D11White) }
                            }
                        } else {
                            Text(userData.name, color = D11White, fontSize = 20.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(userData.phone, color = D11Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            // Verified Badge
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF004D00))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("✓ Verified", color = D11Green, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Stats Cards
            item {
                Text("My Stats", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "💰" to "Balance" to "₹${userData.balance}",
                        "🏆" to "Winnings" to "₹${userData.winnings}",
                        "🏏" to "Matches" to "${userData.matchesPlayed}"
                    ).forEach { (iconLabel, value) ->
                        val (icon, label) = iconLabel
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(icon, fontSize = 24.sp)
                                Text(value, color = D11Yellow, fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(label, color = D11Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Referral Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A00)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🎁 Refer & Earn", color = D11White, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold)
                            Text("Dost ko invite karo — dono ko bonus!",
                                color = D11Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Code: D11${userData.phone.takeLast(4)}",
                                color = D11Yellow, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Share") }
                    }
                }
            }

            // Menu Items
            item {
                Text("Settings", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        listOf(
                            "🔔" to "Notifications",
                            "🔒" to "Privacy & Security",
                            "📞" to "Contact Support",
                            "⭐" to "Rate the App",
                            "📋" to "Terms & Conditions",
                            "ℹ️" to "About Dream11 India"
                        ).forEachIndexed { index, (icon, title) ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(icon, fontSize = 18.sp)
                                    Text(title, color = D11White, fontSize = 14.sp)
                                }
                                Text("→", color = D11Gray, fontSize = 16.sp)
                            }
                            if (index < 5) {
                                HorizontalDivider(color = D11Border, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Logout
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF330000)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🚪 LOGOUT", color = D11Red, fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}