package com.example.dream11india

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════════
// SECURITY — API KEY (move to BuildConfig in production)
// ═══════════════════════════════════════════════════════════
object ApiConfig {
    // TODO: Move to BuildConfig for release:
    //   buildConfigField("String", "CRIC_API_KEY", '"your_key"')
    //   then use: BuildConfig.CRIC_API_KEY
    const val API_KEY      = "26fb0ff7-aad3-4882-a9e5-2d4da812e390"
    const val BASE_URL     = "https://api.cricapi.com/v1/"

    // Timeout suggestions (ms)
    const val CONNECT_TIMEOUT = 15L
    const val READ_TIMEOUT    = 30L
    const val WRITE_TIMEOUT   = 15L
}

// ═══════════════════════════════════════════════════════════
// RETROFIT SINGLETON — reused, not recreated
// ═══════════════════════════════════════════════════════════
object CricApiClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Use BODY in debug only — switch to NONE in release
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val gson = GsonBuilder()
        .setLenient()           // tolerates malformed JSON from API
        .serializeNulls()
        .create()

    val service: CricbuzzApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CricbuzzApiService::class.java)
    }
}

// ═══════════════════════════════════════════════════════════
// API SERVICE INTERFACE
// ═══════════════════════════════════════════════════════════
interface CricbuzzApiService {

    // ── Matches ─────────────────────────────────────────────

    /** Live matches currently in progress */
    @GET("currentMatches")
    suspend fun getLiveMatches(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("offset") offset: Int    = 0
    ): Response<CricApiMatchListResponse>

    /** Upcoming scheduled matches */
    @GET("matches")
    suspend fun getUpcomingMatches(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("offset") offset: Int    = 0
    ): Response<CricApiMatchListResponse>

    /** Recently completed matches */
    @GET("matches")
    suspend fun getRecentMatches(
        @Query("apikey") apiKey: String  = ApiConfig.API_KEY,
        @Query("offset") offset: Int     = 0,
        @Query("status")  status: String = "completed"
    ): Response<CricApiMatchListResponse>

    /** Detailed match info by ID */
    @GET("match_info")
    suspend fun getMatchInfo(
        @Query("apikey") apiKey: String,
        @Query("id")     matchId: String
    ): Response<CricApiMatchInfoResponse>

    /** Full scorecard for a match */
    @GET("match_scorecard")
    suspend fun getScorecard(
        @Query("apikey") apiKey: String  = ApiConfig.API_KEY,
        @Query("id")     matchId: String
    ): Response<CricApiScorecardResponse>

    /** Ball-by-ball commentary */
    @GET("match_bbb")
    suspend fun getCommentary(
        @Query("apikey") apiKey: String  = ApiConfig.API_KEY,
        @Query("id")     matchId: String
    ): Response<CricApiCommentaryResponse>

    /** Squad / players for a match */
    @GET("match_squad")
    suspend fun getSquad(
        @Query("apikey") apiKey: String  = ApiConfig.API_KEY,
        @Query("id")     matchId: String
    ): Response<CricApiSquadResponse>

    /** Series list */
    @GET("series")
    suspend fun getSeriesList(
        @Query("apikey") apiKey: String = ApiConfig.API_KEY,
        @Query("offset") offset: Int    = 0
    ): Response<CricApiSeriesListResponse>

    /** IPL specific fixtures */
    @GET("series_info")
    suspend fun getIplFixtures(
        @Query("apikey") apiKey: String,
        @Query("id")     seriesId: String
    ): Response<CricApiSeriesInfoResponse>

    /** Player info */
    @GET("players_info")
    suspend fun getPlayerInfo(
        @Query("apikey") apiKey: String,
        @Query("id")     playerId: String
    ): Response<CricApiPlayerInfoResponse>
}

// ═══════════════════════════════════════════════════════════
// DTO MODELS  (raw API response shapes)
// ═══════════════════════════════════════════════════════════

// ── Generic wrapper ─────────────────────────────────────────
data class CricApiMatchListResponse(
    @SerializedName("status")    val status:  String?              = null,
    @SerializedName("data")      val data:    List<CricApiMatch>?  = null,
    @SerializedName("info")      val info:    CricApiPageInfo?     = null
)

data class CricApiPageInfo(
    @SerializedName("hitsTotal")   val hitsTotal:  Int? = null,
    @SerializedName("hitsPerPage") val hitsPerPage: Int? = null
)

// ── Match DTO ───────────────────────────────────────────────
data class CricApiMatch(
    @SerializedName("id")            val id:           String?              = null,
    @SerializedName("name")          val name:         String?              = null,
    @SerializedName("matchType")     val matchType:    String?              = null,
    @SerializedName("status")        val status:       String?              = null,
    @SerializedName("venue")         val venue:        String?              = null,
    @SerializedName("date")          val date:         String?              = null,
    @SerializedName("dateTimeGMT")   val dateTimeGMT:  String?              = null,
    @SerializedName("teams")         val teams:        List<String>?        = null,
    @SerializedName("teamInfo")      val teamInfo:     List<CricApiTeamInfo>? = null,
    @SerializedName("score")         val score:        List<CricApiScore>?  = null,
    @SerializedName("series_id")     val seriesId:     String?              = null,
    @SerializedName("fantasyEnabled") val fantasyEnabled: Boolean?          = null,
    @SerializedName("bbbEnabled")    val bbbEnabled:   Boolean?             = null,
    @SerializedName("hasSquad")      val hasSquad:     Boolean?             = null,
    @SerializedName("matchStarted")  val matchStarted: Boolean?             = null,
    @SerializedName("matchEnded")    val matchEnded:   Boolean?             = null
)

data class CricApiTeamInfo(
    @SerializedName("name")      val name:      String? = null,
    @SerializedName("shortname") val shortname: String? = null,
    @SerializedName("img")       val img:       String? = null
)

data class CricApiScore(
    @SerializedName("r")      val r:      Int?    = null,
    @SerializedName("w")      val w:      Int?    = null,
    @SerializedName("o")      val o:      Double? = null,
    @SerializedName("inning") val inning: String? = null
)

// ── Match Info DTO ──────────────────────────────────────────
data class CricApiMatchInfoResponse(
    @SerializedName("status") val status: String?          = null,
    @SerializedName("data")   val data:   CricApiMatchDetail? = null
)

data class CricApiMatchDetail(
    @SerializedName("id")          val id:          String?              = null,
    @SerializedName("name")        val name:        String?              = null,
    @SerializedName("matchType")   val matchType:   String?              = null,
    @SerializedName("status")      val status:      String?              = null,
    @SerializedName("venue")       val venue:       String?              = null,
    @SerializedName("date")        val date:        String?              = null,
    @SerializedName("teams")       val teams:       List<String>?        = null,
    @SerializedName("teamInfo")    val teamInfo:    List<CricApiTeamInfo>? = null,
    @SerializedName("score")       val score:       List<CricApiScore>?  = null,
    @SerializedName("tossWinner")  val tossWinner:  String?              = null,
    @SerializedName("tossChoice")  val tossChoice:  String?              = null,
    @SerializedName("matchWinner") val matchWinner: String?              = null,
    @SerializedName("matchStarted") val matchStarted: Boolean?           = null,
    @SerializedName("matchEnded")  val matchEnded:  Boolean?             = null
)

// ── Scorecard DTO ───────────────────────────────────────────
data class CricApiScorecardResponse(
    @SerializedName("status") val status: String?              = null,
    @SerializedName("data")   val data:   CricApiScorecardData? = null
)

data class CricApiScorecardData(
    @SerializedName("id")       val id:       String?                    = null,
    @SerializedName("name")     val name:     String?                    = null,
    @SerializedName("scorecard") val scorecard: List<CricApiInningsCard>? = null
)

data class CricApiInningsCard(
    @SerializedName("inning")   val inning:   String?                   = null,
    @SerializedName("batting")  val batting:  List<CricApiBatting>?     = null,
    @SerializedName("bowling")  val bowling:  List<CricApiBowling>?     = null,
    @SerializedName("extras")   val extras:   CricApiExtras?            = null,
    @SerializedName("total")    val total:    String?                   = null
)

data class CricApiBatting(
    @SerializedName("batsman")     val batsman:     String? = null,
    @SerializedName("dismissal")   val dismissal:   String? = null,
    @SerializedName("r")           val r:           Int?    = null,
    @SerializedName("b")           val b:           Int?    = null,
    @SerializedName("4s")          val fours:       Int?    = null,
    @SerializedName("6s")          val sixes:       Int?    = null,
    @SerializedName("sr")          val strikeRate:  String? = null
)

data class CricApiBowling(
    @SerializedName("bowler")      val bowler:      String? = null,
    @SerializedName("o")           val o:           String? = null,
    @SerializedName("m")           val m:           Int?    = null,
    @SerializedName("r")           val r:           Int?    = null,
    @SerializedName("w")           val w:           Int?    = null,
    @SerializedName("eco")         val economy:     String? = null
)

data class CricApiExtras(
    @SerializedName("total") val total: Int? = null,
    @SerializedName("b")     val b:     Int? = null,
    @SerializedName("lb")    val lb:    Int? = null,
    @SerializedName("wide")  val wide:  Int? = null,
    @SerializedName("nb")    val nb:    Int? = null
)

// ── Commentary DTO ──────────────────────────────────────────
data class CricApiCommentaryResponse(
    @SerializedName("status") val status: String?              = null,
    @SerializedName("data")   val data:   CricApiCommentaryData? = null
)

data class CricApiCommentaryData(
    @SerializedName("id")          val id:          String?                  = null,
    @SerializedName("commentary")  val commentary:  List<CricApiCommentary>? = null
)

data class CricApiCommentary(
    @SerializedName("ball")        val ball:        String? = null,
    @SerializedName("comment")     val comment:     String? = null,
    @SerializedName("timestamp")   val timestamp:   Long?   = null
)

// ── Squad DTO ───────────────────────────────────────────────
data class CricApiSquadResponse(
    @SerializedName("status") val status: String?           = null,
    @SerializedName("data")   val data:   CricApiSquadData? = null
)

data class CricApiSquadData(
    @SerializedName("id")     val id:     String?                  = null,
    @SerializedName("name")   val name:   String?                  = null,
    @SerializedName("squad")  val squad:  List<CricApiSquadTeam>?  = null
)

data class CricApiSquadTeam(
    @SerializedName("name")    val name:    String?                = null,
    @SerializedName("players") val players: List<CricApiPlayer>?  = null
)

data class CricApiPlayer(
    @SerializedName("id")   val id:   String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String? = null
)

// ── Series DTO ──────────────────────────────────────────────
data class CricApiSeriesListResponse(
    @SerializedName("status") val status: String?              = null,
    @SerializedName("data")   val data:   List<CricApiSeries>? = null
)

data class CricApiSeries(
    @SerializedName("id")        val id:        String? = null,
    @SerializedName("name")      val name:      String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate")   val endDate:   String? = null,
    @SerializedName("odi")       val odi:       Int?    = null,
    @SerializedName("t20")       val t20:       Int?    = null,
    @SerializedName("test")      val test:      Int?    = null,
    @SerializedName("squads")    val squads:    Int?    = null,
    @SerializedName("matches")   val matches:   Int?    = null
)

data class CricApiSeriesInfoResponse(
    @SerializedName("status") val status: String?              = null,
    @SerializedName("data")   val data:   CricApiSeriesDetail? = null
)

data class CricApiSeriesDetail(
    @SerializedName("id")      val id:      String?              = null,
    @SerializedName("name")    val name:    String?              = null,
    @SerializedName("matches") val matches: List<CricApiMatch>?  = null
)

// ── Player Info DTO ─────────────────────────────────────────
data class CricApiPlayerInfoResponse(
    @SerializedName("status") val status: String?             = null,
    @SerializedName("data")   val data:   CricApiPlayerDetail? = null
)

data class CricApiPlayerDetail(
    @SerializedName("id")            val id:            String? = null,
    @SerializedName("name")          val name:          String? = null,
    @SerializedName("dateOfBirth")   val dateOfBirth:   String? = null,
    @SerializedName("role")          val role:          String? = null,
    @SerializedName("battingStyle")  val battingStyle:  String? = null,
    @SerializedName("bowlingStyle")  val bowlingStyle:  String? = null,
    @SerializedName("country")       val country:       String? = null,
    @SerializedName("playerImg")     val playerImg:     String? = null
)

// ═══════════════════════════════════════════════════════════
// LOCAL DOMAIN MODELS (app-wide, used by UI)
// ═══════════════════════════════════════════════════════════
data class CricMatch(
    val id:           String  = "",
    val name:         String  = "",
    val status:       String  = "",
    val venue:        String  = "",
    val date:         String  = "",
    val teams:        List<String>    = emptyList(),
    val teamInfo:     List<TeamInfo>? = null,
    val score:        List<Score>?    = null,
    val matchStarted: Boolean = false,
    val matchEnded:   Boolean = false,
    // Dynamic contest fields
    val prizePool:    String  = "₹50 Crores",
    val totalSpots:   String  = "50,000",
    val filledSpots:  Int     = 75,
    val entryFee:     String  = "₹49",
    val badge:        String  = "MEGA",
    val t1LogoUrl:    String  = "",
    val t2LogoUrl:    String  = "",
    val matchType:    String  = "T20",
    val seriesId:     String  = "",
    val fantasyEnabled: Boolean = true
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

// ═══════════════════════════════════════════════════════════
// SEALED RESULT WRAPPER
// ═══════════════════════════════════════════════════════════
sealed class ApiResult<out T> {
    data class Success<T>(val data: T)  : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

// ═══════════════════════════════════════════════════════════
// MAPPER FUNCTIONS  (DTO → Local model)
// ═══════════════════════════════════════════════════════════
object CricApiMapper {

    fun mapMatch(dto: CricApiMatch): CricMatch = CricMatch(
        id           = dto.id           ?: "",
        name         = dto.name         ?: "",
        status       = dto.status       ?: "",
        venue        = dto.venue        ?: "",
        date         = dto.dateTimeGMT  ?: dto.date ?: "",
        teams        = dto.teams        ?: emptyList(),
        teamInfo     = dto.teamInfo?.map { mapTeamInfo(it) },
        score        = dto.score?.map   { mapScore(it) },
        matchStarted = dto.matchStarted ?: false,
        matchEnded   = dto.matchEnded   ?: false,
        t1LogoUrl    = dto.teamInfo?.getOrNull(0)?.img ?: "",
        t2LogoUrl    = dto.teamInfo?.getOrNull(1)?.img ?: "",
        matchType    = dto.matchType    ?: "T20",
        seriesId     = dto.seriesId     ?: "",
        fantasyEnabled = dto.fantasyEnabled ?: true,
        // Default contest values — override from Firestore if needed
        prizePool    = "₹50 Crores",
        totalSpots   = "50,000",
        filledSpots  = 75,
        entryFee     = "₹49",
        badge        = "MEGA"
    )

    fun mapTeamInfo(dto: CricApiTeamInfo): TeamInfo = TeamInfo(
        name      = dto.name      ?: "",
        shortname = dto.shortname ?: "",
        img       = dto.img       ?: ""
    )

    fun mapScore(dto: CricApiScore): Score = Score(
        r      = dto.r      ?: 0,
        w      = dto.w      ?: 0,
        o      = dto.o      ?: 0.0,
        inning = dto.inning ?: ""
    )

    fun mapPlayer(dto: CricApiPlayer, teamShort: String): Player {
        val role = mapRole(dto.role)
        return Player(
            id               = dto.id        ?: "",
            name             = dto.name      ?: "Player",
            shortName        = dto.name?.split(" ")?.lastOrNull() ?: dto.name ?: "Player",
            team             = teamShort,
            role             = role,
            credits          = defaultCredits(role),
            selectionPercent = (30..85).random(),
            points           = 0f,
            isSelected       = false
        )
    }

    private fun mapRole(role: String?): String = when {
        role == null -> "BAT"
        role.contains("Keeper",     ignoreCase = true) -> "WK"
        role.contains("WK",         ignoreCase = true) -> "WK"
        role.contains("Allrounder", ignoreCase = true) -> "AR"
        role.contains("All-rounder",ignoreCase = true) -> "AR"
        role.contains("Bowler",     ignoreCase = true) -> "BOWL"
        role.contains("Batting",    ignoreCase = true) -> "BAT"
        else -> "BAT"
    }

    private fun defaultCredits(role: String): Float = when (role) {
        "WK"   -> 9.0f
        "BAT"  -> 9.0f
        "AR"   -> 9.5f
        "BOWL" -> 9.0f
        else   -> 8.5f
    }
}

// ═══════════════════════════════════════════════════════════
// REPOSITORY-READY FUNCTIONS
// ═══════════════════════════════════════════════════════════
object CricApiRepository {

    /** Safe API call wrapper */
    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) ApiResult.Success(body)
                    else ApiResult.Error("Empty response body", response.code())
                } else {
                    ApiResult.Error("API error: ${response.message()}", response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown network error")
            }
        }

    /** Live matches → mapped to CricMatch list */
    suspend fun getLiveMatches(): ApiResult<List<CricMatch>> {
        return when (val result = safeCall { CricApiClient.service.getLiveMatches() }) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.data?.map { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** Upcoming matches → mapped */
    suspend fun getUpcomingMatches(): ApiResult<List<CricMatch>> {
        return when (val result = safeCall { CricApiClient.service.getUpcomingMatches() }) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.data?.map { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** Recent/completed matches */
    suspend fun getRecentMatches(): ApiResult<List<CricMatch>> {
        return when (val result = safeCall { CricApiClient.service.getRecentMatches() }) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.data?.map { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** Match detail */
    suspend fun getMatchInfo(matchId: String): ApiResult<CricMatch> {
        return when (val result = safeCall {
            CricApiClient.service.getMatchInfo(ApiConfig.API_KEY, matchId)
        }) {
            is ApiResult.Success -> {
                val d = result.data.data
                if (d != null) ApiResult.Success(
                    CricMatch(
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
                        t2LogoUrl    = d.teamInfo?.getOrNull(1)?.img ?: ""
                    )
                )
                else ApiResult.Error("Match not found")
            }
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** Scorecard */
    suspend fun getScorecard(matchId: String): ApiResult<CricApiScorecardData> {
        return when (val result = safeCall {
            CricApiClient.service.getScorecard(matchId = matchId)
        }) {
            is ApiResult.Success -> {
                val d = result.data.data
                if (d != null) ApiResult.Success(d)
                else ApiResult.Error("Scorecard not available")
            }
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** Ball by ball commentary */
    suspend fun getCommentary(matchId: String): ApiResult<List<CricApiCommentary>> {
        return when (val result = safeCall {
            CricApiClient.service.getCommentary(matchId = matchId)
        }) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.data?.commentary ?: emptyList()
            )
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** Squad → mapped to Player list */
    suspend fun getSquad(matchId: String): ApiResult<List<Player>> {
        return when (val result = safeCall {
            CricApiClient.service.getSquad(matchId = matchId)
        }) {
            is ApiResult.Success -> {
                val players = mutableListOf<Player>()
                result.data.data?.squad?.forEach { squadTeam ->
                    val shortName = squadTeam.name?.take(3)?.uppercase() ?: "UNK"
                    squadTeam.players?.forEach { p ->
                        players.add(CricApiMapper.mapPlayer(p, shortName))
                    }
                }
                ApiResult.Success(players)
            }
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** Series list */
    suspend fun getSeriesList(): ApiResult<List<CricApiSeries>> {
        return when (val result = safeCall { CricApiClient.service.getSeriesList() }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data ?: emptyList())
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /** IPL fixtures by series ID */
    suspend fun getIplFixtures(seriesId: String): ApiResult<List<CricMatch>> {
        return when (val result = safeCall {
            CricApiClient.service.getIplFixtures(ApiConfig.API_KEY, seriesId)
        }) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.data?.matches?.map { CricApiMapper.mapMatch(it) } ?: emptyList()
            )
            is ApiResult.Error   -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HELPER — Player image URL from CricAPI
// ═══════════════════════════════════════════════════════════
fun getPlayerImageUrl(playerId: String): String =
    "https://h.cricapi.com/img/icon512.png" // placeholder; use player.img from API if available

// Legacy compat kept for existing screens
fun mapCricbuzzRole(role: String?): String = when {
    role == null -> "BAT"
    role.contains("Keeper",      ignoreCase = true) -> "WK"
    role.contains("Allrounder",  ignoreCase = true) -> "AR"
    role.contains("All-rounder", ignoreCase = true) -> "AR"
    role.contains("Bowler",      ignoreCase = true) -> "BOWL"
    else -> "BAT"
}

fun getDefaultCredits(role: String): Float = when (role) {
    "WK" -> 9.0f; "BAT" -> 9.0f; "AR" -> 9.5f; "BOWL" -> 9.0f; else -> 8.5f
}

fun convertToPlayer(
    cricPlayer: CricApiPlayer,
    teamName: String,
    teamShortName: String
): Player {
    val role = mapCricbuzzRole(cricPlayer.role)
    return Player(
        id               = cricPlayer.id ?: "",
        name             = cricPlayer.name ?: "Player",
        shortName        = cricPlayer.name?.split(" ")?.lastOrNull() ?: "Player",
        team             = teamShortName,
        role             = role,
        credits          = getDefaultCredits(role),
        selectionPercent = (30..85).random(),
        points           = 0f,
        isSelected       = false
    )
}
