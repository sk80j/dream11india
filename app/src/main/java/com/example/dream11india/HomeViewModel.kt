package com.example.dream11india

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class MatchUiState {
    object Loading : MatchUiState()
    data class Success(val matches: List<CricMatch>) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}

data class HomeUiState(
    val matchState:     MatchUiState = MatchUiState.Loading,
    val selectedLeague: String       = "All",
    val selectedFilter: String       = "All",
    val selectedSport:  String       = "Cricket",
    val searchQuery:    String       = "",
    val isRefreshing:   Boolean      = false,
    val bannerIndex:    Int          = 0,
    val joinedMatchIds: Set<String>  = emptySet()
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var pollingJob: Job? = null
    private var bannerJob:  Job? = null

    init {
        loadMatches(showLoading = true)
        loadJoinedMatchIds()
        startPolling()
        startBannerRotation()
    }

    fun refresh() { loadMatches(showLoading = false, isManualRefresh = true) }
    fun selectLeague(league: String) { _uiState.update { it.copy(selectedLeague = league) } }
    fun selectFilter(filter: String) { _uiState.update { it.copy(selectedFilter = filter) } }
    fun selectSport(sport: String)   { _uiState.update { it.copy(selectedSport = sport) } }
    fun updateSearch(query: String)  { _uiState.update { it.copy(searchQuery = query) } }
    fun clearSearch()                { _uiState.update { it.copy(searchQuery = "") } }

    private fun loadJoinedMatchIds() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("joined_contests")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.mapNotNull { d -> d.getString("matchId") }?.toSet() ?: emptySet()
                _uiState.update { it.copy(joinedMatchIds = ids) }
            }
    }

    // ── Main filter function — called from UI ──
    fun filteredMatches(state: HomeUiState): List<CricMatch> {
        val raw = (state.matchState as? MatchUiState.Success)?.matches
            ?: return emptyList()

        if (raw.isEmpty()) return emptyList()

        return raw
            // 1. Search filter
            .let { list ->
                val q = state.searchQuery.trim()
                if (q.isBlank()) list
                else list.filter {
                    it.name.contains(q, ignoreCase = true) ||
                    it.teams.any { t -> t.contains(q, ignoreCase = true) }
                }
            }
            // 2. League filter
            .let { list ->
                when (state.selectedLeague) {
                    "IPL"  -> list.filter { m ->
                        m.name.contains("IPL", ignoreCase = true) ||
                        m.teams.any { t -> IPL_TEAMS.any { ipl -> t.contains(ipl, ignoreCase = true) } }
                    }
                    "T20"  -> list.filter { it.matchType.contains("t20",  ignoreCase = true) }
                    "ODI"  -> list.filter { it.matchType.contains("odi",  ignoreCase = true) }
                    "Test" -> list.filter { it.matchType.contains("test", ignoreCase = true) }
                    else   -> list
                }
            }
            // 3. Status filter
            .let { list ->
                when (state.selectedFilter) {
                    "Live"      -> list.filter { it.matchStarted && !it.matchEnded }
                    "Upcoming"  -> list.filter { !it.matchStarted }
                    "Completed" -> list.filter { it.matchEnded }
                    else        -> list  // "All" — show everything
                }
            }
    }

    // My Matches — only joined
    fun myMatches(state: HomeUiState): List<CricMatch> {
        val raw = (state.matchState as? MatchUiState.Success)?.matches ?: return emptyList()
        return raw.filter { state.joinedMatchIds.contains(it.id) }
    }

    fun getMatchButtonLabel(match: CricMatch, joinedMatchIds: Set<String>): String = when {
        match.matchEnded                      -> "View Results"
        match.matchStarted                    -> "View Contest"
        joinedMatchIds.contains(match.id)     -> "View Contest"
        else                                  -> "Play"
    }

    private fun loadMatches(showLoading: Boolean, isManualRefresh: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (showLoading)      _uiState.update { it.copy(matchState = MatchUiState.Loading) }
            if (isManualRefresh)  _uiState.update { it.copy(isRefreshing = true) }

            val result = runCatching { MatchRepository.fetchMatches() }

            android.util.Log.d("HomeVM", "Fetched: ${result.getOrNull()?.size} matches, err: ${result.exceptionOrNull()?.message}")

            _uiState.update { state ->
                state.copy(
                    matchState = result.fold(
                        onSuccess = { matches ->
                            if (matches.isEmpty()) MatchUiState.Error("No matches available")
                            else MatchUiState.Success(matches)
                        },
                        onFailure = { MatchUiState.Error(it.message ?: "Failed to load matches") }
                    ),
                    isRefreshing = false
                )
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                loadMatches(showLoading = false)
            }
        }
    }

    private fun startBannerRotation() {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            while (isActive) {
                delay(3_000)
                _uiState.update { it.copy(bannerIndex = (it.bannerIndex + 1) % BANNER_COUNT) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        pollingJob?.cancel()
        bannerJob?.cancel()
    }

    companion object {
        const val BANNER_COUNT = 5
        val IPL_TEAMS = listOf(
            "Mumbai", "Chennai", "Royal", "Kolkata",
            "Delhi", "Punjab", "Rajasthan", "Sunrisers", "Gujarat", "Lucknow"
        )
    }
}
