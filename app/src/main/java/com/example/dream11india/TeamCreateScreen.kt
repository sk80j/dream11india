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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class Player(
    val name: String,
    val team: String,
    val role: String,
    val points: Float,
    val credits: Float,
    val photoUrl: String = "",
    val isSelected: Boolean = false
)

@Composable
fun TeamCreateScreen(
    matchTitle: String = "IND vs AUS",
    onBack: () -> Unit = {},
    onTeamSaved: () -> Unit = {}
) {
    val allPlayers = remember {
        listOf(
            // India Players — Real Photos
            Player("R. Sharma", "IND", "BAT", 9.5f, 10.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316596.jpg"),
            Player("S. Gill", "IND", "BAT", 8.5f, 9.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316590.jpg"),
            Player("V. Kohli", "IND", "BAT", 10.0f, 11.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316592.jpg"),
            Player("S. Iyer", "IND", "BAT", 7.5f, 8.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316598.jpg"),
            Player("H. Pandya", "IND", "AR", 9.0f, 9.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316600.jpg"),
            Player("R. Jadeja", "IND", "AR", 8.5f, 9.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316602.jpg"),
            Player("K.L. Rahul", "IND", "WK", 9.0f, 9.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316594.jpg"),
            Player("J. Bumrah", "IND", "BOWL", 9.5f, 10.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316604.jpg"),
            Player("M. Siraj", "IND", "BOWL", 7.5f, 8.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316606.jpg"),
            Player("Y. Chahal", "IND", "BOWL", 7.0f, 7.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316608.jpg"),
            Player("A. Sharma", "IND", "BOWL", 6.5f, 7.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316610.jpg"),
            // Australia Players
            Player("D. Warner", "AUS", "BAT", 9.0f, 9.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316612.jpg"),
            Player("S. Smith", "AUS", "BAT", 9.5f, 10.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316614.jpg"),
            Player("M. Labuschagne", "AUS", "BAT", 8.5f, 9.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316616.jpg"),
            Player("G. Maxwell", "AUS", "AR", 9.0f, 9.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316618.jpg"),
            Player("M. Stoinis", "AUS", "AR", 7.5f, 8.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316620.jpg"),
            Player("A. Carey", "AUS", "WK", 8.0f, 8.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316622.jpg"),
            Player("P. Cummins", "AUS", "BOWL", 9.5f, 10.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316624.jpg"),
            Player("M. Starc", "AUS", "BOWL", 9.0f, 9.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316626.jpg"),
            Player("A. Zampa", "AUS", "BOWL", 7.5f, 8.0f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316628.jpg"),
            Player("J. Hazlewood", "AUS", "BOWL", 8.0f, 8.5f,
                "https://img1.hscicdn.com/image/upload/f_auto,t_ds_square_w_160/lsci/db/PICTURES/CMS/316500/316630.jpg"),
        )
    }

    var selectedPlayers = remember { mutableStateListOf<Player>() }
    var captain by remember { mutableStateOf<Player?>(null) }
    var viceCaptain by remember { mutableStateOf<Player?>(null) }
    var selectedRole by remember { mutableStateOf("ALL") }
    var showCaptainPicker by remember { mutableStateOf(false) }

    val totalCredits = selectedPlayers.sumOf { it.credits.toDouble() }.toFloat()
    val maxCredits = 100f
    val creditsLeft = maxCredits - totalCredits

    val wkCount = selectedPlayers.count { it.role == "WK" }
    val batCount = selectedPlayers.count { it.role == "BAT" }
    val arCount = selectedPlayers.count { it.role == "AR" }
    val bowlCount = selectedPlayers.count { it.role == "BOWL" }

    val filteredPlayers = if (selectedRole == "ALL") allPlayers
    else allPlayers.filter { it.role == selectedRole }

    if (showCaptainPicker && selectedPlayers.size == 11) {
        CaptainPickerScreen(
            players = selectedPlayers.toList(),
            captain = captain,
            viceCaptain = viceCaptain,
            onCaptainSelect = { captain = it },
            onViceCaptainSelect = { viceCaptain = it },
            onSave = { showCaptainPicker = false; onTeamSaved() },
            onBack = { showCaptainPicker = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {

        // Top Bar
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
                    Text("Create Team", color = Color(0xFFFFCDD2), fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${selectedPlayers.size}/11", color = D11Yellow,
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Players", color = Color(0xFFFFCDD2), fontSize = 11.sp)
            }
        }

        // Credits Bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1A0000))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Credits: ${String.format("%.1f", creditsLeft)} left",
                color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("WK $wkCount", "BAT $batCount", "AR $arCount", "BOWL $bowlCount").forEach {
                    Text(it, color = D11Gray, fontSize = 11.sp)
                }
            }
        }

        // Progress
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(D11LightGray)) {
            Box(modifier = Modifier.fillMaxWidth(selectedPlayers.size / 11f)
                .height(3.dp).background(D11Green))
        }

        // Role Filter
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("ALL", "WK", "BAT", "AR", "BOWL").forEach { role ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (selectedRole == role) D11Red else D11LightGray)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { selectedRole = role }
                ) {
                    Text(role, color = D11White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Player List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PLAYERS", color = D11Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text("PTS", color = D11Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("CR", color = D11Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(filteredPlayers) { player ->
                val isSelected = selectedPlayers.any { it.name == player.name }
                PlayerCard(
                    player = player,
                    isSelected = isSelected,
                    isCaptain = captain?.name == player.name,
                    isViceCaptain = viceCaptain?.name == player.name,
                    onClick = {
                        if (isSelected) {
                            selectedPlayers.removeIf { it.name == player.name }
                            if (captain?.name == player.name) captain = null
                            if (viceCaptain?.name == player.name) viceCaptain = null
                        } else {
                            if (selectedPlayers.size < 11 && creditsLeft >= player.credits) {
                                selectedPlayers.add(player)
                            }
                        }
                    }
                )
            }
        }

        // Bottom Button
        Box(modifier = Modifier.fillMaxWidth().background(D11Black).padding(16.dp)) {
            Button(
                onClick = { if (selectedPlayers.size == 11) showCaptainPicker = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedPlayers.size == 11) D11Red else D11LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = selectedPlayers.size == 11
            ) {
                Text(
                    if (selectedPlayers.size == 11) "NEXT — Pick C & VC ➡️"
                    else "Select ${11 - selectedPlayers.size} more players",
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun PlayerCard(
    player: Player,
    isSelected: Boolean,
    isCaptain: Boolean,
    isViceCaptain: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1A0A00) else D11CardBg
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

                // Player Photo
                Box(modifier = Modifier.size(52.dp)) {
                    AsyncImage(
                        model = player.photoUrl,
                        contentDescription = player.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(
                                if (player.team == "IND") Color(0xFF003580)
                                else Color(0xFF006400)
                            )
                            .border(
                                2.dp,
                                if (isSelected) D11Red else Color.Transparent,
                                CircleShape
                            )
                    )

                    // Captain/VC badge
                    if (isCaptain) {
                        Box(
                            modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
                                .clip(CircleShape).background(D11Yellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("C", color = D11Black, fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (isViceCaptain) {
                        Box(
                            modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
                                .clip(CircleShape).background(D11Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("VC", color = D11White, fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Column {
                    Text(player.name, color = D11White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(when (player.role) {
                                    "WK" -> Color(0xFF4A0080)
                                    "BAT" -> Color(0xFF003580)
                                    "AR" -> Color(0xFF006400)
                                    else -> Color(0xFF8B0000)
                                })
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(player.role, color = D11White, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Text(player.team, color = D11Gray, fontSize = 12.sp)
                        if (isSelected) {
                            Text("✓", color = D11Green, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${player.points}", color = D11White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Text("pts", color = D11Gray, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${player.credits}", color = D11Yellow, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Text("cr", color = D11Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun CaptainPickerScreen(
    players: List<Player>,
    captain: Player?,
    viceCaptain: Player?,
    onCaptainSelect: (Player) -> Unit,
    onViceCaptainSelect: (Player) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(D11Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(D11Red)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", color = D11White, fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Text("Choose C & VC", color = D11White, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
        }

        // C & VC Info
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1A0000))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Captain (C)", color = D11Yellow, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Text("2x Points", color = D11Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                if (captain != null) {
                    AsyncImage(
                        model = captain.photoUrl,
                        contentDescription = captain.name,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(D11LightGray)
                    )
                    Text(captain.name, color = D11White, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                } else {
                    Text("Not Selected", color = D11Gray, fontSize = 12.sp)
                }
            }
            Box(modifier = Modifier.width(1.dp).height(80.dp).background(D11Border))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Vice Captain (VC)", color = D11Gray, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Text("1.5x Points", color = D11Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                if (viceCaptain != null) {
                    AsyncImage(
                        model = viceCaptain.photoUrl,
                        contentDescription = viceCaptain.name,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(D11LightGray)
                    )
                    Text(viceCaptain.name, color = D11White, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                } else {
                    Text("Not Selected", color = D11Gray, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(players) { player ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = D11CardBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AsyncImage(
                                model = player.photoUrl,
                                contentDescription = player.name,
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(D11LightGray),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(player.name, color = D11White, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold)
                                Text("${player.role} | ${player.points} pts",
                                    color = D11Gray, fontSize = 12.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                                    .background(
                                        if (captain?.name == player.name) D11Yellow
                                        else D11LightGray
                                    )
                                    .clickable { onCaptainSelect(player) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = if (captain?.name == player.name) D11Black else D11White,
                                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                                    .background(
                                        if (viceCaptain?.name == player.name) D11Gray
                                        else D11LightGray
                                    )
                                    .clickable { onViceCaptainSelect(player) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("VC", color = D11White, fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (captain != null && viceCaptain != null)
                        D11Red else D11LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = captain != null && viceCaptain != null
            ) {
                Text("SAVE TEAM ✅", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}