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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────

data class ContestData(
    val id: String = "",
    val name: String = "",
    val prize: String = "",
    val spots: String = "",
    val spotsLeft: String = "",
    val fillPercent: Int = 0,
    val entryFee: String = "",
    val entryFeeInt: Int = 0,
    val isFree: Boolean = false,
    val isHot: Boolean = false,
    val winners: String = "",
    val firstPrize: String = "",
    val isGuaranteed: Boolean = false,
    val matchId: String = ""
)

data class SavedTeam(
    val id: String = "",
    val teamNumber: Int = 1,
    val captainName: String = "",
    val viceCaptainName: String = "",
    val wkCount: Int = 0,
    val batCount: Int = 0,
    val arCount: Int = 0,
    val bowlCount: Int = 0,
    val totalPoints: Float = 0f
)

// ─────────────────────────────────────────────
// CONTEST SCREEN
// ─────────────────────────────────────────────

@Composable
fun ContestScreen(
    matchTitle: String = "CSK vs RCB",
    matchId:    String = "",
    userData:   UserData = UserData(),
    onBack:     () -> Unit = {},
    onJoin:     (String) -> Unit = {}
) {
    val db    = FirebaseFirestore.getInstance()
    val uid   = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    // ── State ──
    var selectedTab    by remember { mutableStateOf("contests") }
    var selectedFilter by remember { mutableStateOf("All") }
    var contests       by remember { mutableStateOf<List<ContestData>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var myTeams        by remember { mutableStateOf<List<SavedTeam>>(emptyList()) }
    var joinedIds      by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedContest by remember { mutableStateOf<ContestData?>(null) }
    var isJoining      by remember { mutableStateOf(false) }
    var joinError      by remember { mutableStateOf("") }

    // ── Load contests from Firebase ──
    LaunchedEffect(matchId) {
        isLoading = true
        try {
            val snap = if (matchId.isNotEmpty())
                db.collection("contests").whereEqualTo("matchId", matchId).get().await()
            else
                db.collection("contests").limit(20).get().await()

            contests = if (snap.documents.isNotEmpty()) {
                snap.documents.map { doc ->
                    ContestData(
                        id           = doc.id,
                        name         = doc.getString("name") ?: "Contest",
                        prize        = doc.getString("prizePool") ?: "₹0",
                        spots        = doc.getLong("totalSpots")?.toString() ?: "1000",
                        spotsLeft    = ((doc.getLong("totalSpots") ?: 0) - (doc.getLong("joinedCount") ?: 0)).toString(),
                        fillPercent  = doc.getLong("fillPercent")?.toInt() ?: 0,
                        entryFee     = if ((doc.getLong("entryFee") ?: 0L) == 0L) "FREE" else "₹${doc.getLong("entryFee")}",
                        entryFeeInt  = doc.getLong("entryFee")?.toInt() ?: 0,
                        isFree       = (doc.getLong("entryFee") ?: 0L) == 0L,
                        isHot        = doc.getBoolean("isHot") ?: false,
                        isGuaranteed = doc.getBoolean("isGuaranteed") ?: false,
                        winners      = doc.getString("winners") ?: "100",
                        firstPrize   = doc.getString("firstPrize") ?: "₹0",
                        matchId      = doc.getString("matchId") ?: matchId
                    )
                }
            } else {
                // Fallback sample contests if Firebase empty
                listOf(
                    ContestData("1","Mega Contest","₹50 Crores","1,20,000","6,543",95,"₹49",49,false,true,"30,000","₹1 Cr",true,matchId),
                    ContestData("2","Hot Contest","₹25 Crores","85,000","18,234",78,"₹29",29,false,true,"15,000","₹50 Lakh",true,matchId),
                    ContestData("3","Small League","₹10 Crores","45,000","24,750",45,"₹19",19,false,false,"8,000","₹20 Lakh",false,matchId),
                    ContestData("4","Free Contest","₹1 Lakh","25,000","20,000",20,"FREE",0,true,false,"5,000","₹10,000",false,matchId),
                    ContestData("5","Head to Head","₹90","2","1",50,"₹49",49,false,false,"1","₹90",false,matchId)
                )
            }
        } catch (_: Exception) {
            contests = listOf(
                ContestData("1","Mega Contest","₹50 Crores","1,20,000","6,543",95,"₹49",49,false,true,"30,000","₹1 Cr",true,matchId),
                ContestData("2","Free Contest","₹1 Lakh","25,000","20,000",20,"FREE",0,true,false,"5,000","₹10,000",false,matchId)
            )
        }
        isLoading = false
    }

    // ── Load my teams ──
    LaunchedEffect(uid, matchId) {
        if (uid.isEmpty()) return@LaunchedEffect
        db.collection("users").document(uid).collection("teams")
            .whereEqualTo("matchId", matchId)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    myTeams = it.documents.mapIndexed { idx, doc ->
                        SavedTeam(
                            id              = doc.id,
                            teamNumber      = idx + 1,
                            captainName     = doc.getString("captainId") ?: "",
                            viceCaptainName = doc.getString("viceCaptainId") ?: "",
                            wkCount         = 0, batCount = 0, arCount = 0, bowlCount = 0
                        )
                    }
                }
            }
        // Load joined contest IDs
        db.collection("joined_contests")
            .whereEqualTo("userId", uid)
            .whereEqualTo("matchId", matchId)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    joinedIds = it.documents.map { doc ->
                        doc.getString("contestId") ?: ""
                    }.toSet()
                }
            }
    }

    // ── Join contest ──
    fun joinContest(contest: ContestData) {
        if (uid.isEmpty()) return
        isJoining = true
        joinError = ""
        scope.launch {
            try {
                val userRef   = db.collection("users").document(uid)
                val joinedRef = db.collection("joined_contests").document("${uid}_${contest.id}")
                db.runTransaction { tx ->
                    val userSnap = tx.get(userRef)
                    val balance  = userSnap.getLong("balance")?.toInt() ?: 0
                    if (!contest.isFree && balance < contest.entryFeeInt)
                        throw Exception("Insufficient balance! Need ₹${contest.entryFeeInt}, have ₹$balance")
                    val joinSnap = tx.get(joinedRef)
                    if (joinSnap.exists()) throw Exception("Already joined this contest!")
                    if (!contest.isFree)
                        tx.update(userRef, "balance", balance - contest.entryFeeInt)
                    tx.set(joinedRef, mapOf(
                        "userId" to uid, "contestId" to contest.id,
                        "matchId" to matchId, "contestName" to contest.name,
                        "entryFee" to contest.entryFeeInt, "points" to 0,
                        "rank" to 0, "joinedAt" to System.currentTimeMillis()
                    ))
                    if (!contest.isFree) {
                        tx.set(db.collection("transactions").document(), mapOf(
                            "userId" to uid, "type" to "debit",
                            "amount" to contest.entryFeeInt,
                            "description" to "Joined ${contest.name}",
                            "status" to "completed",
                            "timestamp" to System.currentTimeMillis()
                        ))
                    }
                }.await()
                isJoining = false
                showJoinDialog = false
                scope.launch { snackbarHostState.showSnackbar("Joined ${contest.name}!") }
                onJoin(contest.id)
            } catch (e: Exception) {
                isJoining = false
                joinError = e.message ?: "Join failed"
            }
        }
    }

    // ── Join dialog ──
    if (showJoinDialog && selectedContest != null) {
        val c = selectedContest!!
        val canAfford = c.isFree || userData.balance >= c.entryFeeInt
        AlertDialog(
            onDismissRequest = { if (!isJoining) showJoinDialog = false },
            containerColor   = Color(0xFF1A1A1A),
            shape            = RoundedCornerShape(16.dp),
            title = {
                Text("Join Contest", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(c.name, color = D11Yellow, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    HorizontalDivider(color = Color(0xFF333333))
                    listOf(
                        "Entry Fee" to c.entryFee,
                        "Your Balance" to "₹${userData.balance}",
                        "Prize Pool" to c.prize,
                        "1st Prize" to c.firstPrize
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(label, color = Color(0xFF888888), fontSize = 13.sp)
                            Text(value, color = when (label) {
                                "Your Balance" -> D11Green
                                "1st Prize"    -> D11Green
                                "Prize Pool"   -> D11Yellow
                                else           -> Color.White
                            }, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                    if (!canAfford) {
                        Card(colors = CardDefaults.cardColors(Color(0xFF2A0000)), shape = RoundedCornerShape(8.dp)) {
                            Text("Insufficient balance! Add money to wallet.", color = D11Red, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                    if (joinError.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(Color(0xFF2A0000)), shape = RoundedCornerShape(8.dp)) {
                            Text(joinError, color = D11Red, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { if (!isJoining && canAfford) joinContest(c) },
                    colors   = ButtonDefaults.buttonColors(if (c.isFree) D11Green else D11Red),
                    shape    = RoundedCornerShape(8.dp),
                    enabled  = canAfford && !isJoining
                ) {
                    if (isJoining) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Join Now", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!isJoining) showJoinDialog = false },
                    border  = BorderStroke(1.dp, Color(0xFF888888)),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Cancel", color = Color(0xFF888888)) }
            }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) { data ->
            Snackbar(modifier = Modifier.padding(14.dp), containerColor = Color(0xFF1C1C1C), contentColor = Color.White, shape = RoundedCornerShape(10.dp)) {
                Text(data.visuals.message, fontWeight = FontWeight.SemiBold)
            }
        }},
        containerColor = Color(0xFF0A0A0A)
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // ── Header ──
            ContestTopBar(matchTitle = matchTitle, team1 = team1, team2 = team2, balance = userData.balance, onBack = onBack)

            // ── Tabs ──
            ContestTabs(selectedTab = selectedTab, onTabChange = { selectedTab = it })
            HorizontalDivider(color = Color(0xFF222222))

            // ── Content ──
            when (selectedTab) {
                "contests"    -> ContestsList(
                    contests       = when (selectedFilter) {
                        "Free"  -> contests.filter { it.isFree }
                        "Mega"  -> contests.filter { it.name.contains("Mega", true) }
                        "Small" -> contests.filter { it.entryFeeInt in 1..25 }
                        "H2H"   -> contests.filter { it.spots == "2" }
                        else    -> contests
                    },
                    isLoading      = isLoading,
                    joinedIds      = joinedIds,
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    onJoin         = { contest ->
                        selectedContest = contest
                        joinError = ""
                        showJoinDialog = true
                    }
                )
                "my_teams"    -> MyTeamsTab(
                    matchId     = matchId,
                    matchTitle  = matchTitle,
                    onCreateTeam = { onJoin("create_team") }
                )
                "leaderboard" -> LeaderboardTab(matchId = matchId, uid = uid)
                else          -> ContestsList(
                    contests = contests, isLoading = isLoading,
                    joinedIds = joinedIds, selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    onJoin = { contest -> selectedContest = contest; joinError = ""; showJoinDialog = true }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────

@Composable
fun ContestTopBar(
    matchTitle: String, team1: String, team2: String,
    balance: Int, onBack: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "live")
    val da  by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d")

    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF0D0D2E), Color(0xFF1A1A3A))))) {
        Column(Modifier.statusBarsPadding().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                    Column {
                        Text(matchTitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text("T20 · IPL 2026", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                }
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A44)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("₹$balance", color = D11Green, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF003366)), Alignment.Center) {
                        Text(team1.take(3).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(team1, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).alpha(da).background(D11Green))
                        Text("LIVE", color = D11Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("vs", color = Color(0xFF888888), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(team2, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF006600)), Alignment.Center) {
                        Text(team2.take(3).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// TABS
// ─────────────────────────────────────────────

@Composable
fun ContestTabs(selectedTab: String, onTabChange: (String) -> Unit) {
    val sc = rememberScrollState()
    Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).horizontalScroll(sc).padding(horizontal = 4.dp)) {
        listOf("contests" to "Contests", "my_teams" to "My Teams", "leaderboard" to "Leaderboard", "scorecard" to "Scorecard").forEach { (tab, label) ->
            val active = selectedTab == tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTabChange(tab) }.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(label, color = if (active) D11Red else Color(0xFF888888), fontSize = 12.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal)
                if (active) { Spacer(Modifier.height(3.dp)); Box(Modifier.width(30.dp).height(2.dp).background(D11Red)) }
                else Spacer(Modifier.height(5.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// CONTESTS LIST
// ─────────────────────────────────────────────

@Composable
fun ContestsList(
    contests:       List<ContestData>,
    isLoading:      Boolean,
    joinedIds:      Set<String>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onJoin:         (ContestData) -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = D11Red) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(listOf("All", "Mega", "Small", "Free", "H2H")) { f ->
                    val active = selectedFilter == f
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (active) D11Red else Color(0xFF2A2A2A))
                            .clickable { onFilterChange(f) }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(f, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Stats
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)).padding(12.dp), Arrangement.SpaceEvenly) {
                listOf(contests.size.toString() to "Contests", contests.count { !it.isFree }.toString() to "Paid", contests.count { it.isFree }.toString() to "Free").forEach { (v, l) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, color = D11Yellow, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(l, color = Color(0xFF888888), fontSize = 11.sp)
                    }
                }
            }
        }
        if (contests.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🏏", fontSize = 36.sp)
                        Text("No contests available", color = Color(0xFF888888), fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(contests, key = { it.id }) { contest ->
                ContestCard(
                    contest    = contest,
                    isJoined   = joinedIds.contains(contest.id),
                    onJoin     = { onJoin(contest) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────
// CONTEST CARD
// ─────────────────────────────────────────────

@Composable
fun ContestCard(contest: ContestData, isJoined: Boolean, onJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(Color(0xFF1A1A1A)),
        shape    = RoundedCornerShape(12.dp),
        border   = if (isJoined) BorderStroke(1.dp, D11Green) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (contest.isGuaranteed) ContestBadge("Guaranteed", D11Green, Color(0xFF1A3A1A))
                    if (contest.isHot) ContestBadge("HOT", Color(0xFFFF6B35), Color(0xFF3A1500))
                    if (contest.isFree) ContestBadge("FREE", D11Green, Color(0xFF003300))
                    if (isJoined) ContestBadge("JOINED", Color(0xFF82B1FF), Color(0xFF001A3A))
                }
                if (!isJoined) {
                    Button(
                        onClick  = onJoin,
                        colors   = ButtonDefaults.buttonColors(if (contest.isFree) D11Green else D11Red),
                        shape    = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(contest.entryFee, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                } else {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF003300)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text("Joined ✓", color = D11Green, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Prize Pool", color = Color(0xFF888888), fontSize = 11.sp)
                    Text(contest.prize, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Spots", color = Color(0xFF888888), fontSize = 11.sp)
                    Text(contest.spots, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${contest.spotsLeft} spots left", color = if (contest.fillPercent > 80) D11Red else Color(0xFF888888), fontSize = 11.sp)
                Text("${contest.fillPercent}% filled", color = Color(0xFF888888), fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF333333))) {
                Box(Modifier.fillMaxWidth((contest.fillPercent.coerceIn(0,100) / 100f)).height(5.dp).clip(RoundedCornerShape(2.dp)).background(
                    if (contest.fillPercent > 80) Brush.horizontalGradient(listOf(D11Red, Color(0xFFFF6B35)))
                    else Brush.horizontalGradient(listOf(D11Green, Color(0xFF00E676)))
                ))
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(D11Yellow), Alignment.Center) {
                        Text("W", color = Color(0xFF111111), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("${contest.winners} Winners", color = Color(0xFF888888), fontSize = 11.sp)
                }
                Text("1st: ${contest.firstPrize}", color = D11Green, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ─────────────────────────────────────────────
// BADGE
// ─────────────────────────────────────────────

@Composable
fun ContestBadge(text: String, textColor: Color, bgColor: Color) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bgColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────
// MY TEAMS TAB
// ─────────────────────────────────────────────

@Composable
fun MyTeamsTab(
    matchId:     String = "",
    matchTitle:  String = "",
    onCreateTeam: () -> Unit = {}
) {
    val db  = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var teams by remember { mutableStateOf<List<SavedTeam>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uid, matchId) {
        if (uid.isEmpty()) { isLoading = false; return@LaunchedEffect }
        db.collection("users").document(uid).collection("teams")
            .whereEqualTo("matchId", matchId)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    teams = it.documents.mapIndexed { idx, doc ->
                        @Suppress("UNCHECKED_CAST")
                        val players = doc.get("players") as? List<Map<String,Any>> ?: emptyList()
                        SavedTeam(
                            id              = doc.id,
                            teamNumber      = idx + 1,
                            captainName     = players.find { p -> p["id"] == doc.getString("captainId") }?.get("name") as? String ?: doc.getString("captainId") ?: "",
                            viceCaptainName = players.find { p -> p["id"] == doc.getString("viceCaptainId") }?.get("name") as? String ?: doc.getString("viceCaptainId") ?: "",
                            wkCount         = players.count { p -> p["role"] == "WK" },
                            batCount        = players.count { p -> p["role"] == "BAT" },
                            arCount         = players.count { p -> p["role"] == "AR" },
                            bowlCount       = players.count { p -> p["role"] == "BOWL" }
                        )
                    }
                    isLoading = false
                }
            }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = D11Red) }
        return
    }

    if (teams.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🏏", fontSize = 48.sp)
                Text("No Teams Yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Create a team to join contests", color = Color(0xFF888888), fontSize = 13.sp)
                Button(onClick = onCreateTeam, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp)) {
                    Text("+ Create Team", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(teams, key = { it.id }) { team ->
            Card(colors = CardDefaults.cardColors(Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(D11Red), Alignment.Center) {
                                Text("T${team.teamNumber}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Column {
                                Text("Team ${team.teamNumber}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("C: ${team.captainName.take(12)} · VC: ${team.viceCaptainName.take(12)}", color = Color(0xFF888888), fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WK ${team.wkCount}" to Color(0xFF82B1FF), "BAT ${team.batCount}" to D11Green, "AR ${team.arCount}" to D11Red, "BOWL ${team.bowlCount}" to D11Yellow).forEach { (label, color) ->
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF111111)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onCreateTeam, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(Color(0xFF222222)), shape = RoundedCornerShape(10.dp)) {
                Text("+ Create Another Team", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────
// LEADERBOARD TAB
// ─────────────────────────────────────────────

@Composable
fun LeaderboardTab(matchId: String = "", uid: String = "") {
    val db = FirebaseFirestore.getInstance()
    var entries  by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(matchId) {
        try {
            val contestSnap = db.collection("contests")
                .whereEqualTo("matchId", matchId).limit(1).get().await()
            if (!contestSnap.isEmpty) {
                val cid = contestSnap.documents.first().id
                db.collection("contests").document(cid)
                    .collection("leaderboard")
                    .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50).addSnapshotListener { snap, _ ->
                        snap?.let {
                            entries = it.documents.mapIndexed { idx, doc ->
                                LeaderboardEntry(
                                    uid           = doc.id,
                                    name          = doc.getString("name") ?: "Player",
                                    points        = doc.getDouble("points")?.toFloat() ?: 0f,
                                    rank          = idx + 1,
                                    isCurrentUser = doc.id == uid
                                )
                            }
                            isLoading = false
                        }
                    }
            } else {
                isLoading = false
            }
        } catch (_: Exception) { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = D11Red) }
        return
    }

    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🏆", fontSize = 40.sp)
                Text("No leaderboard yet", color = Color(0xFF888888), fontSize = 14.sp)
                Text("Join a contest to see rankings", color = Color(0xFF666666), fontSize = 12.sp)
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 8.dp), Arrangement.SpaceBetween) {
                Text("Player", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("Points", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Rank",   color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(entries, key = { it.uid }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (entry.isCurrentUser) Color(0xFF1A1600) else Color(0xFF0A0A0A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(
                        when (entry.rank) { 1 -> D11Yellow; 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> if (entry.isCurrentUser) D11Red else Color(0xFF2A2A2A) }
                    ), Alignment.Center) {
                        Text(if (entry.rank <= 3) "${entry.rank}" else entry.name.take(2).uppercase(), color = if (entry.rank <= 3) Color(0xFF111111) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(entry.name, color = if (entry.isCurrentUser) D11Yellow else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(String.format("%.1f", entry.points), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("#${entry.rank}", color = if (entry.isCurrentUser) D11Yellow else Color(0xFF888888), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 0.5.dp)
        }
    }
}

// ─────────────────────────────────────────────
// HELPER - Empty state
// ─────────────────────────────────────────────

@Composable
fun EmptyStateView(
    icon: String, title: String, subtitle: String,
    buttonText: String, onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(icon, fontSize = 44.sp)
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF888888), fontSize = 13.sp)
            Button(onClick = onAction, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp)) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
