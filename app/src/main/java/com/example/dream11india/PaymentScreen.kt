package com.example.dream11india

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


// ===== QR PAYMENT SCREEN =====
@Composable
fun PaymentScreen(
    userData: UserData = UserData(),
    onBack: () -> Unit = {}
) {
    var selectedAmount by remember { mutableStateOf(0) }
    var customAmount by remember { mutableStateOf("") }
    var utrNumber by remember { mutableStateOf("") }
    var showQR by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    val quickAmounts = listOf(50, 100, 200, 500, 1000, 2000)

    if (showSuccess) {
        // Success Screen
        Box(
            modifier = Modifier.fillMaxSize().background(D11Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(D11Green),
                    contentAlignment = Alignment.Center
                ) {
                    Text("âœ“", color = D11White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Request Submitted!", color = D11White, fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Admin will verify and approve your payment within 30 minutes.",
                    color = D11Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("UTR: $utrNumber", color = D11Yellow, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Wallet", color = D11White, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp)
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("â†", color = D11White, fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Text("Add Money", color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Current Balance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Balance", color = D11Gray, fontSize = 12.sp)
                            Text("Rs.${userData.balance}", color = D11Yellow,
                                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(Color(0xFF1A3A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Rs.", color = D11Green, fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Amount Selection
            item {
                Text("Select Amount", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
            }

            item {
                // Quick amounts grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickAmounts.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { amount ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedAmount == amount) D11Red
                                            else D11LightGray
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedAmount == amount) D11Red else D11Border,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedAmount = amount
                                            customAmount = amount.toString()
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Rs.$amount", color = D11White,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Custom amount
            item {
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = {
                        customAmount = it
                        selectedAmount = it.toIntOrNull() ?: 0
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter Custom Amount", color = D11Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = D11Red,
                        unfocusedBorderColor = D11Border,
                        focusedTextColor = D11White,
                        unfocusedTextColor = D11White,
                        cursorColor = D11Red,
                        focusedContainerColor = D11LightGray,
                        unfocusedContainerColor = D11LightGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    prefix = { Text("Rs. ", color = D11Green, fontWeight = FontWeight.Bold) }
                )
            }

            // Proceed to Pay button
            if (!showQR) {
                item {
                    Button(
                        onClick = {
                            if (selectedAmount >= 10) showQR = true
                            else errorMsg = "Minimum amount Rs.10"
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAmount >= 10) D11Red
                            else Color(0xFF444444)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Proceed to Pay Rs.$selectedAmount â†’",
                            color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg, color = Color.Red, fontSize = 13.sp)
                    }
                }
            }

            // QR Code + UTR Section
            if (showQR) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = D11CardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Scan & Pay", color = D11White, fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pay Rs.$selectedAmount using any UPI app",
                                color = D11Gray, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            // QR Code placeholder
                            Box(
                                modifier = Modifier.size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(D11White)
                                    .border(3.dp, D11Red, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("QR CODE", color = D11Black, fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("UPI ID:", color = D11Black, fontSize = 12.sp)
                                    Text("dream11india@upi", color = D11Red,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Rs.$selectedAmount", color = D11Black,
                                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // UPI ID copy
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(D11LightGray)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("UPI ID", color = D11Gray, fontSize = 11.sp)
                                        Text("dream11india@upi", color = D11White,
                                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(D11Red)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Copy", color = D11White, fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // UTR Number input
                item {
                    Text("Enter UTR / Transaction ID", color = D11White,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("After payment, enter the 12-digit UTR number from your UPI app",
                        color = D11Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = { if (it.length <= 12) utrNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("UTR Number", color = D11Gray) },
                        placeholder = { Text("e.g. 123456789012", color = D11Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = D11Red,
                            unfocusedBorderColor = D11Border,
                            focusedTextColor = D11White,
                            unfocusedTextColor = D11White,
                            cursorColor = D11Red,
                            focusedContainerColor = D11LightGray,
                            unfocusedContainerColor = D11LightGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                // Submit button
                item {
                    Button(
                        onClick = {
                            if (utrNumber.length < 6) {
                                errorMsg = "Please enter valid UTR number"
                                return@Button
                            }
                            isSubmitting = true
                            // Save to Firebase
                            val request = hashMapOf(
                                "userId" to (currentUser?.uid ?: ""),
                                "userName" to userData.name,
                                "userPhone" to userData.phone,
                                "amount" to selectedAmount,
                                "utrNumber" to utrNumber,
                                "status" to "pending",
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("payment_requests")
                                .add(request)
                                .addOnSuccessListener {
                                    isSubmitting = false
                                    showSuccess = true
                                }
                                .addOnFailureListener {
                                    isSubmitting = false
                                    errorMsg = "Failed to submit. Try again."
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (utrNumber.length >= 6) D11Green
                            else Color(0xFF444444)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSubmitting && utrNumber.length >= 6
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = D11White,
                                modifier = Modifier.size(24.dp))
                        } else {
                            Text("Submit Payment Request",
                                color = D11White, fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp)
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg, color = Color.Red, fontSize = 13.sp)
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}


