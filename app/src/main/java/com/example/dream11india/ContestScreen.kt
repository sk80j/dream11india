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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ContestData(
    val name: String,
    val prize: String,
    val spots: String,
    val spotsLeft: String,
    val fillPercent: Int,
    val entryFee: String,
    val isFree: Boolean,
    val isHot: Boolean,
    val winners: String,
    val firstPrize: String,
    val isGuaranteed: Boolean = false
)

@Composable
fun ContestScreen(
    matchTitle: String = "IND vs AUS",
    onBack: () -> Unit = {},
    onJoin: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("contests") }

    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    val contests = listOf(
        ContestData("Mega Contest", "â‚¹50 Crores", "1,20,000", "6,543",
            95, "â‚¹49", false, true, "30,000", "â‚¹1 Cr", true),
        ContestData("Hot Contest", "â‚¹25 Crores", "85,000", "18,234",
            78, "â‚¹29", false, true, "15,000", "â‚¹50 Lakh", true),
        ContestData("Small League", "â‚¹10 Crores", "45,000", "24,750",
            45, "â‚¹19", false, false, "8,000", "â‚¹20 Lakh", false),
        ContestData("Free Contest", "â‚¹1 Lakh", "25,000", "20,000",
            20, "FREE", true, false, "5,000", "â‚¹10,000", false),
        ContestData("Head to Head", "â‚¹90", "2", "1",
            50, "â‚¹49", false, false, "1", "â‚¹90", false),
        ContestData("Practice Contest", "No Prize", "50,000", "35,000",
            30, "FREE", true, false, "0", "No Prize", false),
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {

        // TOP SCORE CARD
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFF1A1A2E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Back + Prize
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("â†", color = D11White, fontSize = 24.sp,
                            modifier = Modifier.clickable { onBack() })
                        Column {
                            Text("â‚¹50 Crores", color = D11White, fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("â‚¹49", color = D11Gray, fontSize = 13.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .border(1.dp, D11Gray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("?", color = D11White, fontSize = 14.sp) }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF333355))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("PTS", color = D11White, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Teams Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team 1
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color(0xFF003366)),
                            contentAlignment = Alignment.Center) {
                            Text(team1.take(3).uppercase(), color = D11White,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(team1, color = D11White, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                            Text("186/5 (20)", color = D11White, fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Center
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(D11Green)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("LIVE", color = D11White, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }

                    // Team 2
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(team2, color = D11White, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                            Text("142/3 (14)", color = D11White, fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(Color(0xFF006600)),
                            contentAlignment = Alignment.Center) {
                            Text(team2.take(3).uppercase(), color = D11White,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF004400))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("â— LIVE", color = D11Green, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Need 45 runs in 30 balls", color = D11White, fontSize = 12.sp)
                }
            }
        }

        // TABS
        Row(
            modifier = Modifier.fillMaxWidth().background(D11White),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "contests" to "Contests",
                "my_teams" to "My Teams",
                "leaderboard" to "Leaderboard",
                "commentary" to "Commentary",
                "scorecard" to "Scorecard"
            ).forEach { (tab, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text(
                        label,
                        color = if (selectedTab == tab) D11Red else Color(0xFF666666),
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold
                        else FontWeight.Normal
                    )
                    if (selectedTab == tab) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Red))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        when (selectedTab) {
            "contests" -> ContestsList(contests = contests, onJoin = onJoin)
            "leaderboard" -> LeaderboardTab()
            else -> ContestsList(contests = contests, onJoin = onJoin)
        }
    }
}

@Composable
fun ContestsList(contests: List<ContestData>, onJoin: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A2E))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ðŸ† 1 CROREPATI", color = D11Yellow,
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("7 LAKHPATIS", color = D11White,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("AFG vs AUS â†’", color = D11Gray, fontSize = 12.sp)
                }
            }
        }

        // Filter tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Mega", "Small", "Free", "H2H").forEach { filter ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (filter == "All") D11Red else Color(0xFFEEEEEE))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(filter,
                            color = if (filter == "All") D11White else Color(0xFF333333),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(contests) { contest ->
            ContestCard(contest = contest, onJoin = { onJoin(contest.name) })
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ContestCard(contest: ContestData, onJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = D11White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (contest.isGuaranteed) {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("âœ“", color = D11Green, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                            Text("Guaranteed", color = D11Green, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    if (contest.isHot) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF6B35))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) { Text("ðŸ”¥ HOT", color = D11White, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold) }
                    }
                    if (contest.isFree) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) { Text("FREE", color = D11Green, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold) }
                    }
                }

                // Entry Fee Button
                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (contest.isFree) D11Green else D11Red
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(contest.entryFee, color = D11White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prize + Spots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Prize Pool", color = Color(0xFF888888), fontSize = 11.sp)
                    Text(contest.prize, color = Color(0xFF111111),
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Spots", color = Color(0xFF888888), fontSize = 11.sp)
                    Text(contest.spots, color = Color(0xFF111111),
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${contest.spotsLeft} spots left",
                    color = Color(0xFF666666), fontSize = 11.sp)
                Text("${contest.fillPercent}% filled",
                    color = Color(0xFF666666), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFEEEEEE))
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
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Winners + 1st Prize
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("ðŸ†", fontSize = 12.sp)
                    Text("Winners: ${contest.winners}",
                        color = Color(0xFF666666), fontSize = 11.sp)
                }
                Text("1st: ${contest.firstPrize}",
                    color = D11Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LeaderboardTab() {
    val entries = listOf(
        Triple("Rahul K.", 892.5f, "â‚¹25,000"),
        Triple("Priya S.", 876.0f, "â‚¹10,000"),
        Triple("Amit T.", 865.5f, "â‚¹5,000"),
        Triple("You", 831.0f, "â‚¹500"),
        Triple("Vikram R.", 820.0f, "â‚¹200"),
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {
        // Points info
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(D11White)
                .padding(12.dp)
        ) {
            Text("Points last updated at 14.0 overs",
                color = D11Red, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        }

        // Compare / Download row
        Row(
            modifier = Modifier.fillMaxWidth().background(D11White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("âš–ï¸", fontSize = 20.sp)
                Text("Compare", color = Color(0xFF666666), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("â¬‡ï¸", fontSize = 20.sp)
                Text("Download", color = Color(0xFF666666), fontSize = 11.sp)
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE))

        Row(
            modifier = Modifier.fillMaxWidth().background(D11White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("All Teams (61,50,405)", color = Color(0xFF333333),
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Points", color = Color(0xFF666666), fontSize = 12.sp)
            Text("Rank", color = Color(0xFF666666), fontSize = 12.sp)
        }

        LazyColumn {
            items(entries) { (name, points, prize) ->
                val isYou = name == "You"
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isYou) Color(0xFFFFFDE7) else D11White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (isYou) D11Red else Color(0xFFEEEEEE)),
                            contentAlignment = Alignment.Center) {
                            Text(name.take(2).uppercase(),
                                color = if (isYou) D11White else Color(0xFF333333),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(name, color = Color(0xFF111111),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(prize, color = D11Green, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("$points", color = Color(0xFF111111),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
            }
        }
    }
}
