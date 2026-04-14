package com.example.dream11india

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

data class KycData(
    val userId: String = "",
    val name: String = "",
    val panNumber: String = "",
    val panImageUrl: String = "",
    val selfieUrl: String = "",
    val upiId: String = "",
    val status: String = "pending",
    val submittedAt: Long = 0L,
    val verifiedAt: Long = 0L,
    val rejectionReason: String = ""
)

@Composable
fun KYCScreen(
    userData: UserData = UserData(),
    onBack: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullName by remember { mutableStateOf(userData.name) }
    var panNumber by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var panImageUri by remember { mutableStateOf<Uri?>(null) }
    var selfieUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var kycStatus by remember { mutableStateOf(userData.kycStatus) }
    var rejectionReason by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf(0f) }

    val panLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri -> panImageUri = uri }
    val selfieLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri -> selfieUri = uri }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            db.collection("users").document(uid)
                .addSnapshotListener { doc, _ ->
                    doc?.let {
                        kycStatus = it.getString("kycStatus") ?: "none"
                        panNumber = it.getString("panNumber") ?: ""
                        upiId = it.getString("upiId") ?: ""
                        fullName = it.getString("name") ?: userData.name
                        rejectionReason = it.getString("kycRejectionReason") ?: ""
                    }
                    isLoading = false
                }
        } else isLoading = false
    }

    fun isValidPan(pan: String): Boolean {
        return Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$").matches(pan.uppercase())
    }

    fun isValidUpi(upi: String): Boolean = upi.contains("@") && upi.length > 4

    fun uploadImage(uri: Uri, path: String, onComplete: (String) -> Unit) {
        val ref = storage.reference.child(path)
        ref.putFile(uri)
            .addOnProgressListener { task ->
                uploadProgress = task.bytesTransferred.toFloat() / task.totalByteCount
            }
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url -> onComplete(url.toString()) }
            }
            .addOnFailureListener {
                scope.launch { snackbarHostState.showSnackbar("Image upload failed!") }
                isSubmitting = false
            }
    }

    fun submitKYC() {
        when {
            fullName.length < 3 -> scope.launch {
                snackbarHostState.showSnackbar("Enter valid full name!")
            }
            !isValidPan(panNumber) -> scope.launch {
                snackbarHostState.showSnackbar("Invalid PAN! Format: ABCDE1234F")
            }
            !isValidUpi(upiId) -> scope.launch {
                snackbarHostState.showSnackbar("Enter valid UPI ID!")
            }
            else -> {
                isSubmitting = true
                uploadProgress = 0f
                var panUrl = ""
                var selfieUrl = ""
                var uploadsDone = 0
                val totalUploads = (if (panImageUri != null) 1 else 0) +
                        (if (selfieUri != null) 1 else 0)

                fun saveToFirestore() {
                    val kycData = mapOf(
                        "userId" to uid,
                        "name" to fullName,
                        "panNumber" to panNumber.uppercase(),
                        "panImageUrl" to panUrl,
                        "selfieUrl" to selfieUrl,
                        "upiId" to upiId,
                        "status" to "pending",
                        "submittedAt" to System.currentTimeMillis()
                    )
                    db.collection("kyc_requests").document(uid).set(kycData)
                        .addOnSuccessListener {
                            db.collection("users").document(uid).set(mapOf(
                                "kycStatus" to "pending",
                                "panNumber" to panNumber.uppercase(),
                                "upiId" to upiId,
                                "name" to fullName,
                                "kycSubmittedAt" to System.currentTimeMillis()
                            ), SetOptions.merge())
                            isSubmitting = false
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "KYC submitted! Verification in 24-48 hours.")
                            }
                        }
                        .addOnFailureListener {
                            isSubmitting = false
                            scope.launch { snackbarHostState.showSnackbar("Failed! Try again.") }
                        }
                }

                if (totalUploads == 0) {
                    saveToFirestore()
                    return
                }

                fun checkDone() {
                    uploadsDone++
                    if (uploadsDone >= totalUploads) saveToFirestore()
                }

                panImageUri?.let {
                    uploadImage(it, "kyc/${uid}_pan.jpg") { url -> panUrl = url; checkDone() }
                }
                selfieUri?.let {
                    uploadImage(it, "kyc/${uid}_selfie.jpg") { url -> selfieUrl = url; checkDone() }
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize()
            .background(Color(0xFF1A1A1A)).padding(padding)) {

            // TOP BAR
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
                        Column {
                            Text("KYC Verification", color = D11White,
                                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Verify your identity", color = Color(0xFFFFCDD2),
                                fontSize = 11.sp)
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(when(kycStatus) {
                            "approved" -> Color(0xFF004400)
                            "pending" -> Color(0xFF444400)
                            "rejected" -> Color(0xFF440000)
                            else -> Color(0xFF333333)
                        }).padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(when(kycStatus) {
                            "approved" -> "Verified"
                            "pending" -> "Pending"
                            "rejected" -> "Rejected"
                            else -> "Not Done"
                        }, color = when(kycStatus) {
                            "approved" -> D11Green
                            "pending" -> D11Yellow
                            "rejected" -> D11Red
                            else -> D11Gray
                        }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = D11Red)
                    }
                }
                kycStatus == "approved" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.size(100.dp).clip(CircleShape)
                                .background(Color(0xFF1A3A1A)),
                                contentAlignment = Alignment.Center) {
                                Text("OK", color = D11Green, fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            Text("KYC Verified!", color = D11Green, fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("You can now withdraw money.", color = D11Gray, fontSize = 14.sp)
                            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 32.dp)) {
                                Column(modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Name:", color = D11Gray)
                                        Text(fullName, color = D11White, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("PAN:", color = D11Gray)
                                        Text(panNumber, color = D11White, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("UPI:", color = D11Gray)
                                        Text(upiId, color = D11White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                kycStatus == "pending" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.size(100.dp).clip(CircleShape)
                                .background(Color(0xFF1A1A00)),
                                contentAlignment = Alignment.Center) {
                                Text("...", color = D11Yellow, fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            Text("KYC Under Review", color = D11Yellow,
                                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Verification takes 24-48 hours.", color = D11Gray,
                                fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        if (kycStatus == "rejected") {
                            item {
                                Card(colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A0000)),
                                    shape = RoundedCornerShape(10.dp)) {
                                    Row(modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("X", color = D11Red, fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold)
                                        Column {
                                            Text("KYC Rejected", color = D11Red,
                                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(rejectionReason.ifEmpty {
                                                "Please resubmit with correct details."},
                                                color = D11Gray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A00)),
                                shape = RoundedCornerShape(10.dp)) {
                                Row(modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("!", color = D11Yellow, fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold)
                                    Column {
                                        Text("Why KYC?", color = D11Yellow,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Required for cash withdrawals. Data is secure.",
                                            color = D11Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Full Name (as per PAN)", color = D11Gray, fontSize = 13.sp)
                                OutlinedTextField(value = fullName,
                                    onValueChange = { fullName = it },
                                    placeholder = { Text("Enter full name", color = D11Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = D11Red,
                                        unfocusedBorderColor = D11Border,
                                        focusedTextColor = D11White,
                                        unfocusedTextColor = D11White,
                                        cursorColor = D11Red,
                                        focusedContainerColor = D11LightGray,
                                        unfocusedContainerColor = D11LightGray),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words),
                                    shape = RoundedCornerShape(10.dp), singleLine = true)
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("PAN Card Number", color = D11Gray, fontSize = 13.sp)
                                OutlinedTextField(value = panNumber,
                                    onValueChange = {
                                        if (it.length <= 10) panNumber = it.uppercase() },
                                    placeholder = { Text("ABCDE1234F", color = D11Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (panNumber.length == 10 &&
                                            isValidPan(panNumber)) D11Green else D11Red,
                                        unfocusedBorderColor = D11Border,
                                        focusedTextColor = D11White,
                                        unfocusedTextColor = D11White,
                                        cursorColor = D11Red,
                                        focusedContainerColor = D11LightGray,
                                        unfocusedContainerColor = D11LightGray),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters),
                                    shape = RoundedCornerShape(10.dp), singleLine = true,
                                    trailingIcon = {
                                        if (panNumber.length == 10) {
                                            Text(if (isValidPan(panNumber)) "OK" else "X",
                                                color = if (isValidPan(panNumber)) D11Green else D11Red,
                                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 8.dp))
                                        }
                                    })
                                Text("Format: ABCDE1234F", color = D11Gray, fontSize = 11.sp)
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("UPI ID (for withdrawals)", color = D11Gray, fontSize = 13.sp)
                                OutlinedTextField(value = upiId,
                                    onValueChange = { upiId = it },
                                    placeholder = { Text("yourname@upi", color = D11Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = D11Red,
                                        unfocusedBorderColor = D11Border,
                                        focusedTextColor = D11White,
                                        unfocusedTextColor = D11White,
                                        cursorColor = D11Red,
                                        focusedContainerColor = D11LightGray,
                                        unfocusedContainerColor = D11LightGray),
                                    shape = RoundedCornerShape(10.dp), singleLine = true)
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Upload PAN Card Image", color = D11Gray, fontSize = 13.sp)
                                Box(modifier = Modifier.fillMaxWidth().height(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(D11LightGray)
                                    .border(1.dp,
                                        if (panImageUri != null) D11Green else D11Border,
                                        RoundedCornerShape(10.dp))
                                    .clickable { panLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (panImageUri != null) "OK" else "+",
                                            color = if (panImageUri != null) D11Green else D11Gray,
                                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text(if (panImageUri != null) "PAN Image Selected"
                                            else "Tap to upload PAN image",
                                            color = if (panImageUri != null) D11Green else D11Gray,
                                            fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Upload Selfie (optional)", color = D11Gray, fontSize = 13.sp)
                                Box(modifier = Modifier.fillMaxWidth().height(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(D11LightGray)
                                    .border(1.dp,
                                        if (selfieUri != null) D11Green else D11Border,
                                        RoundedCornerShape(10.dp))
                                    .clickable { selfieLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (selfieUri != null) "OK" else "+",
                                            color = if (selfieUri != null) D11Green else D11Gray,
                                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text(if (selfieUri != null) "Selfie Selected"
                                            else "Tap to upload selfie",
                                            color = if (selfieUri != null) D11Green else D11Gray,
                                            fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        if (isSubmitting && uploadProgress > 0f) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Uploading... ${(uploadProgress * 100).toInt()}%",
                                        color = D11Gray, fontSize = 12.sp)
                                    LinearProgressIndicator(
                                        progress = { uploadProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = D11Red, trackColor = D11LightGray)
                                }
                            }
                        }

                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                                shape = RoundedCornerShape(10.dp)) {
                                Column(modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Important", color = D11White, fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold)
                                    listOf(
                                        "PAN details verified with NSDL",
                                        "Data is encrypted and stored securely",
                                        "KYC required for withdrawals above Rs.100",
                                        "False information may result in account ban"
                                    ).forEach { term ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("*", color = D11Red, fontSize = 12.sp)
                                            Text(term, color = D11Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(onClick = { submitKYC() },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isSubmitting) D11Red
                                    else Color(0xFF444444)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSubmitting,
                                elevation = ButtonDefaults.buttonElevation(4.dp)) {
                                if (isSubmitting) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(color = D11White,
                                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Text("Submitting...", color = D11White,
                                            fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Submit KYC", color = D11White,
                                        fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
