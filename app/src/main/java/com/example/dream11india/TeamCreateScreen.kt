package com.example.dream11india

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────
// ENUMS + VALIDATION
// ─────────────────────────────────────────────

enum class SortType { POINTS, CREDITS, SELECTION }
enum class TeamCreationStep { PLAYER_SELECTION, CVC_SELECTION }

data class TeamValidationState(
    val wkCount:       Int,
    val batCount:      Int,
    val arCount:       Int,
    val bowlCount:     Int,
    val team1Count:    Int,
    val team2Count:    Int,
    val totalSelected: Int,
    val creditsLeft:   Float,
    val isValid:       Boolean
)

// ─────────────────────────────────────────────
// SAVE TEAM TO FIREBASE
// ─────────────────────────────────────────────

private fun saveTeamToFirebase(
    players:       List<Player>,
    captainId:     String,
    viceCaptainId: String,
    matchTitle:    String,
    matchId:       String,
    teamNumber:    Int,
    onSuccess:     () -> Unit,
    onError:       (String) -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("User not logged in"); return }
    val db = FirebaseFirestore.getInstance()

    val teamData = hashMapOf(
        "matchId"         to matchId,
        "matchTitle"      to matchTitle,
        "captainId"       to captainId,
        "captainName"     to (players.find { it.id == captainId }?.name ?: ""),
        "viceCaptainId"   to viceCaptainId,
        "viceCaptainName" to (players.find { it.id == viceCaptainId }?.name ?: ""),
        "teamNumber"      to teamNumber,
        "wkCount"         to players.count { it.role == "WK" },
        "batCount"        to players.count { it.role == "BAT" },
        "arCount"         to players.count { it.role == "AR" },
        "bowlCount"       to players.count { it.role == "BOWL" },
        "creditsUsed"     to players.sumOf { it.credits.toDouble() }.toFloat(),
        "totalPoints"     to 0f,
        "isLocked"        to false,
        "createdAt"       to System.currentTimeMillis(),
        "players"         to players.map { p ->
            hashMapOf(
                "id"           to p.id,
                "name"         to p.name,
                "shortName"    to p.shortName,
                "team"         to p.team,
                "role"         to p.role,
                "credits"      to p.credits,
                "points"       to p.points,
                "isCaptain"    to (p.id == captainId),
                "isViceCaptain" to (p.id == viceCaptainId)
            )
        }
    )

    db.collection("users").document(uid).collection("teams").add(teamData)
        .addOnSuccessListener {
            db.collection("users").document(uid)
                .update("teamsCreated", com.google.firebase.firestore.FieldValue.increment(1))
            onSuccess()
        }
        .addOnFailureListener { e -> onError(e.message ?: "Save failed") }
}

// ─────────────────────────────────────────────
// VALIDATION HELPERS
// ─────────────────────────────────────────────

private fun computeValidation(
    selected: List<Player>,
    team1: String,
    team2: String
): TeamValidationState {
    val wk   = selected.count { it.role == "WK" }
    val bat  = selected.count { it.role == "BAT" }
    val ar   = selected.count { it.role == "AR" }
    val bowl = selected.count { it.role == "BOWL" }
    val t1   = selected.count { it.team == team1 }
    val t2   = selected.count { it.team == team2 }
    val cr   = 100f - selected.sumOf { it.credits.toDouble() }.toFloat()
    val valid = selected.size == 11 &&
            wk in 1..4 && bat in 3..6 && ar in 1..4 && bowl in 3..6 &&
            t1 <= 7 && t2 <= 7 && cr >= 0f
    return TeamValidationState(wk, bat, ar, bowl, t1, t2, selected.size, cr, valid)
}

private fun playerDisabledReason(
    player:     Player,
    selected:   List<Player>,
    validation: TeamValidationState
): String? {
    if (player.isSelected) return null
    val sameTeam = selected.count { it.team == player.team }
    val sameRole = selected.count { it.role == player.role }
    val roleMax  = when (player.role) { "WK" -> 4; "BAT" -> 6; "AR" -> 4; else -> 6 }
    return when {
        validation.totalSelected >= 11          -> "Max 11 players selected"
        validation.creditsLeft < player.credits -> "Need ${String.format("%.1f", player.credits - validation.creditsLeft)} more credits"
        sameTeam >= 7                           -> "Max 7 from one team"
        sameRole >= roleMax                     -> "Max $roleMax ${player.role} players"
        else                                    -> null
    }
}

// ─────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────

@Composable
fun TeamCreateScreen(
    matchTitle:  String = "CSK vs RCB",
    matchId:     String = "",
    onBack:      () -> Unit = {},
    onTeamSaved: () -> Unit = {}
) {
    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic            = LocalHapticFeedback.current

    val allPlayers    = remember { mutableStateListOf<Player>() }
    var step          by remember { mutableStateOf(TeamCreationStep.PLAYER_SELECTION) }
    var selectedTab   by remember { mutableStateOf("WK") }
    var sortType      by remember { mutableStateOf(SortType.POINTS) }
    var searchQuery   by remember { mutableStateOf("") }
    var filterTeam    by remember { mutableStateOf("ALL") }
    var captainId     by remember { mutableStateOf("") }
    var viceCaptainId by remember { mutableStateOf("") }
    var isLoading     by remember { mutableStateOf(true) }
    var loadError     by remember { mutableStateOf("") }
    var isSaving      by remember { mutableStateOf(false) }
    var teamNumber    by remember { mutableStateOf(1) }

    // Load real squad from API only — no dummy fallback
    LaunchedEffect(matchId) {
        isLoading = true
        loadError = ""
        if (matchId.isEmpty()) {
            loadError = "Match ID not found"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            when (val res = CricApiRepository.getSquad(matchId)) {
                is ApiResult.Success -> {
                    if (res.data.isNotEmpty()) {
                        allPlayers.clear()
                        allPlayers.addAll(res.data)
                    } else {
                        loadError = "No squad data available for this match"
                    }
                }
                is ApiResult.Error -> {
                    loadError = res.message
                }
                else -> {
                    loadError = "Failed to load squad"
                }
            }
        } catch (e: Exception) {
            loadError = e.message ?: "Failed to load squad"
        }

        // Get existing team count
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                val snap = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).collection("teams")
                    .whereEqualTo("matchId", matchId).get().await()
                teamNumber = snap.size() + 1
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    val selected   = allPlayers.filter { it.isSelected }
    val validation = computeValidation(selected, team1, team2)

    fun togglePlayer(player: Player) {
        val idx = allPlayers.indexOf(player)
        if (idx < 0) return
        if (player.isSelected) {
            allPlayers[idx] = player.copy(isSelected = false)
            if (captainId == player.id)     captainId = ""
            if (viceCaptainId == player.id) viceCaptainId = ""
        } else {
            val reason = playerDisabledReason(player, selected, validation)
            if (reason != null) {
                scope.launch { snackbarHostState.showSnackbar(reason) }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                allPlayers[idx] = player.copy(isSelected = true)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier       = Modifier.padding(16.dp),
                    containerColor = Color(0xFF1C1C1C),
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(10.dp),
                    action = {
                        TextButton(onClick = { data.dismiss() }) {
                            Text("OK", color = D11Red, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                ) { Text(data.visuals.message, fontWeight = FontWeight.SemiBold) }
            }
        },
        containerColor = Color.Transparent
    ) { pad ->
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "step"
        ) { currentStep ->
            when (currentStep) {
                TeamCreationStep.PLAYER_SELECTION -> PlayerSelectionContent(
                    modifier       = Modifier.padding(pad),
                    team1          = team1,
                    team2          = team2,
                    matchTitle     = matchTitle,
                    allPlayers     = allPlayers,
                    validation     = validation,
                    selected       = selected,
                    selectedTab    = selectedTab,
                    sortType       = sortType,
                    searchQuery    = searchQuery,
                    filterTeam     = filterTeam,
                    isLoading      = isLoading,
                    loadError      = loadError,
                    onTabChange    = { selectedTab = it },
                    onSortChange   = { sortType = it },
                    onSearchChange = { searchQuery = it },
                    onFilterChange = { filterTeam = it },
                    onTogglePlayer = { togglePlayer(it) },
                    onRetry        = {
                        scope.launch {
                            isLoading = true; loadError = ""
                            try {
                                when (val res = CricApiRepository.getSquad(matchId)) {
                                    is ApiResult.Success -> {
                                        if (res.data.isNotEmpty()) { allPlayers.clear(); allPlayers.addAll(res.data) }
                                        else loadError = "No squad available"
                                    }
                                    is ApiResult.Error -> loadError = res.message
                                    else -> loadError = "Failed to load"
                                }
                            } catch (e: Exception) { loadError = e.message ?: "Error" }
                            isLoading = false
                        }
                    },
                    onBack         = onBack,
                    onContinue     = { step = TeamCreationStep.CVC_SELECTION }
                )
                TeamCreationStep.CVC_SELECTION -> CVCSelectionScreen(
                    modifier        = Modifier.padding(pad),
                    players         = selected,
                    matchTitle      = matchTitle,
                    captainId       = captainId,
                    viceCaptainId   = viceCaptainId,
                    isSaving        = isSaving,
                    onCaptainSelect = { id -> if (viceCaptainId != id) captainId = if (captainId == id) "" else id },
                    onVCSelect      = { id -> if (captainId != id) viceCaptainId = if (viceCaptainId == id) "" else id },
                    onBack          = { step = TeamCreationStep.PLAYER_SELECTION },
                    onSave          = {
                        isSaving = true
                        saveTeamToFirebase(
                            players       = selected,
                            captainId     = captainId,
                            viceCaptainId = viceCaptainId,
                            matchTitle    = matchTitle,
                            matchId       = matchId,
                            teamNumber    = teamNumber,
                            onSuccess     = { isSaving = false; onTeamSaved() },
                            onError       = { msg -> isSaving = false; scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// PLAYER SELECTION CONTENT
// ─────────────────────────────────────────────

@Composable
private fun PlayerSelectionContent(
    modifier:       Modifier,
    team1:          String,
    team2:          String,
    matchTitle:     String,
    allPlayers:     List<Player>,
    validation:     TeamValidationState,
    selected:       List<Player>,
    selectedTab:    String,
    sortType:       SortType,
    searchQuery:    String,
    filterTeam:     String,
    isLoading:      Boolean,
    loadError:      String,
    onTabChange:    (String) -> Unit,
    onSortChange:   (SortType) -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onTogglePlayer: (Player) -> Unit,
    onRetry:        () -> Unit,
    onBack:         () -> Unit,
    onContinue:     () -> Unit
) {
    val creditColor = when {
        validation.creditsLeft < 0f  -> D11Red
        validation.creditsLeft < 10f -> Color(0xFFFF6B00)
        validation.creditsLeft < 20f -> D11Yellow
        else                         -> Color.White
    }

    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0D7A42), Color(0xFF08522C)), startY = 0f, endY = 600f)
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Green header
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Create Team", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text(matchTitle, color = Color(0xCCFFFFFF), fontSize = 11.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(String.format("%.1f", validation.creditsLeft), color = creditColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Credits Left", color = Color(0xBBFFFFFF), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Stats row
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 12.dp, vertical = 9.dp),
                    Arrangement.SpaceEvenly, Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${validation.totalSelected}/11", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Players", color = Color(0xBBFFFFFF), fontSize = 10.sp)
                    }
                    Box(Modifier.width(1.dp).height(32.dp).background(Color(0x44FFFFFF)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$team1 ${validation.team1Count}  |  $team2 ${validation.team2Count}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Team Split", color = Color(0xBBFFFFFF), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Role counts
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    listOf(
                        Triple("WK",   validation.wkCount,   "1-4"),
                        Triple("BAT",  validation.batCount,  "3-6"),
                        Triple("AR",   validation.arCount,   "1-4"),
                        Triple("BOWL", validation.bowlCount, "3-6")
                    ).forEach { (role, count, range) ->
                        val color = when (role) { "WK" -> Color(0xFF82B1FF); "BAT" -> D11Green; "AR" -> D11Red; else -> D11Yellow }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$count", color = if (count > 0) color else Color(0x99FFFFFF), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(role, color = Color(0xCCFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(range, color = Color(0x77FFFFFF), fontSize = 9.sp)
                        }
                    }
                }
            }

            // White card section
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Color(0xFFF5F5F5))) {
                Column(Modifier.fillMaxSize()) {
                    TCSearchBar(query = searchQuery, onQueryChange = onSearchChange)
                    TeamFilterRow(team1 = team1, team2 = team2, filterTeam = filterTeam, onFilterChange = onFilterChange)
                    RoleTabRow(allPlayers = allPlayers, selectedTab = selectedTab, onTabChange = onTabChange)
                    SortRow(sortType = sortType, onSortChange = onSortChange)
                    ColumnHeader()

                    when {
                        isLoading -> {
                            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = D11Red)
                                    Text("Loading squad...", color = Color(0xFF888888), fontSize = 13.sp)
                                }
                            }
                        }
                        loadError.isNotEmpty() -> {
                            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
                                    Text("😔", fontSize = 44.sp)
                                    Text("Squad not available", color = Color(0xFF111111), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(loadError, color = Color(0xFF888888), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp)) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        else -> {
                            val filtered = allPlayers
                                .filter { it.role == selectedTab }
                                .filter { filterTeam == "ALL" || it.team == filterTeam }
                                .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                                .sortedByDescending { p ->
                                    when (sortType) {
                                        SortType.POINTS    -> p.points
                                        SortType.CREDITS   -> p.credits
                                        SortType.SELECTION -> p.selectionPercent.toFloat()
                                    }
                                }
                            if (filtered.isEmpty()) {
                                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🏏", fontSize = 36.sp)
                                        Text("No players found", color = Color(0xFF888888), fontSize = 14.sp)
                                    }
                                }
                            } else {
                                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 96.dp)) {
                                    items(filtered, key = { it.id }) { player ->
                                        val reason = playerDisabledReason(
                                            player,
                                            allPlayers.filter { it.isSelected },
                                            computeValidation(allPlayers.filter { it.isSelected }, team1, team2)
                                        )
                                        Dream11PlayerCard(
                                            player     = player,
                                            team1      = team1,
                                            team2      = team2,
                                            isDisabled = reason != null,
                                            onToggle   = { onTogglePlayer(player) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                BottomActionBar(
                    modifier   = Modifier.align(Alignment.BottomCenter),
                    validation = validation,
                    onContinue = onContinue
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// UI COMPONENTS
// ─────────────────────────────────────────────

@Composable
private fun TCSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp)).background(Color(0xFFF0F0F0))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        Arrangement.spacedBy(8.dp), Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
        BasicTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.weight(1f), singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search players...", color = Color(0xFF999999), fontSize = 14.sp)
                inner()
            }
        )
        if (query.isNotEmpty()) Icon(Icons.Default.Close, null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp).clickable { onQueryChange("") })
    }
}

@Composable
private fun TeamFilterRow(team1: String, team2: String, filterTeam: String, onFilterChange: (String) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(listOf("ALL", team1, team2)) { label ->
            val active = filterTeam == label
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(if (active) D11Red else Color(0xFFF0F0F0))
                    .clickable { onFilterChange(label) }.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(label, color = if (active) Color.White else Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RoleTabRow(allPlayers: List<Player>, selectedTab: String, onTabChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("WK", "BAT", "AR", "BOWL").forEach { role ->
            val active = selectedTab == role
            val count  = allPlayers.count { it.role == role && it.isSelected }
            Box(
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(if (active) D11Red else Color.White)
                    .border(1.dp, if (active) D11Red else Color(0xFFDDDDDD), RoundedCornerShape(20.dp))
                    .clickable { onTabChange(role) }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text("$role ($count)", color = if (active) Color.White else Color(0xFF444444), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SortRow(sortType: SortType, onSortChange: (SortType) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(horizontal = 12.dp, vertical = 5.dp),
        Arrangement.spacedBy(8.dp), Alignment.CenterVertically
    ) {
        Text("Sort:", color = Color(0xFF888888), fontSize = 12.sp)
        listOf(SortType.POINTS to "Points", SortType.CREDITS to "Credits", SortType.SELECTION to "Sel %").forEach { (sort, label) ->
            val active = sortType == sort
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (active) Color(0xFF222222) else Color.White)
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                    .clickable { onSortChange(sort) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(label, color = if (active) Color.White else Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ColumnHeader() {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(horizontal = 16.dp, vertical = 5.dp),
        Arrangement.SpaceBetween
    ) {
        Text("Player", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Pts",     color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Credits", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}

@Composable
fun Dream11PlayerCard(player: Player, team1: String, team2: String, isDisabled: Boolean, onToggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val cardBg = when { player.isSelected -> Color(0xFFEEFBF3); isDisabled -> Color(0xFFFAFAFA); else -> Color.White }
    val avatarColors = if (player.team == team1) listOf(Color(0xFF1565C0), Color(0xFF003C8F)) else listOf(Color(0xFF2E7D32), Color(0xFF005005))

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp).scale(scale.value)
            .alpha(if (isDisabled && !player.isSelected) 0.45f else 1f),
        colors = CardDefaults.cardColors(cardBg),
        shape  = RoundedCornerShape(12.dp),
        border = if (player.isSelected) BorderStroke(1.5.dp, D11Green) else null
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(CircleShape).background(Brush.radialGradient(avatarColors))
                    .border(2.dp, if (player.isSelected) D11Green else Color(0xFFDDDDDD), CircleShape),
                Alignment.Center
            ) {
                Text(player.shortName.take(2).uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(player.name, color = Color(0xFF111111), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoleBadge(player.role)
                    TeamBadge(player.team)
                    Text("${player.selectionPercent}% sel", color = Color(0xFF888888), fontSize = 10.sp)
                }
                Text("${player.points} pts", color = D11Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(String.format("%.1f", player.credits), color = Color(0xFF111111), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("Cr", color = Color(0xFF888888), fontSize = 9.sp)
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(
                        when { player.isSelected -> D11Red; isDisabled -> Color(0xFFCCCCCC); else -> D11Green }
                    ).clickable(enabled = !isDisabled || player.isSelected, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        scope.launch { scale.animateTo(1.18f, tween(70)); scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
                        onToggle()
                    },
                    Alignment.Center
                ) {
                    Text(if (player.isSelected) "−" else "+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val (bg, fg) = when (role) {
        "WK"   -> Color(0xFFE8EAF6) to Color(0xFF3949AB)
        "BAT"  -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "AR"   -> Color(0xFFFCE4EC) to Color(0xFFC62828)
        else   -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
    }
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(role, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun TeamBadge(team: String) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFEEEEEE)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(team.take(4).uppercase(), color = Color(0xFF444444), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BottomActionBar(modifier: Modifier, validation: TeamValidationState, onContinue: () -> Unit) {
    val remaining = 11 - validation.totalSelected
    val btnLabel  = when {
        validation.totalSelected < 11 -> "Select $remaining more players"
        !validation.isValid           -> "Fix team composition"
        else                          -> "Preview & Continue →"
    }
    Box(modifier.fillMaxWidth().shadow(12.dp).background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Button(
            onClick  = { if (validation.isValid) onContinue() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(if (validation.isValid) D11Red else Color(0xFFCCCCCC)),
            shape    = RoundedCornerShape(10.dp)
        ) {
            Text(btnLabel, color = if (validation.isValid) Color.White else Color(0xFF888888), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

// ─────────────────────────────────────────────
// C/VC SELECTION SCREEN
// ─────────────────────────────────────────────

@Composable
fun CVCSelectionScreen(
    modifier:        Modifier = Modifier,
    players:         List<Player>,
    matchTitle:      String,
    captainId:       String,
    viceCaptainId:   String,
    isSaving:        Boolean,
    onCaptainSelect: (String) -> Unit,
    onVCSelect:      (String) -> Unit,
    onBack:          () -> Unit,
    onSave:          () -> Unit
) {
    val canSave = captainId.isNotEmpty() && viceCaptainId.isNotEmpty()

    Column(modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {
        // Dark header
        Column(Modifier.fillMaxWidth().background(Color(0xFF111111)).statusBarsPadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Select C & VC", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(matchTitle, color = Color(0x99FFFFFF), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // C/VC info boxes
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E1E1E))) {
                CvcInfoBox(Modifier.weight(1f), "C",  D11Yellow,          "2x points",   players.find { it.id == captainId }?.name ?: "Not selected")
                Box(Modifier.width(1.dp).height(56.dp).background(Color(0xFF333333)).align(Alignment.CenterVertically))
                CvcInfoBox(Modifier.weight(1f), "VC", Color(0xFFAAAAAA), "1.5x points", players.find { it.id == viceCaptainId }?.name ?: "Not selected")
            }
        }

        // Column headers
        Row(Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(horizontal = 16.dp, vertical = 5.dp), Arrangement.SpaceBetween) {
            Text("Player", color = Color(0xFF777777), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Pts",    color = Color(0xFF777777), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("C    VC",color = Color(0xFF777777), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(players.sortedByDescending { it.points }, key = { it.id }) { player ->
                CvcPlayerRow(
                    player       = player,
                    isCaptain    = captainId == player.id,
                    isVC         = viceCaptainId == player.id,
                    canSetCaptain = viceCaptainId != player.id,
                    canSetVC     = captainId != player.id,
                    onCaptain    = { onCaptainSelect(player.id) },
                    onVC         = { onVCSelect(player.id) }
                )
            }
        }

        // Bottom buttons
        Row(
            Modifier.fillMaxWidth().shadow(8.dp).background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                border   = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Text("Back", color = Color(0xFF333333), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick  = { if (canSave && !isSaving) onSave() },
                modifier = Modifier.weight(2f).height(52.dp),
                colors   = ButtonDefaults.buttonColors(if (canSave) D11Green else Color(0xFFCCCCCC)),
                shape    = RoundedCornerShape(10.dp),
                enabled  = canSave && !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (canSave) "Save Team ✓" else "Select C & VC", color = if (canSave) Color.White else Color(0xFF888888), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun CvcInfoBox(modifier: Modifier, badge: String, color: Color, subtitle: String, name: String) {
    Row(modifier.padding(12.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2A2A2A)).border(2.dp, color, CircleShape), Alignment.Center) {
            Text(badge, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column {
            Text(name, color = if (name == "Not selected") Color(0xFF888888) else color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color(0xFF666666), fontSize = 10.sp)
        }
    }
}

@Composable
private fun CvcPlayerRow(
    player: Player, isCaptain: Boolean, isVC: Boolean,
    canSetCaptain: Boolean, canSetVC: Boolean,
    onCaptain: () -> Unit, onVC: () -> Unit
) {
    val cardBg = when { isCaptain -> Color(0xFFFFFDE7); isVC -> Color(0xFFF3F3F3); else -> Color.White }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(cardBg), shape = RoundedCornerShape(10.dp),
        border = when { isCaptain -> BorderStroke(1.5.dp, D11Yellow); isVC -> BorderStroke(1.5.dp, Color(0xFFAAAAAA)); else -> null }
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(Modifier.weight(1f), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEEEEEE)).border(1.5.dp,
                        when { isCaptain -> D11Yellow; isVC -> Color(0xFFAAAAAA); else -> Color(0xFFDDDDDD) }, CircleShape),
                    Alignment.Center
                ) {
                    Text(player.shortName.take(2).uppercase(), color = Color(0xFF333333), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text(player.name, color = Color(0xFF111111), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { RoleBadge(player.role); TeamBadge(player.team) }
                }
            }
            Text("${player.points}", color = D11Green, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CvcBtn("C",  isCaptain, D11Yellow,          canSetCaptain || isCaptain, onCaptain)
                CvcBtn("VC", isVC,      Color(0xFFAAAAAA), canSetVC || isVC,            onVC)
            }
        }
    }
}

@Composable
private fun CvcBtn(label: String, active: Boolean, color: Color, enabled: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.size(44.dp).scale(scale.value).clip(CircleShape)
            .background(if (active) Color(0xFF1A1A00) else Color(0xFFEEEEEE))
            .border(2.dp, if (active) color else Color(0xFFCCCCCC), CircleShape)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) {
                scope.launch { scale.animateTo(1.2f, tween(70)); scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
                onClick()
            },
        Alignment.Center
    ) {
        Text(label, color = if (active) color else Color(0xFF555555), fontSize = if (label.length > 2) 10.sp else 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}