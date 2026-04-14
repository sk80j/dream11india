package com.example.dream11india

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NetworkHelper {
    fun isInternetAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) { false }
    }
}

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(40.dp))
                .background(D11LightGray), contentAlignment = Alignment.Center) {
                Text("!", color = D11Red, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("No Internet Connection", color = D11White, fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("Please check your connection and try again.",
                color = D11Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp).fillMaxWidth(0.6f)) {
                Text("Retry", color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: String = "?",
    title: String,
    subtitle: String,
    buttonText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(40.dp))
                .background(D11LightGray), contentAlignment = Alignment.Center) {
                Text(icon, color = D11Gray, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
            Text(title, color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(subtitle, color = D11Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
            if (buttonText != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(46.dp)) {
                    Text(buttonText, color = D11White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ShimmerCard(height: Int = 120, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha = infiniteTransition.animateFloat(0.3f, 0.9f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
    Box(modifier = modifier.fillMaxWidth().height(height.dp)
        .clip(RoundedCornerShape(12.dp)).alpha(alpha.value).background(D11CardBg))
}

@Composable
fun ShimmerList(count: Int = 4, height: Int = 120) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(12.dp)) {
        repeat(count) { ShimmerCard(height = height) }
    }
}

@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = D11Red,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.97f else 1f, label = "scale")
    Button(
        onClick = { if (!isLoading && enabled) onClick() },
        modifier = modifier.height(52.dp).scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled && !isLoading) color else Color(0xFF444444)),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled && !isLoading,
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        if (isLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = D11White,
                    modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Please wait...", color = D11White, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(text, color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

object CrashHandler {
    fun setup() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                Log.e("CRASH", e.stackTraceToString())
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("crash_logs")
                    .add(mapOf(
                        "error" to (e.message ?: "Unknown"),
                        "stackTrace" to e.stackTraceToString().take(1000),
                        "timestamp" to System.currentTimeMillis()
                    ))
            } catch (ex: Exception) { }
        }
    }
}

object ErrorMessages {
    fun getFirestoreError(msg: String): String = when {
        msg.contains("permission") -> "Access denied. Please login again."
        msg.contains("not-found") -> "Data not found. Please refresh."
        msg.contains("unavailable") -> "Server unavailable. Check internet."
        else -> "Something went wrong. Please try again."
    }
    fun getNetworkError() = "No internet. Please check your connection."
    fun getBalanceError(required: Int, available: Int) =
        "Insufficient balance! Need Rs.$required, have Rs.$available."
    fun getKycError() = "Complete KYC verification to withdraw money."
    fun getContestFullError() = "Contest is full! Try another contest."
    fun getAlreadyJoinedError() = "Already joined this contest with this team!"
}

object ClickGuard {
    private var lastClickTime = 0L
    private val minInterval = 1500L
    fun canClick(): Boolean {
        val now = System.currentTimeMillis()
        return if (now - lastClickTime >= minInterval) {
            lastClickTime = now
            true
        } else false
    }
}
