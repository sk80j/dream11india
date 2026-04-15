package com.example.dream11india

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// UI STATE
// ─────────────────────────────────────────────
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
    val bannerIndex:    Int          = 0
)

// ─────────────────────────────────────────────
// VIEWMODEL
// ─────────────────────────────────────────────
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob:  Job? = null
    private var pollingJob:  Job? = null
    private var bannerJob:   Job? = null

    init {
        loadMatches(showLoading = true)
        startPolling()
        startBannerRotation()
    }

    // ── Public actions ───────────────────────

    fun refresh() {
        loadMatches(showLoading = false, isManualRefresh = true)
    }

    fun selectLeague(league: String) {
        _uiState.update { it.copy(selectedLeague = league) }
    }

    fun selectFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun selectSport(sport: String) {
        _uiState.update { it.copy(selectedSport = sport) }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    // ── Derived filtered list ────────────────
    fun filteredMatches(state: HomeUiState): List<CricMatch> {
        val raw = (state.matchState as? MatchUiState.Success)?.matches ?: return emptyList()

        return raw
            .let { list ->
                val q = state.searchQuery.trim()
                if (q.isBlank()) list
                else list.filter {
                    it.name.contains(q, true) ||
                            it.teams.any { t -> t.contains(q, true) }
                }
            }
            .let { list ->
                when (state.selectedLeague) {
                    "IPL" -> list.filter { m ->
                        m.teams.any { t ->
                            IPL_TEAMS.any { t.contains(it, true) }
                        }
                    }
                    "T20"  -> list.filter { it.status.contains("T20",  ignoreCase = true) }
                    "ODI"  -> list.filter { it.status.contains("ODI",  ignoreCase = true) }
                    "Test" -> list.filter { it.status.contains("Test", ignoreCase = true) }
                    else   -> list
                }
            }
            .let { list ->
                when (state.selectedFilter) {
                    "Live"      -> list.filter {  it.matchStarted && !it.matchEnded }
                    "Upcoming"  -> list.filter { !it.matchStarted }
                    "Completed" -> list.filter {  it.matchEnded }
                    else        -> list
                }
            }
    }

    // ── Private helpers ──────────────────────

    private fun loadMatches(showLoading: Boolean, isManualRefresh: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(matchState = MatchUiState.Loading) }
            if (isManualRefresh) _uiState.update { it.copy(isRefreshing = true) }

            val result = runCatching { MatchRepository.fetchMatches() }

            _uiState.update { state ->
                state.copy(
                    matchState   = result.fold(
                        onSuccess = { MatchUiState.Success(it) },
                        onFailure = { MatchUiState.Error(it.message ?: "Unknown error") }
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
        const val BANNER_COUNT = 3
        val IPL_TEAMS = listOf(
            "Mumbai", "Chennai", "Royal", "Kolkata",
            "Delhi", "Punjab", "Rajasthan", "Sunrisers", "Gujarat", "Lucknow"
        )
    }
}