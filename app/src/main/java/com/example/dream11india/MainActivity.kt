package com.example.dream11india

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// ============================================================
// TYPE-SAFE NAVIGATION ROUTES
// ============================================================
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Language : Screen("language")
    object Phone : Screen("phone")
    object Otp : Screen("otp/{phone}/{vid}") {
        fun createRoute(phone: String, vid: String) = "otp/$phone/$vid"
    }
    object Main : Screen("main")
    object Contest : Screen("contest")
    object TeamCreate : Screen("team_create")
    object TeamPreview : Screen("team_preview")
    object LiveScore : Screen("live_score")
    object Wallet : Screen("wallet")
    object Payment : Screen("payment")
    object Profile : Screen("profile")
    object Admin : Screen("admin")
    object Leaderboard : Screen("leaderboard")
    object MyTeams : Screen("my_teams")
    object KYC : Screen("kyc")
}

// ============================================================
// MAIN ACTIVITY
// ============================================================
class MainActivity : ComponentActivity() {

    // ViewModel scoped to Activity lifecycle
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CrashHandler.setup()
        NotificationHelper.createChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            Dream11App(
                activity = this,
                viewModel = viewModel
            )
        }
    }
}

// ============================================================
// ROOT COMPOSABLE
// ============================================================
@Composable
fun Dream11App(
    activity: ComponentActivity,
    viewModel: AppViewModel
) {
    // ---- Collect state from ViewModel ----
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentMatch by viewModel.currentMatch.collectAsStateWithLifecycle()

    // ---- Connectivity (callback-based, no polling) ----
    val connectivityObserver = remember { ConnectivityObserver(activity) }
    val isOnline by connectivityObserver.isOnline.collectAsStateWithLifecycle(
        initialValue = NetworkHelper.isInternetAvailable(activity)
    )

    // ---- NavController ----
    val navController = rememberNavController()

    // ---- Start user listener when logged in ----
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val uid = com.google.firebase.auth.FirebaseAuth
                .getInstance().currentUser?.uid ?: return@LaunchedEffect
            viewModel.startUserListener(uid)
            NotificationHelper.initFCM(uid)
            InAppNotificationManager.listenForNotifications(activity, uid) { title, _ ->
                android.util.Log.d("Notification", "Received: $title")
            }
        }
    }

    // ---- Start destination based on auth state ----
    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Splash.route

    // ---- No internet overlay ----
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        ) {

            // ---- SPLASH ----
            composable(Screen.Splash.route) {
                SplashScreen {
                    navController.navigate(Screen.Language.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }

            // ---- LANGUAGE ----
            composable(Screen.Language.route) {
                LanguageScreen { isHindi ->
                    Lang.isHindi = isHindi
                    navController.navigate(Screen.Phone.route)
                }
            }

            // ---- PHONE ----
            composable(Screen.Phone.route) {
                PhoneScreen(activity) { vid, phone ->
                    navController.navigate(Screen.Otp.createRoute(phone, vid))
                }
            }

            // ---- OTP ----
            composable(Screen.Otp.route) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                val vid = backStackEntry.arguments?.getString("vid") ?: ""
                OtpScreen(
                    phone = phone,
                    vid = vid,
                    onSuccess = { uid ->
                        viewModel.createUserIfNew(uid, phone)
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ---- MAIN / HOME ----
            composable(Screen.Main.route) {
                HomeScreen(
                    userData = userData,
                    onMatchClick = { match ->
                        viewModel.setCurrentMatch(match)
                        navController.navigate(Screen.Contest.route)
                    },
                    onWalletClick = { navController.navigate(Screen.Wallet.route) },
                    onProfileClick = { navController.navigate(Screen.Profile.route) },
                    onAdminClick = { navController.navigate(Screen.Admin.route) },
                    onLeaderboardClick = { navController.navigate(Screen.Leaderboard.route) }
                )
            }

            // ---- CONTEST ----
            composable(Screen.Contest.route) {
                ContestScreen(
                    matchTitle = currentMatch.run {
                        "${team1Full.ifEmpty { team1 }} vs ${team2Full.ifEmpty { team2 }}"
                    },
                    userData = userData,
                    onBack = { navController.popBackStack() },
                    onJoin = { navController.navigate(Screen.TeamCreate.route) }
                )
            }

            // ---- TEAM CREATE ----
            composable(Screen.TeamCreate.route) {
                TeamCreateScreen(
                    matchTitle = currentMatch.run {
                        "${team1Full.ifEmpty { team1 }} vs ${team2Full.ifEmpty { team2 }}"
                    },
                    onBack = { navController.popBackStack() },
                    onTeamSaved = { navController.navigate(Screen.TeamPreview.route) }
                )
            }

            // ---- TEAM PREVIEW ----
            composable(Screen.TeamPreview.route) {
                TeamPreviewScreen(
                    matchTitle = currentMatch.run {
                        "${team1Full.ifEmpty { team1 }} vs ${team2Full.ifEmpty { team2 }}"
                    },
                    team1 = currentMatch.team1,
                    team2 = currentMatch.team2,
                    onBack = { navController.popBackStack() },
                    onEditTeam = { navController.popBackStack() },
                    onJoinContest = {
                        navController.navigate(Screen.Contest.route) {
                            popUpTo(Screen.Contest.route) { inclusive = true }
                        }
                    }
                )
            }

            // ---- MY TEAMS ----
            composable(Screen.MyTeams.route) {
                MyTeamsScreen(
                    matchId = currentMatch.id,
                    matchTitle = currentMatch.run {
                        "${team1Full.ifEmpty { team1 }} vs ${team2Full.ifEmpty { team2 }}"
                    },
                    onBack = { navController.popBackStack() },
                    onCreateTeam = { navController.navigate(Screen.TeamCreate.route) },
                    onJoinContest = { navController.navigate(Screen.Contest.route) },
                    onEditTeam = { navController.navigate(Screen.TeamCreate.route) }
                )
            }

            // ---- LIVE SCORE ----
            composable(Screen.LiveScore.route) {
                LiveScoreScreen(
                    matchId = currentMatch.id,
                    matchTitle = "${currentMatch.team1} vs ${currentMatch.team2}",
                    onBack = { navController.popBackStack() }
                )
            }

            // ---- WALLET ----
            composable(Screen.Wallet.route) {
                WalletScreen(
                    userData = userData,
                    onBack = { navController.popBackStack() },
                    onAddMoney = { navController.navigate(Screen.Payment.route) }
                )
            }

            // ---- PAYMENT ----
            composable(Screen.Payment.route) {
                PaymentScreen(
                    userData = userData,
                    onBack = { navController.popBackStack() }
                )
            }

            // ---- PROFILE ----
            composable(Screen.Profile.route) {
                ProfileScreen(
                    userData = userData,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate(Screen.Language.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onKYC = { navController.navigate(Screen.KYC.route) },
                    onWallet = { navController.navigate(Screen.Wallet.route) }
                )
            }

            // ---- ADMIN ----
            composable(Screen.Admin.route) {
                AdminPanelScreen(onBack = { navController.popBackStack() })
            }

            // ---- LEADERBOARD ----
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(
                    matchTitle = "${currentMatch.team1} vs ${currentMatch.team2}",
                    onBack = { navController.popBackStack() }
                )
            }

            // ---- KYC ----
            composable(Screen.KYC.route) {
                KYCScreen(
                    userData = userData,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // ---- No Internet Overlay ----
        if (!isOnline) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE000000)),
                contentAlignment = Alignment.Center
            ) {
                NoInternetScreen(onRetry = {})
            }
        }
    }
}

// ============================================================
// SPLASH SCREEN
// ============================================================
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(150f) }
    val textAlpha = remember { Animatable(0f) }
    val bgAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        bgAlpha.animateTo(1f, tween(300))
        logoScale.animateTo(1.2f, tween(400, easing = FastOutSlowInEasing))
        logoAlpha.animateTo(1f, tween(300))
        logoScale.animateTo(1f, spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium))
        // Parallel animation - text slides up while logo bounces
        textOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(400))
        delay(2500)
        onFinish()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .alpha(bgAlpha.value)
        .background(Brush.verticalGradient(
            colors = listOf(Color(0xFF1A0008), D11Red, Color(0xFF8B0000))
        )),
        contentAlignment = Alignment.Center) {

        Box(modifier = Modifier.scale(logoScale.value).alpha(logoAlpha.value),
            contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo", modifier = Modifier.size(160.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = textOffsetY.value.dp)
                .alpha(textAlpha.value)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DREAM11", color = D11White, fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)
            Text("INDIA", color = D11White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Fantasy Sports", color = Color(0xCCFFFFFF), fontSize = 15.sp,
                letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(32.dp))

            // Animated loading dots
            val inf = rememberInfiniteTransition(label = "dots")
            val d1 = inf.animateFloat(0.2f, 1f,
                infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "d1")
            val d2 = inf.animateFloat(0.2f, 1f,
                infiniteRepeatable(tween(500, delayMillis = 166), RepeatMode.Reverse), label = "d2")
            val d3 = inf.animateFloat(0.2f, 1f,
                infiniteRepeatable(tween(500, delayMillis = 332), RepeatMode.Reverse), label = "d3")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(d1.value, d2.value, d3.value).forEach { a ->
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                        .alpha(a).background(D11White))
                }
            }
        }
    }
}

// ============================================================
// LANGUAGE SCREEN
// ============================================================
@Composable
fun LanguageScreen(onSelect: (Boolean) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(D11Black),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo", modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("DREAM11 INDIA", color = D11White, fontSize = 24.sp,
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

// ============================================================
// PHONE SCREEN
// ============================================================
@Composable
fun PhoneScreen(activity: ComponentActivity, onOtpSent: (String, String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {
        Box(modifier = Modifier.fillMaxWidth().background(D11Red)
            .statusBarsPadding().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo", modifier = Modifier.size(48.dp))
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
            Text(Lang.otpSent, color = D11Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp,
                    if (error.isNotEmpty()) Color.Red else D11Border,
                    RoundedCornerShape(8.dp))
                .background(D11LightGray),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(D11CardBg)
                    .padding(horizontal = 16.dp, vertical = 18.dp)) {
                    Text("+91", color = D11White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(D11Border))
                BasicTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 10) phone = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    textStyle = TextStyle(color = D11White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (phone.isEmpty()) Text("00000 00000", color = D11Gray, fontSize = 18.sp)
                        inner()
                    }
                )
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
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(c: PhoneAuthCredential) {
                            FirebaseAuth.getInstance().signInWithCredential(c)
                        }
                        override fun onVerificationFailed(e: FirebaseException) {
                            error = e.message ?: "Verification failed"
                            loading = false
                        }
                        override fun onCodeSent(vid: String,
                                                t: PhoneAuthProvider.ForceResendingToken) {
                            loading = false
                            onOtpSent(vid, fullPhone)
                        }
                    }
                    PhoneAuthProvider.verifyPhoneNumber(
                        PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                            .setPhoneNumber(fullPhone)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity)
                            .setCallbacks(callbacks)
                            .build()
                    )
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

// ============================================================
// OTP SCREEN
// ============================================================
@Composable
fun OtpScreen(
    phone: String,
    vid: String,
    onSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var timer by remember { mutableStateOf(60) }

    LaunchedEffect(Unit) {
        while (timer > 0) { delay(1000); timer-- }
    }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {
        Box(modifier = Modifier.fillMaxWidth().background(D11Red)
            .statusBarsPadding().padding(vertical = 20.dp),
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
                    textAlign = TextAlign.Center, color = D11White)
            )

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
                        .addOnFailureListener {
                            error = Lang.wrongOtp
                            loading = false
                        }
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

// ============================================================
// COUNTDOWN TIMER
// ============================================================
@Composable
fun CountdownTimer(hoursLeft: Int, minutesLeft: Int) {
    var totalSeconds by remember {
        mutableStateOf(hoursLeft * 3600 + minutesLeft * 60)
    }
    LaunchedEffect(Unit) {
        while (totalSeconds > 0) { delay(1000); totalSeconds-- }
    }
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val timeColor = if (h == 0 && m < 30) D11Red else D11Yellow

    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
        .background(Color(0xFF1A1A00))
        .padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(
            text = if (h > 0) "${h}h ${m}m" else "${m}m ${s}s",
            color = timeColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
        )
    }
}




