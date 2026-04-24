package com.example.dream11india

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun TeamPreviewScreen(
    matchTitle:    String       = "CSK vs RCB",
    teamNumber:    Int          = 1,
    selectedTeam:  List<Player> = emptyList(),
    matchId:       String       = "",
    isMatchStarted: Boolean     = false,
    isJoined:      Boolean      = false,
    onBack:        () -> Unit   = {},
    onEditTeam:    () -> Unit   = {},
    onJoinContest: () -> Unit   = {}
) {
    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic            = LocalHapticFeedback.current

    // Load from Firebase if selectedTeam is empty
    var team by remember { mutableStateOf(selectedTeam) }
    var isLoading by remember { mutableStateOf(selectedTeam.isEmpty()) }

    LaunchedEffect(matchId) {
        if (selectedTeam.isNotEmpty()) { isLoading = false; return@LaunchedEffect }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { isLoading = false; return@LaunchedEffect }
        try {
            val snap = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("teams")
                .whereEqualTo("matchId", matchId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1).get().await()
            if (!snap.isEmpty) {
                val doc = snap.documents.first()
                @Suppress("UNCHECKED_CAST")
                val players = doc.get("players") as? List<Map<String, Any>> ?: emptyList()
                val captainId = doc.getString("captainId") ?: ""
                val vcId      = doc.getString("viceCaptainId") ?: ""
                team = players.map { p ->
                    Player(
                        id              = p["id"] as? String ?: "",
                        name            = p["name"] as? String ?: "",
                        shortName       = p["shortName"] as? String ?: (p["name"] as? String ?: "").split(" ").lastOrNull() ?: "",
                        team            = p["team"] as? String ?: "",
                        role            = p["role"] as? String ?: "BAT",
                        credits         = (p["credits"] as? Number)?.toFloat() ?: 9f,
                        selectionPercent = 0,
                        points          = (p["points"] as? Number)?.toFloat() ?: 0f,
                        isSelected      = true,
                        isCaptain       = p["id"] == captainId,
                        isViceCaptain   = p["id"] == vcId
                    )
                }
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    val captain     = team.find { it.isCaptain }
    val viceCaptain = team.find { it.isViceCaptain }
    val wkList      = team.filter { it.role == "WK" }
    val batList     = team.filter { it.role == "BAT" }
    val arList      = team.filter { it.role == "AR" }
    val bowlList    = team.filter { it.role == "BOWL" }
    val totalCr     = team.sumOf { it.credits.toDouble() }.toFloat()
    val t1Count     = team.count { it.team == team1 }
    val t2Count     = team.count { it.team == team2 }
    val canJoin     = team.size == 11 && captain != null && viceCaptain != null && !isMatchStarted && !isJoined

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    fun validateAndJoin() {
        when {
            isMatchStarted        -> scope.launch { snackbarHostState.showSnackbar("Match has started. Cannot join now.") }
            isJoined              -> scope.launch { snackbarHostState.showSnackbar("Already joined this contest!") }
            team.size != 11       -> scope.launch { snackbarHostState.showSnackbar("Team must have exactly 11 players") }
            captain == null       -> scope.launch { snackbarHostState.showSnackbar("Please select a Captain first") }
            viceCaptain == null   -> scope.launch { snackbarHostState.showSnackbar("Please select a Vice Captain first") }
            else -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onJoinContest() }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(modifier = Modifier.padding(16.dp), containerColor = Color(0xFF1C1C1C), contentColor = Color.White, shape = RoundedCornerShape(10.dp),
                    action = { TextButton(onClick = { data.dismiss() }) { Text("OK", color = D11Red, fontWeight = FontWeight.ExtraBold) } }
                ) { Text(data.visuals.message, fontWeight = FontWeight.SemiBold) }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            PreviewTopBar(matchTitle = matchTitle, teamNumber = teamNumber, isMatchStarted = isMatchStarted, onBack = onBack, onEdit = { if (!isMatchStarted) onEditTeam() else scope.launch { snackbarHostState.showSnackbar("Match started — cannot edit team") } })

            when {
                isLoading -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = D11Red)
                            Text("Loading team...", color = Color(0xFF888888), fontSize = 13.sp)
                        }
                    }
                }
                team.isEmpty() -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(32.dp)) {
                            Text("🏏", fontSize = 52.sp)
                            Text("No team data found", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Please create a team first", color = Color(0xFF888888), fontSize = 13.sp)
                            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp)) {
                                Text("Go Back", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                else -> {
                    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
                        CaptainVCBar(captain = captain, viceCaptain = viceCaptain)
                    }
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                        item {
                            AnimatedVisibility(visible = visible, enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.96f, animationSpec = tween(500))) {
                                CricketFieldSection(wkList = wkList, batList = batList, arList = arList, bowlList = bowlList, team1 = team1, team2 = team2)
                            }
                        }
                        item {
                            AnimatedVisibility(visible = visible, enter = fadeIn(tween(600)) + slideInVertically { 40 }) {
                                TeamSummaryCard(team1 = team1, team2 = team2, t1Count = t1Count, t2Count = t2Count, wkCount = wkList.size, batCount = batList.size, arCount = arList.size, bowlCount = bowlList.size, totalCr = totalCr, captain = captain, viceCaptain = viceCaptain)
                            }
                        }
                        item {
                            AnimatedVisibility(visible = visible, enter = fadeIn(tween(700)) + slideInVertically { 60 }) {
                                PlayerListCard(team = team, team1 = team1, team2 = team2)
                            }
                        }
                    }
                }
            }
            BottomPreviewBar(canJoin = canJoin, isJoined = isJoined, isMatchStarted = isMatchStarted, onEdit = { if (!isMatchStarted) onEditTeam() else scope.launch { snackbarHostState.showSnackbar("Match started — cannot edit") } }, onJoinContest = { validateAndJoin() })
        }
    }
}

// ─── TOP BAR ────────────────────────────────

@Composable
private fun PreviewTopBar(matchTitle: String, teamNumber: Int, isMatchStarted: Boolean, onBack: () -> Unit, onEdit: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFCC0000), Color(0xFF880000))))) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Team Preview", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 11.sp)
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x44FFFFFF)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("T$teamNumber", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        if (isMatchStarted) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A3A1A)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("LOCKED", color = D11Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (!isMatchStarted) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FFFFFF)).clickable { onEdit() }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text("Edit Team", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── C/VC BAR ───────────────────────────────

@Composable
private fun CaptainVCBar(captain: Player?, viceCaptain: Player?) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 16.dp, vertical = 9.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        CvcChip("C",  captain?.name ?: "Not selected",    "2× points",   if (captain != null) D11Yellow else Color(0xFF444444),     if (captain != null) Color.White else Color(0xFF888888))
        Box(Modifier.width(1.dp).height(32.dp).background(Color(0xFF333333)))
        CvcChip("VC", viceCaptain?.name ?: "Not selected","1.5× points", if (viceCaptain != null) Color(0xFFAAAAAA) else Color(0xFF444444), if (viceCaptain != null) Color.White else Color(0xFF888888))
    }
}

@Composable
private fun CvcChip(label: String, name: String, mult: String, bgColor: Color, nameColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(bgColor), Alignment.Center) {
            Text(label, color = Color(0xFF111111), fontSize = if (label == "C") 14.sp else 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column {
            Text(name, color = nameColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(mult, color = Color(0xFF888888), fontSize = 10.sp)
        }
    }
}

// ─── FIELD ──────────────────────────────────

@Composable
private fun CricketFieldSection(wkList: List<Player>, batList: List<Player>, arList: List<Player>, bowlList: List<Player>, team1: String, team2: String) {
    Box(Modifier.fillMaxWidth().height(520.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D5C2A), Color(0xFF1A7A38), Color(0xFF22933F), Color(0xFF1A7A38), Color(0xFF0D5C2A)))))
        FieldMarkings()
        Column(Modifier.fillMaxSize().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
            FieldRoleRow("WICKET-KEEPERS", wkList,   team1, team2)
            FieldRoleRow("BATTERS",        batList,  team1, team2)
            FieldRoleRow("ALL-ROUNDERS",   arList,   team1, team2)
            FieldRoleRow("BOWLERS",        bowlList, team1, team2)
        }
    }
}

@Composable
private fun FieldMarkings() {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f; val cy = size.height / 2f
        drawCircle(Color(0x18FFFFFF), size.width * 0.44f, Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
        drawCircle(Color(0x14FFFFFF), size.width * 0.26f, Offset(cx, cy), style = Stroke(1.dp.toPx()))
        drawRoundRect(Color(0x22FFFFFF), Offset(cx - 18.dp.toPx(), cy - 55.dp.toPx()), androidx.compose.ui.geometry.Size(36.dp.toPx(), 110.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
        drawCircle(Color(0x1AFFFFFF), 22.dp.toPx(), Offset(cx, cy))
    }
}

@Composable
private fun FieldRoleRow(label: String, players: List<Player>, team1: String, team2: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xBBFFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            players.forEach { FieldPlayerCard(it, team1, team2) }
        }
    }
}

@Composable
private fun FieldPlayerCard(player: Player, team1: String, team2: String) {
    val glowAnim = rememberInfiniteTransition(label = "g_${player.id}")
    val glowAlpha by glowAnim.animateFloat(
        if (player.isCaptain || player.isViceCaptain) 0.4f else 0f,
        if (player.isCaptain || player.isViceCaptain) 0.9f else 0f,
        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "ga"
    )
    val avatarGradient = if (player.team == team1) listOf(Color(0xFF1565C0), Color(0xFF003580)) else listOf(Color(0xFF2E7D32), Color(0xFF004D00))
    val glowColor = when { player.isCaptain -> D11Yellow; player.isViceCaptain -> Color(0xFFAAAAAA); else -> Color.Transparent }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (player.isCaptain || player.isViceCaptain) Box(Modifier.size(62.dp).clip(CircleShape).background(glowColor.copy(alpha = glowAlpha * 0.25f)).align(Alignment.Center))
            Box(Modifier.size(54.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Brush.radialGradient(avatarGradient)).border(if (player.isCaptain || player.isViceCaptain) 2.dp else 1.dp, glowColor.copy(alpha = if (player.isCaptain || player.isViceCaptain) 1f else 0.3f), CircleShape), Alignment.Center) {
                Text(player.shortName.take(2).uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (player.isCaptain || player.isViceCaptain) Box(Modifier.size(19.dp).clip(CircleShape).background(if (player.isCaptain) D11Yellow else Color(0xFFBBBBBB)).shadow(2.dp, CircleShape), Alignment.Center) {
                Text(if (player.isCaptain) "C" else "VC", color = Color(0xFF111111), fontSize = if (player.isCaptain) 9.sp else 7.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(player.shortName.take(9), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${String.format("%.1f", player.credits)} Cr", color = Color(0x99FFFFFF), fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

// ─── SUMMARY CARD ───────────────────────────

@Composable
private fun TeamSummaryCard(team1: String, team2: String, t1Count: Int, t2Count: Int, wkCount: Int, batCount: Int, arCount: Int, bowlCount: Int, totalCr: Float, captain: Player?, viceCaptain: Player?) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Team Summary", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(D11Green.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("11 Players ✓", color = D11Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                listOf(Triple("WK", wkCount, Color(0xFF82B1FF)), Triple("BAT", batCount, D11Green), Triple("AR", arCount, D11Red), Triple("BOWL", bowlCount, D11Yellow)).forEach { (role, count, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), Alignment.Center) { Text("$count", color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
                        Spacer(Modifier.height(4.dp))
                        Text(role, color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column { Text(team1, color = Color(0xFF888888), fontSize = 11.sp); Text("$t1Count Players", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                Column(horizontalAlignment = Alignment.End) { Text(team2, color = Color(0xFF888888), fontSize = 11.sp); Text("$t2Count Players", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Credits Used", color = Color(0xFF888888), fontSize = 12.sp)
                Text("${String.format("%.1f", totalCr)} / 100", color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("C" to (captain?.name ?: "—") to D11Yellow, "VC" to (viceCaptain?.name ?: "—") to Color(0xFFAAAAAA)).forEach { (pair, color) ->
                    val (badge, name) = pair
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(color), Alignment.Center) { Text(badge, color = Color(0xFF111111), fontSize = if (badge == "C") 12.sp else 9.sp, fontWeight = FontWeight.ExtraBold) }
                        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ─── PLAYER LIST ────────────────────────────

@Composable
private fun PlayerListCard(team: List<Player>, team1: String, team2: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text("All Players", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), Arrangement.SpaceBetween) {
                Text("Player", color = Color(0xFF666666), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { Text("Pts", color = Color(0xFF666666), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Credits", color = Color(0xFF666666), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
            listOf("WK" to "Wicket-Keepers", "BAT" to "Batters", "AR" to "All-Rounders", "BOWL" to "Bowlers").forEach { (role, roleLabel) ->
                val rolePlayers = team.filter { it.role == role }
                if (rolePlayers.isNotEmpty()) {
                    val roleColor = when (role) { "WK" -> Color(0xFF82B1FF); "BAT" -> D11Green; "AR" -> D11Red; else -> D11Yellow }
                    Box(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 5.dp)) {
                        Text(roleLabel, color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    rolePlayers.forEach { PreviewPlayerRow(it, team1, team2) }
                }
            }
        }
    }
}

@Composable
private fun PreviewPlayerRow(player: Player, team1: String, team2: String) {
    val avatarColors = if (player.team == team1) listOf(Color(0xFF1565C0), Color(0xFF003580)) else listOf(Color(0xFF2E7D32), Color(0xFF004D00))
    Row(
        Modifier.fillMaxWidth().background(when { player.isCaptain -> Color(0xFF1A1600); player.isViceCaptain -> Color(0xFF161616); else -> Color.Transparent }).padding(horizontal = 14.dp, vertical = 8.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.radialGradient(avatarColors)).border(1.5.dp, when { player.isCaptain -> D11Yellow; player.isViceCaptain -> Color(0xFFAAAAAA); else -> Color(0xFF333333) }, CircleShape), Alignment.Center) {
                    Text(player.shortName.take(2).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (player.isCaptain || player.isViceCaptain) Box(Modifier.size(16.dp).clip(CircleShape).background(if (player.isCaptain) D11Yellow else Color(0xFFAAAAAA)), Alignment.Center) {
                    Text(if (player.isCaptain) "C" else "VC", color = Color(0xFF111111), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column {
                Text(player.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniRoleBadge(player.role); MiniTeamBadge(player.team)
                    if (player.selectionPercent > 0) Text("${player.selectionPercent}% sel", color = Color(0xFF666666), fontSize = 10.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (player.points > 0) Text("${player.points}", color = D11Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(String.format("%.1f", player.credits), color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
    HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
}

@Composable
private fun MiniRoleBadge(role: String) {
    val (bg, fg) = when (role) { "WK" -> Color(0xFF1A1F3C) to Color(0xFF82B1FF); "BAT" -> Color(0xFF0D2A10) to Color(0xFF69F0AE); "AR" -> Color(0xFF2A0D10) to Color(0xFFFF8A80); else -> Color(0xFF2A2800) to Color(0xFFFFFF8D) }
    Box(Modifier.clip(RoundedCornerShape(3.dp)).background(bg).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(role, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun MiniTeamBadge(team: String) {
    Box(Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF2A2A2A)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(team.take(4).uppercase(), color = Color(0xFF999999), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
}

// ─── BOTTOM BAR ─────────────────────────────

@Composable
private fun BottomPreviewBar(canJoin: Boolean, isJoined: Boolean, isMatchStarted: Boolean, onEdit: () -> Unit, onJoinContest: () -> Unit) {
    Box(Modifier.fillMaxWidth().shadow(16.dp).background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isMatchStarted) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(54.dp), border = BorderStroke(1.5.dp, Color(0xFF555555)), shape = RoundedCornerShape(12.dp)) {
                    Text("Edit Team", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Button(
                onClick  = onJoinContest,
                modifier = Modifier.weight(2f).height(54.dp),
                colors   = ButtonDefaults.buttonColors(when { isJoined -> Color(0xFF003300); isMatchStarted -> Color(0xFF333333); canJoin -> D11Green; else -> Color(0xFF333333) }),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text(
                    when { isJoined -> "Joined ✓"; isMatchStarted -> "Match Started"; canJoin -> "Join Contest →"; else -> "Complete Team" },
                    color = when { isJoined -> D11Green; canJoin -> Color.White; else -> Color(0xFF888888) },
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                )
            }
        }
    }
}