package com.example.dream11india

import androidx.compose.ui.graphics.Color

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
    val isAdmin: Boolean = false,
    val kycStatus: String = "none",
    val fcmToken: String = ""
)

// ===== SAMPLE MATCHES =====
val sampleMatches = listOf(
    MatchData("1","IND","AUS","India","Australia","","",
        "ODI","ODI Champions Cup","Tomorrow, 2:30 PM",14,30,
        "Rs.55 Crores","1,20,000",95,"MEGA",
        Color(0xFF003580), Color(0xFF006400)),
    MatchData("2","MI","CSK","Mumbai Indians","Chennai Super Kings","","",
        "T20","IPL 2026","Today, 7:30 PM",3,15,
        "Rs.25 Crores","85,000",78,"HOT",
        Color(0xFF004BA0), Color(0xFFFFD700)),
    MatchData("3","RCB","KKR","Royal Challengers","Kolkata Knight Riders","","",
        "T20","IPL 2026","Tomorrow, 7:30 PM",20,0,
        "Rs.10 Crores","45,000",45,"",
        Color(0xFFCC0000), Color(0xFF3A015C)),
    MatchData("4","DC","SRH","Delhi Capitals","Sunrisers Hyderabad","","",
        "T20","IPL 2026","Tomorrow, 3:30 PM",16,30,
        "Rs.5 Crores","25,000",20,"FREE",
        Color(0xFF0078D4), Color(0xFFFF6600)),
)
