package com.example.dream11india

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────
// UI MODELS
// ─────────────────────────────────────────────

data class BatterStat(
    val name: String = "", val runs: Int = 0, val balls: Int = 0,
    val fours: Int = 0, val sixes: Int = 0, val sr: Float = 0f,
    val dismissal: String = "", val isOnField: Boolean = false,
    val isCaptain: Boolean = false, val isVC: Boolean = false
)

data class BowlerStat(
    val name: String = "", val overs: Float = 0f, val maidens: Int = 0,
    val runs: Int = 0, val wickets: Int = 0, val economy: Float = 0f,
    val isOnField: Boolean = false
)

data class CommentaryItem(
    val over: String = "", val text: String = "", val type: String = "normal"
)

data class InningsData(
    val teamName: String = "", val total: String = "",
    val batters: List<BatterStat> = emptyList(),
    val bowlers: List<BowlerStat> = emptyList(),
    val extras: String = ""
)

data class MyFantasyTeam(
    val playerPoints: List<Triple<String, Float, String>> = emptyList(),
    val totalPoints: Float = 0f,
    val rank: Int = 0
)

data class FantasyPlayerPoints(
    val playerId: String = "",
    val name: String = "",
    val runs: Int = 0,
    val wickets: Int = 0,
    val catches: Int = 0,
    val stumpings: Int = 0,
    val runOuts: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val maidens: Int = 0,
    val totalPoints: Float = 0f,
    val multiplier: String = "1x"
)

data class LeaderboardEntry(
    val uid: String = "",
    val name: String = "",
    val points: Float = 0f,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false
)

data class LiveScoreUiState(
    // Loading
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = 0L,
    // Match info
    val matchId: String = "",
    val matchName: String = "",
    val matchType: String = "T20",
    val venue: String = "",
    val status: String = "",
    val tossInfo: String = "",
    val resultText: String = "",
    val isLive: Boolean = false,
    val isEnded: Boolean = false,
    // Scores
    val team1: String = "",
    val team2: String = "",
    val score1: String = "",
    val overs1: String = "",
    val score2: String = "",
    val overs2: String = "",
    val currentRR: Float = 0f,
    val requiredRR: Float = 0f,
    val target: Int = 0,
    // Live data
    val recentBalls: List<String> = emptyList(),
    val liveBatters: List<BatterStat> = emptyList(),
    val liveBowlers: List<BowlerStat> = emptyList(),
    val innings: List<InningsData> = emptyList(),
    val commentary: List<CommentaryItem> = emptyList(),
    // Fantasy
    val myTeam: MyFantasyTeam = MyFantasyTeam(),
    val playerBreakdown: List<FantasyPlayerPoints> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val userRank: Int = 0,
    val joinedContestCount: Int = 0,
    // Win probability
    val winProb1: Float = 50f,
    val winProb2: Float = 50f
)

// ─────────────────────────────────────────────
// FANTASY POINTS CALCULATOR
// ─────────────────────────────────────────────

object FantasyCalculator {

    fun battingPoints(b: BatterStat): Float {
        var pts = 0f
        pts += b.runs * 1f
        pts += b.fours * 1f
        pts += b.sixes * 2f
        if (b.runs >= 100) pts += 16f
        else if (b.runs >= 50) pts += 8f
        else if (b.runs >= 30) pts += 4f
        if (b.runs == 0 && b.balls > 0 && !b.isOnField) pts -= 2f
        val sr = b.sr
        if (b.balls >= 10) {
            pts += when {
                sr >= 170 -> 6f
                sr >= 150 -> 4f
                sr >= 130 -> 2f
                sr < 50   -> -6f
                sr < 60   -> -4f
                sr < 70   -> -2f
                else -> 0f
            }
        }
        return pts
    }

    fun bowlingPoints(b: BowlerStat): Float {
        var pts = 0f
        pts += b.wickets * 25f
        if (b.wickets >= 5) pts += 16f
        else if (b.wickets >= 4) pts += 8f
        else if (b.wickets >= 3) pts += 4f
        pts += b.maidens * 12f
        if (b.overs >= 2f) {
            pts += when {
                b.economy < 5f  -> 6f
                b.economy < 6f  -> 4f
                b.economy < 7f  -> 2f
                b.economy > 12f -> -6f
                b.economy > 11f -> -4f
                b.economy > 10f -> -2f
                else -> 0f
            }
        }
        return pts
    }

    fun applyMultiplier(pts: Float, multi: String): Float = when (multi) {
        "2x"   -> pts * 2f
        "1.5x" -> pts * 1.5f
        else   -> pts
    }
}

// ─────────────────────────────────────────────
// VIEWMODEL
// ─────────────────────────────────────────────

class LiveScoreViewModel : ViewModel() {

    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _state = MutableStateFlow(LiveScoreUiState())
    val state: StateFlow<LiveScoreUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var lastMatchInfo: CricMatch? = null
    private var lastScorecardHash: Int = 0
    private var lastCommentarySize: Int = 0

    // ── Load all data ──
    fun load(matchId: String) {
        _state.update { it.copy(matchId = matchId, isLoading = true, error = null) }
        viewModelScope.launch {
            awaitAll(
                async { loadMatchInfo(matchId) },
                async { loadScorecard(matchId) },
                async { loadCommentary(matchId) },
                async { loadMyTeam(matchId) },
                async { loadLeaderboard(matchId) }
            )
            _state.update { it.copy(isLoading = false, lastUpdated = System.currentTimeMillis()) }
            startPolling(matchId)
        }
    }

    // ── Manual refresh ──
    fun refresh(matchId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            loadMatchInfo(matchId)
            loadScorecard(matchId)
            loadCommentary(matchId)
            val balls = parseRecentBalls(_state.value.commentary)
            _state.update { it.copy(recentBalls = balls, isRefreshing = false, lastUpdated = System.currentTimeMillis()) }
        }
    }

    // ── Match info ──
    private suspend fun loadMatchInfo(matchId: String) {
        when (val r = CricApiRepository.getMatchInfo(matchId)) {
            is ApiResult.Success -> {
                val m = r.data
                if (m == lastMatchInfo) return
                lastMatchInfo = m
                val t1 = m.teamInfo?.getOrNull(0)?.shortname ?: m.teams.getOrElse(0) { "" }
                val t2 = m.teamInfo?.getOrNull(1)?.shortname ?: m.teams.getOrElse(1) { "" }
                val s1 = m.score?.firstOrNull { it.inning.contains(t1, true) }
                val s2 = m.score?.firstOrNull { it.inning.contains(t2, true) }
                val cr  = if (s2 != null && s2.o > 0) s2.r.toFloat() / s2.o.toFloat() else 0f
                val tgt = (s1?.r ?: 0) + 1
                val ballsLeft = if (s2 != null) ((20.0 - s2.o) * 6).toInt() else 0
                val rrr = if (ballsLeft > 0) (tgt - (s2?.r ?: 0)).toFloat() / (ballsLeft / 6f) else 0f
                val w1  = if (s2 != null && cr > 0) (cr / (cr + rrr) * 100).coerceIn(10f, 90f) else 50f
                _state.update { st -> st.copy(
                    matchName  = m.name,
                    matchType  = m.matchType,
                    venue      = m.venue,
                    status     = m.status,
                    isLive     = m.matchStarted && !m.matchEnded,
                    isEnded    = m.matchEnded,
                    team1      = t1,
                    team2      = t2,
                    score1     = if (s1 != null) "${s1.r}/${s1.w}" else "—",
                    overs1     = if (s1 != null) String.format("%.1f", s1.o) else "0.0",
                    score2     = if (s2 != null) "${s2.r}/${s2.w}" else "—",
                    overs2     = if (s2 != null) String.format("%.1f", s2.o) else "0.0",
                    currentRR  = String.format("%.2f", cr).toFloat(),
                    requiredRR = String.format("%.2f", rrr).toFloat(),
                    target     = tgt,
                    winProb1   = w1,
                    winProb2   = 100f - w1
                )}
            }
            is ApiResult.Error -> _state.update { it.copy(error = r.message) }
            else -> Unit
        }
    }

    // ── Scorecard ──
    private suspend fun loadScorecard(matchId: String) {
        when (val r = CricApiRepository.getScorecard(matchId)) {
            is ApiResult.Success -> {
                val hash = r.data.hashCode()
                if (hash == lastScorecardHash) return
                lastScorecardHash = hash
                val inns = r.data.scorecard?.map { inn ->
                    val batters = inn.batting?.map { b ->
                        BatterStat(
                            name      = b.batsman ?: "",
                            runs      = b.r ?: 0,
                            balls     = b.b ?: 0,
                            fours     = b.fours ?: 0,
                            sixes     = b.sixes ?: 0,
                            sr        = b.strikeRate?.toFloatOrNull() ?: 0f,
                            dismissal = b.dismissal ?: "not out",
                            isOnField = b.dismissal.isNullOrBlank()
                        )
                    } ?: emptyList()
                    val bowlers = inn.bowling?.map { bw ->
                        BowlerStat(
                            name    = bw.bowler ?: "",
                            overs   = bw.o?.toFloatOrNull() ?: 0f,
                            maidens = bw.m ?: 0,
                            runs    = bw.r ?: 0,
                            wickets = bw.w ?: 0,
                            economy = bw.economy?.toFloatOrNull() ?: 0f
                        )
                    } ?: emptyList()
                    InningsData(
                        teamName = inn.inning ?: "",
                        total    = inn.total ?: "",
                        batters  = batters,
                        bowlers  = bowlers,
                        extras   = "Extras: ${inn.extras?.total ?: 0} (w ${inn.extras?.wide ?: 0}, nb ${inn.extras?.nb ?: 0})"
                    )
                } ?: emptyList()
                val liveBat = inns.lastOrNull()?.batters?.filter { it.isOnField } ?: emptyList()
                val liveBwl = inns.dropLast(1).lastOrNull()?.bowlers
                    ?.sortedByDescending { it.overs }?.take(2) ?: emptyList()
                _state.update { it.copy(innings = inns, liveBatters = liveBat, liveBowlers = liveBwl) }
                computeFantasyBreakdown(inns)
            }
            else -> Unit
        }
    }

    // ── Commentary ──
    private suspend fun loadCommentary(matchId: String) {
        when (val r = CricApiRepository.getCommentary(matchId)) {
            is ApiResult.Success -> {
                if (r.data.size == lastCommentarySize) return
                lastCommentarySize = r.data.size
                val items = r.data.map { c ->
                    val text = c.comment ?: ""
                    CommentaryItem(
                        over = c.ball ?: "",
                        text = text,
                        type = detectBallType(text)
                    )
                }
                val balls = parseRecentBalls(items)
                _state.update { it.copy(commentary = items, recentBalls = balls) }
            }
            else -> Unit
        }
    }

    // ── My team from Firebase ──
    private suspend fun loadMyTeam(matchId: String) {
        if (uid.isEmpty()) return
        try {
            val snap = db.collection("users").document(uid)
                .collection("teams")
                .whereEqualTo("matchId", matchId)
                .limit(1).get().await()
            if (snap.isEmpty) return
            val doc     = snap.documents.first()
            val cap     = doc.getString("captainId") ?: ""
            val vc      = doc.getString("viceCaptainId") ?: ""
            @Suppress("UNCHECKED_CAST")
            val players = doc.get("players") as? List<Map<String, Any>> ?: return
            val ppList  = players.map { p ->
                val name  = p["name"] as? String ?: ""
                val pts   = (p["points"] as? Number)?.toFloat() ?: 0f
                val pid   = p["id"] as? String ?: ""
                val multi = when (pid) { cap -> "2x"; vc -> "1.5x"; else -> "1x" }
                val finalPts = FantasyCalculator.applyMultiplier(pts, multi)
                Triple(
                    name + (if (pid == cap) " (C)" else "") + (if (pid == vc) " (VC)" else ""),
                    finalPts,
                    multi
                )
            }.sortedByDescending { it.second }
            val total = ppList.sumOf { it.second.toDouble() }.toFloat()
            _state.update { it.copy(myTeam = MyFantasyTeam(ppList, total, 0)) }
        } catch (_: Exception) {}
    }

    // ── Leaderboard from Firebase ──
    private suspend fun loadLeaderboard(matchId: String) {
        if (uid.isEmpty()) return
        try {
            val contestSnap = db.collection("contests")
                .whereEqualTo("matchId", matchId)
                .limit(1).get().await()
            if (contestSnap.isEmpty) return
            val contestId = contestSnap.documents.first().id
            val lbSnap = db.collection("contests").document(contestId)
                .collection("leaderboard")
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(50).get().await()
            val entries = lbSnap.documents.mapIndexed { idx, doc ->
                LeaderboardEntry(
                    uid           = doc.id,
                    name          = doc.getString("name") ?: "Player",
                    points        = doc.getDouble("points")?.toFloat() ?: 0f,
                    rank          = idx + 1,
                    isCurrentUser = doc.id == uid
                )
            }
            val myRank = entries.find { it.isCurrentUser }?.rank ?: 0
            _state.update { it.copy(leaderboard = entries, userRank = myRank) }
        } catch (_: Exception) {}
    }

    // ── Fantasy points breakdown from scorecard ──
    private fun computeFantasyBreakdown(innings: List<InningsData>) {
        val breakdown = mutableListOf<FantasyPlayerPoints>()
        innings.flatMap { it.batters }.forEach { b ->
            val pts = FantasyCalculator.battingPoints(b)
            breakdown.add(FantasyPlayerPoints(
                name = b.name, runs = b.runs, fours = b.fours,
                sixes = b.sixes, totalPoints = pts
            ))
        }
        innings.flatMap { it.bowlers }.forEach { bw ->
            val existing = breakdown.indexOfFirst { it.name == bw.name }
            val bowlPts  = FantasyCalculator.bowlingPoints(bw)
            if (existing >= 0) {
                val e = breakdown[existing]
                breakdown[existing] = e.copy(
                    wickets     = bw.wickets,
                    maidens     = bw.maidens,
                    totalPoints = e.totalPoints + bowlPts
                )
            } else {
                breakdown.add(FantasyPlayerPoints(
                    name = bw.name, wickets = bw.wickets,
                    maidens = bw.maidens, totalPoints = bowlPts
                ))
            }
        }
        _state.update { it.copy(playerBreakdown = breakdown.sortedByDescending { it.totalPoints }) }
    }

    // ── Ball type detection ──
    private fun detectBallType(text: String): String = when {
        text.contains("WICKET", true) || text.contains(" out ", true) ||
        text.contains("caught", true) || text.contains("bowled", true) ||
        text.contains("lbw", true) || text.contains("run out", true) -> "wicket"
        text.contains("SIX", true) || text.contains(" 6 ")           -> "six"
        text.contains("FOUR", true) || text.contains(" 4 ")          -> "four"
        text.contains("wide", true)                                   -> "wide"
        text.contains("no ball", true)                                -> "noball"
        else                                                          -> "normal"
    }

    // ── Parse recent balls for this over ──
    private fun parseRecentBalls(list: List<CommentaryItem>): List<String> {
        if (list.isEmpty()) return emptyList()
        val fo = list.first().over.split(".").firstOrNull() ?: return emptyList()
        return list
            .filter { it.over.startsWith("$fo.") }
            .map { c ->
                val t = c.text.lowercase()
                when {
                    c.type == "wicket" -> "W"
                    c.type == "six"    -> "6"
                    c.type == "four"   -> "4"
                    c.type == "wide"   -> "Wd"
                    c.type == "noball" -> "Nb"
                    else -> Regex("(\\d+) run").find(t)?.groupValues?.get(1) ?: "."
                }
            }
            .take(6)
            .reversed()
    }

    // ── Smart polling ──
    private fun startPolling(matchId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val st       = _state.value
                val interval = when {
                    st.isEnded                      -> break
                    st.isLive                       -> 12_000L
                    st.status.contains("break", true) -> 30_000L
                    else                            -> 20_000L
                }
                delay(interval)
                if (_state.value.isLive) {
                    loadMatchInfo(matchId)
                    loadScorecard(matchId)
                    loadCommentary(matchId)
                    _state.update { it.copy(lastUpdated = System.currentTimeMillis()) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
