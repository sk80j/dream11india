package com.example.dream11india

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun MyTeamsScreen(
    matchId:       String = "",
    matchTitle:    String = "CSK vs RCB",
    onBack:        () -> Unit = {},
    onCreateTeam:  () -> Unit = {},
    onJoinContest: (SavedTeam) -> Unit = {},
    onEditTeam:    (SavedTeam) -> Unit = {}
) {
    val db  = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var teams             by remember { mutableStateOf<List<SavedTeam>>(emptyList()) }
    var joinedTeamIds     by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading         by remember { mutableStateOf(true) }
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var teamToDelete      by remember { mutableStateOf<SavedTeam?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var selectedPreview   by remember { mutableStateOf<SavedTeam?>(null) }
    var selectedSort      by remember { mutableStateOf("Newest") }
    val snackbar          = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    var snackMsg          by remember { mutableStateOf("") }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) { snackbar.showSnackbar(snackMsg); snackMsg = "" }
    }

    // Load teams real-time
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
                        val players = doc.get("players") as? List<Map<String, Any>> ?: emptyList()
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
        // Load joined team IDs
        db.collection("joined_contests")
            .whereEqualTo("userId", uid)
            .whereEqualTo("matchId", matchId)
            .addSnapshotListener { snap, _ ->
                snap?.let { joinedTeamIds = it.documents.mapNotNull { d -> d.getString("teamId") }.toSet() }
            }
    }

    // Full clone with all players
    fun cloneTeam(team: SavedTeam) {
        if (teams.size >= 20) { snackMsg = "Maximum 20 teams allowed!"; return }
        db.collection("users").document(uid).collection("teams")
            .document(team.id).get()
            .addOnSuccessListener { doc ->
                val data = doc.data?.toMutableMap() ?: return@addOnSuccessListener
                data["teamNumber"]  = teams.size + 1
                data["createdAt"]   = System.currentTimeMillis()
                data["isLocked"]    = false
                db.collection("users").document(uid).collection("teams").add(data)
                    .addOnSuccessListener { snackMsg = "Team ${team.teamNumber} cloned!" }
                    .addOnFailureListener { snackMsg = "Clone failed!" }
            }
    }

    val sortedTeams = when (selectedSort) {
        "Points"  -> teams.sortedByDescending { it.totalPoints }
        "Locked"  -> teams.sortedByDescending { it.isLocked }
        "Unused"  -> teams.filter { !joinedTeamIds.contains(it.id) }
        else      -> teams.sortedByDescending { it.createdAt }
    }

    // Delete Dialog
    if (showDeleteDialog && teamToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = Color(0xFF1A1A1A),
            shape            = RoundedCornerShape(16.dp),
            title = { Text("Delete Team ${teamToDelete!!.teamNumber}?", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = { Text("This team will be permanently deleted. This cannot be undone.", color = Color(0xFF888888), fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = {
                    if (!teamToDelete!!.isLocked) {
                        db.collection("users").document(uid).collection("teams")
                            .document(teamToDelete!!.id).delete()
                            .addOnSuccessListener { snackMsg = "Team deleted!" }
                        showDeleteDialog = false
                    } else { snackMsg = "Cannot delete — match has started!"; showDeleteDialog = false }
                }, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(8.dp)) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }, border = BorderStroke(1.dp, Color(0xFF888888)), shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", color = Color(0xFF888888))
                }
            }
        )
    }

    // Preview Dialog
    if (showPreviewDialog && selectedPreview != null) {
        val team = selectedPreview!!
        val isJoined = joinedTeamIds.contains(team.id)
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            containerColor   = Color(0xFF1A1A1A),
            shape            = RoundedCornerShape(16.dp),
            title = {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Team ${team.teamNumber}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    if (isJoined) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF003300)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("JOINED", color = D11Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Role counts
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).padding(12.dp), Arrangement.SpaceEvenly) {
                        listOf("WK" to team.wkCount to Color(0xFF82B1FF), "BAT" to team.batCount to D11Green, "AR" to team.arCount to D11Red, "BOWL" to team.bowlCount to D11Yellow).forEach { (pair, color) ->
                            val (role, count) = pair
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$count", color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text(role, color = Color(0xFF888888), fontSize = 10.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF333333))
                    // C / VC
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A00)).padding(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(22.dp).clip(CircleShape).background(D11Yellow), Alignment.Center) { Text("C", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
                                Column {
                                    Text("Captain (2x)", color = Color(0xFF888888), fontSize = 9.sp)
                                    Text(team.captainName.ifEmpty { "Not set" }, color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).padding(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFAAAAAA)), Alignment.Center) { Text("VC", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) }
                                Column {
                                    Text("VC (1.5x)", color = Color(0xFF888888), fontSize = 9.sp)
                                    Text(team.viceCaptainName.ifEmpty { "Not set" }, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    // Players list
                    if (team.playerNames.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFF333333))
                        Text("Players (${team.playerNames.size})", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        team.playerNames.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { name ->
                                    Box(Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF111111)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                        Text(name.split(" ").lastOrNull() ?: name, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    if (team.totalPoints > 0) {
                        HorizontalDivider(color = Color(0xFF333333))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total Points", color = Color(0xFF888888), fontSize = 12.sp)
                            Text("${team.totalPoints}", color = D11Green, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPreviewDialog = false; onJoinContest(team) }, colors = ButtonDefaults.buttonColors(D11Green), shape = RoundedCornerShape(8.dp), enabled = !team.isLocked) {
                    Text("Join Contest", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPreviewDialog = false }, border = BorderStroke(1.dp, Color(0xFF888888)), shape = RoundedCornerShape(8.dp)) {
                    Text("Close", color = Color(0xFF888888))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) { data ->
            Snackbar(modifier = Modifier.padding(14.dp), containerColor = Color(0xFF1C1C1C), contentColor = Color.White, shape = RoundedCornerShape(10.dp)) {
                Text(data.visuals.message, fontWeight = FontWeight.SemiBold)
            }
        }},
        containerColor = Color(0xFF0A0A0A)
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Top bar
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(D11Red, D11DarkRed))).statusBarsPadding()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                            Column {
                                Text("My Teams", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                                Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 11.sp)
                            }
                        }
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("${teams.size}/20", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Sort tabs
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Newest", "Points", "Locked", "Unused")) { sort ->
                            val active = selectedSort == sort
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (active) Color.White else Color(0x33FFFFFF)).clickable { selectedSort = sort }.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text(sort, color = if (active) D11Red else Color.White, fontSize = 12.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    val inf   = rememberInfiniteTransition(label = "s")
                    val alpha by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(3) { Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).alpha(alpha).background(Color(0xFF1A1A1A))) }
                    }
                }
                sortedTeams.isEmpty() && selectedSort == "Unused" -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("✅", fontSize = 44.sp)
                            Text("All teams have been used!", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("All your teams are joined in contests", color = Color(0xFF888888), fontSize = 13.sp)
                        }
                    }
                }
                teams.isEmpty() -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
                            Text("🏏", fontSize = 56.sp)
                            Text("No teams created yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Create your first team and start winning!", color = Color(0xFF888888), fontSize = 13.sp, textAlign = TextAlign.Center)
                            Button(onClick = onCreateTeam, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create Team", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sortedTeams, key = { it.id }) { team ->
                            val isJoined = joinedTeamIds.contains(team.id)
                            FullTeamCard(
                                team     = team,
                                isJoined = isJoined,
                                onPreview = { selectedPreview = team; showPreviewDialog = true },
                                onJoin    = { selectedPreview = team; showPreviewDialog = true },
                                onEdit    = { if (team.isLocked) snackMsg = "Cannot edit — match started!" else onEditTeam(team) },
                                onDelete  = { if (team.isLocked) snackMsg = "Cannot delete — match started!" else { teamToDelete = team; showDeleteDialog = true } },
                                onClone   = { cloneTeam(team) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // Bottom create button
            if (!isLoading) {
                Box(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick  = { if (teams.size >= 20) snackMsg = "Maximum 20 teams allowed!" else onCreateTeam() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors   = ButtonDefaults.buttonColors(if (teams.size < 20) D11Red else Color(0xFF444444)),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = teams.size < 20
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (teams.size < 20) "Create New Team (${teams.size}/20)" else "Maximum 20 Teams Reached",
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullTeamCard(
    team:      SavedTeam,
    isJoined:  Boolean = false,
    onPreview: () -> Unit,
    onJoin:    () -> Unit,
    onEdit:    () -> Unit,
    onDelete:  () -> Unit,
    onClone:   () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(if (team.isLocked) Color(0xFF1A1A2A) else Color(0xFF1A1A1A)),
        shape    = RoundedCornerShape(14.dp),
        border   = when {
            team.isLocked -> BorderStroke(1.dp, Color(0xFF333355))
            isJoined      -> BorderStroke(1.dp, D11Green)
            else          -> BorderStroke(0.5.dp, Color(0xFF2A2A2A))
        }
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).then(
                        if (team.isLocked) Modifier.background(Color(0xFF333355)) else Modifier.background(Brush.linearGradient(listOf(D11Red, Color(0xFF8B0000))))
                    ), Alignment.Center) {
                        Text("T${team.teamNumber}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Team ${team.teamNumber}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            // Status badges
                            if (team.isLocked) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF333355)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("LOCKED", color = Color(0xFF6666FF), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            if (isJoined) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF003300)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("JOINED", color = D11Green, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            val isValid = team.wkCount in 1..4 && team.batCount in 3..6 && team.arCount in 1..4 && team.bowlCount in 3..6 && (team.wkCount + team.batCount + team.arCount + team.bowlCount) == 11
                            if (!isValid) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A1A00)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("INCOMPLETE", color = D11Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        }
                        if (team.totalPoints > 0) Text("${team.totalPoints} pts", color = D11Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(Triple("Clone", D11Green, Color(0xFF0A2A0A)), Triple("Edit", Color.White, Color(0xFF2A2A2A)), Triple("Del", D11Red, Color(0xFF2A0000))).forEach { (label, textColor, bg) ->
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).clickable {
                            when (label) { "Clone" -> onClone(); "Edit" -> onEdit(); "Del" -> onDelete() }
                        }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Spacer(Modifier.height(12.dp))

            // Role counts
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                listOf("WK" to team.wkCount to Color(0xFF82B1FF), "BAT" to team.batCount to D11Green, "AR" to team.arCount to D11Red, "BOWL" to team.bowlCount to D11Yellow).forEach { (pair, color) ->
                    val (role, count) = pair
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), Alignment.Center) {
                            Text("$count", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(role, color = Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // C / VC row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A00)).padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(D11Yellow), Alignment.Center) { Text("C", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
                        Column {
                            Text("Captain (2x)", color = Color(0xFF888888), fontSize = 9.sp)
                            Text(team.captainName.ifEmpty { "Not set" }, color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFAAAAAA)), Alignment.Center) { Text("VC", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) }
                        Column {
                            Text("Vice Cap (1.5x)", color = Color(0xFF888888), fontSize = 9.sp)
                            Text(team.viceCaptainName.ifEmpty { "Not set" }, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Players preview
            if (team.playerNames.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Players", color = Color(0xFF666666), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(team.playerNames.take(11)) { name ->
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF111111)).padding(horizontal = 7.dp, vertical = 4.dp)) {
                            Text(name.split(" ").lastOrNull() ?: name, color = Color(0xFFCCCCCC), fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, Color(0xFF555555)), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick  = onJoin,
                    modifier = Modifier.weight(2f).height(42.dp),
                    colors   = ButtonDefaults.buttonColors(if (team.isLocked) Color(0xFF444444) else D11Green),
                    shape    = RoundedCornerShape(10.dp),
                    enabled  = !team.isLocked
                ) {
                    Text(if (isJoined) "Join Another Contest" else "Join Contest", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
    }
}