package com.example.dream11india

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─── NAV ITEMS ───────────────────────────────

private data class NavItem(
    val key: String, val labelRes: Int,
    val selectedIcon: ImageVector, val unselectedIcon: ImageVector
)

private val BottomNavItems = listOf(
    NavItem("home",    R.string.nav_home,    Icons.Filled.Home,          Icons.Outlined.Home),
    NavItem("matches", R.string.nav_matches, Icons.Filled.Star,          Icons.Outlined.Star),
    NavItem("rewards", R.string.nav_rewards, Icons.Filled.EmojiEvents,   Icons.Outlined.EmojiEvents),
    NavItem("games",   R.string.nav_games,   Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports)
)

private val LeagueFilters = listOf("All", "IPL", "T20", "ODI", "Test")
private val StatusFilters = listOf("All", "Live", "Upcoming", "Completed")

// ─── BANNER ──────────────────────────────────

data class BannerItem(
    val title: String = "", val subtitle: String = "",
    val colorHex: String = "#D4002A", val isActive: Boolean = true
)

suspend fun loadBanners(): List<BannerItem> = try {
    FirebaseFirestore.getInstance().collection("banners")
        .whereEqualTo("isActive", true).orderBy("priority").limit(5).get().await()
        .documents.map { doc ->
            BannerItem(
                title    = doc.getString("title")    ?: "",
                subtitle = doc.getString("subtitle") ?: "",
                colorHex = doc.getString("color")    ?: "#D4002A",
                isActive = doc.getBoolean("isActive") ?: true
            )
        }.filter { it.title.isNotEmpty() }
} catch (_: Exception) { emptyList() }

suspend fun loadJoinedMatchIds(uid: String): Set<String> = try {
    FirebaseFirestore.getInstance().collection("joined_contests")
        .whereEqualTo("userId", uid).get().await()
        .documents.mapNotNull { it.getString("matchId") }.toSet()
} catch (_: Exception) { emptySet() }

fun getHoursLeft(dateStr: String): Int = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val diff = (sdf.parse(dateStr)?.time ?: 0) - System.currentTimeMillis()
    if (diff <= 0) 0 else (diff / 3600000).toInt()
} catch (_: Exception) { 0 }

fun getMinutesLeft(dateStr: String): Int = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val diff = (sdf.parse(dateStr)?.time ?: 0) - System.currentTimeMillis()
    if (diff <= 0) 0 else ((diff % 3600000) / 60000).toInt()
} catch (_: Exception) { 0 }

// ─── HOME SCREEN ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentTab:         String        = "home",
    userData:           UserData      = UserData(),
    homeViewModel:      HomeViewModel = viewModel(),
    onTabChange:        (String) -> Unit = {},
    onMatchClick:       (MatchData) -> Unit = {},
    onWalletClick:      () -> Unit = {},
    onProfileClick:     () -> Unit = {},
    onAdminClick:       () -> Unit = {},
    onLeaderboardClick: () -> Unit = {}
) {
    val uiState        by homeViewModel.uiState.collectAsStateWithLifecycle()
    val listState       = rememberLazyListState()
    val ptrState        = rememberPullToRefreshState()
    val snackbar        = remember { SnackbarHostState() }
    var banners         by remember { mutableStateOf<List<BannerItem>>(emptyList()) }
    var joinedMatchIds  by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) { banners = loadBanners() }
    LaunchedEffect(userData.uid) {
        if (userData.uid.isNotEmpty()) joinedMatchIds = loadJoinedMatchIds(userData.uid)
    }

    val errorMsg = (uiState.matchState as? MatchUiState.Error)?.message
    LaunchedEffect(errorMsg) { if (!errorMsg.isNullOrBlank()) snackbar.showSnackbar(errorMsg) }

    // "My Matches" tab = only joined matches
    val filteredMatches by remember(uiState, joinedMatchIds, currentTab) {
        derivedStateOf {
            if (currentTab == "matches") homeViewModel.myMatches(uiState)
            else homeViewModel.filteredMatches(uiState)
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = Color(0xFFF2F3F5)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                HomeTopBar(
                    userData       = userData,
                    onProfileClick = onProfileClick,
                    onWalletClick  = onWalletClick
                )
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh    = homeViewModel::refresh,
                    state        = ptrState,
                    modifier     = Modifier.weight(1f)
                ) {
                    when {
                        currentTab == "matches" -> MyMatchesContent(
                            joinedMatches  = filteredMatches,
                            joinedMatchIds = joinedMatchIds,
                            isLoading      = uiState.matchState is MatchUiState.Loading,
                            onMatchClick   = { match -> handleMatchClick(match, onMatchClick) },
                            onRefresh      = homeViewModel::refresh
                        )
                        uiState.matchState is MatchUiState.Loading -> CardSkeletonList()
                        else -> {
                            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 80.dp)) {
                                item(key = "search") {
                                    SearchSection(query = uiState.searchQuery, onChange = homeViewModel::updateSearch, onClear = homeViewModel::clearSearch)
                                }
                                item(key = "banner") {
                                    if (banners.isNotEmpty()) DynamicBannerCarousel(banners = banners, currentIndex = uiState.bannerIndex)
                                }
                                item(key = "wallet") { WalletQuickBar(userData = userData, onWalletClick = onWalletClick) }
                                item(key = "filters") {
                                    FilterChips(items = LeagueFilters, selected = uiState.selectedLeague, onSelect = homeViewModel::selectLeague, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                item(key = "status") {
                                    StatusFilterTabs(selected = uiState.selectedFilter, onSelect = homeViewModel::selectFilter)
                                }
                                if (filteredMatches.isEmpty()) {
                                    item(key = "empty") { HomeEmptyState(onRefresh = homeViewModel::refresh) }
                                } else {
                                    items(filteredMatches, key = { it.id }) { match ->
                                        MatchCard(
                                            match    = match,
                                            isJoined = joinedMatchIds.contains(match.id),
                                            onClick  = { handleMatchClick(match, onMatchClick) }
                                        )
                                    }
                                }
                                if (userData.isAdmin || userData.uid == "1irz1sRJ3QNeEtUuN70OSWiUBdq2") {
                                    item(key = "admin") { AdminButton(onClick = onAdminClick) }
                                }
                            }
                        }
                    }
                }
            }
            BottomNav(
                currentTab = currentTab,
                onTabChange = onTabChange,
                modifier    = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun handleMatchClick(match: CricMatch, onMatchClick: (MatchData) -> Unit) {
    val t1 = match.teamInfo?.getOrNull(0)?.shortname?.ifEmpty { match.teams.getOrElse(0) { "T1" }.take(3) } ?: match.teams.getOrElse(0) { "T1" }.take(3)
    val t2 = match.teamInfo?.getOrNull(1)?.shortname?.ifEmpty { match.teams.getOrElse(1) { "T2" }.take(3) } ?: match.teams.getOrElse(1) { "T2" }.take(3)
    onMatchClick(MatchData(
        id = match.id, team1 = t1.uppercase(), team2 = t2.uppercase(),
        team1Full = match.teams.getOrElse(0) { "Team 1" }, team2Full = match.teams.getOrElse(1) { "Team 2" },
        team1Logo = match.t1LogoUrl, team2Logo = match.t2LogoUrl,
        type = match.matchType, league = match.name, matchTime = match.date,
        hoursLeft = getHoursLeft(match.date), minutesLeft = getMinutesLeft(match.date),
        prize = match.prizePool ?: "", spots = match.totalSpots ?: "",
        fillPercent = match.filledSpots ?: 0, badge = match.badge ?: "",
        team1Color = Color(0xFF003366), team2Color = Color(0xFF006600)
    ))
}

// ─── MY MATCHES CONTENT ──────────────────────

@Composable
private fun MyMatchesContent(
    joinedMatches:  List<CricMatch>,
    joinedMatchIds: Set<String>,
    isLoading:      Boolean,
    onMatchClick:   (CricMatch) -> Unit,
    onRefresh:      () -> Unit
) {
    if (isLoading) { CardSkeletonList(); return }
    if (joinedMatches.isEmpty()) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("🏏", fontSize = 52.sp)
            Spacer(Modifier.height(12.dp))
            Text("No matches joined yet", color = Color(0xFF888888), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Join a contest to see your matches here", color = Color(0xFFBBBBBB), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Box(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("My Matches (${joinedMatches.size})", color = Color(0xFF111111), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        items(joinedMatches, key = { it.id }) { match ->
            MatchCard(match = match, isJoined = true, onClick = { onMatchClick(match) })
        }
    }
}

// ─── TOP BAR ─────────────────────────────────

@Composable
fun HomeTopBar(
    userData:       UserData,
    onProfileClick: () -> Unit,
    onWalletClick:  () -> Unit
) {
    Surface(color = Color(0xFF111111), shadowElevation = 6.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                // Left — Logo + Name
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(D11Red).clickable { onProfileClick() },
                        Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter            = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                    Text(
                        stringResource(R.string.app_name),
                        color      = Color.White,
                        fontSize   = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
                // Right — Bell + Gift + Wallet
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF222222)).clickable {}, Alignment.Center) {
                        Icon(Icons.Outlined.Notifications, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF222222)).clickable {}, Alignment.Center) {
                        Icon(Icons.Filled.CardGiftcard, null, tint = D11Yellow, modifier = Modifier.size(18.dp))
                    }
                    // Wallet chip — horizontal proper
                    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF222222), modifier = Modifier.clickable { onWalletClick() }) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, null, tint = D11Green, modifier = Modifier.size(13.dp))
                            Text("₹${userData.balance}", color = D11Green, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            // Cricket tab pill
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, bottom = 6.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(D11Red).padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    Text("🏏  Cricket", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ─── WALLET QUICK BAR ────────────────────────

@Composable
fun WalletQuickBar(userData: UserData, onWalletClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).clickable { onWalletClick() },
        colors   = CardDefaults.cardColors(Color(0xFF1A1A1A)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            listOf(
                Triple("₹${userData.balance}",     "Balance",  D11Green),
                Triple("₹${userData.winnings}",    "Winnings", D11Yellow),
                Triple("₹${userData.bonusBalance}", "Bonus",   Color(0xFF82B1FF))
            ).forEach { (value, label, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(label, color = Color(0xFF666666), fontSize = 9.sp)
                }
            }
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(D11Red)
                    .clickable { onWalletClick() }.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Add Cash", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ─── DYNAMIC BANNER ──────────────────────────

@Composable
fun DynamicBannerCarousel(banners: List<BannerItem>, currentIndex: Int) {
    if (banners.isEmpty()) return
    val idx   = currentIndex % banners.size
    val item  = banners[idx]
    val color = try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (_: Exception) { D11Red }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))).height(88.dp),
        Alignment.Center
    ) {
        Box(Modifier.align(Alignment.CenterEnd).offset(x = 30.dp).size(120.dp).background(Color.White.copy(alpha = 0.07f), CircleShape))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (item.title.isNotEmpty()) Text(item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            if (item.subtitle.isNotEmpty()) { Spacer(Modifier.height(3.dp)); Text(item.subtitle, color = Color(0xDDFFFFFF), fontSize = 12.sp) }
        }
        if (banners.size > 1) {
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                banners.indices.forEach { i ->
                    Box(Modifier.size(if (i == idx) 20.dp else 5.dp, 4.dp).clip(CircleShape).background(if (i == idx) Color.White else Color(0x55FFFFFF)))
                }
            }
        }
    }
}

// ─── SEARCH ──────────────────────────────────

@Composable
fun SearchSection(query: String, onChange: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), RoundedCornerShape(12.dp), Color.White, shadowElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(18.dp))
            BasicTextField(
                value = query, onValueChange = onChange,
                modifier  = Modifier.weight(1f),
                textStyle = TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(stringResource(R.string.search_hint), color = Color(0xFFBBBBBB), fontSize = 14.sp)
                    inner()
                }
            )
            AnimatedVisibility(visible = query.isNotEmpty()) {
                Icon(Icons.Filled.Close, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp).clickable { onClear() })
            }
        }
    }
}

// ─── FILTER CHIPS ────────────────────────────

@Composable
fun FilterChips(items: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        items(items) { item ->
            val isSel = item == selected
            Surface(
                shape          = RoundedCornerShape(20.dp),
                color          = if (isSel) D11Red else Color.White,
                shadowElevation = if (isSel) 3.dp else 1.dp,
                modifier       = Modifier.clickable { onSelect(item) }.then(if (!isSel) Modifier.border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(20.dp)) else Modifier)
            ) {
                Text(item, color = if (isSel) Color.White else Color(0xFF444444), fontSize = 13.sp, fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

// ─── STATUS TABS ─────────────────────────────

@Composable
fun StatusFilterTabs(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Color.White, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatusFilters.forEach { f ->
                    val isSel = f == selected
                    Column(
                        Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { onSelect(f) }),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(f, color = if (isSel) D11Red else Color(0xFF777777), fontSize = 13.sp, fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal)
                        Spacer(Modifier.height(3.dp))
                        AnimatedVisibility(visible = isSel) {
                            Box(Modifier.width(28.dp).height(2.dp).clip(CircleShape).background(D11Red))
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}

// ─── MATCH CARD ──────────────────────────────

@Composable
fun MatchCard(match: CricMatch, isJoined: Boolean = false, onClick: () -> Unit) {
    val t1Info  = match.teamInfo?.getOrNull(0)
    val t2Info  = match.teamInfo?.getOrNull(1)
    val t1Short = t1Info?.shortname?.ifEmpty { match.teams.getOrElse(0) { "T1" }.take(3) } ?: match.teams.getOrElse(0) { "T1" }.take(3)
    val t2Short = t2Info?.shortname?.ifEmpty { match.teams.getOrElse(1) { "T2" }.take(3) } ?: match.teams.getOrElse(1) { "T2" }.take(3)
    val isLive      = match.matchStarted && !match.matchEnded
    val isCompleted = match.matchEnded
    val hoursLeft   = getHoursLeft(match.date)
    val minutesLeft = getMinutesLeft(match.date)
    val inf = rememberInfiniteTransition(label = "l_${match.id}")
    val dotAlpha by inf.animateFloat(0.25f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "d_${match.id}")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(Color.White),
        shape    = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        border   = if (isLive) BorderStroke(1.dp, Color(0xFFFFCDD2)) else null
    ) {
        Column {
            // Header row
            Row(
                Modifier.fillMaxWidth().background(if (isLive) Color(0xFFFFF5F5) else Color(0xFFF7F8FA)).padding(horizontal = 14.dp, vertical = 8.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = D11Yellow, modifier = Modifier.size(13.dp))
                    Text("${match.matchType.uppercase()} • ${match.name.take(28)}", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.65f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (isJoined) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF003300)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("Joined", color = D11Green, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
                    when {
                        isLive      -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Box(Modifier.size(6.dp).clip(CircleShape).alpha(dotAlpha).background(D11Red)); Text("LIVE", color = D11Red, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                        isCompleted -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF888888), modifier = Modifier.size(12.dp)); Text("Done", color = Color(0xFF888888), fontSize = 11.sp) }
                        hoursLeft > 0 || minutesLeft > 0 -> CountdownTimer(hoursLeft = hoursLeft, minutesLeft = minutesLeft)
                        else -> Text("Soon", color = D11Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Teams row
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                HomeTeamBlock(t1Short, match.teams.getOrElse(0) { t1Short }, match.t1LogoUrl, if (isLive) match.score?.getOrNull(0) else null, Brush.radialGradient(listOf(Color(0xFF1E88E5), Color(0xFF003366))), false)
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0F0F0)), Alignment.Center) { Text("vs", color = Color(0xFF999999), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
                HomeTeamBlock(t2Short, match.teams.getOrElse(1) { t2Short }, match.t2LogoUrl, if (isLive) match.score?.getOrNull(1) else null, Brush.radialGradient(listOf(Color(0xFF43A047), Color(0xFF006600))), true)
            }

            // Status text
            if (match.status.isNotEmpty()) Text(match.status, color = if (isLive) D11Red else Color(0xFF888888), fontSize = 11.sp, fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp))

            HorizontalDivider(color = Color(0xFFF0F0F0))

            // Prize + CTA
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    if (!match.prizePool.isNullOrEmpty()) Text(match.prizePool!!, color = Color(0xFF111111), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.label_prize_pool), color = Color(0xFF999999), fontSize = 9.sp)
                }
                when {
                    isCompleted -> OutlinedButton(onClick = onClick, border = BorderStroke(1.dp, Color(0xFFCCCCCC)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)) { Text(stringResource(R.string.btn_view_results), color = Color(0xFF666666), fontSize = 12.sp) }
                    isLive      -> OutlinedButton(onClick = onClick, border = BorderStroke(1.dp, D11Red), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)) { Text("View Contest", color = D11Red, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                    else        -> Button(onClick = onClick, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp), elevation = ButtonDefaults.buttonElevation(2.dp)) { Text(stringResource(R.string.btn_play), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                }
            }

            // Fill bar
            val fillFraction = (match.filledSpots ?: 0) / 100f
            val fillColor = when { fillFraction > 0.85f -> Color(0xFFE53935); fillFraction > 0.6f -> D11Yellow; else -> D11Green }
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFEEEEEE))) {
                Box(Modifier.fillMaxWidth(fillFraction).height(3.dp).background(Brush.horizontalGradient(listOf(fillColor, fillColor.copy(alpha = 0.7f)))))
            }

            // Spots + Entry
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), Arrangement.SpaceBetween) {
                if (match.filledSpots != null) Text("${match.filledSpots}% full", color = Color(0xFF999999), fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (!match.totalSpots.isNullOrEmpty()) { Icon(Icons.Filled.Group, null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(11.dp)); Text(match.totalSpots!!, color = Color(0xFF999999), fontSize = 10.sp) }
                }
            }
            if (!match.entryFee.isNullOrEmpty()) {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF7F8FA)).padding(horizontal = 14.dp, vertical = 5.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                    Icon(Icons.Filled.ConfirmationNumber, null, tint = Color(0xFF999999), modifier = Modifier.size(11.dp))
                    Text("${stringResource(R.string.label_entry)}: ${match.entryFee}", color = Color(0xFF777777), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeTeamBlock(shortName: String, fullName: String, logoUrl: String, score: Score?, gradient: Brush, rtl: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!rtl) { HomeTeamLogo(shortName, logoUrl, gradient); HomeTeamText(shortName, fullName, score, Alignment.Start) }
        else      { HomeTeamText(shortName, fullName, score, Alignment.End); HomeTeamLogo(shortName, logoUrl, gradient) }
    }
}

@Composable
private fun HomeTeamLogo(shortName: String, logoUrl: String, gradient: Brush) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(gradient).border(1.5.dp, Color(0xFFEEEEEE), CircleShape), Alignment.Center) {
        if (logoUrl.isNotBlank()) SubcomposeAsyncImage(
            model = logoUrl, contentDescription = shortName, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            loading = { Text(shortName.take(3).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) },
            error   = { Text(shortName.take(3).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
        )
        else Text(shortName.take(3).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun HomeTeamText(shortName: String, fullName: String, score: Score?, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(shortName.uppercase(), color = Color(0xFF111111), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(fullName.take(14), color = Color(0xFF999999), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        score?.let { s -> Text("${s.r}/${s.w} (${s.o})", color = Color(0xFF111111), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

// ─── BOTTOM NAV ──────────────────────────────

@Composable
fun BottomNav(currentTab: String, onTabChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Color.White, shadowElevation = 16.dp, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp),
                Arrangement.SpaceEvenly
            ) {
                BottomNavItems.forEach { item ->
                    val isSel = currentTab == item.key
                    val scale by animateFloatAsState(if (isSel) 1.1f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "s_${item.key}")
                    Column(
                        Modifier.scale(scale).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { onTabChange(item.key) }).padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (isSel) D11Red.copy(alpha = 0.12f) else Color.Transparent),
                            Alignment.Center
                        ) {
                            Icon(
                                if (isSel) item.selectedIcon else item.unselectedIcon,
                                stringResource(item.labelRes),
                                tint = if (isSel) D11Red else Color(0xFF888888),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(item.labelRes),
                            color      = if (isSel) D11Red else Color(0xFF999999),
                            fontSize   = 10.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal,
                            textAlign  = TextAlign.Center
                        )
                        // Active underline dot
                        AnimatedVisibility(visible = isSel) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(D11Red))
                        }
                    }
                }
            }
        }
    }
}

// ─── SKELETON ────────────────────────────────

@Composable
fun CardSkeletonList() {
    val transition = rememberInfiniteTransition(label = "sk")
    val shimmerX by transition.animateFloat(-300f, 1000f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "sx")
    val brush = Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0)), startX = shimmerX, endX = shimmerX + 600f)
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), userScrollEnabled = false) {
        items(4) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(34.dp).background(brush))
                    Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(brush))
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Box(Modifier.width(44.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush)); Box(Modifier.width(70.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush)) }
                        }
                        Box(Modifier.size(32.dp).clip(CircleShape).background(brush))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) { Box(Modifier.width(44.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush)); Box(Modifier.width(70.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush)) }
                            Box(Modifier.size(44.dp).clip(CircleShape).background(brush))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Box(Modifier.width(90.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(brush)); Box(Modifier.width(55.dp).height(9.dp).clip(RoundedCornerShape(4.dp)).background(brush)) }
                        Box(Modifier.width(72.dp).height(34.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(brush))
                }
            }
        }
    }
}

// ─── EMPTY STATE ─────────────────────────────

@Composable
private fun HomeEmptyState(onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 60.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("🏏", fontSize = 48.sp)
        Spacer(Modifier.height(10.dp))
        Text("No matches available", color = Color(0xFF888888), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Pull down to refresh", color = Color(0xFFBBBBBB), fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(D11Red), shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(stringResource(R.string.btn_refresh), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── ADMIN BUTTON ────────────────────────────

@Composable
private fun AdminButton(onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        colors   = ButtonDefaults.buttonColors(Color(0xFF212121)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Filled.AdminPanelSettings, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.btn_admin), color = Color.White, fontWeight = FontWeight.Bold)
    }
}