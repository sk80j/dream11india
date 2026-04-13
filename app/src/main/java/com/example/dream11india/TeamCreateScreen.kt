package com.example.dream11india

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

// ===== PLAYER DATA =====
data class Player(
    val id: String,
    val name: String,
    val shortName: String,
    val team: String,
    val role: String, // WK, BAT, AR, BOWL
    val credits: Float,
    val selectionPercent: Int,
    val points: Float = 0f,
    val isSelected: Boolean = false,
    val isCaptain: Boolean = false,
    val isViceCaptain: Boolean = false
)

@Composable
fun TeamCreateScreen(
    matchTitle: String = "CSK vs RCB",
    onBack: () -> Unit = {},
    onTeamSaved: () -> Unit = {}
) {
    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    // Sample players
    val allPlayers = remember {
        mutableStateListOf(
            // WK
            Player("1", "MS Dhoni", "Dhoni", team1, "WK", 9.0f, 78),
            Player("2", "KL Rahul", "KL Rahul", team2, "WK", 9.5f, 65),
            Player("3", "Sanju Samson", "Samson", team2, "WK", 8.5f, 45),
            // BAT
            Player("4", "Virat Kohli", "Kohli", team2, "BAT", 10.0f, 89),
            Player("5", "Faf du Plessis", "Faf", team2, "BAT", 9.5f, 72),
            Player("6", "Ruturaj Gaikwad", "Ruturaj", team1, "BAT", 9.0f, 68),
            Player("7", "Devon Conway", "Conway", team1, "BAT", 8.5f, 55),
            Player("8", "Ambati Rayudu", "Rayudu", team1, "BAT", 7.5f, 32),
            // AR
            Player("9", "Ravindra Jadeja", "Jadeja", team1, "AR", 9.5f, 82),
            Player("10", "Glenn Maxwell", "Maxwell", team2, "AR", 9.0f, 71),
            Player("11", "Mitchell Santner", "Santner", team1, "AR", 8.0f, 48),
            Player("12", "Shahbaz Ahmed", "Shahbaz", team2, "AR", 7.5f, 35),
            // BOWL
            Player("13", "Jasprit Bumrah", "Bumrah", team2, "BOWL", 9.5f, 79),
            Player("14", "Mohammed Siraj", "Siraj", team2, "BOWL", 9.0f, 65),
            Player("15", "Deepak Chahar", "Chahar", team1, "BOWL", 8.5f, 58),
            Player("16", "Josh Hazlewood", "Hazlewood", team2, "BOWL", 8.5f, 54),
            Player("17", "Tushar Deshpande", "Deshpande", team1, "BOWL", 7.5f, 38),
            Player("18", "Wanindu Hasaranga", "Hasaranga", team2, "BOWL", 8.0f, 45)
        )
    }

    var selectedTab by remember { mutableStateOf("WK") }
    var captainId by remember { mutableStateOf("") }
    var viceCaptainId by remember { mutableStateOf("") }
    var showCVCScreen by remember { mutableStateOf(false) }

    val selectedPlayers = allPlayers.filter { it.isSelected }
    val totalCredits = selectedPlayers.sumOf { it.credits.toDouble() }.toFloat()
    val creditsLeft = 100f - totalCredits

    // Validation
    val wkCount = selectedPlayers.count { it.role == "WK" }
    val batCount = selectedPlayers.count { it.role == "BAT" }
    val arCount = selectedPlayers.count { it.role == "AR" }
    val bowlCount = selectedPlayers.count { it.role == "BOWL" }
    val team1Count = selectedPlayers.count { it.team == team1 }
    val team2Count = selectedPlayers.count { it.team == team2 }

    val isValid = selectedPlayers.size == 11 &&
            wkCount in 1..4 &&
            batCount in 3..6 &&
            arCount in 1..4 &&
            bowlCount in 3..6 &&
            team1Count <= 7 && team2Count <= 7 &&
            creditsLeft >= 0

    if (showCVCScreen) {
        CVCSelectionScreen(
            players = selectedPlayers,
            captainId = captainId,
            viceCaptainId = viceCaptainId,
            onCaptainSelect = { captainId = it },
            onVCSelect = { viceCaptainId = it },
            onBack = { showCVCScreen = false },
            onSave = { onTeamSaved() }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

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
                    Text("Select 11 Players", color = Color(0xFFFFCDD2), fontSize = 12.sp)
                }
            }
            // Credits left
            Column(horizontalAlignment = Alignment.End) {
                Text("${String.format("%.1f", creditsLeft)}", color = D11Yellow,
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Credits Left", color = Color(0xFFFFCDD2), fontSize = 10.sp)
            }
        }

        // TEAM BALANCE BAR
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$team1: $team1Count", color = D11White, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            // Balance bar
            Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(D11LightGray))
                if (selectedPlayers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(team1Count.toFloat() / selectedPlayers.size.coerceAtLeast(1))
                            .height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(D11Red)
                    )
                }
            }
            Text("$team2: $team2Count", color = D11White, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
        }

        // PLAYER COUNT + ROLE STATS
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D0D))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "WK" to wkCount to "1-4",
                "BAT" to batCount to "3-6",
                "AR" to arCount to "1-4",
                "BOWL" to bowlCount to "3-6"
            ).forEach { (roleCount, range) ->
                val (role, count) = roleCount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", color = if (count > 0) D11Yellow else D11Gray,
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(role, color = D11Gray, fontSize = 11.sp)
                    Text(range, color = Color(0xFF555555), fontSize = 9.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${selectedPlayers.size}/11",
                    color = if (selectedPlayers.size == 11) D11Green else D11White,
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Players", color = D11Gray, fontSize = 11.sp)
            }
        }

        // ROLE TABS
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("WK", "BAT", "AR", "BOWL").forEach { role ->
                val count = allPlayers.count { it.role == role && it.isSelected }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedTab = role }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        when(role) {
                            "WK" -> "WK ($count)"
                            "BAT" -> "BAT ($count)"
                            "AR" -> "AR ($count)"
                            else -> "BOWL ($count)"
                        },
                        color = if (selectedTab == role) D11White else D11Gray,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == role) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selectedTab == role) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Red))
                    }
                }
            }
        }

        // PLAYER LIST
        val filteredPlayers = allPlayers.filter { it.role == selectedTab }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF0D0D0D))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Player", color = D11Gray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        Text("Points", color = D11Gray, fontSize = 12.sp)
                        Text("Credits", color = D11Gray, fontSize = 12.sp)
                    }
                }
            }

            items(filteredPlayers) { player ->
                val index = allPlayers.indexOf(player)
                PlayerSelectionCard(
                    player = player,
                    team1 = team1,
                    team2 = team2,
                    onSelect = {
                        if (player.isSelected) {
                            allPlayers[index] = player.copy(isSelected = false)
                        } else if (selectedPlayers.size < 11 && creditsLeft >= player.credits) {
                            // Check max 7 from one team
                            val sameTeamCount = selectedPlayers.count { it.team == player.team }
                            if (sameTeamCount < 7) {
                                allPlayers[index] = player.copy(isSelected = true)
                            }
                        }
                    }
                )
            }
        }

        // BOTTOM BUTTON
        Box(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    if (isValid) showCVCScreen = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isValid) D11Red else Color(0xFF444444)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isValid
            ) {
                Text(
                    if (selectedPlayers.size < 11)
                        "Select ${11 - selectedPlayers.size} more players"
                    else "Preview Team →",
                    color = D11White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PlayerSelectionCard(
    player: Player,
    team1: String,
    team2: String,
    onSelect: () -> Unit
) {
    val bgColor = if (player.isSelected) Color(0xFF1A2A1A) else Color(0xFF1A1A1A)

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(bgColor)
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            // Selection circle
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(if (player.isSelected) D11Green else D11LightGray)
                    .border(1.dp, if (player.isSelected) D11Green else D11Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (player.isSelected) {
                    Text("✓", color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Player avatar
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(
                        if (player.team == team1) Color(0xFF003366)
                        else Color(0xFF006600)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(player.shortName.take(2).uppercase(), color = D11White,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Column {
                Text(player.name, color = D11White, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(player.team, color = D11Gray, fontSize = 12.sp)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(3.dp))
                            .background(
                                when(player.role) {
                                    "WK" -> Color(0xFF1A1A4A)
                                    "BAT" -> Color(0xFF1A3A1A)
                                    "AR" -> Color(0xFF3A1A1A)
                                    else -> Color(0xFF2A2A1A)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(player.role,
                            color = when(player.role) {
                                "WK" -> Color(0xFF6666FF)
                                "BAT" -> D11Green
                                "AR" -> D11Red
                                else -> D11Yellow
                            },
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${player.selectionPercent}% sel", color = D11Gray, fontSize = 11.sp)
                }
            }
        }

        // Points + Credits
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${player.points}", color = D11White, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text("Pts", color = D11Gray, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${player.credits}", color = D11Yellow, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text("Cr", color = D11Gray, fontSize = 10.sp)
            }
        }
    }
    HorizontalDivider(color = D11Border, thickness = 0.5.dp)
}

// ===== CAPTAIN / VICE CAPTAIN SCREEN =====
@Composable
fun CVCSelectionScreen(
    players: List<Player>,
    captainId: String,
    viceCaptainId: String,
    onCaptainSelect: (String) -> Unit,
    onVCSelect: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", color = D11White, fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Choose Captain & VC", color = D11White, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Text("C gets 2x | VC gets 1.5x points", color = Color(0xFFFFCDD2), fontSize = 12.sp)
            }
        }

        // Info cards
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1500)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(D11Yellow),
                        contentAlignment = Alignment.Center) {
                        Text("C", color = D11Black, fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Captain", color = D11White, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    Text("2x Points", color = D11Yellow, fontSize = 12.sp)
                    if (captainId.isNotEmpty()) {
                        Text(players.find { it.id == captainId }?.shortName ?: "",
                            color = D11Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A00)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color(0xFFAAAAAA)),
                        contentAlignment = Alignment.Center) {
                        Text("VC", color = D11Black, fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Vice Captain", color = D11White, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    Text("1.5x Points", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    if (viceCaptainId.isNotEmpty()) {
                        Text(players.find { it.id == viceCaptainId }?.shortName ?: "",
                            color = D11Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Players list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(players) { player ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(Color(0xFF2A2A2A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(player.shortName.take(2).uppercase(), color = D11White,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(player.name, color = D11White, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold)
                            Text("${player.team} • ${player.role} • ${player.credits}cr",
                                color = D11Gray, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Captain button
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(if (captainId == player.id) D11Yellow else D11LightGray)
                                .clickable {
                                    if (viceCaptainId != player.id) onCaptainSelect(player.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("C", color = if (captainId == player.id) D11Black else D11White,
                                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        // VC button
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(if (viceCaptainId == player.id)
                                    Color(0xFFAAAAAA) else D11LightGray)
                                .clickable {
                                    if (captainId != player.id) onVCSelect(player.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("VC",
                                color = if (viceCaptainId == player.id) D11Black else D11White,
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                HorizontalDivider(color = D11Border, thickness = 0.5.dp)
            }
        }

        // Save button
        Box(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(16.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (captainId.isNotEmpty() && viceCaptainId.isNotEmpty())
                        D11Red else Color(0xFF444444)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = captainId.isNotEmpty() && viceCaptainId.isNotEmpty()
            ) {
                Text("SAVE TEAM →", color = D11White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}