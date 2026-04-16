package com.example.dream11india

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun MyTeamsScreen(
    matchId: String = "",
    matchTitle: String = "CSK vs RCB",
    onBack: () -> Unit = {},
    onCreateTeam: () -> Unit = {},
    onJoinContest: (SavedTeam) -> Unit = {},
    onEditTeam: (SavedTeam) -> Unit = {}
) {
    val db  = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var teams              by remember { mutableStateOf<List<SavedTeam>>(emptyList()) }
    var isLoading          by remember { mutableStateOf(true) }
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var teamToDelete       by remember { mutableStateOf<SavedTeam?>(null) }
    var showPreviewDialog  by remember { mutableStateOf(false) }
    var selectedPreview    by remember { mutableStateOf<SavedTeam?>(null) }
    val snackbar           = remember { SnackbarHostState() }
    var snackMsg           by remember { mutableStateOf("") }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) { snackbar.showSnackbar(snackMsg); snackMsg = "" }
    }

    // Real-time teams from Firebase
    LaunchedEffect(uid, matchId) {
        if (uid.isEmpty()) { isLoading = false; return@LaunchedEffect }
        db.collection("users").document(uid).collection("teams")
            .whereEqualTo("matchId", matchId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    teams = it.documents.mapIndexed { idx, doc ->
                        @Suppress("UNCHECKED_CAST")
                        val players = doc.get("players") as? List<Map<String,Any>> ?: emptyList()
                        SavedTeam(
                            id              = doc.id,
                            userId          = uid,
                            matchId         = matchId,
                            matchTitle      = matchTitle,
                            teamNumber      = doc.getLong("teamNumber")?.toInt() ?: (idx + 1),
                            playerNames     = players.mapNotNull { p -> p["name"] as? String },
                            captainId       = doc.getString("captainId") ?: "",
                            captainName     = players.find { p -> p["id"] == doc.getString("captainId") }?.get("name") as? String ?: "",
                            viceCaptainId   = doc.getString("viceCaptainId") ?: "",
                            viceCaptainName = players.find { p -> p["id"] == doc.getString("viceCaptainId") }?.get("name") as? String ?: "",
                            wkCount         = players.count { p -> p["role"] == "WK" },
                            batCount        = players.count { p -> p["role"] == "BAT" },
                            arCount         = players.count { p -> p["role"] == "AR" },
                            bowlCount       = players.count { p -> p["role"] == "BOWL" },
                            totalPoints     = doc.getDouble("totalPoints")?.toFloat() ?: 0f,
                            isLocked        = doc.getBoolean("isLocked") ?: false,
                            createdAt       = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    isLoading = false
                }
            }
    }

    // Clone team
    fun cloneTeam(team: SavedTeam) {
        if (teams.size >= 20) { snackMsg = "Maximum 20 teams allowed!"; return }
        db.collection("users").document(uid).collection("teams").add(mapOf(
            "matchId" to matchId, "teamNumber" to (teams.size + 1),
            "captainId" to team.captainId, "viceCaptainId" to team.viceCaptainId,
            "isLocked" to false, "createdAt" to System.currentTimeMillis()
        )).addOnSuccessListener { snackMsg = "Team cloned!" }
          .addOnFailureListener { snackMsg = "Clone failed!" }
    }

    // Delete dialog
    if (showDeleteDialog && teamToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = D11CardBg,
            shape            = RoundedCornerShape(16.dp),
            title = { Text("Delete Team ${teamToDelete!!.teamNumber}?", color = D11White, fontWeight = FontWeight.Bold) },
            text  = { Text("This team will be permanently deleted.", color = D11Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        if (!teamToDelete!!.isLocked) {
                            db.collection("users").document(uid).collection("teams")
                                .document(teamToDelete!!.id).delete()
                                .addOnSuccessListener { snackMsg = "Team deleted!" }
                            showDeleteDialog = false
                        } else { snackMsg = "Cannot delete - match has started!"; showDeleteDialog = false }
                    },
                    colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(8.dp)
                ) { Text("Delete", color = D11White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }, border = BorderStroke(1.dp, D11Gray), shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", color = D11Gray)
                }
            }
        )
    }

    // Preview dialog
    if (showPreviewDialog && selectedPreview != null) {
        val team = selectedPreview!!
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            containerColor   = D11CardBg,
            shape            = RoundedCornerShape(16.dp),
            title = { Text("Team ${team.teamNumber} Preview", color = D11White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        listOf("WK" to team.wkCount, "BAT" to team.batCount, "AR" to team.arCount, "BOWL" to team.bowlCount).forEach { (role, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$count", color = D11Yellow, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text(role, color = D11Gray, fontSize = 11.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = D11Border)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column { Text("Captain (2x)", color = D11Gray, fontSize = 11.sp); Text(team.captainName.ifEmpty { "—" }, color = D11Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Vice Captain (1.5x)", color = D11Gray, fontSize = 11.sp); Text(team.viceCaptainName.ifEmpty { "—" }, color = Color(0xFFAAAAAA), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                    HorizontalDivider(color = D11Border)
                    if (team.playerNames.isNotEmpty()) {
                        Text("Players:", color = D11Gray, fontSize = 12.sp)
                        team.playerNames.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { name -> Text(name.split(" ").lastOrNull() ?: name, color = D11White, fontSize = 12.sp, modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPreviewDialog = false; onJoinContest(team) }, colors = ButtonDefaults.buttonColors(D11Green), shape = RoundedCornerShape(8.dp)) {
                    Text("Join Contest", color = D11White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPreviewDialog = false }, border = BorderStroke(1.dp, D11Gray), shape = RoundedCornerShape(8.dp)) {
                    Text("Close", color = D11Gray)
                }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(pad)) {

            // Top bar
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(D11Red, D11DarkRed))).statusBarsPadding()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = D11White, modifier = Modifier.size(17.dp))
                            }
                            Column {
                                Text("My Teams", color = D11White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 12.sp)
                            }
                        }
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("${teams.size}/20", color = D11White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    val inf   = rememberInfiniteTransition(label = "s")
                    val alpha by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(3) { Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).alpha(alpha).background(D11CardBg)) }
                    }
                }
                teams.isEmpty() -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("🏏", fontSize = 56.sp)
                            Text("No teams created yet", color = D11White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Create your first team now!", color = D11Gray, fontSize = 13.sp)
                            Button(onClick = onCreateTeam, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(48.dp)) {
                                Text("+ Create Team", color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(teams, key = { it.id }) { team ->
                            FullTeamCard(
                                team      = team,
                                onPreview = { selectedPreview = team; showPreviewDialog = true },
                                onJoin    = { selectedPreview = team; showPreviewDialog = true },
                                onEdit    = { if (team.isLocked) snackMsg = "Cannot edit - match started!" else onEditTeam(team) },
                                onDelete  = { if (team.isLocked) snackMsg = "Cannot delete - match started!" else { teamToDelete = team; showDeleteDialog = true } },
                                onClone   = { cloneTeam(team) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // Bottom create button
            if (!isLoading) {
                Box(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(16.dp)) {
                    Button(
                        onClick  = { if (teams.size >= 20) snackMsg = "Maximum 20 teams allowed!" else onCreateTeam() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors   = ButtonDefaults.buttonColors(if (teams.size < 20) D11Red else Color(0xFF444444)),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = teams.size < 20
                    ) {
                        Text(if (teams.size < 20) "+ Create New Team (${teams.size}/20)" else "Maximum 20 Teams Reached", color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FullTeamCard(
    team: SavedTeam,
    onPreview: () -> Unit,
    onJoin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(if (team.isLocked) Color(0xFF1A1A2A) else D11CardBg),
        shape    = RoundedCornerShape(14.dp),
        border   = if (team.isLocked) BorderStroke(1.dp, Color(0xFF333355)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(if (team.isLocked) Color(0xFF333355) else D11Red), Alignment.Center) {
                        Text("T${team.teamNumber}", color = D11White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Team ${team.teamNumber}", color = D11White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            if (team.isLocked) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF333355)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("LOCKED", color = Color(0xFF6666FF), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        }
                        if (team.totalPoints > 0) Text("${team.totalPoints} pts", color = D11Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Clone" to D11Green to Color(0xFF1A3A1A) to onClone,
                           "Edit"  to D11White  to D11LightGray     to onEdit,
                           "Del"   to D11Red    to Color(0xFF2A0000) to onDelete).forEach { (pair, action) ->
                        val (labelColor, bg) = pair
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).clickable { action() }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(labelColor.first, color = labelColor.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = D11Border)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                listOf("WK" to team.wkCount to Color(0xFF6666FF), "BAT" to team.batCount to D11Green, "AR" to team.arCount to D11Red, "BOWL" to team.bowlCount to D11Yellow).forEach { (pair, color) ->
                    val (role, count) = pair
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), Alignment.Center) {
                            Text("$count", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(role, color = D11Gray, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A00)).padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(D11Yellow), Alignment.Center) { Text("C", color = D11Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
                        Column { Text("Captain", color = D11Gray, fontSize = 9.sp); Text(team.captainName.ifEmpty { "Not set" }, color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                    }
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFAAAAAA)), Alignment.Center) { Text("VC", color = D11Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) }
                        Column { Text("Vice Captain", color = D11Gray, fontSize = 9.sp); Text(team.viceCaptainName.ifEmpty { "Not set" }, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f).height(44.dp), border = BorderStroke(1.dp, D11Gray), shape = RoundedCornerShape(10.dp)) {
                    Text("Preview", color = D11White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(onClick = onJoin, modifier = Modifier.weight(2f).height(44.dp), colors = ButtonDefaults.buttonColors(D11Green), shape = RoundedCornerShape(10.dp)) {
                    Text("Join Contest", color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}
