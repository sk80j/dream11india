package com.example.dream11india

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage

// ---------------------------------------------
// CONSTANTS
// ---------------------------------------------
private val BannerData = listOf(
    Triple(R.string.banner_mega_title,    R.string.banner_mega_sub,    D11Red),
    Triple(R.string.banner_ipl_title,     R.string.banner_ipl_sub,     Color(0xFF1565C0)),
    Triple(R.string.banner_free_title,    R.string.banner_free_sub,    D11Green)
)
private val SportTabs      = listOf("Cricket", "Football", "Kabaddi", "Basketball", "Baseball")
private val LeagueFilters  = listOf("All", "IPL", "T20", "ODI", "Test")
private val StatusFilters  = listOf("All", "Live", "Upcoming", "Completed")
private val BottomNavItems = listOf(
    NavItem("home",    R.string.nav_home,     Icons.Filled.Home,          Icons.Outlined.Home),
    NavItem("matches", R.string.nav_matches,  Icons.Filled.Star,  Icons.Outlined.Star),
    NavItem("rewards", R.string.nav_rewards,  Icons.Filled.EmojiEvents,          Icons.Outlined.EmojiEvents),
    NavItem("games",   R.string.nav_games,    Icons.Filled.SportsEsports,  Icons.Outlined.SportsEsports)
)

private data class NavItem(
    val key:          String,
    val labelRes:     Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ---------------------------------------------
// ROOT SCREEN
// ---------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentTab:      String       = "home",
    userData:        UserData     = UserData(),
    homeViewModel:   HomeViewModel = viewModel(),
    onTabChange:     (String) -> Unit = {},
    onMatchClick:    (MatchData) -> Unit = {},
    onWalletClick:   () -> Unit = {},
    onProfileClick:  () -> Unit = {},
    onAdminClick:    () -> Unit = {},
    onLeaderboardClick: () -> Unit = {}
) {
    val uiState      by homeViewModel.uiState.collectAsStateWithLifecycle()
    val listState    = rememberLazyListState()
    val ptrState     = rememberPullToRefreshState()
    val snackbar     = remember { SnackbarHostState() }

    // Show error in snackbar
    val errorMsg = (uiState.matchState as? MatchUiState.Error)?.message
    LaunchedEffect(errorMsg) {
        if (!errorMsg.isNullOrBlank()) snackbar.showSnackbar(errorMsg)
    }

    // Derived filtered list — only recomputes when relevant state changes
    val filteredMatches by remember(uiState) {
        derivedStateOf { homeViewModel.filteredMatches(uiState) }
    }

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbar) },
        containerColor  = Color(0xFFF2F3F5)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            Column(modifier = Modifier.fillMaxSize()) {

                HomeTopBar(
                    userData        = userData,
                    selectedSport   = uiState.selectedSport,
                    onSportSelected = homeViewModel::selectSport,
                    onProfileClick  = onProfileClick,
                    onWalletClick = onWalletClick
                )

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh    = homeViewModel::refresh,
                    state        = ptrState,
                    modifier     = Modifier.weight(1f)
                ) {
                    when (val state = uiState.matchState) {
                        is MatchUiState.Loading -> CardSkeletonList()

                        is MatchUiState.Error,
                        is MatchUiState.Success -> {
                            LazyColumn(
                                state          = listState,
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                item(key = "search") {
                                    SearchSection(
                                        query    = uiState.searchQuery,
                                        onChange = homeViewModel::updateSearch,
                                        onClear  = homeViewModel::clearSearch
                                    )
                                }

                                item(key = "banner") {
                                    BannerCarousel(
                                        banners      = BannerData,
                                        currentIndex = uiState.bannerIndex
                                    )
                                }

                                item(key = "leagueChips") {
                                    FilterChips(
                                        items    = LeagueFilters,
                                        selected = uiState.selectedLeague,
                                        onSelect = homeViewModel::selectLeague,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                item(key = "statusTabs") {
                                    StatusFilterTabs(
                                        selected = uiState.selectedFilter,
                                        onSelect = homeViewModel::selectFilter
                                    )
                                }

                                if (filteredMatches.isEmpty()) {
                                    item(key = "empty") {
                                        EmptyState(onRefresh = homeViewModel::refresh)
                                    }
                                } else {
                                    items(filteredMatches, key = { it.id }) { match ->
                                        MatchCard(
                                            match   = match,
                                            onClick = {
                                val t1 = match.teamInfo?.getOrNull(0)?.shortname?.ifEmpty { match.teams.getOrElse(0){"T1"}.take(3) } ?: match.teams.getOrElse(0){"T1"}.take(3)
                                val t2 = match.teamInfo?.getOrNull(1)?.shortname?.ifEmpty { match.teams.getOrElse(1){"T2"}.take(3) } ?: match.teams.getOrElse(1){"T2"}.take(3)
                                onMatchClick(MatchData(
                                    id=match.id, team1=t1.uppercase(), team2=t2.uppercase(),
                                    team1Full=match.teams.getOrElse(0){"Team 1"}, team2Full=match.teams.getOrElse(1){"Team 2"},
                                    team1Logo=match.t1LogoUrl, team2Logo=match.t2LogoUrl,
                                    type="T20", league="IPL 2026", matchTime=match.date,
                                    hoursLeft=2, minutesLeft=30, prize=match.prizePool,
                                    spots=match.totalSpots, fillPercent=match.filledSpots,
                                    badge=match.badge, team1Color=androidx.compose.ui.graphics.Color(0xFF003366),
                                    team2Color=androidx.compose.ui.graphics.Color(0xFF006600)
                                ))
                            }
                                        )
                                    }
                                }

                                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == "1irz1sRJ3QNeEtUuN70OSWiUBdq2") {
                                    item(key = "admin") { AdminButton(onClick = onAdminClick) }
                                }
                            }
                        }
                    }
                }
            }

            BottomNav(
                currentTab  = currentTab,
                onTabChange = onTabChange,
                modifier    = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ---------------------------------------------
// HOME TOP BAR
// ---------------------------------------------
@Composable
fun HomeTopBar(
    userData:        UserData,
    selectedSport:   String,
    onSportSelected: (String) -> Unit,
    onProfileClick:  () -> Unit,
    onWalletClick:   () -> Unit
) {
    Surface(color = Color(0xFF111111), shadowElevation = 8.dp) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Avatar + Logo + Name
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(D11Red, Color(0xFFB71C1C))))
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = (userData.name.firstOrNull() ?: 'P').toString().uppercase(),
                            color      = D11White,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text          = stringResource(R.string.app_name),
                        color         = D11White,
                        fontSize      = 20.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Icons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TopBarIconButton(
                        icon             = Icons.Outlined.Notifications,
                        contentDesc      = stringResource(R.string.cd_notifications),
                        tint             = D11White,
                        onClick          = {}
                    )
                    TopBarIconButton(
                        icon             = Icons.Filled.CardGiftcard,
                        contentDesc      = stringResource(R.string.cd_refer),
                        tint             = D11Yellow,
                        onClick          = {}
                    )
                    Surface(
                        shape    = RoundedCornerShape(20.dp),
                        color    = Color(0xFF2A2A2A),
                        modifier = Modifier.clickable { onWalletClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AddCircle, stringResource(R.string.cd_add_money),
                                tint = D11Green, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Sport tabs
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier              = Modifier.padding(bottom = 4.dp)
            ) {
                items(SportTabs) { sport ->
                    SportTab(label = sport, isSelected = sport == selectedSport,
                        onClick = { onSportSelected(sport) })
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector, contentDesc: String,
    tint: Color, onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDesc, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SportTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label,
            color = if (isSelected) D11White else Color(0xFF888888),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal)
        if (isSelected) {
            Spacer(Modifier.height(3.dp))
            Box(Modifier.width(28.dp).height(2.dp).clip(CircleShape).background(D11Red))
        }
    }
}

// ---------------------------------------------
// SEARCH SECTION
// ---------------------------------------------
@Composable
fun SearchSection(
    query: String, onChange: (String) -> Unit,
    onClear: () -> Unit, modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape           = RoundedCornerShape(12.dp),
        color           = D11White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(20.dp))
            BasicTextField(
                value = query, onValueChange = onChange,
                modifier  = Modifier.weight(1f),
                textStyle = TextStyle(color = Color(0xFF111111), fontSize = 14.sp),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(stringResource(R.string.search_hint),
                        color = Color(0xFFBBBBBB), fontSize = 14.sp)
                    inner()
                }
            )
            AnimatedVisibility(visible = query.isNotEmpty()) {
                Icon(Icons.Filled.Close, stringResource(R.string.cd_clear_search),
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(18.dp).clickable { onClear() })
            }
        }
    }
}

// ---------------------------------------------
// BANNER CAROUSEL
// ---------------------------------------------
@Composable
fun BannerCarousel(
    banners: List<Triple<Int, Int, Color>>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(banners[currentIndex].third,
                        banners[currentIndex].third.copy(alpha = 0.7f))
                )
            )
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 30.dp)
                .size(130.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )

        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            label = "banner_content"
        ) { idx ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(banners[idx].first),
                    color = D11White, fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(banners[idx].second),
                    color = Color(0xDDFFFFFF), fontSize = 13.sp)
            }
        }

        // Dot indicators
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            banners.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == currentIndex) 22.dp else 6.dp, 5.dp)
                        .clip(CircleShape)
                        .background(if (i == currentIndex) D11White else Color(0x55FFFFFF))
                )
            }
        }
    }
}

// ---------------------------------------------
// FILTER CHIPS (league)
// ---------------------------------------------
@Composable
fun FilterChips(
    items: List<String>, selected: String,
    onSelect: (String) -> Unit, modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = modifier
    ) {
        items(items) { item ->
            val isSelected = item == selected
            Surface(
                shape           = RoundedCornerShape(20.dp),
                color           = if (isSelected) D11Red else D11White,
                shadowElevation = if (isSelected) 3.dp else 1.dp,
                modifier        = Modifier
                    .clickable { onSelect(item) }
                    .then(if (!isSelected) Modifier.border(
                        1.dp, Color(0xFFDDDDDD), RoundedCornerShape(20.dp)) else Modifier)
            ) {
                Text(item,
                    color = if (isSelected) D11White else Color(0xFF444444),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            }
        }
    }
}

// ---------------------------------------------
// STATUS FILTER TABS
// ---------------------------------------------
@Composable
fun StatusFilterTabs(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = D11White, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatusFilters.forEach { filter ->
                    val isSel = filter == selected
                    Column(
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onSelect(filter) }),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(filter,
                            color = if (isSel) D11Red else Color(0xFF777777),
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal)
                        Spacer(Modifier.height(4.dp))
                        AnimatedVisibility(visible = isSel) {
                            Box(Modifier.width(30.dp).height(2.dp)
                                .clip(CircleShape).background(D11Red))
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}

// ---------------------------------------------
// MATCH CARD  (fully dynamic data)
// ---------------------------------------------
@Composable
fun MatchCard(match: CricMatch, onClick: () -> Unit) {
    val t1Info    = match.teamInfo?.getOrNull(0)
    val t2Info    = match.teamInfo?.getOrNull(1)
    val t1Short   = t1Info?.shortname?.ifEmpty { match.teams.getOrElse(0){"T1"}.take(3) }
        ?: match.teams.getOrElse(0){"T1"}.take(3)
    val t2Short   = t2Info?.shortname?.ifEmpty { match.teams.getOrElse(1){"T2"}.take(3) }
        ?: match.teams.getOrElse(1){"T2"}.take(3)
    val team1Full = match.teams.getOrElse(0) { t1Short }
    val team2Full = match.teams.getOrElse(1) { t2Short }
    val isLive      = match.matchStarted && !match.matchEnded
    val isCompleted = match.matchEnded

    val infiniteTransition = rememberInfiniteTransition(label = "live_${match.id}")
    val liveDotAlpha by infiniteTransition.animateFloat(0.25f, 1f,
        infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "dot_${match.id}")

    // Badge color
    val badgeColor = when (match.badge) {
        "FREE"        -> D11Green
        "GUARANTEED"  -> Color(0xFF7B1FA2)
        else          -> D11Red   // MEGA
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors    = CardDefaults.cardColors(containerColor = D11White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        border    = if (isLive)
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)) else null
    ) {
        Column {
            // -- HEADER --
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (isLive) Color(0xFFFFF5F5) else Color(0xFFF7F8FA))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = D11Yellow,
                        modifier = Modifier.size(14.dp))
                    Text("T20 · IPL 2026", color = Color(0xFF555555),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                when {
                    isLive -> Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape)
                            .alpha(liveDotAlpha).background(D11Red))
                        Text(stringResource(R.string.label_live), color = D11Red,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    isCompleted -> Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF888888),
                            modifier = Modifier.size(13.dp))
                        Text(stringResource(R.string.label_completed),
                            color = Color(0xFF888888), fontSize = 12.sp)
                    }
                    else -> CountdownTimer(hoursLeft = 2, minutesLeft = 30)
                }
            }

            // -- TEAMS --
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Team 1
                TeamBlock(
                    shortName = t1Short,
                    fullName  = team1Full,
                    logoUrl   = match.t1LogoUrl,
                    score     = if (isLive) match.score?.getOrNull(0) else null,
                    gradient  = Brush.radialGradient(listOf(Color(0xFF1E88E5), Color(0xFF003366))),
                    rtl       = false
                )

                // VS badge
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("vs", color = Color(0xFF999999),
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Team 2
                TeamBlock(
                    shortName = t2Short,
                    fullName  = team2Full,
                    logoUrl   = match.t2LogoUrl,
                    score     = if (isLive) match.score?.getOrNull(1) else null,
                    gradient  = Brush.radialGradient(listOf(Color(0xFF43A047), Color(0xFF006600))),
                    rtl       = true
                )
            }

            // -- STATUS TEXT --
            if (match.status.isNotEmpty()) {
                Text(match.status,
                    color = if (isLive) D11Red else Color(0xFF777777),
                    fontSize = 11.sp,
                    fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp))
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))

            // -- PRIZE + BADGE + CTA --
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(match.badge, color = badgeColor,
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(match.prizePool, color = Color(0xFF111111),
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(R.string.label_prize_pool),
                            color = Color(0xFF999999), fontSize = 10.sp)
                    }
                }

                when {
                    isCompleted -> OutlinedButton(onClick = onClick,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCCCCC)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)) {
                        Text(stringResource(R.string.btn_view_results),
                            color = Color(0xFF666666), fontSize = 13.sp)
                    }
                    else -> Button(onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = D11Red),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 9.dp),
                        elevation = ButtonDefaults.buttonElevation(3.dp)) {
                        Text(if (isLive) stringResource(R.string.btn_join_now)
                        else stringResource(R.string.btn_play),
                            color = D11White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }

            // -- FILL PROGRESS — DYNAMIC --
            val fillFraction = match.filledSpots / 100f
            val fillColor = when {
                fillFraction > 0.85f -> Color(0xFFE53935)
                fillFraction > 0.60f -> D11Yellow
                else                 -> D11Green
            }
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFEEEEEE))) {
                Box(modifier = Modifier.fillMaxWidth(fillFraction).height(4.dp)
                    .background(Brush.horizontalGradient(listOf(fillColor, fillColor.copy(alpha=0.7f)))))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${match.filledSpots}% ${stringResource(R.string.label_full)}",
                    color = Color(0xFF999999), fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Filled.Group, null, tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(12.dp))
                    Text(match.totalSpots, color = Color(0xFF999999), fontSize = 11.sp)
                }
            }

            // -- ENTRY FEE --
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFFF7F8FA))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.ConfirmationNumber, null, tint = Color(0xFF999999),
                    modifier = Modifier.size(12.dp))
                Text("${stringResource(R.string.label_entry)}: ${match.entryFee}",
                    color = Color(0xFF777777), fontSize = 11.sp)
            }
        }
    }
}

// ---------------------------------------------
// TEAM BLOCK — Coil logo with initials fallback
// ---------------------------------------------
@Composable
private fun TeamBlock(
    shortName: String, fullName: String,
    logoUrl: String, score: Score?,
    gradient: Brush, rtl: Boolean
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!rtl) {
            TeamLogo(shortName = shortName, logoUrl = logoUrl, gradient = gradient)
            TeamTextBlock(shortName = shortName, fullName = fullName,
                score = score, alignment = Alignment.Start)
        } else {
            TeamTextBlock(shortName = shortName, fullName = fullName,
                score = score, alignment = Alignment.End)
            TeamLogo(shortName = shortName, logoUrl = logoUrl, gradient = gradient)
        }
    }
}

@Composable
private fun TeamLogo(shortName: String, logoUrl: String, gradient: Brush, size: Dp = 48.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(gradient)
            .border(2.dp, Color(0xFFEEEEEE), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model             = logoUrl,
                contentDescription = shortName,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier.fillMaxSize().clip(CircleShape),
                loading           = {
                    // Show initials while loading
                    Text(shortName.take(3).uppercase(), color = D11White,
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                },
                error = {
                    // Fallback to initials on error
                    Text(shortName.take(3).uppercase(), color = D11White,
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            )
        } else {
            Text(shortName.take(3).uppercase(), color = D11White,
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun TeamTextBlock(
    shortName: String, fullName: String,
    score: Score?, alignment: Alignment.Horizontal
) {
    Column(horizontalAlignment = alignment) {
        Text(shortName.uppercase(), color = Color(0xFF111111),
            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(fullName.take(18), color = Color(0xFF999999),
            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        score?.let { s ->
            Text("${s.r}/${s.w} (${s.o})", color = Color(0xFF111111),
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------
// BOTTOM NAV
// ---------------------------------------------
@Composable
fun BottomNav(currentTab: String, onTabChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = D11White, shadowElevation = 16.dp, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItems.forEach { item ->
                    val isSelected = currentTab == item.key
                    Column(
                        modifier = Modifier
                            .clickable(indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onTabChange(item.key) })
                            .padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(if (isSelected) D11Red.copy(alpha = 0.12f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.labelRes),
                                tint = if (isSelected) D11Red else Color(0xFF888888),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(item.labelRes),
                            color = if (isSelected) D11Red else Color(0xFF999999),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------
// CARD SKELETON SHIMMER  (matches real card shape)
// ---------------------------------------------
@Composable
fun CardSkeletonList() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmerX by transition.animateFloat(
        initialValue  = -300f,
        targetValue   = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label         = "shimmerX"
    )
    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0)),
        startX = shimmerX,
        endX   = shimmerX + 600f
    )

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled   = false
    ) {
        items(3) {
            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = D11White),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Header skeleton
                    Box(Modifier.fillMaxWidth().height(36.dp).background(shimmerBrush))
                    Spacer(Modifier.height(1.dp))

                    // Teams skeleton
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Team 1
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(shimmerBrush))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.width(48.dp).height(16.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                                Box(Modifier.width(80.dp).height(11.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                            }
                        }
                        // VS
                        Box(Modifier.size(36.dp).clip(CircleShape).background(shimmerBrush))
                        // Team 2
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.width(48.dp).height(16.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                                Box(Modifier.width(80.dp).height(11.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                            }
                            Box(Modifier.size(48.dp).clip(CircleShape).background(shimmerBrush))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    // Prize + button skeleton
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.width(100.dp).height(14.dp)
                                .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                            Box(Modifier.width(60.dp).height(10.dp)
                                .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                        }
                        Box(Modifier.width(80.dp).height(36.dp)
                            .clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
                    }

                    // Progress skeleton
                    Box(Modifier.fillMaxWidth().height(4.dp).background(shimmerBrush))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(Modifier.width(60.dp).height(10.dp)
                            .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                        Box(Modifier.width(80.dp).height(10.dp)
                            .clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------
// EMPTY STATE
// ---------------------------------------------
@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.SearchOff, null, tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(52.dp))
        Text(stringResource(R.string.empty_title), color = Color(0xFF888888),
            fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.empty_sub), color = Color(0xFFBBBBBB), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(containerColor = D11Red),
            shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.btn_refresh), color = D11White, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------
// ADMIN BUTTON
// ---------------------------------------------
@Composable
private fun AdminButton(onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Filled.AdminPanelSettings, null, tint = D11White,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.btn_admin), color = D11White, fontWeight = FontWeight.Bold)
    }
}










