package com.example.dream11india

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// SAMPLE DATA FALLBACK
// ─────────────────────────────────────────────

private fun getSamplePreviewTeam(team1: String, team2: String): List<Player> = listOf(
    Player("1",  "MS Dhoni",          "Dhoni",     team1, "WK",   9.0f,  78, 45f, isSelected = true),
    Player("4",  "Virat Kohli",       "Kohli",     team2, "BAT", 10.0f,  89, 95f, isSelected = true, isCaptain = true),
    Player("5",  "Faf du Plessis",    "Faf",       team2, "BAT",  9.5f,  72, 72f, isSelected = true, isViceCaptain = true),
    Player("6",  "Ruturaj Gaikwad",   "Ruturaj",   team1, "BAT",  9.0f,  68, 68f, isSelected = true),
    Player("9",  "Ravindra Jadeja",   "Jadeja",    team1, "AR",   9.5f,  82, 88f, isSelected = true),
    Player("10", "Glenn Maxwell",     "Maxwell",   team2, "AR",   9.0f,  71, 75f, isSelected = true),
    Player("11", "Mitchell Santner",  "Santner",   team1, "AR",   8.0f,  48, 42f, isSelected = true),
    Player("13", "Jasprit Bumrah",    "Bumrah",    team2, "BOWL", 9.5f,  79, 92f, isSelected = true),
    Player("14", "Mohammed Siraj",    "Siraj",     team2, "BOWL", 9.0f,  65, 68f, isSelected = true),
    Player("15", "Deepak Chahar",     "Chahar",    team1, "BOWL", 8.5f,  58, 55f, isSelected = true),
    Player("16", "Josh Hazlewood",    "Hazlewood", team2, "BOWL", 8.5f,  54, 52f, isSelected = true),
)

// ─────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────

@Composable
fun TeamPreviewScreen(
    matchTitle:    String       = "CSK vs RCB",
    teamNumber:    Int          = 1,
    selectedTeam:  List<Player> = emptyList(),
    onBack:        () -> Unit   = {},
    onEditTeam:    () -> Unit   = {},
    onJoinContest: () -> Unit   = {}
) {
    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    // Use passed team or fallback sample
    val team = if (selectedTeam.size == 11) selectedTeam
    else getSamplePreviewTeam(team1, team2)

    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic            = LocalHapticFeedback.current

    val captain    = team.find { it.isCaptain }
    val viceCaptain= team.find { it.isViceCaptain }
    val wkList     = team.filter { it.role == "WK" }
    val batList    = team.filter { it.role == "BAT" }
    val arList     = team.filter { it.role == "AR" }
    val bowlList   = team.filter { it.role == "BOWL" }
    val totalCr    = team.sumOf { it.credits.toDouble() }.toFloat()
    val t1Count    = team.count { it.team == team1 }
    val t2Count    = team.count { it.team == team2 }

    // Animated entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    fun validateAndJoin() {
        when {
            team.size != 11       -> scope.launch { snackbarHostState.showSnackbar("Team must have exactly 11 players") }
            captain == null       -> scope.launch { snackbarHostState.showSnackbar("Please select a Captain first") }
            viceCaptain == null   -> scope.launch { snackbarHostState.showSnackbar("Please select a Vice Captain first") }
            else                  -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onJoinContest() }
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
                    action         = {
                        TextButton(onClick = { data.dismiss() }) {
                            Text("OK", color = D11Red, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                ) { Text(data.visuals.message, fontWeight = FontWeight.SemiBold) }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { pad ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {

            // ── TOP HEADER ──
            PreviewTopBar(
                matchTitle  = matchTitle,
                teamNumber  = teamNumber,
                onBack      = onBack,
                onEdit      = onEditTeam
            )

            // ── C / VC BAR ──
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(400)) + slideInVertically { -20 }
            ) {
                CaptainVCBar(captain = captain, viceCaptain = viceCaptain)
            }

            // ── SCROLLABLE CONTENT ──
            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {

                // Cricket Field
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(600)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(500)
                        )
                    ) {
                        CricketFieldSection(
                            wkList   = wkList,
                            batList  = batList,
                            arList   = arList,
                            bowlList = bowlList,
                            team1    = team1,
                            team2    = team2
                        )
                    }
                }

                // Team Summary Card
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(700)) + slideInVertically { 40 }
                    ) {
                        TeamSummaryCard(
                            team1      = team1,
                            team2      = team2,
                            t1Count    = t1Count,
                            t2Count    = t2Count,
                            wkCount    = wkList.size,
                            batCount   = batList.size,
                            arCount    = arList.size,
                            bowlCount  = bowlList.size,
                            totalCr    = totalCr,
                            captain    = captain,
                            viceCaptain= viceCaptain
                        )
                    }
                }

                // Player list by role
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(800)) + slideInVertically { 60 }
                    ) {
                        PlayerListCard(
                            team   = team,
                            team1  = team1,
                            team2  = team2
                        )
                    }
                }
            }

            // ── BOTTOM ACTIONS ──
            BottomPreviewBar(
                canJoin        = team.size == 11 && captain != null && viceCaptain != null,
                onEdit         = onEditTeam,
                onJoinContest  = { validateAndJoin() }
            )
        }
    }
}

// ─────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────

@Composable
private fun PreviewTopBar(
    matchTitle: String,
    teamNumber: Int,
    onBack:     () -> Unit,
    onEdit:     () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFCC0000), Color(0xFF880000))
                )
            )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, null,
                        tint     = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        "Team Preview",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x44FFFFFF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "T$teamNumber",
                                color      = Color.White,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { onEdit() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "Edit Team",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// CAPTAIN / VC BAR
// ─────────────────────────────────────────────

@Composable
private fun CaptainVCBar(captain: Player?, viceCaptain: Player?) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        CvcChip(
            label    = "C",
            name     = captain?.name ?: "Not selected",
            mult     = "2×",
            bgColor  = if (captain != null) D11Yellow else Color(0xFF444444),
            nameColor= if (captain != null) Color.White else Color(0xFF888888)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(Color(0xFF333333))
        )
        CvcChip(
            label    = "VC",
            name     = viceCaptain?.name ?: "Not selected",
            mult     = "1.5×",
            bgColor  = if (viceCaptain != null) Color(0xFFAAAAAA) else Color(0xFF444444),
            nameColor= if (viceCaptain != null) Color.White else Color(0xFF888888)
        )
    }
}

@Composable
private fun CvcChip(
    label:     String,
    name:      String,
    mult:      String,
    bgColor:   Color,
    nameColor: Color
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color      = Color(0xFF111111),
                fontSize   = if (label == "C") 14.sp else 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Column {
            Text(
                name,
                color      = nameColor,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                "$mult points",
                color    = Color(0xFF888888),
                fontSize = 10.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// CRICKET FIELD SECTION
// ─────────────────────────────────────────────

@Composable
private fun CricketFieldSection(
    wkList:   List<Player>,
    batList:  List<Player>,
    arList:   List<Player>,
    bowlList: List<Player>,
    team1:    String,
    team2:    String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        // Grass gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D5C2A),
                            Color(0xFF1A7A38),
                            Color(0xFF22933F),
                            Color(0xFF1A7A38),
                            Color(0xFF0D5C2A)
                        )
                    )
                )
        )

        // Field markings (canvas)
        FieldMarkings()

        // Players
        Column(
            modifier              = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.SpaceEvenly
        ) {
            FieldRoleRow(label = "WICKET-KEEPERS", players = wkList, team1 = team1, team2 = team2)
            FieldRoleRow(label = "BATTERS",        players = batList, team1 = team1, team2 = team2)
            FieldRoleRow(label = "ALL-ROUNDERS",   players = arList,  team1 = team1, team2 = team2)
            FieldRoleRow(label = "BOWLERS",        players = bowlList,team1 = team1, team2 = team2)
        }
    }
}

@Composable
private fun FieldMarkings() {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Outer boundary
        drawCircle(
            color  = Color(0x18FFFFFF),
            radius = size.width * 0.44f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.5.dp.toPx())
        )
        // 30-yard circle
        drawCircle(
            color  = Color(0x14FFFFFF),
            radius = size.width * 0.26f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.dp.toPx())
        )
        // Pitch rectangle
        drawRoundRect(
            color       = Color(0x22FFFFFF),
            topLeft     = Offset(cx - 18.dp.toPx(), cy - 55.dp.toPx()),
            size        = androidx.compose.ui.geometry.Size(36.dp.toPx(), 110.dp.toPx()),
            cornerRadius= androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Center circle
        drawCircle(
            color  = Color(0x1AFFFFFF),
            radius = 22.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}

@Composable
private fun FieldRoleRow(
    label:   String,
    players: List<Player>,
    team1:   String,
    team2:   String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color         = Color(0xBBFFFFFF),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            players.forEach { player ->
                FieldPlayerCard(player = player, team1 = team1, team2 = team2)
            }
        }
    }
}

@Composable
private fun FieldPlayerCard(player: Player, team1: String, team2: String) {
    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue   = if (player.isCaptain || player.isViceCaptain) 0.4f else 0f,
        targetValue    = if (player.isCaptain || player.isViceCaptain) 0.9f else 0f,
        animationSpec  = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode= RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val avatarGradient = when (player.team) {
        team1 -> listOf(Color(0xFF1565C0), Color(0xFF003580))
        else  -> listOf(Color(0xFF2E7D32), Color(0xFF004D00))
    }
    val glowColor = when {
        player.isCaptain    -> D11Yellow
        player.isViceCaptain-> Color(0xFFAAAAAA)
        else                -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.width(68.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            // Glow ring
            if (player.isCaptain || player.isViceCaptain) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = glowAlpha * 0.25f))
                        .align(Alignment.Center)
                )
            }
            // Avatar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(avatarGradient))
                    .border(
                        width = if (player.isCaptain || player.isViceCaptain) 2.dp else 1.dp,
                        color = glowColor.copy(alpha = if (player.isCaptain || player.isViceCaptain) 1f else 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.shortName.take(2).uppercase(),
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            // C / VC badge
            if (player.isCaptain || player.isViceCaptain) {
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(if (player.isCaptain) D11Yellow else Color(0xFFBBBBBB))
                        .shadow(2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (player.isCaptain) "C" else "VC",
                        color      = Color(0xFF111111),
                        fontSize   = if (player.isCaptain) 9.sp else 7.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            player.shortName.take(9),
            color      = Color.White,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        Text(
            "${String.format("%.1f", player.credits)} Cr",
            color     = Color(0x99FFFFFF),
            fontSize  = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────
// TEAM SUMMARY CARD
// ─────────────────────────────────────────────

@Composable
private fun TeamSummaryCard(
    team1:       String,
    team2:       String,
    t1Count:     Int,
    t2Count:     Int,
    wkCount:     Int,
    batCount:    Int,
    arCount:     Int,
    bowlCount:   Int,
    totalCr:     Float,
    captain:     Player?,
    viceCaptain: Player?
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Team Summary",
                    color      = Color.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(D11Green.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "11 Players ✓",
                        color      = D11Green,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Role counts
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    Triple("WK",   wkCount,   Color(0xFF82B1FF)),
                    Triple("BAT",  batCount,  Color(0xFF69F0AE)),
                    Triple("AR",   arCount,   Color(0xFFFF8A80)),
                    Triple("BOWL", bowlCount, Color(0xFFFFFF8D))
                ).forEach { (role, count, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$count",
                                color      = color,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(role, color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Spacer(Modifier.height(12.dp))

            // Team split
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatItem(label = team1, value = "$t1Count Players")
                SummaryStatItem(label = team2, value = "$t2Count Players", alignEnd = true)
            }

            Spacer(Modifier.height(10.dp))

            // Credits bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF111111))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Credits Used", color = Color(0xFF888888), fontSize = 12.sp)
                Text(
                    "${String.format("%.1f", totalCr)} / 100",
                    color      = D11Yellow,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(8.dp))

            // C/VC summary
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CvcSummaryChip(
                    modifier = Modifier.weight(1f),
                    badge    = "C",
                    color    = D11Yellow,
                    name     = captain?.name ?: "—"
                )
                CvcSummaryChip(
                    modifier = Modifier.weight(1f),
                    badge    = "VC",
                    color    = Color(0xFFAAAAAA),
                    name     = viceCaptain?.name ?: "—"
                )
            }
        }
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, color = Color(0xFF888888), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CvcSummaryChip(modifier: Modifier, badge: String, color: Color, name: String) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                badge,
                color      = Color(0xFF111111),
                fontSize   = if (badge == "C") 12.sp else 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            name,
            color      = Color.White,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────
// PLAYER LIST CARD
// ─────────────────────────────────────────────

@Composable
private fun PlayerListCard(team: List<Player>, team1: String, team2: String) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                "All Players",
                color      = Color.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = Color(0xFF2A2A2A))

            // Column header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Player",  color = Color(0xFF666666), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Pts",    color = Color(0xFF666666), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Credits",color = Color(0xFF666666), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            listOf("WK", "BAT", "AR", "BOWL").forEach { role ->
                val rolePlayers = team.filter { it.role == role }
                if (rolePlayers.isNotEmpty()) {
                    // Role section header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111111))
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Text(
                            when (role) {
                                "WK"   -> "Wicket-Keepers"
                                "BAT"  -> "Batters"
                                "AR"   -> "All-Rounders"
                                else   -> "Bowlers"
                            },
                            color      = roleLabelColor(role),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    rolePlayers.forEach { player ->
                        PreviewPlayerRow(player = player, team1 = team1, team2 = team2)
                    }
                }
            }
        }
    }
}

private fun roleLabelColor(role: String): Color = when (role) {
    "WK"   -> Color(0xFF82B1FF)
    "BAT"  -> Color(0xFF69F0AE)
    "AR"   -> Color(0xFFFF8A80)
    else   -> Color(0xFFFFFF8D)
}

@Composable
private fun PreviewPlayerRow(player: Player, team1: String, team2: String) {
    val avatarColors = if (player.team == team1)
        listOf(Color(0xFF1565C0), Color(0xFF003580))
    else
        listOf(Color(0xFF2E7D32), Color(0xFF004D00))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    player.isCaptain    -> Color(0xFF1A1600)
                    player.isViceCaptain-> Color(0xFF161616)
                    else                -> Color.Transparent
                }
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(avatarColors))
                        .border(
                            1.5.dp,
                            if (player.isCaptain) D11Yellow
                            else if (player.isViceCaptain) Color(0xFFAAAAAA)
                            else Color(0xFF333333),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        player.shortName.take(2).uppercase(),
                        color      = Color.White,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (player.isCaptain || player.isViceCaptain) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (player.isCaptain) D11Yellow else Color(0xFFAAAAAA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (player.isCaptain) "C" else "VC",
                            color      = Color(0xFF111111),
                            fontSize   = 7.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Column {
                Text(
                    player.name,
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniRoleBadge(player.role)
                    MiniTeamBadge(player.team)
                    Text(
                        "${player.selectionPercent}% sel",
                        color    = Color(0xFF666666),
                        fontSize = 10.sp
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "${player.points}",
                color      = D11Green,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                String.format("%.1f", player.credits),
                color      = D11Yellow,
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
    HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
}

@Composable
private fun MiniRoleBadge(role: String) {
    val (bg, fg) = when (role) {
        "WK"   -> Color(0xFF1A1F3C) to Color(0xFF82B1FF)
        "BAT"  -> Color(0xFF0D2A10) to Color(0xFF69F0AE)
        "AR"   -> Color(0xFF2A0D10) to Color(0xFFFF8A80)
        else   -> Color(0xFF2A2800) to Color(0xFFFFFF8D)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(role, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MiniTeamBadge(team: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            team.take(4).uppercase(),
            color      = Color(0xFF999999),
            fontSize   = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ─────────────────────────────────────────────
// BOTTOM ACTION BAR
// ─────────────────────────────────────────────

@Composable
private fun BottomPreviewBar(
    canJoin:       Boolean,
    onEdit:        () -> Unit,
    onJoinContest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp)
            .background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                border   = BorderStroke(1.5.dp, Color(0xFF555555)),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Edit Team",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
            Button(
                onClick  = onJoinContest,
                modifier = Modifier
                    .weight(2f)
                    .height(54.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (canJoin) D11Green else Color(0xFF333333)
                ),
                shape    = RoundedCornerShape(12.dp),
                elevation= ButtonDefaults.buttonElevation(if (canJoin) 8.dp else 0.dp)
            ) {
                Text(
                    if (canJoin) "Join Contest  →" else "Complete Team",
                    color      = if (canJoin) Color.White else Color(0xFF888888),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp
                )
            }
        }
    }
}