package com.example.dream11india

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
    var selectedLeague by remember { mutableStateOf("All") }
    var selectedFilter by remember { mutableStateOf("All") }
    var bannerIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val banners = listOf(
        Triple("MEGA CONTEST", "Win Rs.1 Crore!", D11Red),
        Triple("IPL 2026", "Play Now & Win Big!", Color(0xFF1565C0)),
        Triple("FREE CONTEST", "No Entry Fee!", D11Green)
    )

    val sampleMatches = listOf(
        CricMatch(id="1", name="RR vs MI",
            status="Rajasthan Royals need 45 runs in 30 balls",
            venue="Sawai Mansingh Stadium", date="2026-04-14",
            teams=listOf("Rajasthan Royals","Mumbai Indians"),
            teamInfo=listOf(TeamInfo("Rajasthan Royals","RR",""),TeamInfo("Mumbai Indians","MI","")),
            score=listOf(Score(186,5,20.0,"MI"),Score(142,3,14.0,"RR")),
            matchStarted=true, matchEnded=false),
        CricMatch(id="2", name="CSK vs RCB", status="Today, 7:30 PM",
            venue="MA Chidambaram Stadium", date="2026-04-14",
            teams=listOf("Chennai Super Kings","Royal Challengers"),
            teamInfo=listOf(TeamInfo("Chennai Super Kings","CSK",""),TeamInfo("Royal Challengers","RCB","")),
            score=null, matchStarted=false, matchEnded=false),
        CricMatch(id="3", name="KKR vs DC", status="Tomorrow, 3:30 PM",
            venue="Eden Gardens", date="2026-04-15",
            teams=listOf("Kolkata Knight Riders","Delhi Capitals"),
            teamInfo=listOf(TeamInfo("Kolkata Knight Riders","KKR",""),TeamInfo("Delhi Capitals","DC","")),
            score=null, matchStarted=false, matchEnded=false),
        CricMatch(id="4", name="PBKS vs SRH", status="Tomorrow, 7:30 PM",
            venue="PCA Stadium", date="2026-04-15",
            teams=listOf("Punjab Kings","Sunrisers Hyderabad"),
            teamInfo=listOf(TeamInfo("Punjab Kings","PBKS",""),TeamInfo("Sunrisers Hyderabad","SRH","")),
            score=null, matchStarted=false, matchEnded=false),
        CricMatch(id="5", name="GT vs LSG", status="15 Apr, 7:30 PM",
            venue="Narendra Modi Stadium", date="2026-04-15",
            teams=listOf("Gujarat Titans","Lucknow Super Giants"),
            teamInfo=listOf(TeamInfo("Gujarat Titans","GT",""),TeamInfo("Lucknow Super Giants","LSG","")),
            score=null, matchStarted=false, matchEnded=false)
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
                        status = info.status ?: "",
                        venue = "${info.venueInfo?.ground ?: ""}, ${info.venueInfo?.city ?: ""}",
                        date = info.startDate ?: "",
                        teams = listOf(info.team1?.teamName ?: "Team 1", info.team2?.teamName ?: "Team 2"),
                        teamInfo = listOf(
                            TeamInfo(info.team1?.teamName ?: "", info.team1?.teamSName ?: "", ""),
                            TeamInfo(info.team2?.teamName ?: "", info.team2?.teamSName ?: "", "")
                        ),
                        score = cbMatch.matchScore?.let { ms ->
                            listOfNotNull(
                                ms.team1Score?.inngs1?.let { Score(it.runs, it.wickets, it.overs, "1st") },
                                ms.team2Score?.inngs1?.let { Score(it.runs, it.wickets, it.overs, "2nd") }
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
                matches = sampleMatches
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadMatches()
        while (true) {
            delay(30000)
            loadMatches()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            bannerIndex = (bannerIndex + 1) % banners.size
        }
    }

    val filteredMatches = remember(matches, selectedLeague, selectedFilter) {
        var list = matches
        list = when(selectedLeague) {
            "IPL" -> list.filter { m -> m.teams.any { t ->
                listOf("Mumbai","Chennai","Royal","Kolkata","Delhi","Punjab",
                    "Rajasthan","Sunrisers","Gujarat","Lucknow").any { t.contains(it,true) }}}
            "WPL" -> list.filter { it.name.contains("WPL",true) }
            "T20I" -> list.filter { it.status.contains("T20I",true) }
            "ODI" -> list.filter { it.status.contains("ODI",true) }
            else -> list
        }
        list = when(selectedFilter) {
            "Live" -> list.filter { it.matchStarted && !it.matchEnded }
            "Upcoming" -> list.filter { !it.matchStarted }
            "Completed" -> list.filter { it.matchEnded }
            else -> list
        }
        list
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // TOP BAR
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).statusBarsPadding()) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(D11Red)
                                    .clickable { onProfileClick() },
                                    contentAlignment = Alignment.Center) {
                                    Text((userData.name.firstOrNull() ?: "P").toString().uppercase(),
                                        color = D11White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Image(painter = painterResource(id = R.drawable.ic_logo),
                                    contentDescription = "Logo", modifier = Modifier.size(32.dp))
                                Text("DREAM11", color = D11White, fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                                    .background(Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center) {
                                    Text("B", color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF2A2A2A)).clickable { onWalletClick() }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("Rs.${userData.balance}", color = D11White,
                                            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(D11Green),
                                            contentAlignment = Alignment.Center) {
                                            Text("+", color = D11White, fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Cricket", color = D11White, fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.width(50.dp).height(2.dp).background(D11Red))
                            }
                        }
                    }
                }

                when {
                    isLoading -> ShimmerLoadingUI()
                    else -> {
                        LazyColumn(state = listState, modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp)) {

                            // BANNER
                            item {
                                Box(modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(banners[bannerIndex].third).height(90.dp),
                                    contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(banners[bannerIndex].first, color = D11White,
                                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(banners[bannerIndex].second, color = Color(0xCCFFFFFF),
                                            fontSize = 13.sp)
                                    }
                                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        banners.indices.forEach { i ->
                                            Box(modifier = Modifier
                                                .size(if (i == bannerIndex) 20.dp else 6.dp, 6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (i == bannerIndex) D11White else Color(0x66FFFFFF)))
                                        }
                                    }
                                }
                            }

                            // LEAGUE FILTER
                            item {
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf("All","IPL","WPL","T20I","ODI")) { league ->
                                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                            .background(if (selectedLeague == league) D11Red else D11White)
                                            .border(1.dp,
                                                if (selectedLeague == league) D11Red else Color(0xFFDDDDDD),
                                                RoundedCornerShape(20.dp))
                                            .clickable { selectedLeague = league }
                                            .padding(horizontal = 18.dp, vertical = 8.dp)) {
                                            Text(league,
                                                color = if (selectedLeague == league) D11White else Color(0xFF333333),
                                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // STATUS FILTER
                            item {
                                Row(modifier = Modifier.fillMaxWidth().background(D11White)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    listOf("All","Live","Upcoming","Completed").forEach { filter ->
                                        val isSelected = selectedFilter == filter
                                        Column(modifier = Modifier.clickable { selectedFilter = filter },
                                            horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(filter,
                                                color = if (isSelected) D11Red else Color(0xFF666666),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal)
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Box(modifier = Modifier.width(30.dp).height(2.dp).background(D11Red))
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                            }

                            // MATCHES
                            if (filteredMatches.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp),
                                        contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("No matches found", color = Color(0xFF888888),
                                                fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Text("Try a different filter", color = Color(0xFFAAAAAA),
                                                fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                items(filteredMatches, key = { it.id }) { match ->
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
                                        Text("Admin Panel", color = D11White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM NAV
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(D11White)) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(
                        Triple("home","Home",0),
                        Triple("matches","My Matches",1),
                        Triple("rewards","DreamCoins",2),
                        Triple("refer","Games",3)
                    ).forEach { (tab, label, index) ->
                        val isSelected = currentTab == tab
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onTabChange(tab) }.padding(horizontal = 12.dp)) {
                            Box(modifier = Modifier.size(30.dp).clip(CircleShape)
                                .background(if (isSelected) D11Red else Color(0xFFF0F0F0)),
                                contentAlignment = Alignment.Center) {
                                when(index) {
                                    0 -> Image(painter = painterResource(id = R.drawable.ic_logo),
                                        contentDescription = null, modifier = Modifier.size(22.dp))
                                    1 -> Text("M", color = if (isSelected) D11White else Color(0xFF666666),
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    2 -> Text("D", color = if (isSelected) D11White else Color(0xFF666666),
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    3 -> Text("G", color = if (isSelected) D11White else Color(0xFF666666),
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(label, color = if (isSelected) D11Red else Color(0xFF888888),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerLoadingUI() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha = infiniteTransition.animateFloat(0.3f, 1f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(4) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(12.dp)).alpha(alpha.value).background(Color(0xFFDDDDDD)))
        }
    }
}

fun matchDataFromCric(match: CricMatch): MatchData {
    val team1 = match.teams.getOrElse(0) { "Team 1" }.trim()
    val team2 = match.teams.getOrElse(1) { "Team 2" }.trim()
    val t1Short = match.teamInfo?.getOrNull(0)?.shortname?.ifEmpty { team1.take(3) } ?: team1.take(3)
    val t2Short = match.teamInfo?.getOrNull(1)?.shortname?.ifEmpty { team2.take(3) } ?: team2.take(3)
    return MatchData(
        id = match.id,
        team1 = t1Short.uppercase(),
        team2 = t2Short.uppercase(),
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
fun Dream11MatchCard(match: CricMatch, onClick: () -> Unit) {
    val t1Short = match.teamInfo?.getOrNull(0)?.shortname?.ifEmpty { match.teams.getOrElse(0){"T1"}.take(3) }
        ?: match.teams.getOrElse(0){"T1"}.take(3)
    val t2Short = match.teamInfo?.getOrNull(1)?.shortname?.ifEmpty { match.teams.getOrElse(1){"T2"}.take(3) }
        ?: match.teams.getOrElse(1){"T2"}.take(3)
    val team1Full = match.teams.getOrElse(0) { t1Short }
    val team2Full = match.teams.getOrElse(1) { t2Short }
    val isLive = match.matchStarted && !match.matchEnded
    val isCompleted = match.matchEnded

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = D11White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)) {
        Column {
            // Header
            Row(modifier = Modifier.fillMaxWidth()
                .background(if (isLive) Color(0xFFFFF3F3) else Color(0xFFF8F8F8))
                .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("T20 - IPL 2026", color = Color(0xFF444444),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                when {
                    isLive -> Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(D11Red))
                        Text("LIVE", color = D11Red, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    isCompleted -> Text("Completed", color = Color(0xFF888888), fontSize = 12.sp)
                    else -> CountdownTimer(hoursLeft = 2, minutesLeft = 30)
                }
            }

            // Teams
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF1E88E5), Color(0xFF003366))))
                        .border(2.dp, Color(0xFFEEEEEE), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(t1Short.take(3).uppercase(), color = D11White,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(t1Short.uppercase(), color = Color(0xFF111111),
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(team1Full.take(16), color = Color(0xFF888888), fontSize = 11.sp)
                        if (isLive) {
                            match.score?.getOrNull(0)?.let { s ->
                                Text("${s.r}/${s.w} (${s.o})", color = Color(0xFF111111),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text("vs", color = Color(0xFF888888), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(t2Short.uppercase(), color = Color(0xFF111111),
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(team2Full.take(16), color = Color(0xFF888888), fontSize = 11.sp)
                        if (isLive) {
                            match.score?.getOrNull(1)?.let { s ->
                                Text("${s.r}/${s.w} (${s.o})", color = Color(0xFF111111),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF43A047), Color(0xFF006600))))
                        .border(2.dp, Color(0xFFEEEEEE), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(t2Short.take(3).uppercase(), color = D11White,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            if (match.status.isNotEmpty()) {
                Text(match.status,
                    color = if (isLive) D11Red else Color(0xFF666666),
                    fontSize = 11.sp,
                    fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 6.dp))
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(D11Yellow),
                        contentAlignment = Alignment.Center) {
                        Text("M", color = D11Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("Rs.50 Crores +", color = Color(0xFF111111),
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Prize Pool", color = Color(0xFF888888), fontSize = 10.sp)
                    }
                }
                if (!isCompleted) {
                    Button(onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        elevation = ButtonDefaults.buttonElevation(2.dp)) {
                        Text("Play", color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                } else {
                    OutlinedButton(onClick = onClick,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCCCCC)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                        Text("View", color = Color(0xFF666666), fontSize = 14.sp)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFEEEEEE))) {
                Box(modifier = Modifier.fillMaxWidth(0.75f).height(4.dp)
                    .background(Brush.horizontalGradient(listOf(D11Green, Color(0xFF00E676)))))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("75% Full", color = Color(0xFF888888), fontSize = 11.sp)
                Text("50,000 Spots", color = Color(0xFF888888), fontSize = 11.sp)
            }
        }
    }
}
