package com.example.dream11india

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class LSTab(val label: String) {
    LIVE("Live"), SCORECARD("Scorecard"), MY_POINTS("My Points"),
    COMMENTARY("Commentary"), STATS("Stats")
}

@Composable
fun LiveScoreScreen(matchId: String = "", matchTitle: String = "", onBack: () -> Unit = {}) {
    val vm: LiveScoreViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(matchId) { if (matchId.isNotEmpty()) vm.load(matchId) }
    var tab by remember { mutableStateOf(LSTab.LIVE) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        MatchHeader(state = state, onBack = onBack)
        LSTabRow(selected = tab, onSelect = { tab = it })
        when {
            state.isLoading && state.innings.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = D11Red) }
            state.error != null && state.innings.isEmpty() ->
                ErrorContent(state.error!!) { vm.refresh(matchId) }
            else -> when (tab) {
                LSTab.LIVE       -> LiveTabContent(state)
                LSTab.SCORECARD  -> ScorecardContent(state)
                LSTab.MY_POINTS  -> MyPointsContent(state)
                LSTab.COMMENTARY -> CommentaryContent(state.commentary)
                LSTab.STATS      -> StatsContent(state)
            }
        }
    }
}

@Composable
private fun MatchHeader(state: LiveScoreUiState, onBack: () -> Unit) {
    val blink = rememberInfiniteTransition(label = "b")
    val da by blink.animateFloat(1f, 0.2f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "d")
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF0D0D2E), Color(0xFF111133))))) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                    Column {
                        Text(if (state.team1.isNotEmpty()) "${state.team1} vs ${state.team2}" else state.matchName,
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${state.matchType} • ${state.venue}", color = Color(0xFF888888), fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (state.isLive) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).alpha(da).background(D11Green))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF003300)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("LIVE", color = D11Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            if (state.score1.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A1A4A)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(state.team1, color = Color(0xFF82B1FF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(state.score1, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("(${state.overs1})", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VS", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(state.matchType, color = Color(0xFF555555), fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A3A1A)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(state.team2, color = Color(0xFF69F0AE), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(state.score2, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("(${state.overs2})", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                }
            }
            if (state.status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x22FFFFFF)).padding(horizontal = 10.dp, vertical = 6.dp), Alignment.Center) {
                    Text(state.status, color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            if (state.isLive && state.currentRR > 0) {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    RRChip("CRR",    String.format("%.2f", state.currentRR),  Color(0xFF82B1FF))
                    RRChip("RRR",    String.format("%.2f", state.requiredRR), D11Yellow)
                    RRChip("Target", "${state.target}",                       D11Red)
                }
            }
        }
    }
}

@Composable
private fun RRChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color(0xFF888888), fontSize = 9.sp)
    }
}

@Composable
private fun LSTabRow(selected: LSTab, onSelect: (LSTab) -> Unit) {
    val sc = rememberScrollState()
    Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).horizontalScroll(sc).padding(horizontal = 4.dp)) {
        LSTab.values().forEach { t ->
            val active = selected == t
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(t) }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(t.label, color = if (active) D11Red else Color(0xFF888888), fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal)
                if (active) { Spacer(Modifier.height(3.dp)); Box(Modifier.width(30.dp).height(2.dp).background(D11Red)) }
                else Spacer(Modifier.height(5.dp))
            }
        }
    }
    HorizontalDivider(color = Color(0xFF222222))
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Failed to load", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(error, color = Color(0xFF888888), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp)) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LiveCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF111111)),
        shape = RoundedCornerShape(12.dp), border = BorderStroke(0.5.dp, Color(0xFF222222))) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun BallCircle(ball: String) {
    val sc = remember { Animatable(0f) }
    LaunchedEffect(ball) { sc.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
    Box(Modifier.size(36.dp).scale(sc.value).clip(CircleShape)
        .background(when (ball) { "W" -> Color(0xFFCC0000); "4" -> Color(0xFF1565C0); "6" -> Color(0xFF2E7D32); "0" -> Color(0xFF2A2A2A); else -> Color(0xFF424242) })
        .border(1.dp, Color(0xFF555555), CircleShape), Alignment.Center) {
        Text(ball, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BatterRow(b: BatterStat) {
    Row(Modifier.fillMaxWidth()
        .background(if (b.isOnField) Color(0xFF0A1A0A) else Color.Transparent, RoundedCornerShape(6.dp))
        .padding(horizontal = if (b.isOnField) 6.dp else 0.dp, vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(Modifier.weight(1f), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
            if (b.isOnField) Box(Modifier.size(6.dp).clip(CircleShape).background(D11Green))
            Text(b.name + if (b.isOnField) " *" else "", color = Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (b.isCaptain) Box(Modifier.size(14.dp).clip(CircleShape).background(D11Yellow), Alignment.Center) {
                Text("C", color = Color(0xFF111111), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (b.isVC) Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFFAAAAAA)), Alignment.Center) {
                Text("VC", color = Color(0xFF111111), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        listOf(b.runs.toString(), b.balls.toString(), b.fours.toString(), b.sixes.toString(), String.format("%.1f", b.sr)).forEach { v ->
            Text(v, color = if (b.isOnField) Color.White else Color(0xAAFFFFFF), fontSize = 12.sp,
                textAlign = TextAlign.End, modifier = Modifier.width(30.dp))
        }
    }
}

@Composable
private fun BowlerRow(b: BowlerStat) {
    Row(Modifier.fillMaxWidth()
        .background(if (b.isOnField) Color(0xFF0A0A1A) else Color.Transparent, RoundedCornerShape(6.dp))
        .padding(horizontal = if (b.isOnField) 6.dp else 0.dp, vertical = 2.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(Modifier.weight(1f), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
            if (b.isOnField) Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF82B1FF)))
            Text(b.name + if (b.isOnField) " *" else "", color = Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        listOf(String.format("%.1f", b.overs), b.maidens.toString(), b.runs.toString(),
            b.wickets.toString(), String.format("%.2f", b.economy)).forEach { v ->
            Text(v, color = if (b.isOnField) Color.White else Color(0xAAFFFFFF), fontSize = 12.sp,
                textAlign = TextAlign.End, modifier = Modifier.width(30.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 2)
    }
}

@Composable
private fun LiveTabContent(state: LiveScoreUiState) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.balls.isNotEmpty()) item {
            LiveCard("This Over") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.balls.forEach { BallCircle(it) }
                    repeat((6 - state.balls.size).coerceAtLeast(0)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).border(1.dp, Color(0xFF333333), CircleShape))
                    }
                }
            }
        }
        if (state.liveBatters.isNotEmpty()) item {
            LiveCard("Batting - ${state.team2}") {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Batter", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(1f))
                    listOf("R","B","4s","6s","SR").forEach { Text(it, color = Color(0xFF888888), fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(30.dp)) }
                }
                HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 6.dp))
                state.liveBatters.forEach { b -> BatterRow(b); Spacer(Modifier.height(6.dp)) }
            }
        }
        if (state.liveBowlers.isNotEmpty()) item {
            LiveCard("Bowling - ${state.team1}") {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Bowler", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(1f))
                    listOf("O","M","R","W","Eco").forEach { Text(it, color = Color(0xFF888888), fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(30.dp)) }
                }
                HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 6.dp))
                state.liveBowlers.forEach { b -> BowlerRow(b); Spacer(Modifier.height(6.dp)) }
            }
        }
        if (state.isLive) item {
            LiveCard("Run Rate Info") {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(String.format("%.2f", state.currentRR), color = Color(0xFF82B1FF), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("Current RR", color = Color(0xFF888888), fontSize = 10.sp) }
                    Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFF222222)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(String.format("%.2f", state.requiredRR), color = D11Yellow, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("Required RR", color = Color(0xFF888888), fontSize = 10.sp) }
                    Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFF222222)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${state.target}", color = D11Red, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("Target", color = Color(0xFF888888), fontSize = 10.sp) }
                }
            }
        }
        if (state.venue.isNotEmpty()) item { LiveCard("Match Info") { InfoRow("Venue", state.venue); InfoRow("Type", state.matchType) } }
    }
}

@Composable
private fun ScorecardContent(state: LiveScoreUiState) {
    if (state.innings.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Scorecard not available", color = Color(0xFF888888), fontSize = 13.sp) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.innings.forEach { inn ->
            item {
                LiveCard(inn.teamName + if (inn.total.isNotEmpty()) " — ${inn.total}" else "") {
                    if (inn.batters.isNotEmpty()) {
                        Text("Batting", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Batter", color = Color(0xFF888888), fontSize = 10.sp, modifier = Modifier.weight(1f))
                            listOf("R","B","4s","6s","SR").forEach { Text(it, color = Color(0xFF888888), fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.width(28.dp)) }
                        }
                        HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 5.dp))
                        inn.batters.forEach { b ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(b.name, color = if (b.isOnField) Color.White else Color(0xCCFFFFFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        if (b.dismissal.isNotEmpty() && !b.isOnField)
                                            Text(b.dismissal, color = Color(0xFF666666), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    listOf(b.runs.toString(), b.balls.toString(), b.fours.toString(), b.sixes.toString(), String.format("%.1f", b.sr)).forEach {
                                        Text(it, color = Color(0xCCFFFFFF), fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 0.5.dp)
                        }
                        if (inn.extras.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(inn.extras, color = Color(0xFF666666), fontSize = 10.sp) }
                    }
                    if (inn.bowlers.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Bowling", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Bowler", color = Color(0xFF888888), fontSize = 10.sp, modifier = Modifier.weight(1f))
                            listOf("O","M","R","W","Eco").forEach { Text(it, color = Color(0xFF888888), fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.width(30.dp)) }
                        }
                        HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 5.dp))
                        inn.bowlers.forEach { b ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(b.name, color = Color(0xCCFFFFFF), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                listOf(String.format("%.1f", b.overs), b.maidens.toString(), b.runs.toString(), b.wickets.toString(), String.format("%.2f", b.economy)).forEach {
                                    Text(it, color = Color(0xCCFFFFFF), fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(30.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPointsContent(state: LiveScoreUiState) {
    val team = state.myTeam
    if (team.playerPoints.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No team created yet", color = Color(0xFF888888), fontSize = 14.sp)
                Text("Create a team to see fantasy points", color = Color(0xFF666666), fontSize = 12.sp)
            }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(Color.Transparent), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF333333))) {
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF1A0008), Color(0xFF330011)))).padding(20.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("My Fantasy Points", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                        Text(String.format("%.1f", team.totalPoints), color = D11Yellow, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                        if (team.rank > 0) Text("Rank #${team.rank}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            LiveCard("Player Points") {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Player", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Pts", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    Text("Multi", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 6.dp))
                team.playerPoints.forEach { entry ->
                    val name  = entry.first
                    val pts   = entry.second
                    val multi = entry.third
                    val isCap = name.contains("(C)")
                    val isVC  = name.contains("(VC)")
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .background(if (isCap) Color(0xFF1A1600) else if (isVC) Color(0xFF111111) else Color.Transparent, RoundedCornerShape(5.dp))
                        .padding(horizontal = if (isCap || isVC) 6.dp else 0.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(String.format("%.1f", pts), color = D11Yellow, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                        Box(Modifier.width(40.dp), Alignment.CenterEnd) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp))
                                .background(if (isCap) Color(0xFF2A2A00) else if (isVC) Color(0xFF1A1A1A) else Color.Transparent)
                                .padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text(multi, color = if (isCap) D11Yellow else if (isVC) Color(0xFFAAAAAA) else Color(0xFF666666),
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun CommentaryContent(items: List<CommentaryItem>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Commentary not available", color = Color(0xFF888888), fontSize = 13.sp) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items, key = { it.over + "_" + it.text.take(15) }) { item ->
            Card(colors = CardDefaults.cardColors(when (item.type) { "wicket" -> Color(0xFF1A0000); "six" -> Color(0xFF001A00); "four" -> Color(0xFF00001A); else -> Color(0xFF111111) }),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, when (item.type) { "wicket" -> Color(0xFF440000); "six" -> Color(0xFF004400); "four" -> Color(0xFF000044); else -> Color(0xFF222222) })) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1E1E1E)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Text(item.over, color = Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.type != "normal") {
                            Box(Modifier.clip(RoundedCornerShape(4.dp))
                                .background(when (item.type) { "wicket" -> Color(0xFF440000); "six" -> Color(0xFF004400); else -> Color(0xFF000044) })
                                .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(when (item.type) { "wicket" -> "WICKET"; "six" -> "SIX"; else -> "FOUR" },
                                    color = when (item.type) { "wicket" -> D11Red; "six" -> D11Green; else -> Color(0xFF82B1FF) },
                                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(item.text, color = Color(0xCCFFFFFF), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsContent(state: LiveScoreUiState) {
    val t1p = if (state.isLive && state.requiredRR > 0)
        ((state.requiredRR / (state.currentRR + state.requiredRR)) * 100).coerceIn(10f, 90f) else 50f
    val t2p = 100f - t1p
    val topBat = state.innings.flatMap { it.batters }.filter { it.runs > 0 }.sortedByDescending { it.runs }.take(5)
    val topBwl = state.innings.flatMap { it.bowlers }.filter { it.wickets > 0 }.sortedByDescending { it.wickets }.take(5)

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LiveCard("Win Probability") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(state.team1.ifEmpty { "Team 1" }, color = Color(0xFF82B1FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.0f", t1p)}%", color = Color(0xFF82B1FF), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF1E1E1E))) {
                        Box(Modifier.fillMaxWidth(t1p / 100f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Color(0xFF1565C0)))
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(state.team2.ifEmpty { "Team 2" }, color = D11Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.0f", t2p)}%", color = D11Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF1E1E1E))) {
                        Box(Modifier.fillMaxWidth(t2p / 100f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Color(0xFF2E7D32)))
                    }
                }
            }
        }
        if (topBat.isNotEmpty()) item {
            LiveCard("Top Scorers") {
                topBat.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(b.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("${b.runs}(${b.balls})", color = D11Yellow, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 0.5.dp)
                }
            }
        }
        if (topBwl.isNotEmpty()) item {
            LiveCard("Top Wicket Takers") {
                topBwl.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(b.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("${b.wickets}/${b.runs}(${String.format("%.1f", b.overs)})", color = D11Red, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 0.5.dp)
                }
            }
        }
        item {
            LiveCard("Match Details") {
                InfoRow("Type", state.matchType)
                InfoRow("Venue", state.venue)
                InfoRow("Status", if (state.isEnded) "Completed" else if (state.isLive) "Live" else "Upcoming")
            }
        }
    }
}