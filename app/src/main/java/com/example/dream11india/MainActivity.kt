package com.example.dream11india

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// ===== COLORS =====
val D11Red = Color(0xFFD4002A)
val D11DarkRed = Color(0xFF8B0000)
val D11Black = Color(0xFF0D0D0D)
val D11CardBg = Color(0xFF1E1E1E)
val D11White = Color(0xFFFFFFFF)
val D11Yellow = Color(0xFFFFCC00)
val D11Green = Color(0xFF00C853)
val D11Gray = Color(0xFF888888)
val D11LightGray = Color(0xFF2A2A2A)
val D11Border = Color(0xFF333333)
val D11DarkGreen = Color(0xFF1B5E20)

// ===== LANGUAGE =====
object Lang {
    var isHindi = false
    val enterMobile get() = if (isHindi) "Mobile Number Dalein" else "Enter Mobile Number"
    val otpSent get() = if (isHindi) "OTP bheja jayega" else "OTP will be sent"
    val getOtp get() = if (isHindi) "OTP Prapt Karein" else "GET OTP"
    val verifyOtp get() = if (isHindi) "OTP Verify Karein" else "VERIFY OTP"
    val otpSentTo get() = if (isHindi) "OTP bheja gaya" else "OTP sent to"
    val verify get() = if (isHindi) "Verify Karein" else "VERIFY"
    val back get() = if (isHindi) "Wapas" else "Back"
    val play get() = if (isHindi) "Khelein" else "JOIN"
    val selectLang get() = "Bhasha Chunein / Select Language"
    val numError get() = if (isHindi) "10 ank ka number dalein" else "Enter 10 digit number"
    val otpError get() = if (isHindi) "6 ank ka OTP dalein" else "Enter 6 digit OTP"
    val wrongOtp get() = if (isHindi) "Galat OTP" else "Wrong OTP"
}

// ===== MATCH DATA =====
data class MatchData(
    val id: String,
    val team1: String, val team2: String,
    val team1Full: String, val team2Full: String,
    val team1Logo: String, val team2Logo: String,
    val type: String, val league: String,
    val matchTime: String,
    val hoursLeft: Int, val minutesLeft: Int,
    val prize: String, val spots: String,
    val fillPercent: Int, val badge: String,
    val team1Color: Color, val team2Color: Color,
    val featuredPlayer: String = "",
    val featuredPlayerPhoto: String = ""
)

// ===== USER DATA =====
data class UserData(
    val uid: String = "",
    val phone: String = "",
    val name: String = "Player",
    val balance: Int = 0,
    val winnings: Int = 0,
    val matchesPlayed: Int = 0,
    val teamsCreated: Int = 0,
    val isAdmin: Boolean = false
)

val sampleMatches = listOf(
    MatchData("1", "IND", "AUS", "India", "Australia", "", "",
        "ODI", "ODI Champions Cup", "Tomorrow, 2:30 PM", 14, 30,
        "Rs.55 Crores", "1,20,000", 95, "MEGA",
        Color(0xFF003580), Color(0xFF006400)),
    MatchData("2", "MI", "CSK", "Mumbai Indians", "Chennai Super Kings", "", "",
        "T20", "IPL 2025", "Today, 7:30 PM", 3, 15,
        "Rs.25 Crores", "85,000", 78, "HOT",
        Color(0xFF004BA0), Color(0xFFFFD700)),
    MatchData("3", "RCB", "KKR", "Royal Challengers", "Kolkata Knight Riders", "", "",
        "T20", "IPL 2025", "Tomorrow, 7:30 PM", 20, 0,
        "Rs.10 Crores", "45,000", 45, "",
        Color(0xFFCC0000), Color(0xFF3A015C)),
    MatchData("4", "DC", "SRH", "Delhi Capitals", "Sunrisers Hyderabad", "", "",
        "T20", "IPL 2025", "Tomorrow, 3:30 PM", 16, 30,
        "Rs.5 Crores", "25,000", 20, "FREE",
        Color(0xFF0078D4), Color(0xFFFF6600)),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Dream11App(activity = this) }
    }
}

@Composable
fun Dream11App(activity: ComponentActivity) {
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    var screen by remember { mutableStateOf(if (firebaseUser != null) "main" else "splash") }
    var phone by remember { mutableStateOf("") }
    var vid by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("home") }
    var currentMatch by remember { mutableStateOf(sampleMatches[0]) }
    var currentCricMatch by remember { mutableStateOf<CricMatch?>(null) }
    var userData by remember { mutableStateOf(UserData()) }
    val backStack = remember { mutableStateListOf<String>() }

    LaunchedEffect(userData.uid) {
        if (userData.uid.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userData.uid)
                .addSnapshotListener { doc, _ ->
                    doc?.let {
                        userData = UserData(
                            uid = it.id,
                            phone = it.getString("phone") ?: "",
                            name = it.getString("name") ?: "Player",
                            balance = it.getLong("balance")?.toInt() ?: 0,
                            winnings = it.getLong("winnings")?.toInt() ?: 0,
                            matchesPlayed = it.getLong("matchesPlayed")?.toInt() ?: 0,
                            teamsCreated = it.getLong("teamsCreated")?.toInt() ?: 0,
                            isAdmin = it.getBoolean("isAdmin") ?: false
                        )
                    }
                }
        }
    }

    fun navigateTo(newScreen: String) {
        backStack.add(screen)
        screen = newScreen
    }

    fun goBack() {
        if (backStack.isNotEmpty()) {
            screen = backStack.removeLast()
        }
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(backStack.size) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backStack.isNotEmpty()) { goBack() }
            }
        }
        backDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    when (screen) {
        "splash" -> SplashScreen { screen = "language" }
        "language" -> LanguageScreen { isHindi ->
            Lang.isHindi = isHindi
            screen = "phone"
        }
        "phone" -> PhoneScreen(activity) { v, p ->
            vid = v; phone = p; navigateTo("otp")
        }
        "otp" -> OtpScreen(phone, vid, { uid ->
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(uid)
            userRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    userRef.set(mapOf(
                        "phone" to phone,
                        "name" to "Player${phone.takeLast(4)}",
                        "balance" to 0,
                        "winnings" to 0,
                        "matchesPlayed" to 0,
                        "teamsCreated" to 0,
                        "isAdmin" to false,
                        "createdAt" to System.currentTimeMillis()
                    ))
                }
                userData = UserData(uid = uid, phone = phone)
            }
            screen = "main"
            backStack.clear()
        }) { goBack() }
        "main" -> HomeScreen(
            currentTab = tab,
            userData = userData,
            onTabChange = { tab = it },
            onMatchClick = { match ->
                currentMatch = match
                navigateTo("live_score")
            },
            onWalletClick = { navigateTo("wallet") },
            onProfileClick = { navigateTo("profile") },
            onAdminClick = { navigateTo("admin") },
            onLeaderboardClick = { navigateTo("leaderboard") }
        )
        "live_score" -> LiveScoreScreen(
            matchId = currentMatch.id,
            matchTitle = "${currentMatch.team1} vs ${currentMatch.team2}",
            onBack = { goBack() }
        )
        "contest" -> ContestScreen(
            matchTitle = "${currentMatch.team1} vs ${currentMatch.team2}",
            onBack = { goBack() },
            onJoin = { navigateTo("team_create") }
        )
        "team_create" -> TeamCreateScreen(
            matchTitle = "${currentMatch.team1} vs ${currentMatch.team2}",
            onBack = { goBack() },
            onTeamSaved = { navigateTo("team_preview") }
        )
        "team_preview" -> TeamPreviewScreen(
            matchTitle = "${currentMatch.team1} vs ${currentMatch.team2}",
            team1 = currentMatch.team1,
            team2 = currentMatch.team2,
            onBack = { goBack() },
            onEditTeam = { goBack() },
            onJoinContest = { navigateTo("contest") }
        )
        "wallet" -> WalletScreen(
            userData = userData,
            onBack = { goBack() }
        )
        "profile" -> ProfileScreen(
            userData = userData,
            onBack = { goBack() },
            onLogout = { screen = "language"; backStack.clear() }
        )
        "admin" -> AdminPanelScreen(onBack = { goBack() })
        "leaderboard" -> LeaderboardScreen(
            matchTitle = "${currentMatch.team1} vs ${currentMatch.team2}",
            onBack = { goBack() }
        )
    }
}

// ===== SPLASH SCREEN =====
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow))
        alpha.animateTo(1f, animationSpec = tween(500))
        textAlpha.animateTo(1f, animationSpec = tween(800))
        delay(2500)
        onFinish()
    }

    Box(modifier = Modifier.fillMaxSize().background(D11Black),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.scale(scale.value).alpha(alpha.value)
                .size(130.dp).clip(CircleShape).background(D11Red)
                .border(3.dp, D11White, CircleShape),
                contentAlignment = Alignment.Center) {

                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(130.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.alpha(textAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DREAM11", color = D11White, fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                Text("INDIA", color = D11Red, fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fantasy Sports", color = D11Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(60.dp))
            val infiniteTransition = rememberInfiniteTransition(label = "dots")
            val dot1 = infiniteTransition.animateFloat(0.3f, 1f,
                infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
            val dot2 = infiniteTransition.animateFloat(0.3f, 1f,
                infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2")
            val dot3 = infiniteTransition.animateFloat(0.3f, 1f,
                infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(dot1.value, dot2.value, dot3.value).forEach { a ->
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape)
                        .alpha(a).background(D11Red))
                }
            }
        }
    }
}

// ===== LANGUAGE SCREEN =====
@Composable
fun LanguageScreen(onSelect: (Boolean) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(D11Black),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("DREAM11 INDIA", color = D11White, fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Text("Fantasy Sports", color = D11Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(48.dp))
            Text(Lang.selectLang, color = D11White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onSelect(true) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(8.dp)) {
                Text("Hindi mein Khelein", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { onSelect(false) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = D11White),
                border = androidx.compose.foundation.BorderStroke(1.dp, D11Red),
                shape = RoundedCornerShape(8.dp)) {
                Text("Play in English", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ===== PHONE SCREEN =====
@Composable
fun PhoneScreen(activity: ComponentActivity, onOtpSent: (String, String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {
        Box(modifier = Modifier.fillMaxWidth().background(D11Red).padding(vertical = 32.dp),
            contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("DREAM11", color = D11White, fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("INDIA", color = D11Yellow, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                }
            }
        }
        Column(modifier = Modifier.padding(24.dp)) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(Lang.enterMobile, color = D11White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(Lang.otpSent, color = D11Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (error.isNotEmpty()) Color.Red else D11Border,
                    RoundedCornerShape(8.dp)).background(D11LightGray),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(D11CardBg)
                    .padding(horizontal = 16.dp, vertical = 18.dp)) {
                    Text("+91", color = D11White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(D11Border))
                BasicTextField(value = phone,
                    onValueChange = { if (it.length <= 10) phone = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    textStyle = TextStyle(color = D11White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (phone.isEmpty()) Text("00000 00000", color = D11Gray,
                            fontSize = 18.sp)
                        inner()
                    })
            }
            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = Color.Red, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (phone.length != 10) { error = Lang.numError; return@Button }
                    loading = true; error = ""
                    val fullPhone = "+91$phone"
                    val cb = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(c: PhoneAuthCredential) {
                            FirebaseAuth.getInstance().signInWithCredential(c)
                        }
                        override fun onVerificationFailed(e: FirebaseException) {
                            error = e.message ?: "Failed"; loading = false
                        }
                        override fun onCodeSent(v: String,
                                                t: PhoneAuthProvider.ForceResendingToken) {
                            loading = false; onOtpSent(v, fullPhone)
                        }
                    }
                    PhoneAuthProvider.verifyPhoneNumber(
                        PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                            .setPhoneNumber(fullPhone).setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity).setCallbacks(cb).build())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (loading) CircularProgressIndicator(color = D11White,
                    modifier = Modifier.size(24.dp))
                else Text(Lang.getOtp, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ===== OTP SCREEN =====
@Composable
fun OtpScreen(phone: String, vid: String,
              onSuccess: (String) -> Unit, onBack: () -> Unit) {
    var otp by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var timer by remember { mutableStateOf(60) }

    LaunchedEffect(Unit) {
        while (timer > 0) { delay(1000); timer-- }
    }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {
        Box(modifier = Modifier.fillMaxWidth().background(D11Red).padding(vertical = 20.dp),
            contentAlignment = Alignment.Center) {
            Text(Lang.verifyOtp, color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(Lang.otpSentTo, color = D11Gray, fontSize = 14.sp)
            Text(phone, color = D11White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("OTP expires in: ${timer}s", color = D11Yellow, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                placeholder = { Text("* * * * * *", color = D11Gray, fontSize = 24.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = D11Red, unfocusedBorderColor = D11Border,
                    focusedTextColor = D11White, unfocusedTextColor = D11White,
                    cursorColor = D11Red, focusedContainerColor = D11LightGray,
                    unfocusedContainerColor = D11LightGray),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp), singleLine = true,
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, color = D11White))
            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = Color.Red, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (otp.length != 6) { error = Lang.otpError; return@Button }
                    loading = true
                    val cred = PhoneAuthProvider.getCredential(vid, otp)
                    FirebaseAuth.getInstance().signInWithCredential(cred)
                        .addOnSuccessListener { result ->
                            onSuccess(result.user?.uid ?: "")
                        }
                        .addOnFailureListener { error = Lang.wrongOtp; loading = false }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (loading) CircularProgressIndicator(color = D11White,
                    modifier = Modifier.size(24.dp))
                else Text(Lang.verify, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("← ${Lang.back}", color = D11Red, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() })
        }
    }
}

// ===== COUNTDOWN TIMER =====
@Composable
fun CountdownTimer(hoursLeft: Int, minutesLeft: Int) {
    var totalSeconds by remember { mutableStateOf(hoursLeft * 3600 + minutesLeft * 60) }
    LaunchedEffect(Unit) {
        while (totalSeconds > 0) { delay(1000); totalSeconds-- }
    }
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val timeColor = if (h == 0 && m < 30) D11Red else D11Yellow
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
        .background(Color(0xFF1A1A00)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text = if (h > 0) "${h}h ${m}m" else "${m}m ${s}s",
            color = timeColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}



