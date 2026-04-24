package com.example.dream11india

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MatchRepository {
    suspend fun fetchMatches(): List<CricMatch> = withContext(Dispatchers.IO) {
        val all = mutableListOf<CricMatch>()
        runCatching {
            val live = CricApiClient.service.getLiveMatches()
            if (live.isSuccessful) {
                live.body()?.data?.mapNotNull { CricApiMapper.mapMatch(it) }?.let { all.addAll(it) }
            }
        }
        runCatching {
            val upcoming = CricApiClient.service.getUpcomingMatches()
            if (upcoming.isSuccessful) {
                upcoming.body()?.data?.mapNotNull { CricApiMapper.mapMatch(it) }?.let { all.addAll(it) }
            }
        }
        all.distinctBy { it.id }
    }
}
