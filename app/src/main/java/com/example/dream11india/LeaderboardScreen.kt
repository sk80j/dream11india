package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val points: Float,
    val prize: String,
    val teamName: String = "T1",
    val isYou: Boolean = false
)

@Composable
fun LeaderboardScreen(
    matchTitle: String = "RR vs MI",
    onBack: () -> Unit = {}
) {
    val entries = listOf(
        LeaderboardEntry(1, "Rahul Kumar", 982.5f, "Rs.1,00,00,000", "T1"),
        LeaderboardEntry(2, "Priya Sharma", 976.0f, "Rs.50,00,000", "T1"),
        LeaderboardEntry(3, "Amit Tiwari", 965.5f, "Rs.25,00,000", "T1"),
        LeaderboardEntry(4, "Sneha Patel", 954.0f, "Rs.10,00,000", "T1"),
        LeaderboardEntry(5, "Vikram Rao", 948.5f, "Rs.5,00,000", "T1"),
        LeaderboardEntry(6, "You", 831.0f, "Rs.500", "T1", isYou = true),
        LeaderboardEntry(7, "Neha Singh", 820.0f, "Rs.200", "T1"),
        LeaderboardEntry(8, "Arjun Mehta", 815.5f, "Rs.200", "T1"),
        LeaderboardEntry(9, "Kavya Reddy", 810.0f, "Rs.100", "T1"),
        LeaderboardEntry(10, "Raj Verma", 805.5f, "Rs.100", "T1"),
    )

    val top3 = entries.take(3)
    val rest = entries.drop(3)
    val myEntry = entries.find { it.isYou }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        // TOP BAR
        Row(modifier = Modifier.fillMaxWidth().background(D11Red)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = D11White, fontSize = 24.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.width(8.dp))
                Image(painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo", modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Leaderboard", color = D11White, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 12.sp)
                }
            }
            Text("${entries.size} Teams", color = D11White, fontSize = 13.sp)
        }

        // MY RANK BAR
        myEntry?.let {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1500))
                .padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(D11Red),
                            contentAlignment = Alignment.Center) {
                            Text("#${it.rank}", color = D11White, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Your Rank", color = D11Gray, fontSize = 11.sp)
                            Text("${it.points} pts", color = D11Yellow, fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Your Winnings", color = D11Gray, fontSize = 11.sp)
                        Text(it.prize, color = D11Green, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)) {

            // PODIUM
            item {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF0D0D2E))
                    .padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Top Winners", color = D11White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Podium
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom) {

                            // 2nd Place
                            if (top3.size > 1) {
                                PodiumCard(entry = top3[1], height = 100.dp,
                                    color = Color(0xFFC0C0C0))
                            }

                            // 1st Place
                            if (top3.isNotEmpty()) {
                                PodiumCard(entry = top3[0], height = 130.dp,
                                    color = D11Yellow)
                            }

                            // 3rd Place
                            if (top3.size > 2) {
                                PodiumCard(entry = top3[2], height = 80.dp,
                                    color = Color(0xFFCD7F32))
                            }
                        }
                    }
                }
            }

            // HEADER
            item {
                Row(modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF0D0D0D))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("All Teams (${entries.size})", color = D11White,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Text("Points", color = D11Gray, fontSize = 12.sp)
                        Text("Rank", color = D11Gray, fontSize = 12.sp)
                    }
                }
            }

            // LIST
            items(entries) { entry ->
                LeaderboardRow(entry = entry)
                HorizontalDivider(color = D11Border, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun PodiumCard(entry: LeaderboardEntry, height: androidx.compose.ui.unit.Dp, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(52.dp).clip(CircleShape)
            .background(color),
            contentAlignment = Alignment.Center) {
            Text(entry.name.take(2).uppercase(), color = D11Black,
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(entry.name.take(8), color = D11White, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("${entry.points}", color = color, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
        Text(entry.prize, color = D11Green, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.width(80.dp).height(height)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(color.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center) {
            Text("#${entry.rank}", color = color, fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun LeaderboardRow(entry: LeaderboardEntry) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(if (entry.isYou) Color(0xFF1A1500) else Color(0xFF1A1A1A))
        .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(if (entry.isYou) D11Red else Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center) {
                Text(entry.name.take(2).uppercase(), color = D11White,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (entry.isYou) "You" else entry.name,
                        color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (entry.isYou) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(D11Red)
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("YOU", color = D11White, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(entry.prize, color = D11Green, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("${entry.points}", color = D11White, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
            Text("#${entry.rank}", color = if (entry.isYou) D11Yellow else D11Gray,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}