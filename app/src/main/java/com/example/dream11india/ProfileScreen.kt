package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userData: UserData = UserData(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onKYC: () -> Unit = {},
    onWallet: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isEditing by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(userData.name) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize()
            .background(Color(0xFF1A1A1A)).padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)) {

            // TOP BAR
            item {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(D11Red, D11DarkRed)))
                    .statusBarsPadding()) {
                    Row(modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("<", color = D11White, fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onBack() })
                            Image(painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = null, modifier = Modifier.size(28.dp))
                            Text("My Profile", color = D11White, fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        TextButton(onClick = { isEditing = !isEditing }) {
                            Text(if (isEditing) "Cancel" else "Edit",
                                color = D11White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // PROFILE CARD
            item {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(
                        listOf(D11DarkRed, Color(0xFF1A1A1A))))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar
                        Box(modifier = Modifier.size(90.dp).clip(CircleShape)
                            .background(Brush.radialGradient(
                                listOf(Color(0xFFFF4444), D11Red))),
                            contentAlignment = Alignment.Center) {
                            Text((userData.name.firstOrNull() ?: "P").toString().uppercase(),
                                color = D11White, fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                modifier = Modifier.fillMaxWidth(0.8f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = D11White,
                                    unfocusedBorderColor = Color(0x88FFFFFF),
                                    focusedTextColor = D11White,
                                    unfocusedTextColor = D11White,
                                    cursorColor = D11White,
                                    focusedContainerColor = Color(0x22FFFFFF),
                                    unfocusedContainerColor = Color(0x11FFFFFF)),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LoadingButton(
                                text = "Save Name",
                                isLoading = isSaving,
                                onClick = {
                                    if (newName.length < 3) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Name too short!")
                                        }
                                        return@LoadingButton
                                    }
                                    isSaving = true
                                    db.collection("users").document(uid)
                                        .update("name", newName)
                                        .addOnSuccessListener {
                                            isSaving = false
                                            isEditing = false
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Name updated!")
                                            }
                                        }
                                        .addOnFailureListener {
                                            isSaving = false
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    ErrorMessages.getFirestoreError(
                                                        it.message ?: ""))
                                            }
                                        }
                                },
                                modifier = Modifier.fillMaxWidth(0.6f),
                                color = D11White
                            )
                        } else {
                            Text(userData.name, color = D11White, fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(userData.phone, color = Color(0xCCFFFFFF), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // KYC Badge
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(when(userData.kycStatus) {
                                "approved" -> Color(0xFF004400)
                                "pending" -> Color(0xFF444400)
                                else -> Color(0xFF333333)
                            }).padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Text(when(userData.kycStatus) {
                                "approved" -> "KYC Verified"
                                "pending" -> "KYC Pending"
                                else -> "KYC Not Done"
                            }, color = when(userData.kycStatus) {
                                "approved" -> D11Green
                                "pending" -> D11Yellow
                                else -> D11Gray
                            }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // STATS CARDS
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("Balance", "Rs.${userData.balance}", D11Green),
                        Triple("Winnings", "Rs.${userData.winnings}", D11Yellow),
                        Triple("Matches", "${userData.matchesPlayed}", D11White)
                    ).forEach { (label, value, color) ->
                        Card(modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp)) {
                            Column(modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, color = color, fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(label, color = D11Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // MENU ITEMS
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text("Account", color = D11Gray, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp))

                    listOf(
                        Triple("Wallet & Payments", "Rs.${userData.balance}", { onWallet() }),
                        Triple("KYC Verification",
                            when(userData.kycStatus) {
                                "approved" -> "Verified"
                                "pending" -> "Pending"
                                else -> "Complete Now"
                            }, { onKYC() }),
                        Triple("My Teams", "Manage teams", { }),
                        Triple("Transaction History", "View all", { onWallet() })
                    ).forEach { (title, subtitle, action) ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { action() },
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(title, color = D11White, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text(subtitle, color = D11Gray, fontSize = 12.sp)
                                }
                                Text(">", color = D11Gray, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Support", color = D11Gray, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp))

                    listOf(
                        "Privacy Policy" to "Read our policy",
                        "Terms & Conditions" to "App rules",
                        "Help & Support" to "Contact us",
                        "About App" to "Version 1.0.0"
                    ).forEach { (title, subtitle) ->
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(title, color = D11White, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text(subtitle, color = D11Gray, fontSize = 12.sp)
                                }
                                Text(">", color = D11Gray, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Logout
                    Button(onClick = {
                        if (ClickGuard.canClick()) {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        }
                    }, modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A0000)),
                        shape = RoundedCornerShape(12.dp)) {
                        Text("Logout", color = D11Red, fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
