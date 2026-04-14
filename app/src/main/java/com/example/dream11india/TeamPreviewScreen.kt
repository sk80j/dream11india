package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TeamPreviewScreen(
    matchTitle: String = "CSK vs RCB",
    team1: String = "CSK",
    team2: String = "RCB",
    onBack: () -> Unit = {},
    onEditTeam: () -> Unit = {},
    onJoinContest: () -> Unit = {}
) {
    val sampleTeam = listOf(
        Player("1","MS Dhoni","Dhoni",team1,"WK",9.0f,78,isCaptain=false,isViceCaptain=false,isSelected=true),
        Player("4","Virat Kohli","Kohli",team2,"BAT",10.0f,89,isCaptain=true,isViceCaptain=false,isSelected=true),
        Player("5","Faf du Plessis","Faf",team2,"BAT",9.5f,72,isCaptain=false,isViceCaptain=true,isSelected=true),
        Player("6","Ruturaj Gaikwad","Ruturaj",team1,"BAT",9.0f,68,isSelected=true),
        Player("9","Ravindra Jadeja","Jadeja",team1,"AR",9.5f,82,isSelected=true),
        Player("10","Glenn Maxwell","Maxwell",team2,"AR",9.0f,71,isSelected=true),
        Player("11","Mitchell Santner","Santner",team1,"AR",8.0f,48,isSelected=true),
        Player("13","Jasprit Bumrah","Bumrah",team2,"BOWL",9.5f,79,isSelected=true),
        Player("14","Mohammed Siraj","Siraj",team2,"BOWL",9.0f,65,isSelected=true),
        Player("15","Deepak Chahar","Chahar",team1,"BOWL",8.5f,58,isSelected=true),
        Player("16","Josh Hazlewood","Hazlewood",team2,"BOWL",8.5f,54,isSelected=true),
    )

    val wkPlayers = sampleTeam.filter { it.role == "WK" }
    val batPlayers = sampleTeam.filter { it.role == "BAT" }
    val arPlayers = sampleTeam.filter { it.role == "AR" }
    val bowlPlayers = sampleTeam.filter { it.role == "BOWL" }

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
                Spacer(modifier = Modifier.width(12.dp))
                Image(painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo", modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Team Preview", color = D11White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
            }
            Text("Edit", color = D11White, fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onEditTeam() })
        }

        LazyColumn(modifier = Modifier.weight(1f)) {

            // CRICKET FIELD
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(480.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32),
                                    Color(0xFF388E3C), Color(0xFF2E7D32), Color(0xFF1B5E20))
                            )
                        )
                ) {
                    // Field lines
                    Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(200.dp).clip(CircleShape)
                            .background(Color(0x20FFFFFF)))
                        Box(modifier = Modifier.size(120.dp).clip(CircleShape)
                            .background(Color(0x15FFFFFF)))
                    }

                    Column(modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally) {

                        // WK
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("WICKET-KEEPERS", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            wkPlayers.forEach { player ->
                                PlayerFieldCard(player = player)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // BAT
                        Text("BATTERS", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            batPlayers.forEach { player ->
                                PlayerFieldCard(player = player)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // AR
                        Text("ALL-ROUNDERS", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            arPlayers.forEach { player ->
                                PlayerFieldCard(player = player)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // BOWL
                        Text("BOWLERS", color = Color(0xAAFFFFFF), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            bowlPlayers.forEach { player ->
                                PlayerFieldCard(player = player)
                            }
                        }
                    }
                }
            }

            // TEAM STATS
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Team Summary", color = D11White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(
                                "WK" to wkPlayers.size,
                                "BAT" to batPlayers.size,
                                "AR" to arPlayers.size,
                                "BOWL" to bowlPlayers.size
                            ).forEach { (role, count) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$count", color = D11Yellow, fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold)
                                    Text(role, color = D11Gray, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = D11Border)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$team1: ${sampleTeam.count { it.team == team1 }}",
                                color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("$team2: ${sampleTeam.count { it.team == team2 }}",
                                color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // BOTTOM BUTTONS
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))
            .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onEditTeam,
                modifier = Modifier.weight(1f).height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, D11Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Edit Team", color = D11Red, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onJoinContest,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Join Contest", color = D11White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun PlayerFieldCard(player: Player) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                .background(
                    when(player.role) {
                        "WK" -> Color(0xFF1A237E)
                        "BAT" -> Color(0xFF1B5E20)
                        "AR" -> Color(0xFF4A148C)
                        else -> Color(0xFF880E4F)
                    }
                ),
                contentAlignment = Alignment.Center) {
                Text(player.shortName.take(2).uppercase(), color = D11White,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (player.isCaptain) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape)
                    .background(D11Yellow),
                    contentAlignment = Alignment.Center) {
                    Text("C", color = D11Black, fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            } else if (player.isViceCaptain) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape)
                    .background(Color(0xFFAAAAAA)),
                    contentAlignment = Alignment.Center) {
                    Text("VC", color = D11Black, fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(player.shortName.take(8), color = D11White, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            maxLines = 1)
        Text("${player.credits} Cr", color = Color(0xAAFFFFFF), fontSize = 10.sp,
            textAlign = TextAlign.Center)
    }
}