package com.example.dream11india

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val team: String,
    val points: Float,
    val prize: String,
    val isCurrentUser: Boolean = false
)

@Composable
fun LeaderboardScreen(
    matchTitle: String = "IND vs AUS",
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("live") }

    val entries = listOf(
        LeaderboardEntry(1, "Rahul K.", "Team Eagle", 892.5f, "₹25,000", false),
        LeaderboardEntry(2, "Priya S.", "Super Kings", 876.0f, "₹10,000", false),
        LeaderboardEntry(3, "Amit T.", "Thunder Bolts", 865.5f, "₹5,000", false),
        LeaderboardEntry(4, "Sneha M.", "Dream Team", 854.0f, "₹2,000", false),
        LeaderboardEntry(5, "Vikram R.", "Power Play", 843.5f, "₹1,000", false),
        LeaderboardEntry(6, "You", "My Team 1", 831.0f, "₹500", true),
        LeaderboardEntry(7, "Kiran P.", "Warriors", 820.0f, "₹200", false),
        LeaderboardEntry(8, "Deepak S.", "Champions", 812.5f, "₹100", false),
        LeaderboardEntry(9, "Meera J.", "Blazers", 798.0f, "₹0", false),
        LeaderboardEntry(10, "Arjun V.", "Titans", 785.5f, "₹0", false),
        LeaderboardEntry(11, "Pooja R.", "Strikers", 774.0f, "₹0", false),
        LeaderboardEntry(12, "Suresh K.", "Challengers", 762.5f, "₹0", false),
    )

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", color = D11White, fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(matchTitle, color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Text("Leaderboard", color = Color(0xFFFFCDD2), fontSize = 12.sp)
            }
        }

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("live" to "🔴 Live", "winners" to "🏆 Winners", "my_rank" to "👤 My Rank")
                .forEach { (tab, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = tab }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            color = if (selectedTab == tab) D11Yellow else D11Gray,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                        if (selectedTab == tab) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Yellow))
                        }
                    }
                }
        }

        // Top 3 Podium
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1A0A00))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd Place
            PodiumItem(entry = entries[1], height = 80.dp, color = Color(0xFFAAAAAA))
            // 1st Place
            PodiumItem(entry = entries[0], height = 100.dp, color = D11Yellow)
            // 3rd Place
            PodiumItem(entry = entries[2], height = 60.dp, color = Color(0xFFCD7F32))
        }

        HorizontalDivider(color = D11Border, thickness = 0.5.dp)

        // Full Leaderboard
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                LeaderboardCard(entry = entry, index = index)
            }
        }
    }
}

@Composable
fun PodiumItem(entry: LeaderboardEntry, height: androidx.compose.ui.unit.Dp, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            when (entry.rank) {
                1 -> "👑"
                2 -> "🥈"
                else -> "🥉"
            },
            fontSize = 24.sp
        )
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape)
                .background(D11LightGray)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.name.take(2).uppercase(), color = D11White, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(entry.name, color = D11White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("${entry.points} pts", color = D11Gray, fontSize = 11.sp)
        Text(entry.prize, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier.width(70.dp).height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("#${entry.rank}", color = color, fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun LeaderboardCard(entry: LeaderboardEntry, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCurrentUser) Color(0xFF1A0A00) else D11CardBg
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                // Rank
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(
                            when (entry.rank) {
                                1 -> D11Yellow
                                2 -> Color(0xFFAAAAAA)
                                3 -> Color(0xFFCD7F32)
                                else -> D11LightGray
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "#${entry.rank}",
                        color = if (entry.rank <= 3) D11Black else D11White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Avatar
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (entry.isCurrentUser) D11Red else D11LightGray)
                        .border(
                            2.dp,
                            if (entry.isCurrentUser) D11Yellow else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entry.name.take(2).uppercase(), color = D11White, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                }

                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.name, color = D11White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                        if (entry.isCurrentUser) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(D11Red)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("YOU", color = D11White, fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(entry.team, color = D11Gray, fontSize = 12.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.points} pts", color = D11White, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    entry.prize,
                    color = if (entry.prize == "₹0") D11Gray else D11Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}