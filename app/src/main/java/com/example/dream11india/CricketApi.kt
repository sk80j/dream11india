package com.example.dream11india

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────
// API CONFIG
// ─────────────────────────────────────────────

object ApiConfig {
    // Key from BuildConfig — never hardcode in release
    val API_KEY: String get() = try {
        BuildConfig.CRIC_API_KEY
    } catch (e: Exception) {
        "26fb0ff7-aad3-4882-a9e5-2d4da812e390"
    }
    const val BASE_URL        = "https://api.cricapi.com/v1/"
    const val CONNECT_TIMEOUT = 15L
    const val READ_TIMEOUT    = 30L
    const val WRITE_TIMEOUT   = 15L
    const val CACHE_SIZE      = 10L * 1024 * 1024 // 10 MB
}

// ─────────────────────────────────────────────
// RETROFIT CLIENT
// ─────────────────────────────────────────────

object CricApiClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BASIC
        else
            HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: CricbuzzApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().setLenient().serializeNulls().create()
            ))
            .build()
            .create(CricbuzzApiService::class.java)
    }
}

// ─────────────────────────────────────────────
// API SERVICE
// ─────────────────────────────────────────────

interface CricbuzzApiService {

    @GET("currentMatches")
    suspend fun getLiveMatches(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("offset") offset: Int = 0
    ): Response<CricApiMatchListResponse>

    @GET("matches")
    suspend fun getUpcomingMatches(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("offset") offset: Int = 0
    ): Response<CricApiMatchListResponse>

    @GET("matches")
    suspend fun getRecentMatches(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("offset") offset: Int = 0,
        @Query("status") status: String = "completed"
    ): Response<CricApiMatchListResponse>

    @GET("match_info")
    suspend fun getMatchInfo(
        @Query("apikey") apiKey: String,
        @Query("id") matchId: String
    ): Response<CricApiMatchInfoResponse>

    @GET("match_scorecard")
    suspend fun getScorecard(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("id") matchId: String
    ): Response<CricApiScorecardResponse>

    @GET("match_bbb")
    suspend fun getCommentary(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("id") matchId: String
    ): Response<CricApiCommentaryResponse>

    @GET("match_squad")
    suspend fun getSquad(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("id") matchId: String
    ): Response<CricApiSquadResponse>

    @GET("series")
    suspend fun getSeriesList(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("offset") offset: Int = 0
    ): Response<CricApiSeriesListResponse>

    @GET("series_info")
    suspend fun getIplFixtures(
        @Query("apikey") apiKey: String,
        @Query("id") seriesId: String
    ): Response<CricApiSeriesInfoResponse>

    @GET("players_info")
    suspend fun getPlayerInfo(
        @Query("apikey") apiKey: String,
        @Query("id") playerId: String
    ): Response<CricApiPlayerInfoResponse>
}

// ─────────────────────────────────────────────
// DTOs
// ─────────────────────────────────────────────

data class CricApiMatchListResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   List<CricApiMatch>? = null,
    @SerializedName("info")   val info:   CricApiPageInfo? = null
)
data class CricApiPageInfo(
    @SerializedName("hitsTotal")   val hitsTotal:   Int? = null,
    @SerializedName("hitsPerPage") val hitsPerPage: Int? = null
)
data class CricApiMatch(
    @SerializedName("id")             val id:             String? = null,
    @SerializedName("name")           val name:           String? = null,
    @SerializedName("matchType")      val matchType:      String? = null,
    @SerializedName("status")         val status:         String? = null,
    @SerializedName("venue")          val venue:          String? = null,
    @SerializedName("date")           val date:           String? = null,
    @SerializedName("dateTimeGMT")    val dateTimeGMT:    String? = null,
    @SerializedName("teams")          val teams:          List<String>? = null,
    @SerializedName("teamInfo")       val teamInfo:       List<CricApiTeamInfo>? = null,
    @SerializedName("score")          val score:          List<CricApiScore>? = null,
    @SerializedName("series_id")      val seriesId:       String? = null,
    @SerializedName("fantasyEnabled") val fantasyEnabled: Boolean? = null,
    @SerializedName("bbbEnabled")     val bbbEnabled:     Boolean? = null,
    @SerializedName("hasSquad")       val hasSquad:       Boolean? = null,
    @SerializedName("matchStarted")   val matchStarted:   Boolean? = null,
    @SerializedName("matchEnded")     val matchEnded:     Boolean? = null
)
data class CricApiTeamInfo(
    @SerializedName("name")      val name:      String? = null,
    @SerializedName("shortname") val shortname: String? = null,
    @SerializedName("img")       val img:       String? = null
)
data class CricApiScore(
    @SerializedName("r")      val r:      Int? = null,
    @SerializedName("w")      val w:      Int? = null,
    @SerializedName("o")      val o:      Double? = null,
    @SerializedName("inning") val inning: String? = null
)
data class CricApiMatchInfoResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   CricApiMatchDetail? = null
)
data class CricApiMatchDetail(
    @SerializedName("id")           val id:           String? = null,
    @SerializedName("name")         val name:         String? = null,
    @SerializedName("matchType")    val matchType:    String? = null,
    @SerializedName("status")       val status:       String? = null,
    @SerializedName("venue")        val venue:        String? = null,
    @SerializedName("date")         val date:         String? = null,
    @SerializedName("teams")        val teams:        List<String>? = null,
    @SerializedName("teamInfo")     val teamInfo:     List<CricApiTeamInfo>? = null,
    @SerializedName("score")        val score:        List<CricApiScore>? = null,
    @SerializedName("tossWinner")   val tossWinner:   String? = null,
    @SerializedName("tossChoice")   val tossChoice:   String? = null,
    @SerializedName("matchWinner")  val matchWinner:  String? = null,
    @SerializedName("matchStarted") val matchStarted: Boolean? = null,
    @SerializedName("matchEnded")   val matchEnded:   Boolean? = null
)
data class CricApiScorecardResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   CricApiScorecardData? = null
)
data class CricApiScorecardData(
    @SerializedName("id")        val id:        String? = null,
    @SerializedName("name")      val name:      String? = null,
    @SerializedName("scorecard") val scorecard: List<CricApiInningsCard>? = null
)
data class CricApiInningsCard(
    @SerializedName("inning")  val inning:  String? = null,
    @SerializedName("batting") val batting: List<CricApiBatting>? = null,
    @SerializedName("bowling") val bowling: List<CricApiBowling>? = null,
    @SerializedName("extras")  val extras:  CricApiExtras? = null,
    @SerializedName("total")   val total:   String? = null
)
data class CricApiBatting(
    @SerializedName("batsman")   val batsman:    String? = null,
    @SerializedName("dismissal") val dismissal:  String? = null,
    @SerializedName("r")         val r:          Int? = null,
    @SerializedName("b")         val b:          Int? = null,
    @SerializedName("4s")        val fours:      Int? = null,
    @SerializedName("6s")        val sixes:      Int? = null,
    @SerializedName("sr")        val strikeRate: String? = null
)
data class CricApiBowling(
    @SerializedName("bowler") val bowler:  String? = null,
    @SerializedName("o")      val o:       String? = null,
    @SerializedName("m")      val m:       Int? = null,
    @SerializedName("r")      val r:       Int? = null,
    @SerializedName("w")      val w:       Int? = null,
    @SerializedName("eco")    val economy: String? = null
)
data class CricApiExtras(
    @SerializedName("total") val total: Int? = null,
    @SerializedName("b")     val b:     Int? = null,
    @SerializedName("lb")    val lb:    Int? = null,
    @SerializedName("wide")  val wide:  Int? = null,
    @SerializedName("nb")    val nb:    Int? = null
)
data class CricApiCommentaryResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   CricApiCommentaryData? = null
)
data class CricApiCommentaryData(
    @SerializedName("id")          val id:          String? = null,
    @SerializedName("commentary")  val commentary:  List<CricApiCommentary>? = null
)
data class CricApiCommentary(
    @SerializedName("ball")      val ball:      String? = null,
    @SerializedName("comment")   val comment:   String? = null,
    @SerializedName("timestamp") val timestamp: Long? = null
)
data class CricApiSquadResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   CricApiSquadData? = null
)
data class CricApiSquadData(
    @SerializedName("id")    val id:    String? = null,
    @SerializedName("name")  val name:  String? = null,
    @SerializedName("squad") val squad: List<CricApiSquadTeam>? = null
)
data class CricApiSquadTeam(
    @SerializedName("name")    val name:    String? = null,
    @SerializedName("players") val players: List<CricApiPlayer>? = null
)
data class CricApiPlayer(
    @SerializedName("id")   val id:   String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String? = null
)
data class CricApiSeriesListResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   List<CricApiSeries>? = null
)
data class CricApiSeries(
    @SerializedName("id")        val id:        String? = null,
    @SerializedName("name")      val name:      String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate")   val endDate:   String? = null,
    @SerializedName("odi")       val odi:       Int? = null,
    @SerializedName("t20")       val t20:       Int? = null,
    @SerializedName("test")      val test:      Int? = null,
    @SerializedName("squads")    val squads:    Int? = null,
    @SerializedName("matches")   val matches:   Int? = null
)
data class CricApiSeriesInfoResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   CricApiSeriesDetail? = null
)
data class CricApiSeriesDetail(
    @SerializedName("id")      val id:      String? = null,
    @SerializedName("name")    val name:    String? = null,
    @SerializedName("matches") val matches: List<CricApiMatch>? = null
)
data class CricApiPlayerInfoResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data")   val data:   CricApiPlayerDetail? = null
)
data class CricApiPlayerDetail(
    @SerializedName("id")           val id:           String? = null,
    @SerializedName("name")         val name:         String? = null,
    @SerializedName("dateOfBirth")  val dateOfBirth:  String? = null,
    @SerializedName("role")         val role:         String? = null,
    @SerializedName("battingStyle") val battingStyle: String? = null,
    @SerializedName("bowlingStyle") val bowlingStyle: String? = null,
    @SerializedName("country")      val country:      String? = null,
    @SerializedName("playerImg")    val playerImg:    String? = null
)

// ─────────────────────────────────────────────
// DOMAIN MODELS
// ─────────────────────────────────────────────

data class CricMatch(
    val id:             String  = "",
    val name:           String  = "",
    val status:         String  = "",
    val venue:          String  = "",
    val date:           String  = "",
    val teams:          List<String>     = emptyList(),
    val teamInfo:       List<TeamInfo>?  = null,
    val score:          List<Score>?     = null,
    val matchStarted:   Boolean = false,
    val matchEnded:     Boolean = false,
    val t1LogoUrl:      String  = "",
    val t2LogoUrl:      String  = "",
    val matchType:      String  = "T20",
    val seriesId:       String  = "",
    val fantasyEnabled: Boolean = true,
    // Contest data from Firestore — null until loaded
    val prizePool:      String? = null,
    val totalSpots:     String? = null,
    val filledSpots:    Int?    = null,
    val entryFee:       String? = null,
    val badge:          String? = null
)

data class TeamInfo(
    val name:      String = "",
    val shortname: String = "",
    val img:       String = ""
)

data class Score(
    val r:      Int    = 0,
    val w:      Int    = 0,
    val o:      Double = 0.0,
    val inning: String = ""
)

// ─────────────────────────────────────────────
// API RESULT
// ─────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

// ─────────────────────────────────────────────
// ERROR MAPPER
// ─────────────────────────────────────────────

object ApiErrorMapper {
    fun map(e: Exception): String = when {
        e is java.net.UnknownHostException       -> "No internet connection"
        e is java.net.SocketTimeoutException     -> "Request timed out. Try again."
        e is java.io.IOException                 -> "Network error. Check connection."
        e.message?.contains("429") == true       -> "Too many requests. Please wait."
        e.message?.contains("500") == true       -> "Server error. Try later."
        else                                     -> "Something went wrong: ${e.message}"
    }

    fun mapCode(code: Int, message: String): String = when (code) {
        400 -> "Bad request"
        401 -> "Invalid API key"
        404 -> "Match not found"
        429 -> "Rate limit exceeded. Please wait."
        500 -> "CricAPI server error"
        503 -> "Service temporarily unavailable"
        else -> "Error $code: $message"
    }
}

// ─────────────────────────────────────────────
// MAPPER
// ─────────────────────────────────────────────

object CricApiMapper {

    fun mapMatch(dto: CricApiMatch): CricMatch? {
        // Filter invalid matches
        if (dto.id.isNullOrEmpty()) return null
        if (dto.teams.isNullOrEmpty() || dto.teams!!.size < 2) return null
        return CricMatch(
            id             = dto.id ?: "",
            name           = dto.name ?: "",
            status         = dto.status ?: "",
            venue          = dto.venue ?: "",
            date           = dto.dateTimeGMT ?: dto.date ?: "",
            teams          = dto.teams ?: emptyList(),
            teamInfo       = dto.teamInfo?.map { mapTeamInfo(it) },
            score          = dto.score?.map { mapScore(it) },
            matchStarted   = dto.matchStarted ?: false,
            matchEnded     = dto.matchEnded ?: false,
            t1LogoUrl      = dto.teamInfo?.getOrNull(0)?.img ?: "",
            t2LogoUrl      = dto.teamInfo?.getOrNull(1)?.img ?: "",
            matchType      = dto.matchType ?: "T20",
            seriesId       = dto.seriesId ?: "",
            fantasyEnabled = dto.fantasyEnabled ?: true
            // prizePool, entryFee etc loaded from Firestore separately
        )
    }

    fun mapTeamInfo(dto: CricApiTeamInfo) = TeamInfo(
        name      = dto.name      ?: "",
        shortname = dto.shortname ?: "",
        img       = dto.img       ?: ""
    )

    fun mapScore(dto: CricApiScore) = Score(
        r      = dto.r      ?: 0,
        w      = dto.w      ?: 0,
        o      = dto.o      ?: 0.0,
        inning = dto.inning ?: ""
    )

    fun mapPlayer(dto: CricApiPlayer, teamShort: String): Player {
        val role = mapRole(dto.role)
        return Player(
            id               = dto.id ?: "",
            name             = dto.name ?: "Player",
            shortName        = dto.name?.split(" ")?.lastOrNull() ?: "Player",
            team             = teamShort,
            role             = role,
            credits          = defaultCredits(role),
            selectionPercent = 0,
            points           = 0f,
            isSelected       = false
        )
    }

    fun mapRole(role: String?): String = when {
        role == null -> "BAT"
        role.contains("Keeper",      ignoreCase = true) -> "WK"
        role.contains("WK",          ignoreCase = true) -> "WK"
        role.contains("Allrounder",  ignoreCase = true) -> "AR"
        role.contains("All-rounder", ignoreCase = true) -> "AR"
        role.contains("Bowler",      ignoreCase = true) -> "BOWL"
        role.contains("Batting",     ignoreCase = true) -> "BAT"
        else -> "BAT"
    }

    fun defaultCredits(role: String): Float = when (role) {
        "WK" -> 9.0f; "BAT" -> 9.0f; "AR" -> 9.5f; "BOWL" -> 9.0f; else -> 8.5f
    }
}

// ─────────────────────────────────────────────
// REPOSITORY
// ─────────────────────────────────────────────

object CricApiRepository {

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) ApiResult.Success(body)
                    else ApiResult.Error("Empty response", response.code())
                } else {
                    ApiResult.Error(
                        ApiErrorMapper.mapCode(response.code(), response.message()),
                        response.code()
                    )
                }
            } catch (e: Exception) {
                ApiResult.Error(ApiErrorMapper.map(e))
            }
        }

    suspend fun getLiveMatches(): ApiResult<List<CricMatch>> =
        when (val r = safeCall { CricApiClient.service.getLiveMatches() }) {
            is ApiResult.Success -> ApiResult.Success(
                r.data.data?.mapNotNull { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getUpcomingMatches(): ApiResult<List<CricMatch>> =
        when (val r = safeCall { CricApiClient.service.getUpcomingMatches() }) {
            is ApiResult.Success -> ApiResult.Success(
                r.data.data?.mapNotNull { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getRecentMatches(): ApiResult<List<CricMatch>> =
        when (val r = safeCall { CricApiClient.service.getRecentMatches() }) {
            is ApiResult.Success -> ApiResult.Success(
                r.data.data?.mapNotNull { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getMatchInfo(matchId: String): ApiResult<CricMatch> =
        when (val r = safeCall { CricApiClient.service.getMatchInfo(ApiConfig.API_KEY, matchId) }) {
            is ApiResult.Success -> {
                val d = r.data.data
                if (d != null) ApiResult.Success(CricMatch(
                    id           = d.id       ?: "",
                    name         = d.name     ?: "",
                    status       = d.status   ?: "",
                    venue        = d.venue    ?: "",
                    date         = d.date     ?: "",
                    teams        = d.teams    ?: emptyList(),
                    teamInfo     = d.teamInfo?.map { CricApiMapper.mapTeamInfo(it) },
                    score        = d.score?.map { CricApiMapper.mapScore(it) },
                    matchStarted = d.matchStarted ?: false,
                    matchEnded   = d.matchEnded   ?: false,
                    t1LogoUrl    = d.teamInfo?.getOrNull(0)?.img ?: "",
                    t2LogoUrl    = d.teamInfo?.getOrNull(1)?.img ?: "",
                    matchType    = d.matchType ?: "T20"
                ))
                else ApiResult.Error("Match not found")
            }
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getScorecard(matchId: String): ApiResult<CricApiScorecardData> =
        when (val r = safeCall { CricApiClient.service.getScorecard(matchId = matchId) }) {
            is ApiResult.Success -> {
                val d = r.data.data
                if (d != null) ApiResult.Success(d)
                else ApiResult.Error("Scorecard not available")
            }
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getCommentary(matchId: String): ApiResult<List<CricApiCommentary>> =
        when (val r = safeCall { CricApiClient.service.getCommentary(matchId = matchId) }) {
            is ApiResult.Success -> ApiResult.Success(r.data.data?.commentary ?: emptyList())
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getSquad(matchId: String): ApiResult<List<Player>> =
        when (val r = safeCall { CricApiClient.service.getSquad(matchId = matchId) }) {
            is ApiResult.Success -> {
                val players = mutableListOf<Player>()
                r.data.data?.squad?.forEach { team ->
                    val short = team.name?.take(3)?.uppercase() ?: "UNK"
                    team.players?.forEach { p -> players.add(CricApiMapper.mapPlayer(p, short)) }
                }
                ApiResult.Success(players)
            }
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getSeriesList(): ApiResult<List<CricApiSeries>> =
        when (val r = safeCall { CricApiClient.service.getSeriesList() }) {
            is ApiResult.Success -> ApiResult.Success(r.data.data ?: emptyList())
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }

    suspend fun getIplFixtures(seriesId: String): ApiResult<List<CricMatch>> =
        when (val r = safeCall { CricApiClient.service.getIplFixtures(ApiConfig.API_KEY, seriesId) }) {
            is ApiResult.Success -> ApiResult.Success(
                r.data.data?.matches?.mapNotNull { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> r
            is ApiResult.Loading -> ApiResult.Loading
        }
}

// ─────────────────────────────────────────────
// LEGACY COMPAT HELPERS
// ─────────────────────────────────────────────

fun mapCricbuzzRole(role: String?): String = CricApiMapper.mapRole(role)
fun getDefaultCredits(role: String): Float = CricApiMapper.defaultCredits(role)
fun getPlayerImageUrl(playerId: String): String =
    "https://h.cricapi.com/img/icon512.png"

fun convertToPlayer(cricPlayer: CricApiPlayer, teamName: String, teamShortName: String): Player =
    CricApiMapper.mapPlayer(cricPlayer, teamShortName)
