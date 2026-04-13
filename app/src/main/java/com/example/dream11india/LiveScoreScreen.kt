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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BallEvent(
    val over: String,
    val ball: String,
    val runs: Int,
    val isWicket: Boolean,
    val isBoundary: Boolean,
    val isSix: Boolean,
    val batsman: String,
    val bowler: String
)

@Composable
fun LiveScoreScreen(
    matchId: String = "",
    matchTitle: String = "IND vs AUS",
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    // Match detail from API
    var matchDetail by remember { mutableStateOf<MatchDetail?>(null) }
    var myPoints by remember { mutableStateOf(245.5f) }
    var myRank by remember { mutableStateOf(1243) }

    val sampleBalls = listOf(
        BallEvent("18", "4", 4, false, true, false, "V Kohli", "P Cummins"),
        BallEvent("18", "3", 1, false, false, false, "V Kohli", "P Cummins"),
        BallEvent("18", "2", 0, false, false, false, "KL Rahul", "P Cummins"),
        BallEvent("18", "1", 6, false, false, true, "V Kohli", "P Cummins"),
        BallEvent("17", "6", 1, false, false, false, "KL Rahul", "M Starc"),
        BallEvent("17", "5", 4, false, true, false, "V Kohli", "M Starc"),
        BallEvent("17", "4", 0, true, false, false, "SK Yadav", "M Starc"),
        BallEvent("17", "3", 2, false, false, false, "SK Yadav", "M Starc"),
    )

    // Load match info from API
    LaunchedEffect(matchId) {
        scope.launch {
            try {
                if (matchId.isNotEmpty()) {
                    val response = CricApi.service.getMatchInfo(matchId = matchId)
                    matchDetail = response.data
                }
                isLoading = false
            } catch (e: Exception) {
                errorMsg = "Score load nahi hua"
                isLoading = false
            }
        }
    }

    // Auto refresh every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            try {
                if (matchId.isNotEmpty()) {
                    val response = CricApi.service.getMatchInfo(matchId = matchId)
                    matchDetail = response.data
                }
                myPoints += (1..5).random().toFloat()
                if (myRank > 1) myRank -= (0..3).random()
            } catch (e: Exception) { }
        }
    }

    // Display data — API se aaya ya sample
    val team1 = matchDetail?.teams?.getOrElse(0) { "IND" } ?: "IND"
    val team2 = matchDetail?.teams?.getOrElse(1) { "AUS" } ?: "AUS"
    val score1 = matchDetail?.score?.getOrNull(0)
    val score2 = matchDetail?.score?.getOrNull(1)
    val team1Score = if (score1 != null) "${score1.r}/${score1.w} (${score1.o})" else "0/0"
    val team2Score = if (score2 != null) "${score2.r}/${score2.w} (${score2.o})" else "Yet to bat"
    val matchStatus = matchDetail?.status ?: "Live"
    val tossInfo = if (matchDetail?.tossWinner?.isNotEmpty() == true)
        "${matchDetail?.tossWinner} won toss, chose to ${matchDetail?.tossChoice}"
    else "Toss info loading..."

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", color = D11White, fontSize = 24.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(matchTitle, color = D11White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold)
                    Text("Live Score", color = Color(0xFFFFCDD2), fontSize = 12.sp)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(Color(0xFFFF4444)))
                Text("LIVE", color = Color(0xFFFF4444), fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold)
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = D11Red)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Live score load ho raha hai...",
                            color = D11Gray, fontSize = 14.sp)
                    }
                }
            }

            errorMsg.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😔", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMsg, color = D11Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMsg = ""
                                scope.launch {
                                    try {
                                        if (matchId.isNotEmpty()) {
                                            val r = CricApi.service.getMatchInfo(matchId = matchId)
                                            matchDetail = r.data
                                        }
                                        isLoading = false
                                    } catch (e: Exception) {
                                        errorMsg = "Error: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("🔄 Dobara Try Karo") }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // SCORE CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(tossInfo, color = D11Gray, fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier.size(56.dp).clip(CircleShape)
                                                .background(Color(0xFF003366)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(team1.take(3).uppercase(), color = D11White,
                                                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(team1Score, color = D11White,
                                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("VS", color = D11Red, fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF8B0000))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("LIVE", color = Color(0xFFFF4444),
                                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier.size(56.dp).clip(CircleShape)
                                                .background(Color(0xFF006600)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(team2.take(3).uppercase(), color = D11White,
                                                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(team2Score, color = D11Gray,
                                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = D11Border, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(matchStatus, color = D11Green, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // MY POINTS CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1500)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("My Points", color = D11Gray, fontSize = 12.sp)
                                    Text("$myPoints", color = D11Yellow, fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold)
                                    Text("pts", color = D11Gray, fontSize = 11.sp)
                                }
                                Box(modifier = Modifier.width(1.dp).height(60.dp)
                                    .background(D11Border))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("My Rank", color = D11Gray, fontSize = 12.sp)
                                    Text("#$myRank", color = D11White, fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold)
                                    Text("rank", color = D11Gray, fontSize = 11.sp)
                                }
                                Box(modifier = Modifier.width(1.dp).height(60.dp)
                                    .background(D11Border))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Prize", color = D11Gray, fontSize = 12.sp)
                                    Text("₹500", color = D11Green, fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold)
                                    Text("winning", color = D11Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // LAST 6 BALLS
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = D11CardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Last 6 Balls", color = D11White, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    sampleBalls.take(6).forEach { ball ->
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                                .background(when {
                                                    ball.isWicket -> Color(0xFF8B0000)
                                                    ball.isSix -> Color(0xFF004400)
                                                    ball.isBoundary -> Color(0xFF003300)
                                                    else -> D11LightGray
                                                })
                                                .border(1.dp, when {
                                                    ball.isWicket -> D11Red
                                                    ball.isSix -> D11Green
                                                    ball.isBoundary -> D11Green
                                                    else -> D11Border
                                                }, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                if (ball.isWicket) "W" else "${ball.runs}",
                                                color = when {
                                                    ball.isWicket -> D11Red
                                                    ball.isSix -> D11Green
                                                    ball.isBoundary -> D11Green
                                                    else -> D11White
                                                },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // BALL BY BALL
                    item {
                        Text("Ball by Ball", color = D11White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                    }

                    items(sampleBalls) { ball ->
                        BallEventCard(ball = ball)
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun BallEventCard(ball: BallEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                ball.isWicket -> Color(0xFF1A0000)
                ball.isSix -> Color(0xFF001A00)
                ball.isBoundary -> Color(0xFF001500)
                else -> D11CardBg
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(when {
                            ball.isWicket -> Color(0xFF8B0000)
                            ball.isSix -> Color(0xFF004400)
                            ball.isBoundary -> Color(0xFF003300)
                            else -> D11LightGray
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (ball.isWicket) "W" else "${ball.runs}",
                        color = when {
                            ball.isWicket -> D11Red
                            ball.isSix -> D11Green
                            ball.isBoundary -> D11Green
                            else -> D11White
                        },
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                Column {
                    Text("Over ${ball.over}.${ball.ball}", color = D11Gray, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ball.batsman, color = D11White, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold)
                        Text("vs", color = D11Gray, fontSize = 11.sp)
                        Text(ball.bowler, color = D11Gray, fontSize = 13.sp)
                    }
                }
            }
            if (ball.isWicket || ball.isSix || ball.isBoundary) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(when {
                            ball.isWicket -> D11Red
                            ball.isSix -> D11Green
                            else -> Color(0xFF005500)
                        })
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        when {
                            ball.isWicket -> "WICKET!"
                            ball.isSix -> "SIX!"
                            else -> "FOUR!"
                        },
                        color = D11White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}