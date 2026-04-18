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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
// CONSTANTS
// ============================================================

private const val OWNER_UID = "1irz1sRJ3QNeEtUuN70OSWiUBdq2"

// ============================================================
// TYPE-SAFE NAVIGATION ROUTES
// ============================================================

sealed class Screen(val route: String) {
    object Splash      : Screen("splash")
    object Language    : Screen("language")
    object Phone       : Screen("phone")
    object Otp         : Screen("otp/{phone}/{vid}") {
        fun createRoute(phone: String, vid: String) = "otp/$phone/$vid"
    }
    object Main        : Screen("main")
    object Contest     : Screen("contest")
    object TeamCreate  : Screen("team_create")
    object TeamPreview : Screen("team_preview")
    object MyTeams     : Screen("my_teams")
    object LiveScore   : Screen("live_score")
    object Wallet      : Screen("wallet")
    object Payment     : Screen("payment")
    object Profile     : Screen("profile")
    object Admin       : Screen("admin")
    object Leaderboard : Screen("leaderboard")
    object KYC         : Screen("kyc")
}

// ============================================================
// MAIN ACTIVITY
// ============================================================

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CrashHandler.setup()
        NotificationHelper.createChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            Dream11App(activity = this, viewModel = viewModel)
        }
    }
}

// ============================================================
// ROOT COMPOSABLE
// ============================================================

@Composable
fun Dream11App(
    activity:  ComponentActivity,
    viewModel: AppViewModel
) {
    val userData      by viewModel.userData.collectAsStateWithLifecycle()
    val isLoggedIn    by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentMatch  by viewModel.currentMatch.collectAsStateWithLifecycle()

    val connectivityObserver = remember { ConnectivityObserver(activity) }
    val isOnline by connectivityObserver.isOnline.collectAsStateWithLifecycle(
        initialValue = NetworkHelper.isInternetAvailable(activity)
    )

    val navController = rememberNavController()

    // Start user listener on login
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
            viewModel.startUserListener(uid)
            NotificationHelper.initFCM(uid)
            InAppNotificationManager.listenForNotifications(activity, uid) { title, _ ->
                android.util.Log.d("FCM", "Notification: $title")
            }
        }
    }

    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Splash.route

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            enterTransition  = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)) +
                        fadeIn(tween(200))
            },
            exitTransition   = {
                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(280)) +
                        fadeOut(tween(150))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(280)) +
                        fadeIn(tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280)) +
                        fadeOut(tween(150))
            }
        ) {

            // ── SPLASH ──
            composable(Screen.Splash.route) {
                SplashScreen {
                    navController.navigate(Screen.Language.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }

            // ── LANGUAGE ──
            composable(Screen.Language.route) {
                LanguageScreen { isHindi ->
                    Lang.isHindi = isHindi
                    navController.navigate(Screen.Phone.route)
                }
            }

            // ── PHONE ──
            composable(Screen.Phone.route) {
                PhoneScreen(activity) { vid, phone ->
                    navController.navigate(Screen.Otp.createRoute(phone, vid))
                }
            }

            // ── OTP ──
            composable(Screen.Otp.route) { back ->
                val phone = back.arguments?.getString("phone") ?: ""
                val vid   = back.arguments?.getString("vid")   ?: ""
                OtpScreen(
                    phone     = phone,
                    vid       = vid,
                    onSuccess = { uid ->
                        viewModel.createUserIfNew(uid, phone)
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── MAIN ──
            composable(Screen.Main.route) {
                HomeScreen(
                    userData        = userData,
                    onMatchClick    = { match ->
                        viewModel.setCurrentMatch(match)
                        navController.navigate(Screen.Contest.route)
                    },
                    onWalletClick   = { navController.navigate(Screen.Wallet.route) },
                    onProfileClick  = { navController.navigate(Screen.Profile.route) },
                    onAdminClick    = { navController.navigate(Screen.Admin.route) },
                    onLeaderboardClick = { navController.navigate(Screen.Leaderboard.route) }
                )
            }

            // ── CONTEST ──
            composable(Screen.Contest.route) {
                ContestScreen(
                    matchTitle   = (currentMatch?.fullTitle() ?: ""),
                    matchId      = (currentMatch?.id ?: ""),
                    userData     = userData,
                    onBack       = { navController.popBackStack() },
                    onJoin       = { navController.popBackStack() },
                    onCreateTeam = { navController.navigate(Screen.TeamCreate.route) }
                )
            }

            // ── TEAM CREATE ──
            composable(Screen.TeamCreate.route) {
                TeamCreateScreen(
                    matchTitle  = (currentMatch?.fullTitle() ?: ""),
                    matchId     = (currentMatch?.id ?: ""),
                    onBack      = { navController.popBackStack() },
                    onTeamSaved = {
                        navController.navigate(Screen.TeamPreview.route) {
                            popUpTo(Screen.TeamCreate.route) { inclusive = false }
                        }
                    }
                )
            }

            // ── TEAM PREVIEW ── (team1/team2 removed — derived from matchTitle internally)
            composable(Screen.TeamPreview.route) {
                TeamPreviewScreen(
                    matchTitle    = (currentMatch?.fullTitle() ?: ""),
                    teamNumber    = 1,
                    onBack        = { navController.popBackStack() },
                    onEditTeam    = { navController.popBackStack() },
                    onJoinContest = {
                        navController.navigate(Screen.Contest.route) {
                            popUpTo(Screen.Contest.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── MY TEAMS ──
            composable(Screen.MyTeams.route) {
                MyTeamsScreen(
                    matchId       = (currentMatch?.id ?: ""),
                    matchTitle    = (currentMatch?.fullTitle() ?: ""),
                    onBack        = { navController.popBackStack() },
                    onCreateTeam  = { navController.navigate(Screen.TeamCreate.route) },
                    onJoinContest = { navController.navigate(Screen.Contest.route) },
                    onEditTeam    = { navController.navigate(Screen.TeamCreate.route) }
                )
            }

            // ── LIVE SCORE ──
            composable(Screen.LiveScore.route) {
                LiveScoreScreen(
                    matchId    = (currentMatch?.id ?: ""),
                    matchTitle = "${(currentMatch?.team1 ?: "")} vs ${(currentMatch?.team2 ?: "")}",
                    onBack     = { navController.popBackStack() }
                )
            }

            // ── WALLET ──
            composable(Screen.Wallet.route) {
                WalletScreen(
                    userData   = userData,
                    onBack     = { navController.popBackStack() },
                    onAddMoney = { navController.navigate(Screen.Payment.route) }
                )
            }

            // ── PAYMENT ──
            composable(Screen.Payment.route) {
                PaymentScreen(
                    userData = userData,
                    onBack   = { navController.popBackStack() }
                )
            }

            // ── PROFILE ──
            composable(Screen.Profile.route) {
                ProfileScreen(
                    userData   = userData,
                    onBack     = { navController.popBackStack() },
                    onLogout   = {
                        viewModel.logout()
                        navController.navigate(Screen.Language.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onKYC      = { navController.navigate(Screen.KYC.route) },
                    onWallet   = { navController.navigate(Screen.Wallet.route) },
                    onMyTeams  = { navController.navigate(Screen.MyTeams.route) }
                )
            }

            // ── ADMIN (owner-only protected) ──
            composable(Screen.Admin.route) {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (currentUid == OWNER_UID) {
                    AdminPanelScreen(onBack = { navController.popBackStack() })
                } else {
                    // Unauthorized — bounce back to main with message
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Admin.route) { inclusive = true }
                        }
                    }
                    AdminDeniedScreen()
                }
            }

            // ── LEADERBOARD ──
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(
                    matchTitle = "${(currentMatch?.team1 ?: "")} vs ${(currentMatch?.team2 ?: "")}",
                    onBack     = { navController.popBackStack() }
                )
            }

            // ── KYC ──
            composable(Screen.KYC.route) {
                KYCScreen(
                    userData = userData,
                    onBack   = { navController.popBackStack() }
                )
            }
        }

        // ── NO INTERNET OVERLAY ──
        AnimatedVisibility(
            visible = !isOnline,
            enter   = fadeIn(tween(300)),
            exit    = fadeOut(tween(300)),
            modifier= Modifier.fillMaxSize()
        ) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE000000)),
                contentAlignment = Alignment.Center
            ) {
                NoInternetScreen(onRetry = {})
            }
        }
    }
}

// ── Extension: safe match title ──
private fun MatchData.fullTitle(): String {
    val t1 = team1Full.ifEmpty { team1 }
    val t2 = team2Full.ifEmpty { team2 }
    return "$t1 vs $t2"
}

// ============================================================
// ADMIN DENIED SCREEN
// ============================================================

@Composable
private fun AdminDeniedScreen() {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔒", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Access Denied",
                color      = D11Red,
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Admin access is restricted.",
                color    = Color(0xFF888888),
                fontSize = 13.sp
            )
        }
    }
}

// ============================================================
// SPLASH SCREEN  (premium animated)
// ============================================================

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val logoScale  = remember { Animatable(0f) }
    val logoAlpha  = remember { Animatable(0f) }
    val textOffset = remember { Animatable(80f) }
    val textAlpha  = remember { Animatable(0f) }
    val bgAlpha    = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        bgAlpha.animateTo(1f, tween(250))
        logoAlpha.animateTo(1f, tween(300))
        logoScale.animateTo(
            1.15f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        logoScale.animateTo(1f, tween(200))
        textOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(400))
        delay(2200)
        onFinish()
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .alpha(bgAlpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0008), D11Red, Color(0xFF8B0000))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Logo
        Box(
            modifier         = Modifier
                .scale(logoScale.value)
                .alpha(logoAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter           = painterResource(id = R.drawable.ic_logo),
                contentDescription= "Logo",
                modifier          = Modifier.size(160.dp)
            )
        }

        // Bottom text + dots
        Column(
            modifier              = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = textOffset.value.dp)
                .alpha(textAlpha.value)
                .padding(bottom = 90.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                "DREAM11",
                color         = Color.White,
                fontSize      = 36.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )
            Text(
                "INDIA",
                color         = Color.White,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 10.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Fantasy Cricket",
                color         = Color(0xCCFFFFFF),
                fontSize      = 14.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(28.dp))
            LoadingDots()
        }
    }
}

@Composable
private fun LoadingDots() {
    val inf = rememberInfiniteTransition(label = "dots")
    val delays = listOf(0, 166, 332)
    val alphas = delays.map { d ->
        inf.animateFloat(
            initialValue   = 0.2f,
            targetValue    = 1f,
            animationSpec  = infiniteRepeatable(tween(500, delayMillis = d), RepeatMode.Reverse),
            label          = "dot$d"
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        alphas.forEach { a ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .alpha(a.value)
                    .background(Color.White)
            )
        }
    }
}

// ============================================================
// LANGUAGE SCREEN
// ============================================================

@Composable
fun LanguageScreen(onSelect: (Boolean) -> Unit) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(D11Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Image(
                painter           = painterResource(id = R.drawable.ic_logo),
                contentDescription= "Logo",
                modifier          = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "DREAM11 INDIA",
                color         = Color.White,
                fontSize      = 24.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Text("Fantasy Sports", color = D11Gray, fontSize = 14.sp)
            Spacer(Modifier.height(48.dp))
            Text(Lang.selectLang, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick  = { onSelect(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text("Hindi mein Khelein", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick  = { onSelect(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border   = BorderStroke(1.dp, D11Red),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text("Play in English", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================
// PHONE SCREEN
// ============================================================

@Composable
fun PhoneScreen(activity: ComponentActivity, onOtpSent: (String, String) -> Unit) {
    var phone   by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error   by remember { mutableStateOf("") }

    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(D11Black)
    ) {
        // Header
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .background(D11Red)
                .statusBarsPadding()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter           = painterResource(id = R.drawable.ic_logo),
                    contentDescription= "Logo",
                    modifier          = Modifier.size(48.dp)
                )
                Column {
                    Text("DREAM11", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("INDIA",   color = D11Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                }
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Spacer(Modifier.height(28.dp))
            Text(Lang.enterMobile, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(Lang.otpSent, color = D11Gray, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))

            // Phone input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (error.isNotEmpty()) Color.Red else D11Border,
                        RoundedCornerShape(8.dp)
                    )
                    .background(D11LightGray),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(D11CardBg)
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text("+91", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(D11Border))
                BasicTextField(
                    value         = phone,
                    onValueChange = {
                        if (it.length <= 10) {
                            phone = it
                            error = ""
                        }
                    },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textStyle     = TextStyle(
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine      = true,
                    decorationBox   = { inner ->
                        if (phone.isEmpty()) Text("00000 00000", color = D11Gray, fontSize = 18.sp)
                        inner()
                    }
                )
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(error, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (phone.length != 10) { error = Lang.numError; return@Button }
                    loading = true
                    error   = ""
                    keyboard?.hide()
                    val fullPhone = "+91$phone"
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(c: PhoneAuthCredential) {
                            FirebaseAuth.getInstance().signInWithCredential(c)
                        }
                        override fun onVerificationFailed(e: FirebaseException) {
                            error   = e.message ?: "Verification failed"
                            loading = false
                        }
                        override fun onCodeSent(vid: String, t: PhoneAuthProvider.ForceResendingToken) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (phone.length == 10) D11Red else Color(0xFF555555)
                ),
                shape    = RoundedCornerShape(8.dp)
            ) {
                if (loading)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else
                    Text(Lang.getOtp, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ============================================================
// OTP SCREEN  (6-box UI + resend)
// ============================================================

@Composable
fun OtpScreen(
    phone:     String,
    vid:       String,
    onSuccess: (String) -> Unit,
    onBack:    () -> Unit
) {
    var otpDigits   by remember { mutableStateOf(List(6) { "" }) }
    var loading     by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf("") }
    var timer       by remember { mutableStateOf(60) }
    var canResend   by remember { mutableStateOf(false) }
    val focusers    = remember { List(6) { FocusRequester() } }
    val keyboard    = LocalSoftwareKeyboardController.current

    val otp = otpDigits.joinToString("")

    LaunchedEffect(Unit) {
        focusers[0].requestFocus()
        while (timer > 0) { delay(1000); timer-- }
        canResend = true
    }

    fun verify() {
        if (otp.length != 6) { error = Lang.otpError; return }
        loading = true
        error   = ""
        keyboard?.hide()
        val cred = PhoneAuthProvider.getCredential(vid, otp)
        FirebaseAuth.getInstance().signInWithCredential(cred)
            .addOnSuccessListener { result -> onSuccess(result.user?.uid ?: "") }
            .addOnFailureListener { loading = false; error = Lang.wrongOtp }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(D11Black)
    ) {
        // Header
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .background(D11Red)
                .statusBarsPadding()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(Lang.verifyOtp, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(Lang.otpSentTo, color = D11Gray, fontSize = 13.sp)
            Text(phone, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))

            // Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (!canResend) "Resend in ${timer}s" else "Didn't receive OTP?",
                    color    = if (!canResend) D11Yellow else D11Gray,
                    fontSize = 12.sp
                )
                if (canResend) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Resend",
                        color      = D11Red,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier   = Modifier.clickable {
                            // Reset timer — resend logic can be added here
                            timer     = 60
                            canResend = false
                            error     = ""
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // 6-box OTP input
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                otpDigits.forEachIndexed { i, digit ->
                    OtpBox(
                        digit        = digit,
                        isFocused    = false,
                        focusRequester = focusers[i],
                        onValueChange  = { v ->
                            val cleaned = v.filter { it.isDigit() }.take(1)
                            val newList = otpDigits.toMutableList()
                            newList[i] = cleaned
                            otpDigits = newList
                            error = ""
                            if (cleaned.isNotEmpty() && i < 5) focusers[i + 1].requestFocus()
                            if (i == 5 && cleaned.isNotEmpty()) verify()
                        },
                        onBackspace    = {
                            val newList = otpDigits.toMutableList()
                            if (newList[i].isEmpty() && i > 0) {
                                newList[i - 1] = ""
                                otpDigits = newList
                                focusers[i - 1].requestFocus()
                            } else {
                                newList[i] = ""
                                otpDigits = newList
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick  = { verify() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (otp.length == 6) D11Red else Color(0xFF555555)
                ),
                shape    = RoundedCornerShape(8.dp),
                enabled  = !loading
            ) {
                if (loading)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else
                    Text(Lang.verify, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "← ${Lang.back}",
                color      = D11Red,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.clickable { onBack() }
            )
        }
    }
}

// ── Single OTP digit box ──
@Composable
private fun OtpBox(
    digit:          String,
    isFocused:      Boolean,
    focusRequester: FocusRequester,
    onValueChange:  (String) -> Unit,
    onBackspace:    () -> Unit,
    modifier:       Modifier = Modifier
) {
    BasicTextField(
        value         = digit,
        onValueChange = onValueChange,
        modifier      = modifier
            .focusRequester(focusRequester)
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(D11LightGray)
            .border(
                1.5.dp,
                if (digit.isNotEmpty()) D11Red else D11Border,
                RoundedCornerShape(8.dp)
            ),
        textStyle     = TextStyle(
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine      = true,
        decorationBox   = { inner ->
            Box(contentAlignment = Alignment.Center) { inner() }
        }
    )
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

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1A1A00))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = if (h > 0) "${h}h ${m}m" else "${m}m ${s}s",
            color      = if (h == 0 && m < 30) D11Red else D11Yellow,
            fontSize   = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}