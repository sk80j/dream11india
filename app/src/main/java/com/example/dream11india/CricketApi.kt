package com.example.dream11india

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

const val RAPIDAPI_KEY = "18ad7c6629msh2916b44e24755c8p193182jsn4df4f01d4867"
const val RAPIDAPI_HOST = "cricbuzz-cricket.p.rapidapi.com"
const val CRICBUZZ_BASE_URL = "https://cricbuzz-cricket.p.rapidapi.com/"

// ===== MATCH LIST MODELS =====
data class CricbuzzMatchListResponse(
    val typeMatches: List<TypeMatch>? = null
)
data class TypeMatch(
    val matchType: String? = null,
    val seriesMatches: List<SeriesMatchWrapper>? = null
)
data class SeriesMatchWrapper(
    val seriesAdWrapper: SeriesAdWrapper? = null
)
data class SeriesAdWrapper(
    val seriesName: String? = null,
    val matches: List<CricbuzzMatch>? = null
)
data class CricbuzzMatch(
    val matchInfo: MatchInfo? = null,
    val matchScore: MatchScore? = null
)
data class MatchInfo(
    val matchId: Int = 0,
    val team1: TeamInfo2? = null,
    val team2: TeamInfo2? = null,
    val status: String? = null,
    val state: String? = null,
    val startDate: String? = null,
    val venueInfo: VenueInfo? = null
)
data class TeamInfo2(
    val teamId: Int = 0,
    val teamName: String? = null,
    val teamSName: String? = null,
    val imageId: Int = 0
)
data class VenueInfo(
    val ground: String? = null,
    val city: String? = null
)
data class MatchScore(
    val team1Score: TeamScore? = null,
    val team2Score: TeamScore? = null
)
data class TeamScore(
    val inngs1: Innings? = null
)
data class Innings(
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int = 0,
    @SerializedName("overs") val overs: Double = 0.0
)

// ===== MATCH CENTER / PLAYERS MODELS =====
data class MatchCenterResponse(
    val matchInfo: MatchCenterInfo? = null,
    val matchScore: MatchScore? = null,
    val team1: TeamPlayers? = null,
    val team2: TeamPlayers? = null
)
data class MatchCenterInfo(
    val matchId: Int = 0,
    val team1: TeamInfo2? = null,
    val team2: TeamInfo2? = null,
    val status: String? = null,
    val state: String? = null
)
data class TeamPlayers(
    val teamId: Int = 0,
    val teamName: String? = null,
    val teamSName: String? = null,
    val players: List<CricbuzzPlayer>? = null
)
data class CricbuzzPlayer(
    val id: Int = 0,
    val name: String? = null,
    val fullName: String? = null,
    val role: String? = null,
    val battingStyle: String? = null,
    val bowlingStyle: String? = null,
    val imageId: Int = 0,
    val isPlaying: Boolean = false
)

// ===== LOCAL MODELS =====
data class CricMatch(
    val id: String = "",
    val name: String = "",
    val status: String = "",
    val venue: String = "",
    val date: String = "",
    val teams: List<String> = emptyList(),
    val teamInfo: List<TeamInfo>? = null,
    val score: List<Score>? = null,
    val matchStarted: Boolean = false,
    val matchEnded: Boolean = false
)
data class TeamInfo(
    val name: String = "",
    val shortname: String = "",
    val img: String = ""
)
data class Score(
    val r: Int = 0,
    val w: Int = 0,
    val o: Double = 0.0,
    val inning: String = ""
)

// ===== API SERVICE =====
interface CricbuzzApiService {
    @GET("matches/v1/live")
    suspend fun getLiveMatches(
        @Header("x-rapidapi-host") host: String = RAPIDAPI_HOST,
        @Header("x-rapidapi-key") key: String = RAPIDAPI_KEY
    ): CricbuzzMatchListResponse

    @GET("matches/v1/upcoming")
    suspend fun getUpcomingMatches(
        @Header("x-rapidapi-host") host: String = RAPIDAPI_HOST,
        @Header("x-rapidapi-key") key: String = RAPIDAPI_KEY
    ): CricbuzzMatchListResponse

    @GET("matches/v1/recent")
    suspend fun getRecentMatches(
        @Header("x-rapidapi-host") host: String = RAPIDAPI_HOST,
        @Header("x-rapidapi-key") key: String = RAPIDAPI_KEY
    ): CricbuzzMatchListResponse

    @GET("mcenter/v1/{matchId}")
    suspend fun getMatchCenter(
        @Path("matchId") matchId: String,
        @Header("x-rapidapi-host") host: String = RAPIDAPI_HOST,
        @Header("x-rapidapi-key") key: String = RAPIDAPI_KEY
    ): MatchCenterResponse

    @GET("mcenter/v1/{matchId}/hscard")
    suspend fun getMatchScorecard(
        @Path("matchId") matchId: String,
        @Header("x-rapidapi-host") host: String = RAPIDAPI_HOST,
        @Header("x-rapidapi-key") key: String = RAPIDAPI_KEY
    ): MatchCenterResponse
}

object CricbuzzApi {
    val service: CricbuzzApiService by lazy {
        Retrofit.Builder()
            .baseUrl(CRICBUZZ_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CricbuzzApiService::class.java)
    }
}

// ===== PLAYER HELPER FUNCTIONS =====
fun getPlayerImageUrl(imageId: Int): String {
    return "https://cricbuzz-cricket.p.rapidapi.com/img/v1/i1/c$imageId/i.jpg"
}

fun mapCricbuzzRole(role: String?): String {
    return when {
        role == null -> "BAT"
        role.contains("Keeper", true) || role.contains("WK", true) -> "WK"
        role.contains("Allrounder", true) || role.contains("All-rounder", true) -> "AR"
        role.contains("Bowler", true) || role.contains("Bowl", true) -> "BOWL"
        role.contains("Batsman", true) || role.contains("Bat", true) -> "BAT"
        else -> "BAT"
    }
}

fun getDefaultCredits(role: String): Float {
    return when(role) {
        "WK" -> 9.0f
        "BAT" -> 9.0f
        "AR" -> 9.5f
        "BOWL" -> 9.0f
        else -> 8.5f
    }
}

fun convertToPlayer(
    cricPlayer: CricbuzzPlayer,
    teamName: String,
    teamShortName: String
): Player {
    val role = mapCricbuzzRole(cricPlayer.role)
    val shortName = cricPlayer.name?.split(" ")?.lastOrNull() ?: cricPlayer.name ?: "Player"
    return Player(
        id = cricPlayer.id.toString(),
        name = cricPlayer.fullName ?: cricPlayer.name ?: "Player",
        shortName = shortName,
        team = teamShortName,
        role = role,
        credits = getDefaultCredits(role),
        selectionPercent = (30..85).random(),
        points = 0f,
        isSelected = false
    )
}
