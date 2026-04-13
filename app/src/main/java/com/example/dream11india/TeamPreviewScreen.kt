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

data class FantasyPlayer(
    val name: String,
    val role: String,
    val team: String,
    val points: Float = 0f,
    val credits: Float = 8f,
    val isCaptain: Boolean = false,
    val isViceCaptain: Boolean = false,
    val isSelected: Boolean = false
)

@Composable
fun TeamPreviewScreen(
    matchTitle: String = "IND vs AUS",
    team1: String = "IND",
    team2: String = "AUS",
    onBack: () -> Unit = {},
    onEditTeam: () -> Unit = {},
    onJoinContest: () -> Unit = {}
) {
    val players = listOf(
        FantasyPlayer("KL Rahul", "WK", "IND", 89.5f, 9.0f, false, false, true),
        FantasyPlayer("V Kohli", "BAT", "IND", 95.0f, 10.0f, true, false, true),
        FantasyPlayer("S Smith", "BAT", "AUS", 87.5f, 9.5f, false, false, true),
        FantasyPlayer("D Warner", "BAT", "AUS", 82.0f, 8.5f, false, false, true),
        FantasyPlayer("H Pandya", "AR", "IND", 91.0f, 9.0f, false, true, true),
        FantasyPlayer("C Green", "AR", "AUS", 78.5f, 8.0f, false, false, true),
        FantasyPlayer("J Bumrah", "BOWL", "IND", 93.5f, 9.5f, false, false, true),
        FantasyPlayer("P Cummins", "BOWL", "AUS", 88.0f, 9.0f, false, false, true),
        FantasyPlayer("M Starc", "BOWL", "AUS", 84.5f, 8.5f, false, false, true),
        FantasyPlayer("R Jadeja", "BOWL", "IND", 86.0f, 8.5f, false, false, true),
        FantasyPlayer("A Zampa", "BOWL", "AUS", 76.0f, 7.5f, false, false, true),
    )

    val captain = players.find { it.isCaptain }
    val viceCaptain = players.find { it.isViceCaptain }
    val totalCredits = players.sumOf { it.credits.toDouble() }.toFloat()

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
                    Text("Team Preview", color = Color(0xFFFFCDD2), fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { onEditTeam() }
            ) {
                Text("✏️ Edit", color = D11White, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
            }
        }

        // TEAM STATS ROW
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip("Players", "${players.size}/11")
            StatChip("Credits", "$totalCredits")
            StatChip(team1, "${players.count { it.team == team1 }}")
            StatChip(team2, "${players.count { it.team == team2 }}")
        }

        // CRICKET FIELD VIEW
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp)
                .background(Color(0xFF1A3A1A))
        ) {
            // Field circles
            Box(
                modifier = Modifier.size(260.dp).clip(CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier.size(150.dp).clip(CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape)
                    .background(Color(0xFF2A5A2A))
                    .border(1.dp, Color(0x55FFFFFF), CircleShape)
                    .align(Alignment.Center)
            )

            val wk = players.filter { it.role == "WK" }
            val batsmen = players.filter { it.role == "BAT" }
            val allRounders = players.filter { it.role == "AR" }
            val bowlers = players.filter { it.role == "BOWL" }

            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bowlers.take(4).forEach { player ->
                        PlayerFieldItem(player = player)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    allRounders.forEach { player ->
                        PlayerFieldItem(player = player)
                    }
                    if (bowlers.size > 4) {
                        PlayerFieldItem(player = bowlers[4])
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    batsmen.forEach { player ->
                        PlayerFieldItem(player = player)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    wk.forEach { player ->
                        PlayerFieldItem(player = player)
                    }
                }
            }
        }

        // CAPTAIN & VC INFO
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(D11Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", color = D11Black, fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("Captain (2x)", color = D11Gray, fontSize = 11.sp)
                    Text(captain?.name ?: "Not selected", color = D11White,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(D11Border))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(Color(0xFFAAAAAA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VC", color = D11Black, fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("Vice Captain (1.5x)", color = D11Gray, fontSize = 11.sp)
                    Text(viceCaptain?.name ?: "Not selected", color = D11White,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // PLAYER LIST
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "WK" to "Wicket Keeper",
                "BAT" to "Batsmen",
                "AR" to "All Rounders",
                "BOWL" to "Bowlers"
            ).forEach { (role, label) ->
                val rolePlayers = players.filter { it.role == role }
                if (rolePlayers.isNotEmpty()) {
                    item {
                        Text(
                            label, color = D11Gray, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(rolePlayers) { player ->
                        PlayerListCard(player = player)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // BOTTOM BUTTON
        Box(
            modifier = Modifier.fillMaxWidth().background(D11Black).padding(16.dp)
        ) {
            Button(
                onClick = onJoinContest,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("JOIN CONTEST →", color = D11White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PlayerFieldItem(player: FantasyPlayer) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(
                        when (player.team) {
                            "IND" -> Color(0xFF003366)
                            else -> Color(0xFF006600)
                        }
                    )
                    .border(
                        2.dp,
                        when {
                            player.isCaptain -> D11Yellow
                            player.isViceCaptain -> Color(0xFFAAAAAA)
                            else -> Color.Transparent
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.split(" ").last().take(3).uppercase(),
                    color = D11White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            if (player.isCaptain) {
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape)
                        .background(D11Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", color = D11Black, fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            } else if (player.isViceCaptain) {
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape)
                        .background(Color(0xFFAAAAAA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", color = D11Black, fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            player.name.split(" ").last().take(8),
            color = D11White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "${player.credits} cr",
            color = D11Gray, fontSize = 9.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PlayerListCard(player: FantasyPlayer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                player.isCaptain -> Color(0xFF1A1500)
                player.isViceCaptain -> Color(0xFF1A1A1A)
                else -> D11CardBg
            }
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(
                            when (player.team) {
                                "IND" -> Color(0xFF003366)
                                else -> Color(0xFF006600)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        player.name.split(" ").last().take(2).uppercase(),
                        color = D11White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(player.name, color = D11White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                        if (player.isCaptain) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .background(D11Yellow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = D11Black, fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        if (player.isViceCaptain) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .background(Color(0xFFAAAAAA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("V", color = D11Black, fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(player.team, color = D11Gray, fontSize = 12.sp)
                        Text("•", color = D11Gray, fontSize = 12.sp)
                        Text(player.role, color = D11Red, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                        Text("•", color = D11Gray, fontSize = 12.sp)
                        Text("${player.credits} cr", color = D11Gray, fontSize = 12.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${player.points}", color = D11Yellow, fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("pts", color = D11Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = D11Gray, fontSize = 10.sp)
    }
}