package com.example.dream11india

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// MATCH DATA
// ─────────────────────────────────────────────

data class MatchData(
    val id:                 String  = "",
    val team1:              String  = "",
    val team2:              String  = "",
    val team1Full:          String  = "",
    val team2Full:          String  = "",
    val team1Logo:          String  = "",
    val team2Logo:          String  = "",
    val type:               String  = "T20",
    val league:             String  = "",
    val matchTime:          String  = "",
    val hoursLeft:          Int     = 0,
    val minutesLeft:        Int     = 0,
    val prize:              String  = "₹0",
    val spots:              String  = "1000",
    val fillPercent:        Int     = 0,
    val badge:              String  = "",
    val team1Color:         Color   = Color(0xFF003580),
    val team2Color:         Color   = Color(0xFF006400),
    val featuredPlayer:     String  = "",
    val featuredPlayerPhoto:String  = "",
    val venue:              String  = "",
    val tossInfo:           String  = "",
    val status:             String  = "",
    val isLive:             Boolean = false,
    val isCompleted:        Boolean = false,
    val score1:             String  = "",
    val score2:             String  = "",
    val entryFee:           Int     = 49,
    val matchStarted:       Boolean = false
)

// ─────────────────────────────────────────────
// USER DATA
// ─────────────────────────────────────────────

data class UserData(
    val uid:              String  = "",
    val phone:            String  = "",
    val name:             String  = "Player",
    val avatarUrl:        String  = "",
    val balance:          Int     = 0,
    val winnings:         Int     = 0,
    val bonusBalance:     Int     = 0,
    val matchesPlayed:    Int     = 0,
    val teamsCreated:     Int     = 0,
    val joinedContests:   Int     = 0,
    val totalDeposits:    Int     = 0,
    val totalWithdrawals: Int     = 0,
    val referralCode:     String  = "",
    val kycStatus:        String  = "none",
    val fcmToken:         String  = "",
    val isAdmin:          Boolean = false,
    val isBlocked:        Boolean = false,
    val walletFrozen:     Boolean = false,
    val createdAt:        Long    = 0L
)

// ─────────────────────────────────────────────
// TRANSACTION
// ─────────────────────────────────────────────

data class Transaction(
    val id:          String = "",
    val userId:      String = "",
    val type:        String = "",
    val amount:      Int    = 0,
    val description: String = "",
    val timestamp:   Long   = 0L,
    val status:      String = "completed",
    val orderId:     String = "",
    val balanceAfter:Int    = 0
)

// ─────────────────────────────────────────────
// WITHDRAW REQUEST
// ─────────────────────────────────────────────

data class WithdrawRequest(
    val id:          String = "",
    val userId:      String = "",
    val amount:      Int    = 0,
    val upiId:       String = "",
    val upiName:     String = "",
    val status:      String = "pending",
    val createdAt:   Long   = 0L,
    val processedAt: Long   = 0L,
    val remarks:     String = ""
)

// ─────────────────────────────────────────────
// NOTIFICATION
// ─────────────────────────────────────────────

data class NotificationData(
    val id:        String  = "",
    val userId:    String  = "",
    val title:     String  = "",
    val body:      String  = "",
    val type:      String  = "general",
    val isRead:    Boolean = false,
    val timestamp: Long    = 0L,
    val deepLink:  String  = ""
)

// ─────────────────────────────────────────────
// PLAYER
// ─────────────────────────────────────────────

data class Player(
    val id:               String  = "",
    val name:             String  = "",
    val shortName:        String  = "",
    val team:             String  = "",
    val role:             String  = "BAT",
    val credits:          Float   = 9.0f,
    val selectionPercent: Int     = 0,
    val points:           Float   = 0f,
    val isSelected:       Boolean = false,
    val isCaptain:        Boolean = false,
    val isViceCaptain:    Boolean = false,
    val imageUrl:         String  = "",
    val isPlaying:        Boolean = true
)

// ─────────────────────────────────────────────
// PROMO CODE
// ─────────────────────────────────────────────

data class PromoCode(
    val code:       String  = "",
    val type:       String  = "flat",
    val value:      Int     = 0,
    val maxUses:    Int     = 100,
    val usedCount:  Int     = 0,
    val isActive:   Boolean = true,
    val minDeposit: Int     = 0,
    val expiresAt:  Long    = 0L
)

// ─────────────────────────────────────────────
// SAMPLE MATCHES
// ─────────────────────────────────────────────

val sampleMatches = listOf(
    MatchData("1","IND","AUS","India","Australia","","","ODI","ODI Champions Cup",
        "Tomorrow, 2:30 PM",14,30,"₹55 Crores","1,20,000",95,"MEGA",
        Color(0xFF003580),Color(0xFF006400),entryFee=49),
    MatchData("2","MI","CSK","Mumbai Indians","Chennai Super Kings","","","T20","IPL 2026",
        "Today, 7:30 PM",3,15,"₹25 Crores","85,000",78,"HOT",
        Color(0xFF004BA0),Color(0xFFFFD700),entryFee=29),
    MatchData("3","RCB","KKR","Royal Challengers","Kolkata Knight Riders","","","T20","IPL 2026",
        "Tomorrow, 7:30 PM",20,0,"₹10 Crores","45,000",45,"",
        Color(0xFFCC0000),Color(0xFF3A015C),entryFee=19),
    MatchData("4","DC","SRH","Delhi Capitals","Sunrisers Hyderabad","","","T20","IPL 2026",
        "Tomorrow, 3:30 PM",16,30,"₹5 Crores","25,000",20,"FREE",
        Color(0xFF0078D4),Color(0xFFFF6600),entryFee=0)
)

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────

object ErrorMessages {
    fun getBalanceError(required: Int, available: Int) =
        "Insufficient balance! Need ₹$required, available ₹$available."
    fun getAlreadyJoinedError() = "You have already joined this contest!"
    fun getContestFullError()   = "Contest is full! No spots available."
    fun getFirestoreError(msg: String) = "Something went wrong. Please try again."
    fun getNetworkError()       = "No internet connection. Check your network."
    fun getKycError()           = "KYC verification required before withdrawing."
    fun getTeamIncompleteError()= "Please select exactly 11 players."
    fun getWalletFrozenError()  = "Your wallet is temporarily frozen. Contact support."
}

object ClickGuard {
    private var lastClickTime = 0L
    fun canClick(debounceMs: Long = 800L): Boolean {
        val now = System.currentTimeMillis()
        return if (now - lastClickTime > debounceMs) { lastClickTime = now; true } else false
    }
}

// ─────────────────────────────────────────────
// LOADING BUTTON
// ─────────────────────────────────────────────

@Composable
fun LoadingButton(
    text:      String,
    isLoading: Boolean,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
    color:     Color    = D11Red,
    enabled:   Boolean  = true
) {
    Button(
        onClick  = { if (!isLoading && enabled) onClick() },
        modifier = modifier.height(48.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = if (enabled) color else Color(0xFF444444),
            disabledContainerColor = Color(0xFF444444)
        ),
        shape   = RoundedCornerShape(10.dp),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}
