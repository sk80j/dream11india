package com.example.dream11india

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

const val RAPID_API_KEY = "18ad7c6629msh2916b44e24755c8p193182jsn4df4f01d4867"
const val RAPID_API_HOST = "cricbuzz-cricket.p.rapidapi.com"
const val CRICBUZZ_BASE_URL = "https://cricbuzz-cricket.p.rapidapi.com/"
const val CRIC_API_KEY = "26fb0ff7-aad3-4882-a9e5-2d4da812e390"
const val BASE_URL = "https://api.cricapi.com/v1/"

data class CricbuzzMatchListResponse(val typeMatches: List<TypeMatch>? = null)
data class TypeMatch(val matchType: String = "", val seriesMatches: List<SeriesMatchWrapper>? = null)
data class SeriesMatchWrapper(val seriesAdWrapper: SeriesAdWrapper? = null)
data class SeriesAdWrapper(val seriesId: Int = 0, val seriesName: String = "", val matches: List<CricbuzzMatch>? = null)
data class CricbuzzMatch(val matchInfo: MatchInfo? = null, val matchScore: MatchScore? = null)
data class MatchInfo(val matchId: Int = 0, val seriesId: Int = 0, val seriesName: String = "", val matchDesc: String = "", val matchFormat: String = "", val startDate: String = "", val endDate: String = "", val state: String = "", val status: String = "", val team1: CricbuzzTeam? = null, val team2: CricbuzzTeam? = null, val venueInfo: VenueInfo? = null)
data class CricbuzzTeam(val teamId: Int = 0, val teamName: String = "", val teamSName: String = "", val imageId: Int = 0)
data class VenueInfo(val ground: String = "", val city: String = "", val country: String = "")
data class MatchScore(val team1Score: TeamScore? = null, val team2Score: TeamScore? = null)
data class TeamScore(val inngs1: Innings? = null, val inngs2: Innings? = null)
data class Innings(val inningsId: Int = 0, val runs: Int = 0, val wickets: Int = 0, val overs: Double = 0.0)
data class MatchListResponse(val data: List<CricMatch>? = null, val status: String = "")
data class MatchInfoResponse(val data: MatchDetail? = null, val status: String = "")
data class MatchDetail(val id: String = "", val name: String = "", val status: String = "", val venue: String = "", val date: String = "", val dateTimeGMT: String = "", val teams: List<String> = emptyList(), val teamInfo: List<TeamInfo>? = null, val score: List<Score>? = null, val tossWinner: String = "", val tossChoice: String = "", val matchWinner: String = "", val matchStarted: Boolean = false, val matchEnded: Boolean = false)
data class CricMatch(val id: String = "", val name: String = "", val status: String = "", val venue: String = "", val date: String = "", val dateTimeGMT: String = "", val teams: List<String> = emptyList(), val teamInfo: List<TeamInfo>? = null, val score: List<Score>? = null, val matchStarted: Boolean = false, val matchEnded: Boolean = false)
data class TeamInfo(val name: String = "", val shortname: String = "", val img: String = "")
data class Score(val r: Int = 0, val w: Int = 0, val o: Double = 0.0, val inning: String = "")

interface CricbuzzApiService {
    @GET("matches/v1/live")
    suspend fun getLiveMatches(@Header("x-rapidapi-key") apiKey: String = RAPID_API_KEY, @Header("x-rapidapi-host") host: String = RAPID_API_HOST): CricbuzzMatchListResponse
    @GET("matches/v1/upcoming")
    suspend fun getUpcomingMatches(@Header("x-rapidapi-key") apiKey: String = RAPID_API_KEY, @Header("x-rapidapi-host") host: String = RAPID_API_HOST): CricbuzzMatchListResponse
    @GET("matches/v1/recent")
    suspend fun getRecentMatches(@Header("x-rapidapi-key") apiKey: String = RAPID_API_KEY, @Header("x-rapidapi-host") host: String = RAPID_API_HOST): CricbuzzMatchListResponse
}

interface CricApiService {
    @GET("matches")
    suspend fun getMatches(@Query("apikey") apiKey: String = CRIC_API_KEY, @Query("offset") offset: Int = 0): MatchListResponse
    @GET("currentMatches")
    suspend fun getLiveMatches(@Query("apikey") apiKey: String = CRIC_API_KEY, @Query("offset") offset: Int = 0): MatchListResponse
    @GET("match_info")
    suspend fun getMatchInfo(@Query("apikey") apiKey: String = CRIC_API_KEY, @Query("id") matchId: String): MatchInfoResponse
}

object CricApi {
    val service: CricApiService by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(CricApiService::class.java)
    }
}

object CricbuzzApi {
    val service: CricbuzzApiService by lazy {
        Retrofit.Builder().baseUrl(CRICBUZZ_BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(CricbuzzApiService::class.java)
    }
}

