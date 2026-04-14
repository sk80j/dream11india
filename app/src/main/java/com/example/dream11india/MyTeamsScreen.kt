package com.example.dream11india

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class SavedTeam(
    val id: String = "",
    val userId: String = "",
    val matchId: String = "",
    val matchTitle: String = "",
    val teamNumber: Int = 1,
    val players: List<String> = emptyList(),
    val playerNames: List<String> = emptyList(),
    val captainId: String = "",
    val captainName: String = "",
    val viceCaptainId: String = "",
    val viceCaptainName: String = "",
    val wkCount: Int = 0,
    val batCount: Int = 0,
    val arCount: Int = 0,
    val bowlCount: Int = 0,
    val totalPoints: Float = 0f,
    val isLocked: Boolean = false,
    val createdAt: Long = 0L
)

data class ContestEntry(
    val id: String = "",
    val userId: String = "",
    val matchId: String = "",
    val contestId: String = "",
    val contestName: String = "",
    val teamId: String = "",
    val teamNumber: Int = 1,
    val entryFee: Int = 0,
    val points: Float = 0f,
    val rank: Int = 0,
    val joinedAt: Long = 0L
)

@Composable
fun MyTeamsScreen(
    matchId: String = "1",
    matchTitle: String = "CSK vs RCB",
    onBack: () -> Unit = {},
    onCreateTeam: () -> Unit = {},
    onJoinContest: (SavedTeam) -> Unit = {},
    onEditTeam: (SavedTeam) -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var teams by remember { mutableStateOf<List<SavedTeam>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var teamToDelete by remember { mutableStateOf<SavedTeam?>(null) }
    var showContestPicker by remember { mutableStateOf(false) }
    var selectedTeamForJoin by remember { mutableStateOf<SavedTeam?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var selectedPreviewTeam by remember { mutableStateOf<SavedTeam?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf("") }

    val safeMatchId = "match_${matchTitle.replace(" ", "_")}"

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    LaunchedEffect(uid, matchId) {
        if (uid.isNotEmpty()) {
            db.collection("teams")
                .whereEqualTo("userId", uid)
                .whereEqualTo("matchId", safeMatchId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(20)
                .addSnapshotListener { snap, _ ->
                    snap?.let {
                        teams = it.documents.map { doc ->
                            SavedTeam(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                matchId = doc.getString("matchId") ?: "",
                                matchTitle = doc.getString("matchTitle") ?: matchTitle,
                                teamNumber = doc.getLong("teamNumber")?.toInt() ?: 1,
                                players = (doc.get("players") as? List<String>) ?: emptyList(),
                                playerNames = (doc.get("playerNames") as? List<String>) ?: emptyList(),
                                captainId = doc.getString("captainId") ?: "",
                                captainName = doc.getString("captainName") ?: "",
                                viceCaptainId = doc.getString("viceCaptainId") ?: "",
                                viceCaptainName = doc.getString("viceCaptainName") ?: "",
                                wkCount = doc.getLong("wkCount")?.toInt() ?: 0,
                                batCount = doc.getLong("batCount")?.toInt() ?: 0,
                                arCount = doc.getLong("arCount")?.toInt() ?: 0,
                                bowlCount = doc.getLong("bowlCount")?.toInt() ?: 0,
                                totalPoints = doc.getDouble("totalPoints")?.toFloat() ?: 0f,
                                isLocked = doc.getBoolean("isLocked") ?: false,
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        }
                        isLoading = false
                    }
                }
        } else { isLoading = false }
    }

    // Clone team function
    fun cloneTeam(team: SavedTeam) {
        if (teams.size >= 20) {
            snackMsg = "Maximum 20 teams allowed!"
            return
        }
        val newTeamNumber = teams.size + 1
        val clonedTeam = mapOf(
            "userId" to uid,
            "matchId" to safeMatchId,
            "matchTitle" to matchTitle,
            "teamNumber" to newTeamNumber,
            "players" to team.players,
            "playerNames" to team.playerNames,
            "captainId" to team.captainId,
            "captainName" to team.captainName,
            "viceCaptainId" to team.viceCaptainId,
            "viceCaptainName" to team.viceCaptainName,
            "wkCount" to team.wkCount,
            "batCount" to team.batCount,
            "arCount" to team.arCount,
            "bowlCount" to team.bowlCount,
            "totalPoints" to 0f,
            "isLocked" to false,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("teams").add(clonedTeam)
            .addOnSuccessListener { snackMsg = "Team ${newTeamNumber} cloned!" }
            .addOnFailureListener { snackMsg = "Clone failed!" }
    }

    // Delete dialog
    if (showDeleteDialog && teamToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = D11CardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Delete Team ${teamToDelete!!.teamNumber}?",
                    color = D11White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This team will be permanently deleted.",
                    color = D11Gray)
            },
            confirmButton = {
                Button(onClick = {
                    if (!teamToDelete!!.isLocked) {
                        db.collection("teams").document(teamToDelete!!.id).delete()
                            .addOnSuccessListener { snackMsg = "Team deleted!" }
                        showDeleteDialog = false
                    } else {
                        snackMsg = "Cannot delete - match has started!"
                        showDeleteDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Delete", color = D11White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false },
                    border = androidx.compose.foundation.BorderStroke(1.dp, D11Gray),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", color = D11Gray)
                }
            }
        )
    }

    // Preview dialog
    if (showPreviewDialog && selectedPreviewTeam != null) {
        val team = selectedPreviewTeam!!
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            containerColor = D11CardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Team ${team.teamNumber} Preview",
                    color = D11White, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Role counts
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(
                            Triple("WK", team.wkCount, Color(0xFF6666FF)),
                            Triple("BAT", team.batCount, D11Green),
                            Triple("AR", team.arCount, D11Red),
                            Triple("BOWL", team.bowlCount, D11Yellow)
                        ).forEach { (role, count, color) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$count", color = color, fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(role, color = D11Gray, fontSize = 11.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = D11Border)
                    // Captain + VC
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Captain (2x)", color = D11Gray, fontSize = 11.sp)
                            Text(team.captainName, color = D11Yellow, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Vice Captain (1.5x)", color = D11Gray, fontSize = 11.sp)
                            Text(team.viceCaptainName, color = Color(0xFFAAAAAA),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = D11Border)
                    // Players list
                    Text("Players:", color = D11Gray, fontSize = 12.sp)
                    team.playerNames.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { name ->
                                Text(name.split(" ").lastOrNull() ?: name,
                                    color = D11White, fontSize = 12.sp,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPreviewDialog = false
                    onJoinContest(team)
                }, colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Join Contest", color = D11White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPreviewDialog = false },
                    border = androidx.compose.foundation.BorderStroke(1.dp, D11Gray),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Close", color = D11Gray)
                }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize()
            .background(Color(0xFF1A1A1A)).padding(padding)) {

            // TOP BAR
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(D11Red, D11DarkRed)))
                .statusBarsPadding()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("<", color = D11White, fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onBack() })
                            Image(painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = null, modifier = Modifier.size(28.dp))
                            Column {
                                Text("My Teams", color = D11White, fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 12.sp)
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("${teams.size}/20 Teams", color = D11White,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "s")
                    val alpha = infiniteTransition.animateFloat(0.3f, 1f,
                        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
                    LazyColumn(contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(3) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp)
                                .clip(RoundedCornerShape(12.dp)).alpha(alpha.value)
                                .background(D11CardBg))
                        }
                    }
                }
                teams.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                                .background(D11LightGray),
                                contentAlignment = Alignment.Center) {
                                Text("T", color = D11Gray, fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Text("No teams created yet", color = D11White,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Create your first team now!",
                                color = D11Gray, fontSize = 13.sp)
                            Button(onClick = {
                                if (teams.size >= 20) snackMsg = "Max 20 teams allowed!"
                                else onCreateTeam()
                            },
                                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)) {
                                Text("+ Create Team", color = D11White,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(teams, key = { it.id }) { team ->
                            FullTeamCard(
                                team = team,
                                onPreview = {
                                    selectedPreviewTeam = team
                                    showPreviewDialog = true
                                },
                                onJoin = {
                                    selectedPreviewTeam = team
                                    showPreviewDialog = true
                                },
                                onEdit = {
                                    if (team.isLocked) snackMsg = "Cannot edit - match started!"
                                    else onEditTeam(team)
                                },
                                onDelete = {
                                    if (team.isLocked) snackMsg = "Cannot delete - match started!"
                                    else {
                                        teamToDelete = team
                                        showDeleteDialog = true
                                    }
                                },
                                onClone = { cloneTeam(team) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // BOTTOM CREATE BUTTON
            if (!isLoading) {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF111111)).padding(16.dp)) {
                    Button(
                        onClick = {
                            if (teams.size >= 20) snackMsg = "Maximum 20 teams allowed!"
                            else onCreateTeam()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (teams.size < 20) D11Red else Color(0xFF444444)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = teams.size < 20
                    ) {
                        Text(
                            if (teams.size < 20) "+ Create New Team (${teams.size}/20)"
                            else "Maximum 20 Teams Reached",
                            color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                        )
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
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (team.isLocked) Color(0xFF1A1A2A) else D11CardBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        border = if (team.isLocked)
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333355)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (team.isLocked) Color(0xFF333355) else D11Red),
                        contentAlignment = Alignment.Center) {
                        Text("T${team.teamNumber}", color = D11White,
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Team ${team.teamNumber}", color = D11White,
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            if (team.isLocked) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF333355))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("LOCKED", color = Color(0xFF6666FF),
                                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (team.totalPoints > 0) {
                            Text("${team.totalPoints} pts", color = D11Green,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Clone
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1A3A1A)).clickable { onClone() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text("Clone", color = D11Green, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    // Edit
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(D11LightGray).clickable { onEdit() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text("Edit", color = D11White, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    // Delete
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2A0000)).clickable { onDelete() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text("Del", color = D11Red, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = D11Border)
            Spacer(modifier = Modifier.height(12.dp))

            // Role counts
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    Triple("WK", team.wkCount, Color(0xFF6666FF)),
                    Triple("BAT", team.batCount, D11Green),
                    Triple("AR", team.arCount, D11Red),
                    Triple("BOWL", team.bowlCount, D11Yellow)
                ).forEach { (role, count, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center) {
                            Text("$count", color = color, fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(role, color = D11Gray, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Captain + VC row
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A00))
                    .padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape)
                            .background(D11Yellow),
                            contentAlignment = Alignment.Center) {
                            Text("C", color = D11Black, fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text("Captain", color = D11Gray, fontSize = 9.sp)
                            Text(team.captainName.ifEmpty { "Not set" },
                                color = D11Yellow, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF111111))
                    .padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape)
                            .background(Color(0xFFAAAAAA)),
                            contentAlignment = Alignment.Center) {
                            Text("VC", color = D11Black, fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text("Vice Captain", color = D11Gray, fontSize = 9.sp)
                            Text(team.viceCaptainName.ifEmpty { "Not set" },
                                color = Color(0xFFAAAAAA), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview + Join buttons
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f).height(44.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, D11Gray),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Preview", color = D11White, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp)
                }
                Button(
                    onClick = onJoin,
                    modifier = Modifier.weight(2f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = D11Green),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Text("Join Contest -->", color = D11White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}
