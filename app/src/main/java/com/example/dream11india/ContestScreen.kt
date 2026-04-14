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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ContestData(
    val id: String = "",
    val name: String,
    val prize: String,
    val spots: String,
    val spotsLeft: String,
    val fillPercent: Int,
    val entryFee: String,
    val entryFeeInt: Int = 0,
    val isFree: Boolean,
    val isHot: Boolean,
    val winners: String,
    val firstPrize: String,
    val isGuaranteed: Boolean = false
)

@Composable
fun ContestScreen(
    matchTitle: String = "CSK vs RCB",
    matchId: String = "1",
    userData: UserData = UserData(),
    onBack: () -> Unit = {},
    onJoin: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("contests") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedContest by remember { mutableStateOf<ContestData?>(null) }

    val team1 = matchTitle.split(" vs ").getOrElse(0) { "T1" }.trim()
    val team2 = matchTitle.split(" vs ").getOrElse(1) { "T2" }.trim()

    val allContests = listOf(
        ContestData("1","Mega Contest","Rs.50 Crores","1,20,000","6,543",95,"Rs.49",49,false,true,"30,000","Rs.1 Cr",true),
        ContestData("2","Hot Contest","Rs.25 Crores","85,000","18,234",78,"Rs.29",29,false,true,"15,000","Rs.50 Lakh",true),
        ContestData("3","Small League","Rs.10 Crores","45,000","24,750",45,"Rs.19",19,false,false,"8,000","Rs.20 Lakh",false),
        ContestData("4","Free Contest","Rs.1 Lakh","25,000","20,000",20,"FREE",0,true,false,"5,000","Rs.10,000",false),
        ContestData("5","Head to Head","Rs.90","2","1",50,"Rs.49",49,false,false,"1","Rs.90",false),
        ContestData("6","Practice Contest","No Prize","50,000","35,000",30,"FREE",0,true,false,"0","No Prize",false),
    )

    val filteredContests = when(selectedFilter) {
        "Mega" -> allContests.filter { it.name.contains("Mega") }
        "Small" -> allContests.filter { it.name.contains("Small") }
        "Free" -> allContests.filter { it.isFree }
        "H2H" -> allContests.filter { it.name.contains("Head") }
        else -> allContests
    }

    // Join Dialog
    if (showJoinDialog && selectedContest != null) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            containerColor = D11CardBg,
            title = {
                Text("Join Contest", color = D11White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(selectedContest!!.name, color = D11Yellow,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = D11Border)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Entry Fee:", color = D11Gray)
                        Text(selectedContest!!.entryFee, color = D11White,
                            fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Your Balance:", color = D11Gray)
                        Text("Rs.${userData.balance}", color = D11Green,
                            fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Prize Pool:", color = D11Gray)
                        Text(selectedContest!!.prize, color = D11Yellow,
                            fontWeight = FontWeight.Bold)
                    }
                    if (userData.balance < selectedContest!!.entryFeeInt) {
                        Box(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A0000))
                            .padding(8.dp)) {
                            Text("Insufficient balance! Add money to wallet.",
                                color = D11Red, fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showJoinDialog = false
                        onJoin(selectedContest!!.name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                    enabled = userData.balance >= selectedContest!!.entryFeeInt ||
                            selectedContest!!.isFree
                ) {
                    Text("Join Now", color = D11White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showJoinDialog = false },
                    border = androidx.compose.foundation.BorderStroke(1.dp, D11Gray)
                ) {
                    Text("Cancel", color = D11Gray)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {

        // TOP SCORE CARD
        Box(modifier = Modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D2E), Color(0xFF1A1A3A))
                )
            )) {
            Column(modifier = Modifier.statusBarsPadding().padding(16.dp)) {

                // Back + Logo + Title
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("←", color = D11White, fontSize = 24.sp,
                            modifier = Modifier.clickable { onBack() })
                        Image(painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo", modifier = Modifier.size(26.dp))
                        Column {
                            Text(matchTitle, color = D11White, fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("T20 - IPL 2026", color = D11Gray, fontSize = 11.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333355))
                            .padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("PTS", color = D11White, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape)
                            .border(1.dp, D11Gray, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("?", color = D11White, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Score
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {

                    // Team 1
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(Color(0xFF003366)),
                            contentAlignment = Alignment.Center) {
                            Text(team1.take(3).uppercase(), color = D11White,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(team1, color = D11Gray, fontSize = 12.sp)
                            Text("186/5", color = D11White, fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("(20.0 ov)", color = D11Gray, fontSize = 11.sp)
                        }
                    }

                    // Center
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(D11Green)
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("LIVE", color = D11White, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Text("vs", color = D11Gray, fontSize = 12.sp)
                    }

                    // Team 2
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(team2, color = D11Gray, fontSize = 12.sp)
                            Text("142/3", color = D11White, fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("(14.0 ov)", color = D11Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(Color(0xFF006600)),
                            contentAlignment = Alignment.Center) {
                            Text(team2.take(3).uppercase(), color = D11White,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status
                Box(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(D11Green))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$team1 need 45 runs in 36 balls",
                            color = D11White, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TABS
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111)),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                "contests" to "Contests",
                "my_teams" to "My Teams",
                "leaderboard" to "Leaderboard",
                "scorecard" to "Scorecard"
            ).forEach { (tab, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedTab = tab }
                        .padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(label,
                        color = if (selectedTab == tab) D11Red else D11Gray,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold
                        else FontWeight.Normal)
                    if (selectedTab == tab) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.width(40.dp).height(2.dp).background(D11Red))
                    }
                }
            }
        }

        HorizontalDivider(color = D11Border)

        when (selectedTab) {
            "contests" -> ContestsList(
                contests = filteredContests,
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                onJoin = { contest ->
                    selectedContest = contest
                    showJoinDialog = true
                }
            )
            "leaderboard" -> LeaderboardTab()
            "my_teams" -> MyTeamsTab()
            else -> ContestsList(
                contests = filteredContests,
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                onJoin = { contest ->
                    selectedContest = contest
                    showJoinDialog = true
                }
            )
        }
    }
}

@Composable
fun ContestsList(
    contests: List<ContestData>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onJoin: (ContestData) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner
        item {
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0D0D2E), Color(0xFF1A1A4A))
                    )
                )
                .border(1.dp, Color(0xFF333366), RoundedCornerShape(12.dp))
                .padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("1", color = D11Yellow, fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Text("CROREPATI", color = D11White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Text("7 LAKHPATIS", color = D11Gray, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Prize Pool", color = D11Gray, fontSize = 11.sp)
                        Text("Rs.50 Crores", color = D11Yellow, fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Filter tabs
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(listOf("All", "Mega", "Small", "Free", "H2H")) { filter ->
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selectedFilter == filter) D11Red
                            else Color(0xFF2A2A2A)
                        )
                        .clickable { onFilterChange(filter) }
                        .padding(horizontal = 16.dp, vertical = 7.dp)) {
                        Text(filter, color = D11White,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stats row
        item {
            Row(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
                .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    "${contests.size}" to "Contests",
                    contests.count { !it.isFree }.toString() to "Paid",
                    contests.count { it.isFree }.toString() to "Free"
                ).forEach { (value, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = D11Yellow, fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text(label, color = D11Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        items(contests) { contest ->
            ContestCard(contest = contest, onJoin = { onJoin(contest) })
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ContestCard(contest: ContestData, onJoin: () -> Unit) {
    val scale = remember { Animatable(1f) }

    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top badges + Join button
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (contest.isGuaranteed) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A3A1A))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("Guaranteed", color = D11Green,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (contest.isHot) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3A1500))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("HOT", color = Color(0xFFFF6B35),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (contest.isFree) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF003300))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("FREE", color = D11Green,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (contest.isFree) D11Green else D11Red
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Text(contest.entryFee, color = D11White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prize + Spots
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Prize Pool", color = D11Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(contest.prize, color = D11White,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Spots", color = D11Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(contest.spots, color = D11White,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${contest.spotsLeft} spots left",
                    color = if (contest.fillPercent > 80) D11Red else D11Gray,
                    fontSize = 11.sp)
                Text("${contest.fillPercent}% filled", color = D11Gray, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(5.dp)
                .clip(RoundedCornerShape(2.dp)).background(Color(0xFF333333))) {
                Box(modifier = Modifier
                    .fillMaxWidth(contest.fillPercent.toFloat() / 100f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (contest.fillPercent > 80)
                                listOf(D11Red, Color(0xFFFF6B35))
                            else listOf(D11Green, Color(0xFF00E676))
                        )
                    ))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = D11Border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Winners + 1st prize
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape)
                        .background(D11Yellow),
                        contentAlignment = Alignment.Center) {
                        Text("W", color = D11Black, fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Text("${contest.winners} Winners",
                        color = D11Gray, fontSize = 11.sp)
                }
                Text("1st: ${contest.firstPrize}", color = D11Green,
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun MyTeamsTab() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(D11LightGray),
                contentAlignment = Alignment.Center) {
                Text("T", color = D11Gray, fontSize = 36.sp,
                    fontWeight = FontWeight.Bold)
            }
            Text("No teams created yet", color = D11White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
            Text("Create a team to join contests",
                color = D11Gray, fontSize = 13.sp)
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create Team", color = D11White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LeaderboardTab() {
    val entries = listOf(
        Triple("Rahul K.", 892.5f, "Rs.25,000"),
        Triple("Priya S.", 876.0f, "Rs.10,000"),
        Triple("Amit T.", 865.5f, "Rs.5,000"),
        Triple("You", 831.0f, "Rs.500"),
        Triple("Vikram R.", 820.0f, "Rs.200"),
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D0D))
            .padding(12.dp)) {
            Text("Points last updated at 14.0 overs",
                color = D11Red, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Compare", color = D11Gray, fontSize = 12.sp,
                modifier = Modifier.clickable { })
            Text("Download", color = D11Gray, fontSize = 12.sp,
                modifier = Modifier.clickable { })
        }

        HorizontalDivider(color = D11Border)

        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("All Teams (61,50,405)", color = D11White,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Text("Points", color = D11Gray, fontSize = 12.sp)
                Text("Rank", color = D11Gray, fontSize = 12.sp)
            }
        }

        LazyColumn {
            items(entries.withIndex().toList()) { (index, entry) ->
                val (name, points, prize) = entry
                val isYou = name == "You"
                Row(modifier = Modifier.fillMaxWidth()
                    .background(if (isYou) Color(0xFF1A1500) else Color(0xFF1A1A1A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(
                                when(index) {
                                    0 -> D11Yellow
                                    1 -> Color(0xFFC0C0C0)
                                    2 -> Color(0xFFCD7F32)
                                    else -> if (isYou) D11Red else Color(0xFF2A2A2A)
                                }
                            ),
                            contentAlignment = Alignment.Center) {
                            Text(
                                if (index < 3) "${index + 1}"
                                else name.take(2).uppercase(),
                                color = if (index < 3) D11Black else D11White,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = D11White, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold)
                                if (isYou) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                        .background(D11Red)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)) {
                                        Text("YOU", color = D11White, fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(prize, color = D11Green, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("$points", color = D11White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                        Text("#${index + 1}",
                            color = if (isYou) D11Yellow else D11Gray,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = D11Border, thickness = 0.5.dp)
            }
        }
    }
}