package com.example.dream11india

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    userData:      UserData = UserData(),
    onBack:        () -> Unit = {},
    onLogout:      () -> Unit = {},
    onKYC:         () -> Unit = {},
    onWallet:      () -> Unit = {},
    onMyTeams:     () -> Unit = {}
) {
    val db    = FirebaseFirestore.getInstance()
    val uid   = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var isEditing by remember { mutableStateOf(false) }
    var newName   by remember { mutableStateOf(userData.name) }
    var isSaving  by remember { mutableStateOf(false) }

    LaunchedEffect(userData.name) { newName = userData.name }

    Scaffold(
        snackbarHost   = { SnackbarHost(snack) },
        containerColor = Color(0xFF0A0A0A)
    ) { pad ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // TOP BAR
            item {
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(D11Red, D11DarkRed))).statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = D11White, modifier = Modifier.size(17.dp))
                            }
                            Text("My Profile", color = D11White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        TextButton(onClick = { isEditing = !isEditing }) {
                            Text(if (isEditing) "Cancel" else "Edit", color = D11White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // PROFILE CARD
            item {
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(D11DarkRed, Color(0xFF0A0A0A))))) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(90.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFF4444), D11Red))), Alignment.Center) {
                            Text((userData.name.firstOrNull() ?: "P").toString().uppercase(), color = D11White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (isEditing) {
                            OutlinedTextField(
                                value = newName, onValueChange = { newName = it },
                                modifier = Modifier.fillMaxWidth(0.8f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = D11White, unfocusedBorderColor = Color(0x88FFFFFF),
                                    focusedTextColor = D11White, unfocusedTextColor = D11White,
                                    cursorColor = D11White, focusedContainerColor = Color(0x22FFFFFF),
                                    unfocusedContainerColor = Color(0x11FFFFFF)
                                ),
                                shape = RoundedCornerShape(10.dp), singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (newName.length < 3) { scope.launch { snack.showSnackbar("Name too short!") }; return@Button }
                                    isSaving = true
                                    scope.launch {
                                        try {
                                            db.collection("users").document(uid).update("name", newName.trim()).await()
                                            isSaving = false; isEditing = false
                                            snack.showSnackbar("Name updated!")
                                        } catch (e: Exception) { isSaving = false; snack.showSnackbar("Update failed!") }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(D11White), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(0.6f).height(44.dp), enabled = !isSaving
                            ) {
                                if (isSaving) CircularProgressIndicator(color = D11Red, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("Save", color = D11Red, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            Text(userData.name, color = D11White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(userData.phone, color = Color(0xCCFFFFFF), fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(when(userData.kycStatus) { "approved"->Color(0xFF004400); "pending"->Color(0xFF444400); else->Color(0xFF333333) }).padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Text(when(userData.kycStatus) { "approved"->"KYC Verified"; "pending"->"KYC Pending"; else->"KYC Not Done" },
                                color = when(userData.kycStatus) { "approved"->D11Green; "pending"->D11Yellow; else->D11Gray },
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (userData.referralCode.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Referral:", color = Color(0xFF888888), fontSize = 12.sp)
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(userData.referralCode, color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
            // STATS
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("Balance",  "₹${userData.balance}",       D11Green),
                        Triple("Winnings", "₹${userData.winnings}",      D11Yellow),
                        Triple("Contests", "${userData.joinedContests}", D11White)
                    ).forEach { (label, value, color) ->
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text(label, color = D11Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // SECTION: ACCOUNT
            item { ProfileSectionHeader("Account") }
            item {
                ProfileMenuItem(icon = Icons.Default.AccountBalanceWallet, title = "Wallet & Payments",
                    subtitle = "Balance: ₹${userData.balance}", onClick = onWallet)
            }
            item {
                ProfileMenuItem(icon = Icons.Default.VerifiedUser, title = "KYC Verification",
                    subtitle = when(userData.kycStatus) { "approved"->"Verified ✓"; "pending"->"Pending review"; else->"Complete to withdraw" },
                    onClick = onKYC, badge = if(userData.kycStatus != "approved") "Action needed" else null)
            }
            item {
                ProfileMenuItem(icon = Icons.Default.Groups, title = "My Teams",
                    subtitle = "${userData.teamsCreated} teams created", onClick = onMyTeams)
            }
            item {
                ProfileMenuItem(icon = Icons.Default.History, title = "Transaction History",
                    subtitle = "Deposits, withdrawals & winnings", onClick = onWallet)
            }
            item {
                ProfileMenuItem(icon = Icons.Default.CardGiftcard, title = "Refer & Earn",
                    subtitle = "Earn ₹100 per referral", onClick = {})
            }

            // SECTION: SUPPORT
            item { ProfileSectionHeader("Support") }
            item { ProfileMenuItem(icon = Icons.Default.Help, title = "Help & Support", subtitle = "FAQs and contact", onClick = {}) }
            item { ProfileMenuItem(icon = Icons.Default.Security, title = "Privacy Policy", subtitle = "Read our policy", onClick = {}) }
            item { ProfileMenuItem(icon = Icons.Default.Description, title = "Terms & Conditions", subtitle = "App rules", onClick = {}) }
            item { ProfileMenuItem(icon = Icons.Default.Info, title = "About App", subtitle = "Version 1.0.0", onClick = {}) }

            // LOGOUT
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (ClickGuard.canClick()) {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                    colors   = ButtonDefaults.buttonColors(Color(0xFF2A0000)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null, tint = D11Red, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", color = D11Red, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(title, color = D11Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun ProfileMenuItem(
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    title:    String,
    subtitle: String,
    onClick:  () -> Unit,
    badge:    String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() },
        colors   = CardDefaults.cardColors(Color(0xFF1A1A1A)),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF2A2A2A)), Alignment.Center) {
                    Icon(icon, null, tint = D11Red, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(title, color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = D11Gray, fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (badge != null) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(D11Red.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(badge, color = D11Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = D11Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}
