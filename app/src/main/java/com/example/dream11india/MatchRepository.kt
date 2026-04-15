package com.example.dream11india

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MatchRepository {

    suspend fun fetchMatches(): List<CricMatch> = withContext(Dispatchers.IO) {
        val all = mutableListOf<CricMatch>()
        runCatching {
            val live = CricApiClient.service.getLiveMatches()
            if (live.isSuccessful) {
                live.body()?.data?.map { CricApiMapper.mapMatch(it) }?.let { all += it }
            }
        }
        runCatching {
            val upcoming = CricApiClient.service.getUpcomingMatches()
            if (upcoming.isSuccessful) {
                upcoming.body()?.data?.map { CricApiMapper.mapMatch(it) }?.let { all += it }
            }
        }
        if (all.isEmpty()) sampleMatches() else all
    }

    fun sampleMatches(): List<CricMatch> = listOf(
        CricMatch(id="1", name="RR vs MI",
            status="RR need 45 runs in 30 balls",
            venue="Sawai Mansingh Stadium, Jaipur", date="2026-04-14",
            teams=listOf("Rajasthan Royals","Mumbai Indians"),
            teamInfo=listOf(TeamInfo("Rajasthan Royals","RR",""), TeamInfo("Mumbai Indians","MI","")),
            score=listOf(Score(186,5,20.0,"MI"), Score(142,3,14.0,"RR")),
            matchStarted=true, matchEnded=false,
            prizePool="₹1 Crore", totalSpots="1,00,000", filledSpots=88, badge="MEGA", entryFee="₹49"),
        CricMatch(id="2", name="CSK vs RCB", status="Today, 7:30 PM",
            venue="MA Chidambaram Stadium, Chennai", date="2026-04-14",
            teams=listOf("Chennai Super Kings","Royal Challengers"),
            teamInfo=listOf(TeamInfo("Chennai Super Kings","CSK",""), TeamInfo("Royal Challengers","RCB","")),
            score=null, matchStarted=false, matchEnded=false,
            prizePool="₹50 Crores", totalSpots="50,000", filledSpots=62, badge="MEGA", entryFee="₹49"),
        CricMatch(id="3", name="KKR vs DC", status="Tomorrow, 3:30 PM",
            venue="Eden Gardens, Kolkata", date="2026-04-15",
            teams=listOf("Kolkata Knight Riders","Delhi Capitals"),
            teamInfo=listOf(TeamInfo("Kolkata Knight Riders","KKR",""), TeamInfo("Delhi Capitals","DC","")),
            score=null, matchStarted=false, matchEnded=false,
            prizePool="₹25 Crores", totalSpots="75,000", filledSpots=41, badge="GUARANTEED", entryFee="₹25"),
        CricMatch(id="4", name="PBKS vs SRH", status="Tomorrow, 7:30 PM",
            venue="PCA Stadium, Mohali", date="2026-04-15",
            teams=listOf("Punjab Kings","Sunrisers Hyderabad"),
            teamInfo=listOf(TeamInfo("Punjab Kings","PBKS",""), TeamInfo("Sunrisers Hyderabad","SRH","")),
            score=null, matchStarted=false, matchEnded=false,
            prizePool="₹10 Crores", totalSpots="30,000", filledSpots=55, badge="FREE", entryFee="FREE"),
        CricMatch(id="5", name="GT vs LSG", status="15 Apr, 7:30 PM",
            venue="Narendra Modi Stadium, Ahmedabad", date="2026-04-15",
            teams=listOf("Gujarat Titans","Lucknow Super Giants"),
            teamInfo=listOf(TeamInfo("Gujarat Titans","GT",""), TeamInfo("Lucknow Super Giants","LSG","")),
            score=null, matchStarted=false, matchEnded=false,
            prizePool="₹75 Crores", totalSpots="2,00,000", filledSpots=30, badge="MEGA", entryFee="₹99")
    )
}
