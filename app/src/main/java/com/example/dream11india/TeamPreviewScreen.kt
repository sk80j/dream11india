package com.example.dream11india

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
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
    val captain = sampleTeam.find { it.isCaptain }
    val viceCaptain = sampleTeam.find { it.isViceCaptain }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {

        // TOP BAR
        Box(modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(
                colors = listOf(D11Red, D11DarkRed)
            ))) {
            Row(modifier = Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("←", color = D11White, fontSize = 24.sp,
                        modifier = Modifier.clickable { onBack() })
                    Image(painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo", modifier = Modifier.size(28.dp))
                    Column {
                        Text("Team Preview", color = D11White, fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 11.sp)
                    }
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { onEditTeam() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text("Edit Team", color = D11White, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }

        // C/VC Info Bar
        Row(modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(D11Yellow),
                    contentAlignment = Alignment.Center) {
                    Text("C", color = D11Black, fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Text(captain?.name ?: "Not selected", color = D11White,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("2x", color = D11Yellow, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(Color(0xFFAAAAAA)),
                    contentAlignment = Alignment.Center) {
                    Text("VC", color = D11Black, fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Text(viceCaptain?.name ?: "Not selected", color = D11White,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("1.5x", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {

            // CRICKET FIELD
            item {
                Box(modifier = Modifier.fillMaxWidth().height(500.dp)
                    .background(Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B5E20),
                            Color(0xFF2E7D32),
                            Color(0xFF388E3C),
                            Color(0xFF2E7D32),
                            Color(0xFF1B5E20)
                        )
                    ))) {

                    // Field markings
                    Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        // Outer circle
                        Box(modifier = Modifier.size(280.dp).clip(CircleShape)
                            .background(Color(0x15FFFFFF)))
                        // Inner circle
                        Box(modifier = Modifier.size(160.dp).clip(CircleShape)
                            .background(Color(0x10FFFFFF)))
                        // Center dot
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color(0x20FFFFFF)))
                    }

                    // Players on field
                    Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly) {

                        // WK Row
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WICKET-KEEPERS", color = Color(0xCCFFFFFF),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly) {
                                wkPlayers.forEach { PlayerFieldCard(player = it) }
                            }
                        }

                        // BAT Row
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BATTERS", color = Color(0xCCFFFFFF),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly) {
                                batPlayers.forEach { PlayerFieldCard(player = it) }
                            }
                        }

                        // AR Row
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ALL-ROUNDERS", color = Color(0xCCFFFFFF),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly) {
                                arPlayers.forEach { PlayerFieldCard(player = it) }
                            }
                        }

                        // BOWL Row
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BOWLERS", color = Color(0xCCFFFFFF),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly) {
                                bowlPlayers.forEach { PlayerFieldCard(player = it) }
                            }
                        }
                    }
                }
            }

            // TEAM STATS
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Team Summary", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("11 Players", color = D11Green, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Role counts
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(
                                Triple("WK", wkPlayers.size, Color(0xFF6666FF)),
                                Triple("BAT", batPlayers.size, D11Green),
                                Triple("AR", arPlayers.size, D11Red),
                                Triple("BOWL", bowlPlayers.size, D11Yellow)
                            ).forEach { (role, count, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center) {
                                        Text("$count", color = color, fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(role, color = D11Gray, fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = D11Border)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Team split
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(team1, color = D11Gray, fontSize = 12.sp)
                                Text("${sampleTeam.count { it.team == team1 }} Players",
                                    color = D11White, fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(team2, color = D11Gray, fontSize = 12.sp)
                                Text("${sampleTeam.count { it.team == team2 }} Players",
                                    color = D11White, fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Total credits
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Credits Used", color = D11Gray, fontSize = 13.sp)
                            Text("${sampleTeam.sumOf { it.credits.toDouble() }.toFloat()} / 100",
                                color = D11Yellow, fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // BOTTOM BUTTONS
        Box(modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onEditTeam,
                    modifier = Modifier.weight(1f).height(54.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, D11White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Edit Team", color = D11White, fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp)
                }
                Button(
                    onClick = onJoinContest,
                    modifier = Modifier.weight(2f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text("Join Contest →", color = D11White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PlayerFieldCard(player: Player) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            // Player circle with shadow
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        when(player.role) {
                            "WK" -> Brush.radialGradient(listOf(Color(0xFF3949AB), Color(0xFF1A237E)))
                            "BAT" -> Brush.radialGradient(listOf(Color(0xFF43A047), Color(0xFF1B5E20)))
                            "AR" -> Brush.radialGradient(listOf(Color(0xFF8E24AA), Color(0xFF4A148C)))
                            else -> Brush.radialGradient(listOf(Color(0xFFE53935), Color(0xFF880E4F)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(player.shortName.take(2).uppercase(), color = D11White,
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }

            // C/VC Badge
            if (player.isCaptain) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                        .background(D11Yellow)
                        .shadow(2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", color = D11Black, fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            } else if (player.isViceCaptain) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                        .background(Color(0xFFBBBBBB))
                        .shadow(2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VC", color = D11Black, fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            player.shortName.take(8),
            color = D11White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            "${player.credits} Cr",
            color = Color(0xAAFFFFFF),
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}