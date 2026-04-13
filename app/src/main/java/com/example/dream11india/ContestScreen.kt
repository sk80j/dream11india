package com.example.dream11india

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContestScreen(
    matchTitle: String = "IND vs AUS",
    onBack: () -> Unit = {},
    onJoin: (String) -> Unit = {}
) {
    val contests = listOf(
        ContestData("Mega Contest", "₹50 Cr", "1,20,000", 95, "₹49", false, true),
        ContestData("Hot Contest", "₹25 Cr", "85,000", 78, "₹29", false, true),
        ContestData("Small League", "₹10 Cr", "45,000", 45, "₹19", false, false),
        ContestData("Free Contest", "₹1 Lakh", "25,000", 20, "FREE", true, false),
        ContestData("Head to Head", "₹10K", "2", 50, "₹5K", false, false),
        ContestData("Practice Contest", "No Prize", "50,000", 30, "FREE", true, false),
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
                Text(matchTitle, color = D11White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Contests", color = Color(0xFFFFCDD2), fontSize = 12.sp)
            }
        }

        // Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("All", "Mega", "Small", "Free", "H2H").forEach { filter ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (filter == "All") D11Red else D11LightGray)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(filter, color = D11White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Contest List
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(contests) { contest ->
                ContestCard(contest = contest, onJoin = { onJoin(contest.name) })
            }
        }
    }
}

data class ContestData(
    val name: String,
    val prize: String,
    val spots: String,
    val fillPercent: Int,
    val entryFee: String,
    val isFree: Boolean,
    val isHot: Boolean
)

@Composable
fun ContestCard(contest: ContestData, onJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = D11CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(contest.name, color = D11White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (contest.isHot) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(D11Red).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("HOT", color = D11White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (contest.isFree) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(D11Green).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("FREE", color = D11White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (contest.isFree) D11Green else D11Red
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(contest.entryFee, color = D11White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Prize Pool", color = D11Gray, fontSize = 11.sp)
                    Text(contest.prize, color = D11White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Spots", color = D11Gray, fontSize = 11.sp)
                    Text(contest.spots, color = D11White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fill Progress
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${contest.fillPercent}% Full", color = D11Gray, fontSize = 11.sp)
                Text("${100 - contest.fillPercent}% Left", color = D11Green, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(D11LightGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(contest.fillPercent.toFloat() / 100f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(D11Green)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = D11Border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🏆 Winners: ${(contest.spots.replace(",","").toIntOrNull() ?: 0) / 5}",
                    color = D11Gray, fontSize = 11.sp)
                Text("💰 1st Prize: ₹${contest.prize}", color = D11Yellow, fontSize = 11.sp)
            }
        }
    }
}