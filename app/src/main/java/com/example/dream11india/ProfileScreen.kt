package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
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
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(userData.name) }
    val db = FirebaseFirestore.getInstance()

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
                Text("My Profile", color = D11White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
            }
            Text(if (isEditing) "Save" else "Edit",
                color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    if (isEditing) {
                        db.collection("users").document(userData.uid)
                            .update("name", name)
                        isEditing = false
                    } else isEditing = true
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Card
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(D11Red),
                            contentAlignment = Alignment.Center) {
                            Text((userData.name.firstOrNull() ?: "P").toString().uppercase(),
                                color = D11White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name", color = D11Gray) },
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
                                    singleLine = true
                                )
                            } else {
                                Text(userData.name, color = D11White, fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(userData.phone, color = D11Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1A3A1A))
                                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("Verified", color = D11Green, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Stats
            item {
                Text("My Stats", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Rs.${userData.balance}" to "Balance",
                        "Rs.${userData.winnings}" to "Winnings",
                        "${userData.matchesPlayed}" to "Matches"
                    ).forEach { (value, label) ->
                        Card(modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, color = D11Yellow, fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(label, color = D11Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Refer Card
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1500)),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Refer & Earn", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                            Text("Dost ko invite karo - dono ko bonus!",
                                color = D11Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Code: D11${userData.phone.takeLast(4)}",
                                color = D11Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                            shape = RoundedCornerShape(8.dp)) {
                            Text("Share", color = D11White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Settings
            item {
                Text("Settings", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)) {
                    Column {
                        listOf(
                            "Notifications",
                            "Privacy & Security",
                            "Contact Support",
                            "Rate the App",
                            "Terms & Conditions",
                            "About Dream11 India"
                        ).forEachIndexed { index, item ->
                            Row(modifier = Modifier.fillMaxWidth()
                                .clickable { }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(item, color = D11White, fontSize = 14.sp)
                                Text(">", color = D11Gray, fontSize = 16.sp)
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
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A0000)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LOGOUT", color = D11Red, fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}