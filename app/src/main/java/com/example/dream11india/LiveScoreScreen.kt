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
import kotlinx.coroutines.delay

@Composable
fun LiveScoreScreen(
    matchId: String = "1",
    matchTitle: String = "RR vs MI",
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("live") }
    var balls by remember { mutableStateOf(listOf(
        "4", "1", "W", "0", "6", "2", "1", "4", "0", "1",
        "W", "6", "1", "0", "4", "2", "1", "6", "0", "4"
    )) }

    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        // TOP BAR
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D2E))
            .statusBarsPadding()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("←", color = D11White, fontSize = 24.sp,
                            modifier = Modifier.clickable { onBack() })
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo", modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(matchTitle, color = D11White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(D11Green)
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("LIVE", color = D11White, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(team1, color = D11Gray, fontSize = 13.sp)
                        Text("186/5", color = D11White, fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text("(20.0 overs)", color = D11Gray, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("vs", color = D11Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("T20", color = D11Gray, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(team2, color = D11Gray, fontSize = 13.sp)
                        Text("142/3", color = D11White, fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text("(14.0 overs)", color = D11Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("$team1 need 45 runs in 36 balls",
                    color = D11Yellow, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }

        // TABS
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                "live" to "Live",
                "scorecard" to "Scorecard",
                "my_points" to "My Points",
                "commentary" to "Commentary"
            ).forEach { (tab, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedTab = tab }
                        .padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(label,
                        color = if (selectedTab == tab) D11Red else D11Gray,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold
                        else FontWeight.Normal)
                    if (selectedTab == tab) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Red))
                    }
                }
            }
        }

        HorizontalDivider(color = D11Border)

        when (selectedTab) {
            "live" -> LiveTab(balls = balls, team1 = team1, team2 = team2)
            "scorecard" -> ScorecardTab(team1 = team1, team2 = team2)
            "my_points" -> MyPointsTab()
            "commentary" -> CommentaryTab()
        }
    }
}

@Composable
fun LiveTab(balls: List<String>, team1: String, team2: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Ball by ball
        item {
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("This Over", color = D11White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        balls.takeLast(6).forEach { ball ->
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(when(ball) {
                                    "W" -> D11Red
                                    "4" -> Color(0xFF1565C0)
                                    "6" -> Color(0xFF2E7D32)
                                    "0" -> D11LightGray
                                    else -> Color(0xFF424242)
                                }),
                                contentAlignment = Alignment.Center) {
                                Text(ball, color = D11White, fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // Batting
        item {
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Batting - $team2", color = D11White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Batter", color = D11Gray, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("R", color = D11Gray, fontSize = 12.sp)
                            Text("B", color = D11Gray, fontSize = 12.sp)
                            Text("SR", color = D11Gray, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = D11Border, modifier = Modifier.padding(vertical = 8.dp))
                    listOf(
                        listOf("KL Rahul *", "68", "42", "161.9"),
                        listOf("Virat Kohli", "52", "38", "136.8")
                    ).forEach { batter ->
                        Row(modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(batter[0], color = D11White, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                listOf(batter[1], batter[2], batter[3]).forEach { stat ->
                                    Text(stat, color = D11White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bowling
        item {
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bowling - $team1", color = D11White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bowler", color = D11Gray, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("O", color = D11Gray, fontSize = 12.sp)
                            Text("R", color = D11Gray, fontSize = 12.sp)
                            Text("W", color = D11Gray, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = D11Border, modifier = Modifier.padding(vertical = 8.dp))
                    listOf(
                        listOf("Bumrah *", "3.0", "22", "2"),
                        listOf("Chahar", "4.0", "35", "1")
                    ).forEach { bowler ->
                        Row(modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(bowler[0], color = D11White, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                listOf(bowler[1], bowler[2], bowler[3]).forEach { stat ->
                                    Text(stat, color = D11White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScorecardTab(team1: String, team2: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("$team1 Innings - 186/5 (20.0)",
                        color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        listOf("Ruturaj Gaikwad", "72", "48", "150.0"),
                        listOf("Devon Conway", "45", "32", "140.6"),
                        listOf("MS Dhoni", "38", "22", "172.7"),
                        listOf("Jadeja", "21", "12", "175.0")
                    ).forEach { batter ->
                        Row(modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(batter[0], color = D11White, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(batter[1], batter[2], batter[3]).forEach { stat ->
                                    Text(stat, color = D11White, fontSize = 12.sp)
                                }
                            }
                        }
                        HorizontalDivider(color = D11Border, thickness = 0.3.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun MyPointsTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("My Points", color = D11Gray, fontSize = 13.sp)
                    Text("831.5", color = D11Yellow, fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("My Rank: #4", color = D11White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Player Points Breakdown",
                        color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        listOf("Virat Kohli (C)", "152.0", "2x"),
                        listOf("KL Rahul (VC)", "114.0", "1.5x"),
                        listOf("Bumrah", "85.5", "1x"),
                        listOf("Jadeja", "76.0", "1x"),
                        listOf("MS Dhoni", "68.5", "1x")
                    ).forEach { player ->
                        Row(modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(player[0], color = D11White, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(player[1], color = D11Yellow, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold)
                                Text(player[2], color = D11Green, fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = D11Border, thickness = 0.3.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun CommentaryTab() {
    val commentary = listOf(
        "14.2: Bumrah to KL Rahul, FOUR! Pulled away through mid-wicket",
        "14.1: Bumrah to KL Rahul, 1 run, worked to mid-on",
        "13.6: Chahar to Kohli, SIX! Over long-on! What a shot!",
        "13.5: Chahar to Kohli, no run, defended back",
        "13.4: Chahar to Kohli, FOUR! Through covers",
        "13.3: Chahar to Kohli, 1 run, pushed to mid-off",
        "13.2: Chahar to KL Rahul, 2 runs, driven to long-on",
        "13.1: Chahar to KL Rahul, no run, played to covers",
        "12.6: Jadeja to Kohli, WICKET! Caught at long-on!"
    )

    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(commentary) { comment ->
            Card(colors = CardDefaults.cardColors(containerColor = D11CardBg),
                shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(
                            when {
                                comment.contains("FOUR") -> Color(0xFF1565C0)
                                comment.contains("SIX") -> D11Green
                                comment.contains("WICKET") -> D11Red
                                else -> D11Gray
                            }
                        ).align(Alignment.CenterVertically))
                    Text(comment, color = D11White, fontSize = 13.sp,
                        lineHeight = 18.sp)
                }
            }
        }
    }
}