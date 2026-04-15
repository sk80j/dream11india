package com.example.dream11india

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Player(
    val id: String,
    val name: String,
    val shortName: String,
    val team: String,
    val role: String,
    val credits: Float,
    val selectionPercent: Int,
    val points: Float = 0f,
    val isSelected: Boolean = false,
    val isCaptain: Boolean = false,
    val isViceCaptain: Boolean = false
)

enum class SortType { POINTS, CREDITS, SELECTION }

@Composable
fun TeamCreateScreen(
    matchTitle: String = "CSK vs RCB",
    onBack: () -> Unit = {},
    onTeamSaved: () -> Unit = {}
) {
    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allPlayers = remember {
        mutableStateListOf(
            Player("1","MS Dhoni","Dhoni",team1,"WK",9.0f,78,45f),
            Player("2","KL Rahul","KL Rahul",team2,"WK",9.5f,65,38f),
            Player("3","Sanju Samson","Samson",team2,"WK",8.5f,45,32f),
            Player("4","Virat Kohli","Kohli",team2,"BAT",10.0f,89,95f),
            Player("5","Faf du Plessis","Faf",team2,"BAT",9.5f,72,72f),
            Player("6","Ruturaj Gaikwad","Ruturaj",team1,"BAT",9.0f,68,68f),
            Player("7","Devon Conway","Conway",team1,"BAT",8.5f,55,55f),
            Player("8","Ambati Rayudu","Rayudu",team1,"BAT",7.5f,32,28f),
            Player("9","Ravindra Jadeja","Jadeja",team1,"AR",9.5f,82,88f),
            Player("10","Glenn Maxwell","Maxwell",team2,"AR",9.0f,71,75f),
            Player("11","Mitchell Santner","Santner",team1,"AR",8.0f,48,42f),
            Player("12","Shahbaz Ahmed","Shahbaz",team2,"AR",7.5f,35,30f),
            Player("13","Jasprit Bumrah","Bumrah",team2,"BOWL",9.5f,79,92f),
            Player("14","Mohammed Siraj","Siraj",team2,"BOWL",9.0f,65,68f),
            Player("15","Deepak Chahar","Chahar",team1,"BOWL",8.5f,58,55f),
            Player("16","Josh Hazlewood","Hazlewood",team2,"BOWL",8.5f,54,52f),
            Player("17","Tushar Deshpande","Deshpande",team1,"BOWL",7.5f,38,35f),
            Player("18","Wanindu Hasaranga","Hasaranga",team2,"BOWL",8.0f,45,48f)
        )
    }

    var selectedTab by remember { mutableStateOf("WK") }
    var captainId by remember { mutableStateOf("") }
    var viceCaptainId by remember { mutableStateOf("") }
    var showCVCScreen by remember { mutableStateOf(false) }
    var isLoadingPlayers by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf("") }

    // Load real players from API
    LaunchedEffect(matchTitle) {
    isLoadingPlayers = true
    try {
        val matchId = "95001"

        when (val response = CricApiRepository.getSquad(matchId)) {
            is ApiResult.Success -> {
                if (response.data.isNotEmpty()) {
                    allPlayers.clear()
                    allPlayers.addAll(response.data)
                }
            }

            is ApiResult.Error -> {
                apiError = response.message
            }

            is ApiResult.Loading -> {
                // no-op
            }
        }

    } catch (e: Exception) {
        apiError = "Using sample players"
    }
    isLoadingPlayers = false
}
    var sortType by remember { mutableStateOf(SortType.POINTS) }
    val listState = rememberLazyListState()

    val selectedPlayers = allPlayers.filter { it.isSelected }
    val totalCredits = selectedPlayers.sumOf { it.credits.toDouble() }.toFloat()
    val creditsLeft = 100f - totalCredits
    val wkCount = selectedPlayers.count { it.role == "WK" }
    val batCount = selectedPlayers.count { it.role == "BAT" }
    val arCount = selectedPlayers.count { it.role == "AR" }
    val bowlCount = selectedPlayers.count { it.role == "BOWL" }
    val team1Count = selectedPlayers.count { it.team == team1 }
    val team2Count = selectedPlayers.count { it.team == team2 }

    val isValid = selectedPlayers.size == 11 &&
            wkCount in 1..4 && batCount in 3..6 &&
            arCount in 1..4 && bowlCount in 3..6 &&
            team1Count <= 7 && team2Count <= 7 && creditsLeft >= 0

    // Credit color warning
    val creditColor = when {
        creditsLeft < 10f -> D11Red
        creditsLeft < 20f -> D11Yellow
        else -> D11White
    }

    // Check if player is disabled
    fun isPlayerDisabled(player: Player): Boolean {
        if (player.isSelected) return false
        val sameTeamCount = selectedPlayers.count { it.team == player.team }
        val sameRoleCount = selectedPlayers.count { it.role == player.role }
        val roleMax = when(player.role) {
            "WK" -> 4; "BAT" -> 6; "AR" -> 4; "BOWL" -> 6; else -> 4
        }
        return selectedPlayers.size >= 11 ||
                creditsLeft < player.credits ||
                sameTeamCount >= 7 ||
                sameRoleCount >= roleMax
    }

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

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF333333),
                    contentColor = D11White,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(data.visuals.message, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)
            .background(Brush.verticalGradient(
                colors = listOf(Color(0xFF0F9D58), Color(0xFF087F23))
            ))) {

            Column(modifier = Modifier.fillMaxSize()) {

                // TOP HEADER
                Column(modifier = Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {

                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onBack() },
                                contentAlignment = Alignment.Center) {
                                Text("<", color = D11White, fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            Column {
                                Text("Create Team", color = D11White, fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(matchTitle, color = Color(0xCCFFFFFF), fontSize = 11.sp)
                            }
                        }
                        // Credits left with color warning
                        Column(horizontalAlignment = Alignment.End) {
                            Text(String.format("%.1f", creditsLeft),
                                color = creditColor,
                                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Credits Left",
                                color = if (creditsLeft < 10f) D11Red else Color(0xCCFFFFFF),
                                fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats bar
                    Row(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x44FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${selectedPlayers.size}/11", color = D11White,
                                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Players", color = Color(0xCCFFFFFF), fontSize = 10.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(36.dp)
                            .background(Color(0x44FFFFFF)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$team1 $team1Count | $team2 $team2Count",
                                color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Team Split", color = Color(0xCCFFFFFF), fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role counts
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(
                            Triple("WK", wkCount, "1-4"),
                            Triple("BAT", batCount, "3-6"),
                            Triple("AR", arCount, "1-4"),
                            Triple("BOWL", bowlCount, "3-6")
                        ).forEach { (role, count, range) ->
                            val roleColor = when(role) {
                                "WK" -> Color(0xFF82B1FF)
                                "BAT" -> Color(0xFF69F0AE)
                                "AR" -> Color(0xFFFF8A80)
                                else -> Color(0xFFFFFF8D)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$count", color = if (count > 0) roleColor else Color(0xCCFFFFFF),
                                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text(role, color = Color(0xCCFFFFFF), fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold)
                                Text(range, color = Color(0x88FFFFFF), fontSize = 9.sp)
                            }
                        }
                    }
                }

                // MAIN WHITE CARD
                Box(modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFFF5F5F5))) {

                    Column(modifier = Modifier.fillMaxSize()) {

                        // CATEGORY TABS
                        Row(modifier = Modifier.fillMaxWidth().background(D11White)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("WK", "BAT", "AR", "BOWL").forEach { role ->
                                val isSelected = selectedTab == role
                                val count = allPlayers.count { it.role == role && it.isSelected }
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) D11Red else D11White)
                                    .border(1.dp,
                                        if (isSelected) D11Red else Color(0xFFDDDDDD),
                                        RoundedCornerShape(20.dp))
                                    .clickable { selectedTab = role }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)) {
                                    Text("$role ($count)",
                                        color = if (isSelected) D11White else Color(0xFF333333),
                                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        // SORT TABS
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F0))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Sort:", color = Color(0xFF888888), fontSize = 12.sp)
                            listOf(
                                SortType.POINTS to "Points",
                                SortType.CREDITS to "Credits",
                                SortType.SELECTION to "Selection %"
                            ).forEach { (sort, label) ->
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sortType == sort) Color(0xFF333333) else D11White)
                                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                                    .clickable { sortType = sort }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(label,
                                        color = if (sortType == sort) D11White else Color(0xFF555555),
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        // Sticky header
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F8F8))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Player", color = Color(0xFF888888), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Text("Points", color = Color(0xFF888888), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                                Text("Credits", color = Color(0xFF888888), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        // PLAYER LIST
                        val filteredPlayers = allPlayers
                            .filter { it.role == selectedTab }
                            .sortedByDescending { player ->
                                when(sortType) {
                                    SortType.POINTS -> player.points
                                    SortType.CREDITS -> player.credits
                                    SortType.SELECTION -> player.selectionPercent.toFloat()
                                }
                            }

                        LazyColumn(state = listState, modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 90.dp)) {
                            items(filteredPlayers, key = { it.id }) { player ->
                                val index = allPlayers.indexOf(player)
                                val isDisabled = isPlayerDisabled(player)

                                Dream11PlayerCard(
                                    player = player,
                                    team1 = team1,
                                    team2 = team2,
                                    isDisabled = isDisabled,
                                    onAdd = {
                                        if (player.isSelected) {
                                            allPlayers[index] = player.copy(isSelected = false)
                                        } else {
                                            val sameTeamCount = selectedPlayers.count { it.team == player.team }
                                            val sameRoleCount = selectedPlayers.count { it.role == player.role }
                                            val roleMax = when(player.role) {
                                                "WK" -> 4; "BAT" -> 6; "AR" -> 4; "BOWL" -> 6; else -> 4
                                            }
                                            val errorMsg = when {
                                                selectedPlayers.size >= 11 -> "Maximum 11 players selected!"
                                                creditsLeft < player.credits -> "Not enough credits! Need ${String.format("%.1f", player.credits - creditsLeft)} more"
                                                sameTeamCount >= 7 -> "Max 7 players from one team!"
                                                sameRoleCount >= roleMax -> "Max $roleMax ${ player.role} players allowed!"
                                                else -> null
                                            }
                                            if (errorMsg != null) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(errorMsg)
                                                }
                                            } else {
                                                allPlayers[index] = player.copy(isSelected = true)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM BAR
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .shadow(8.dp).background(D11White)
                .padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { if (selectedPlayers.isNotEmpty()) showCVCScreen = true },
                        modifier = Modifier.weight(1f).height(52.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFCCCCCC)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Preview", color = Color(0xFF333333),
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { if (isValid) showCVCScreen = true },
                        modifier = Modifier.weight(2f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isValid) D11Red else Color(0xFFCCCCCC)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = isValid,
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text(
                            if (selectedPlayers.size < 11)
                                "Select ${11 - selectedPlayers.size} more players"
                            else "Continue -->",
                            color = if (isValid) D11White else Color(0xFF888888),
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Dream11PlayerCard(
    player: Player,
    team1: String,
    team2: String,
    isDisabled: Boolean,
    onAdd: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 4.dp)
        .scale(scale.value)
        .alpha(if (isDisabled && !player.isSelected) 0.5f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (player.isSelected) Color(0xFFF0FFF4) else D11White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (player.isSelected) 4.dp else 2.dp),
        border = if (player.isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, D11Green)
        else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)) {

                // Avatar with gradient
                Box(modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(Brush.radialGradient(
                        colors = if (player.team == team1)
                            listOf(Color(0xFF1E88E5), Color(0xFF003366))
                        else listOf(Color(0xFF43A047), Color(0xFF006600))
                    )).border(2.dp,
                        if (player.isSelected) D11Green else Color(0xFFDDDDDD),
                        CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(player.shortName.take(2).uppercase(), color = D11White,
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Player info
                Column {
                    Text(player.name, color = Color(0xFF111111),
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEEEEEE))
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(player.team.take(4).uppercase(),
                                color = Color(0xFF444444), fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(when(player.role) {
                                "WK" -> Color(0xFFE8EAF6)
                                "BAT" -> Color(0xFFE8F5E9)
                                "AR" -> Color(0xFFFCE4EC)
                                else -> Color(0xFFFFF9C4)
                            }).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(player.role, color = when(player.role) {
                                "WK" -> Color(0xFF3949AB)
                                "BAT" -> Color(0xFF2E7D32)
                                "AR" -> Color(0xFFC62828)
                                else -> Color(0xFFF57F17)
                            }, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("${player.selectionPercent}% sel",
                            color = Color(0xFF888888), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${player.points} pts", color = D11Green,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Credits + Button
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${player.credits}", color = Color(0xFF111111),
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("Cr", color = Color(0xFF888888), fontSize = 10.sp)
                Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(
                        if (player.isSelected) D11Red
                        else if (isDisabled) Color(0xFFCCCCCC)
                        else D11Green
                    )
                    .clickable(enabled = !isDisabled || player.isSelected) {
                        scope.launch {
                            scale.animateTo(1.15f, animationSpec = tween(80))
                            scale.animateTo(1f, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onAdd()
                    },
                    contentAlignment = Alignment.Center) {
                    Text(if (player.isSelected) "-" else "+", color = D11White,
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun PlayerSelectionCard(player: Player, team1: String, team2: String, onSelect: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(if (player.isSelected) Color(0xFF1A2A1A) else Color(0xFF1A1A1A))
        .clickable { onSelect() }
        .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(if (player.isSelected) D11Green else D11LightGray)
                .border(1.dp, if (player.isSelected) D11Green else D11Gray, CircleShape),
                contentAlignment = Alignment.Center) {
                if (player.isSelected) Text("OK", color = D11White, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.size(50.dp).clip(CircleShape)
                .background(if (player.team == team1) Color(0xFF003366) else Color(0xFF006600)),
                contentAlignment = Alignment.Center) {
                Text(player.shortName.take(2).uppercase(), color = D11White,
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Text(player.name, color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(player.team, color = D11Gray, fontSize = 11.sp)
                    Text(player.role, color = D11Gray, fontSize = 11.sp)
                    Text("${player.selectionPercent}%", color = D11Gray, fontSize = 11.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${player.points}", color = D11White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Pts", color = D11Gray, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${player.credits}", color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Cr", color = D11Gray, fontSize = 9.sp)
            }
        }
    }
    HorizontalDivider(color = D11Border, thickness = 0.5.dp)
}

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
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {

        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).statusBarsPadding()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("<", color = D11White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onBack() })
                        Column {
                            Text("Create Team", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("Select Captain and Vice Captain",
                                color = D11Gray, fontSize = 12.sp)
                        }
                    }
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .border(2.dp, D11Gray, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("PTS", color = D11White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A))) {
                    Row(modifier = Modifier.weight(1f).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(if (captainId.isNotEmpty()) Color(0xFF2A2A00) else D11LightGray)
                            .border(2.dp, D11Yellow, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("C", color = D11Yellow, fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text(players.find { it.id == captainId }?.name ?: "Not selected",
                                color = if (captainId.isNotEmpty()) D11Yellow else D11Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("2x (double) points", color = D11Gray, fontSize = 10.sp)
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).height(56.dp)
                        .background(D11Border).align(Alignment.CenterVertically))
                    Row(modifier = Modifier.weight(1f).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(if (viceCaptainId.isNotEmpty()) Color(0xFF222222) else D11LightGray)
                            .border(2.dp, Color(0xFFAAAAAA), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("VC", color = Color(0xFFAAAAAA), fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text(players.find { it.id == viceCaptainId }?.name ?: "Not selected",
                                color = if (viceCaptainId.isNotEmpty()) D11White else D11Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("1.5x points", color = D11Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE))
            .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Type", "Points", "% Captain By", "% Vice Captain By").forEach { filter ->
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(20.dp))
                    .background(D11White).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(filter, color = Color(0xFF333333), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE))
            .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Type", color = Color(0xFF666666), fontSize = 11.sp)
            Text("Points", color = Color(0xFF666666), fontSize = 11.sp)
            Text("% C", color = Color(0xFF666666), fontSize = 11.sp)
            Text("% VC", color = Color(0xFF666666), fontSize = 11.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)) {
            items(players, key = { it.id }) { player ->
                val isCaptain = captainId == player.id
                val isVC = viceCaptainId == player.id

                Card(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCaptain -> Color(0xFFFFFDE7)
                            isVC -> Color(0xFFF3F3F3)
                            else -> D11White
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = when {
                        isCaptain -> androidx.compose.foundation.BorderStroke(1.5.dp, D11Yellow)
                        isVC -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFAAAAAA))
                        else -> null
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {

                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(Color(0xFFEEEEEE))
                                .border(1.dp,
                                    if (isCaptain) D11Yellow
                                    else if (isVC) Color(0xFFAAAAAA)
                                    else Color(0xFFDDDDDD), CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(player.shortName.take(2).uppercase(),
                                    color = Color(0xFF333333), fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFEEEEEE))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                        Text(player.team.take(3).uppercase(),
                                            color = Color(0xFF444444), fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFDDEEFF))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                        Text(player.role, color = Color(0xFF0044AA),
                                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(player.name, color = Color(0xFF111111),
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${player.points} pts", color = D11Green,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${player.selectionPercent}%",
                                color = Color(0xFF888888), fontSize = 11.sp,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center)

                            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(if (isCaptain) Color(0xFF1A1A00) else Color(0xFFEEEEEE))
                                .border(2.dp, if (isCaptain) D11Yellow else Color(0xFFCCCCCC), CircleShape)
                                .clickable { if (viceCaptainId != player.id) onCaptainSelect(player.id) },
                                contentAlignment = Alignment.Center) {
                                Text(if (isCaptain) "2x" else "C",
                                    color = if (isCaptain) D11Yellow else Color(0xFF333333),
                                    fontSize = if (isCaptain) 14.sp else 15.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }

                            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(if (isVC) Color(0xFF1A1A1A) else Color(0xFFEEEEEE))
                                .border(2.dp, if (isVC) Color(0xFFAAAAAA) else Color(0xFFCCCCCC), CircleShape)
                                .clickable { if (captainId != player.id) onVCSelect(player.id) },
                                contentAlignment = Alignment.Center) {
                                Text(if (isVC) "1.5x" else "VC",
                                    color = if (isVC) Color(0xFFAAAAAA) else Color(0xFF333333),
                                    fontSize = if (isVC) 11.sp else 12.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(D11White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCCCCC)),
                shape = RoundedCornerShape(10.dp)) {
                Text("PREVIEW", color = Color(0xFF333333), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Button(onClick = { }, modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(10.dp)) {
                Text("Past Lineup", color = D11White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (captainId.isNotEmpty() && viceCaptainId.isNotEmpty())
                        D11Green else Color(0xFFCCCCCC)),
                shape = RoundedCornerShape(10.dp),
                enabled = captainId.isNotEmpty() && viceCaptainId.isNotEmpty(),
                elevation = ButtonDefaults.buttonElevation(4.dp)) {
                Text("SAVE", color = if (captainId.isNotEmpty() && viceCaptainId.isNotEmpty())
                    D11White else Color(0xFF888888),
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}



