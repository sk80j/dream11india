package com.example.dream11india

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentTab: String = "home",
    userData: UserData = UserData(),
    onTabChange: (String) -> Unit = {},
    onMatchClick: (MatchData) -> Unit = {},
    onWalletClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var matches by remember { mutableStateOf<List<CricMatch>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedLeague by remember { mutableStateOf("All") }

    val sampleMatches = listOf(
        CricMatch(id="1", name="RR vs MI",
            status="Rajasthan Royals need 45 runs in 30 balls",
            venue="Sawai Mansingh Stadium, Jaipur", date="2026-04-13",
            teams=listOf("Rajasthan Royals","Mumbai Indians"),
            score=listOf(Score(186,5,20.0,"MI Innings"),Score(142,3,14.0,"RR Innings")),
            matchStarted=true, matchEnded=false),
        CricMatch(id="2", name="CSK vs RCB", status="Today, 7:30 PM",
            venue="MA Chidambaram Stadium, Chennai", date="2026-04-13",
            teams=listOf("Chennai Super Kings","Royal Challengers"),
            matchStarted=false, matchEnded=false),
        CricMatch(id="3", name="KKR vs DC", status="Tomorrow, 3:30 PM",
            venue="Eden Gardens, Kolkata", date="2026-04-14",
            teams=listOf("Kolkata Knight Riders","Delhi Capitals"),
            matchStarted=false, matchEnded=false),
        CricMatch(id="4", name="PBKS vs SRH", status="Tomorrow, 7:30 PM",
            venue="Punjab Cricket Association Stadium, Mohali", date="2026-04-14",
            teams=listOf("Punjab Kings","Sunrisers Hyderabad"),
            matchStarted=false, matchEnded=false),
        CricMatch(id="5", name="GT vs LSG", status="15 Apr, 7:30 PM",
            venue="Narendra Modi Stadium, Ahmedabad", date="2026-04-15",
            teams=listOf("Gujarat Titans","Lucknow Super Giants"),
            matchStarted=false, matchEnded=false),
        CricMatch(id="6", name="MI vs CSK", status="16 Apr, 7:30 PM",
            venue="Wankhede Stadium, Mumbai", date="2026-04-16",
            teams=listOf("Mumbai Indians","Chennai Super Kings"),
            matchStarted=false, matchEnded=false)
    )

    fun parseCricbuzzMatches(response: CricbuzzMatchListResponse): List<CricMatch> {
        val allMatches = mutableListOf<CricMatch>()
        response.typeMatches?.forEach { typeMatch ->
            typeMatch.seriesMatches?.forEach { sw ->
                val wrapper = sw.seriesAdWrapper ?: return@forEach
                wrapper.matches?.forEach { cbMatch ->
                    val info = cbMatch.matchInfo ?: return@forEach
                    allMatches.add(CricMatch(
                        id = info.matchId.toString(),
                        name = "${info.team1?.teamSName ?: "T1"} vs ${info.team2?.teamSName ?: "T2"}",
                        status = info.status,
                        venue = "${info.venueInfo?.ground ?: ""}, ${info.venueInfo?.city ?: ""}",
                        date = info.startDate,
                        teams = listOf(info.team1?.teamName ?: "Team 1", info.team2?.teamName ?: "Team 2"),
                        teamInfo = listOf(
                            TeamInfo(name=info.team1?.teamName?:"", shortname=info.team1?.teamSName?:"",
                                img="https://cricbuzz-cricket.p.rapidapi.com/img/v1/i1/c${info.team1?.imageId}/i.jpg"),
                            TeamInfo(name=info.team2?.teamName?:"", shortname=info.team2?.teamSName?:"",
                                img="https://cricbuzz-cricket.p.rapidapi.com/img/v1/i1/c${info.team2?.imageId}/i.jpg")
                        ),
                        score = cbMatch.matchScore?.let { ms ->
                            listOfNotNull(
                                ms.team1Score?.inngs1?.let { Score(it.runs,it.wickets,it.overs,"1st") },
                                ms.team2Score?.inngs1?.let { Score(it.runs,it.wickets,it.overs,"2nd") }
                            )
                        },
                        matchStarted = info.state == "In Progress",
                        matchEnded = info.state == "Complete"
                    ))
                }
            }
        }
        return allMatches
    }

    fun loadMatches() {
        scope.launch {
            try {
                val allMatches = mutableListOf<CricMatch>()
                try {
                    val liveResp = CricbuzzApi.service.getLiveMatches()
                    allMatches.addAll(parseCricbuzzMatches(liveResp))
                    val upcomingResp = CricbuzzApi.service.getUpcomingMatches()
                    allMatches.addAll(parseCricbuzzMatches(upcomingResp))
                } catch (e: Exception) { }
                if (allMatches.isEmpty()) allMatches.addAll(sampleMatches)
                matches = allMatches
            } catch (e: Exception) {
                matches = sampleMatches.toMutableList()
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { loadMatches() }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(D11LightGray)
                        .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center) {
                        Text((userData.name.firstOrNull() ?: "P").toString().uppercase(),
                            color = D11White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Image(painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo", modifier = Modifier.size(32.dp))
                    Text("DREAM11", color = D11White, fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(D11LightGray),
                        contentAlignment = Alignment.Center) {
                        Text("B", color = D11White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(D11LightGray)
                        .clickable { onWalletClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Rs.${userData.balance}", color = D11White,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("+", color = D11Green, fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // CRICKET TAB
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Cricket", color = D11White, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(50.dp).height(2.dp).background(D11Red))
                }
            }

            HorizontalDivider(color = D11Border, thickness = 0.5.dp)

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = D11Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Matches load ho rahe hain...",
                                color = D11Gray, fontSize = 14.sp)
                        }
                    }
                }
                else -> {
                    val filteredMatches = when (selectedLeague) {
                        "IPL" -> matches.filter { m ->
                            m.teams.any { t -> listOf("Mumbai","Chennai","Royal","Kolkata",
                                "Delhi","Punjab","Rajasthan","Sunrisers","Gujarat","Lucknow")
                                .any { t.contains(it, true) } }
                        }
                        "WPL" -> matches.filter { it.name.contains("WPL", true) }
                        "T20I" -> matches.filter { it.status.contains("T20I", true) }
                        "ODI" -> matches.filter { it.status.contains("ODI", true) }
                        else -> matches
                    }

                    LazyColumn(modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)) {

                        // LEAGUE FILTER
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(listOf("All","IPL","WPL","T20I","ODI")) { league ->
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                            .border(1.dp,
                                                if (selectedLeague == league) D11Red else D11Border,
                                                RoundedCornerShape(20.dp))
                                            .background(
                                                if (selectedLeague == league) D11Red
                                                else Color(0xFF2A2A2A))
                                            .clickable { selectedLeague = league }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(league, color = D11White,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // TABS
                        item {
                            Row(modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFF1A1A1A))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                listOf("Recommended","Starting Soon","Popular").forEach { tab ->
                                    Text(tab,
                                        color = if (tab == "Recommended") D11Red else D11Gray,
                                        fontSize = 13.sp,
                                        fontWeight = if (tab == "Recommended") FontWeight.Bold
                                        else FontWeight.Normal)
                                }
                            }
                        }

                        if (filteredMatches.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp),
                                    contentAlignment = Alignment.Center) {
                                    Text("No matches available",
                                        color = D11Gray, fontSize = 16.sp)
                                }
                            }
                        } else {
                            items(filteredMatches) { match ->
                                Dream11MatchCard(match = match,
                                    onClick = { onMatchClick(matchDataFromCric(match)) })
                            }
                        }

                        if (userData.isAdmin) {
                            item {
                                Button(onClick = onAdminClick,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF333333)),
                                    shape = RoundedCornerShape(8.dp)) {
                                    Text("Admin Panel", color = D11White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM NAV
        Row(modifier = Modifier.fillMaxWidth().background(D11White)
            .padding(vertical = 8.dp).align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                "home" to "Home",
                "matches" to "My Matches",
                "rewards" to "DreamCoins",
                "refer" to "Games"
            ).forEachIndexed { index, (tab, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabChange(tab) }
                        .padding(horizontal = 8.dp)) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(if (currentTab == tab) D11Red else Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center) {
                        when(index) {
                            0 -> Image(painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = null, modifier = Modifier.size(22.dp))
                            1 -> Text("M", color = if (currentTab == tab) D11White else D11Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            2 -> Text("D", color = if (currentTab == tab) D11White else D11Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            3 -> Text("G", color = if (currentTab == tab) D11White else D11Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(label,
                        color = if (currentTab == tab) D11Red else D11Gray,
                        fontSize = 10.sp,
                        fontWeight = if (currentTab == tab) FontWeight.Bold
                        else FontWeight.Normal)
                }
            }
        }
    }
}

fun matchDataFromCric(match: CricMatch): MatchData {
    val team1 = match.teams.getOrElse(0) { "Team 1" }
    val team2 = match.teams.getOrElse(1) { "Team 2" }
    return MatchData(
        id = match.id,
        team1 = team1.take(5).uppercase(),
        team2 = team2.take(5).uppercase(),
        team1Full = team1, team2Full = team2,
        team1Logo = match.teamInfo?.getOrNull(0)?.img ?: "",
        team2Logo = match.teamInfo?.getOrNull(1)?.img ?: "",
        type = "T20", league = "IPL 2026",
        matchTime = match.date, hoursLeft = 2, minutesLeft = 30,
        prize = "Rs.50 Crores", spots = "50,000",
        fillPercent = 75, badge = "MEGA",
        team1Color = Color(0xFF003366), team2Color = Color(0xFF006600)
    )
}

@Composable
fun MyMatchCard(match: CricMatch, onClick: () -> Unit) {
    val teams = match.name.split(" vs ", ignoreCase = true)
    val team1 = teams.getOrElse(0) { match.teams.getOrElse(0) { "T1" } }.trim()
    val team2 = teams.getOrElse(1) { match.teams.getOrElse(1) { "T2" } }.trim()
    val isLive = match.matchStarted && !match.matchEnded

    Card(modifier = Modifier.width(200.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = D11CardBg),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("IPL 2026", color = D11Gray, fontSize = 10.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (isLive) Color(0xFF8B0000) else Color(0xFF003300))
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(if (isLive) "LIVE" else "Upcoming",
                        color = if (isLive) Color(0xFFFF4444) else D11Green,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(Color(0xFF003366)),
                    contentAlignment = Alignment.Center) {
                    Text(team1.take(2).uppercase(), color = D11White,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(team1.take(10), color = D11White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(Color(0xFF006600)),
                    contentAlignment = Alignment.Center) {
                    Text(team2.take(2).uppercase(), color = D11White,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(team2.take(10), color = D11White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = D11Border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(match.status.take(30), color = D11Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun Dream11MatchCard(match: CricMatch, onClick: () -> Unit) {
    val teams = match.name.split(" vs ", ignoreCase = true)
    val team1 = teams.getOrElse(0) { match.teams.getOrElse(0) { "Team 1" } }.trim()
    val team2 = teams.getOrElse(1) { match.teams.getOrElse(1) { "Team 2" } }.trim()
    val isLive = match.matchStarted && !match.matchEnded
    val isCompleted = match.matchEnded

    Card(modifier = Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = D11White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F8F8))
                .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("T20 - IPL 2026", color = Color(0xFF333333),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                when {
                    isLive -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(D11Red))
                        Text("LIVE", color = D11Red, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    isCompleted -> Text("Completed", color = D11Gray, fontSize = 11.sp)
                    else -> CountdownTimer(hoursLeft = 2, minutesLeft = 30)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Color(0xFF003366)),
                        contentAlignment = Alignment.Center) {
                        Text(team1.take(3).uppercase(), color = D11White,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(team1.take(3).uppercase(), color = Color(0xFF111111),
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(team1.take(15), color = Color(0xFF666666), fontSize = 11.sp)
                        if (isLive) {
                            val score1 = match.score?.getOrNull(0)
                            if (score1 != null)
                                Text("${score1.r}/${score1.w} (${score1.o})",
                                    color = Color(0xFF111111), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("vs", color = Color(0xFF888888), fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(team2.take(3).uppercase(), color = Color(0xFF111111),
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(team2.take(15), color = Color(0xFF666666), fontSize = 11.sp)
                        if (isLive) {
                            val score2 = match.score?.getOrNull(1)
                            if (score2 != null)
                                Text("${score2.r}/${score2.w} (${score2.o})",
                                    color = Color(0xFF111111), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Color(0xFF006600)),
                        contentAlignment = Alignment.Center) {
                        Text(team2.take(3).uppercase(), color = D11White,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            if (match.status.isNotEmpty()) {
                Text(match.status,
                    color = if (isLive) D11Red else Color(0xFF666666),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape)
                        .background(D11Yellow),
                        contentAlignment = Alignment.Center) {
                        Text("M", color = D11Black, fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Rs.50 Crores +", color = Color(0xFF111111),
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (!isCompleted) {
                    Button(onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("Play", color = D11White,
                            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(onClick = onClick,
                        border = androidx.compose.foundation.BorderStroke(1.dp, D11Gray),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("View", color = D11Gray, fontSize = 13.sp)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                .background(Color(0xFFEEEEEE))) {
                Box(modifier = Modifier.fillMaxWidth(0.75f).height(3.dp)
                    .background(D11Green))
            }
            Row(modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("75% Full", color = Color(0xFF666666), fontSize = 11.sp)
                Text("50,000 Spots", color = Color(0xFF666666), fontSize = 11.sp)
            }
        }
    }
}
