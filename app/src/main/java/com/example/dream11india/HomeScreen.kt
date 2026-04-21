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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private data class NavItem(val key:String, val labelRes:Int, val selectedIcon:ImageVector, val unselectedIcon:ImageVector)

private val BottomNavItems = listOf(
    NavItem("home",    R.string.nav_home,    Icons.Filled.Home,          Icons.Outlined.Home),
    NavItem("matches", R.string.nav_matches, Icons.Filled.Star,          Icons.Outlined.Star),
    NavItem("rewards", R.string.nav_rewards, Icons.Filled.EmojiEvents,   Icons.Outlined.EmojiEvents),
    NavItem("games",   R.string.nav_games,   Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports)
)

private val LeagueFilters = listOf("All","IPL","T20","ODI","Test")
private val StatusFilters = listOf("All","Live","Upcoming","Completed")

data class BannerItem(val title:String="", val subtitle:String="", val colorHex:String="#D4002A", val isActive:Boolean=true)

suspend fun loadBanners(): List<BannerItem> = try {
    val snap = FirebaseFirestore.getInstance().collection("banners")
        .whereEqualTo("isActive", true).orderBy("priority").limit(5).get().await()
    snap.documents.map { doc ->
        BannerItem(
            title    = doc.getString("title")    ?: "",
            subtitle = doc.getString("subtitle") ?: "",
            colorHex = doc.getString("color")    ?: "#D4002A",
            isActive = doc.getBoolean("isActive") ?: true
        )
    }.ifEmpty { emptyList() }
} catch (_: Exception) { emptyList() }

suspend fun loadJoinedMatchIds(uid: String): Set<String> = try {
    val snap = FirebaseFirestore.getInstance().collection("joined_contests")
        .whereEqualTo("userId", uid).get().await()
    snap.documents.mapNotNull { it.getString("matchId") }.toSet()
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
    val uiState   by homeViewModel.uiState.collectAsStateWithLifecycle()
    val listState  = rememberLazyListState()
    val ptrState   = rememberPullToRefreshState()
    val snackbar   = remember { SnackbarHostState() }

    var banners       by remember { mutableStateOf<List<BannerItem>>(emptyList()) }
    var joinedMatchIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) { banners = loadBanners() }
    LaunchedEffect(userData.uid) { if (userData.uid.isNotEmpty()) joinedMatchIds = loadJoinedMatchIds(userData.uid) }

    val errorMsg = (uiState.matchState as? MatchUiState.Error)?.message
    LaunchedEffect(errorMsg) { if (!errorMsg.isNullOrBlank()) snackbar.showSnackbar(errorMsg) }

    val filteredMatches by remember(uiState) { derivedStateOf { homeViewModel.filteredMatches(uiState) } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Color(0xFFF2F3F5)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                HomeTopBar(userData=userData, selectedSport=uiState.selectedSport, onSportSelected=homeViewModel::selectSport, onProfileClick=onProfileClick, onWalletClick=onWalletClick)
                PullToRefreshBox(isRefreshing=uiState.isRefreshing, onRefresh=homeViewModel::refresh, state=ptrState, modifier=Modifier.weight(1f)) {
                    when (uiState.matchState) {
                        is MatchUiState.Loading -> CardSkeletonList()
                        else -> {
                            LazyColumn(state=listState, contentPadding=PaddingValues(bottom=80.dp)) {
                                item(key="search") { SearchSection(query=uiState.searchQuery, onChange=homeViewModel::updateSearch, onClear=homeViewModel::clearSearch) }
                                item(key="banner") { if (banners.isNotEmpty()) DynamicBannerCarousel(banners=banners, currentIndex=uiState.bannerIndex) else StaticBannerCarousel(currentIndex=uiState.bannerIndex) }
                                item(key="wallet") { WalletQuickBar(userData=userData, onWalletClick=onWalletClick) }
                                item(key="filters") { FilterChips(items=LeagueFilters, selected=uiState.selectedLeague, onSelect=homeViewModel::selectLeague, modifier=Modifier.padding(vertical=4.dp)) }
                                item(key="status") { StatusFilterTabs(selected=uiState.selectedFilter, onSelect=homeViewModel::selectFilter) }
                                if (filteredMatches.isEmpty()) {
                                    item(key="empty") { HomeEmptyState(onRefresh=homeViewModel::refresh) }
                                } else {
                                    items(filteredMatches, key={it.id}) { match ->
                                        MatchCard(match=match, isJoined=joinedMatchIds.contains(match.id), onClick={
                                            val t1 = match.teamInfo?.getOrNull(0)?.shortname?.ifEmpty{match.teams.getOrElse(0){"T1"}.take(3)} ?: match.teams.getOrElse(0){"T1"}.take(3)
                                            val t2 = match.teamInfo?.getOrNull(1)?.shortname?.ifEmpty{match.teams.getOrElse(1){"T2"}.take(3)} ?: match.teams.getOrElse(1){"T2"}.take(3)
                                            onMatchClick(MatchData(
                                                id=match.id, team1=t1.uppercase(), team2=t2.uppercase(),
                                                team1Full=match.teams.getOrElse(0){"Team 1"}, team2Full=match.teams.getOrElse(1){"Team 2"},
                                                team1Logo=match.t1LogoUrl, team2Logo=match.t2LogoUrl,
                                                type=match.matchType, league=match.name, matchTime=match.date,
                                                hoursLeft=getHoursLeft(match.date), minutesLeft=getMinutesLeft(match.date),
                                                prize=match.prizePool ?: "₹50 Crores", spots=match.totalSpots ?: "50,000",
                                                fillPercent=match.filledSpots ?: 0, badge=match.badge ?: "",
                                                team1Color=Color(0xFF003366), team2Color=Color(0xFF006600)
                                            ))
                                        })
                                    }
                                }
                                if (userData.isAdmin || userData.uid == "1irz1sRJ3QNeEtUuN70OSWiUBdq2") {
                                    item(key="admin") { AdminButton(onClick=onAdminClick) }
                                }
                            }
                        }
                    }
                }
            }
            BottomNav(currentTab=currentTab, onTabChange=onTabChange, modifier=Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun HomeTopBar(userData: UserData, selectedSport: String, onSportSelected: (String) -> Unit, onProfileClick: () -> Unit, onWalletClick: () -> Unit) {
    val sportTabs = listOf("Cricket")
    Surface(color=Color(0xFF111111), shadowElevation=8.dp) {
        Column {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=16.dp,vertical=10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Brush.linearGradient(listOf(D11Red,Color(0xFFB71C1C)))).clickable{onProfileClick()}, Alignment.Center) {
                        Text((userData.name.firstOrNull()?:'P').toString().uppercase(), color=D11White, fontSize=15.sp, fontWeight=FontWeight.ExtraBold)
                    }
                    Text(stringResource(R.string.app_name), color=D11White, fontSize=20.sp, fontWeight=FontWeight.ExtraBold, letterSpacing=1.sp)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF2A2A2A)).clickable{}, Alignment.Center) { Icon(Icons.Outlined.Notifications, null, tint=D11White, modifier=Modifier.size(18.dp)) }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF2A2A2A)).clickable{}, Alignment.Center) { Icon(Icons.Filled.CardGiftcard, null, tint=D11Yellow, modifier=Modifier.size(18.dp)) }
                    Surface(shape=RoundedCornerShape(20.dp), color=Color(0xFF2A2A2A), modifier=Modifier.clickable{onWalletClick()}) {
                        Row(Modifier.padding(horizontal=12.dp,vertical=7.dp), horizontalArrangement=Arrangement.spacedBy(5.dp), verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalanceWallet, null, tint=D11Green, modifier=Modifier.size(14.dp))
                            Text("₹${userData.balance}", color=D11Green, fontSize=13.sp, fontWeight=FontWeight.ExtraBold)
                        }
                    }
                }
            }
            LazyRow(contentPadding=PaddingValues(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(4.dp), modifier=Modifier.padding(bottom=4.dp)) {
                items(sportTabs) { sport ->
                    Column(Modifier.clickable(indication=null,interactionSource=remember{MutableInteractionSource()},onClick={onSportSelected(sport)}).padding(horizontal=10.dp,vertical=6.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(sport, color=if(sport==selectedSport)D11White else Color(0xFF888888), fontSize=13.sp, fontWeight=if(sport==selectedSport)FontWeight.ExtraBold else FontWeight.Normal)
                        if(sport==selectedSport){Spacer(Modifier.height(3.dp));Box(Modifier.width(28.dp).height(2.dp).clip(CircleShape).background(D11Red))}
                    }
                }
            }
        }
    }
}

@Composable
fun WalletQuickBar(userData: UserData, onWalletClick: () -> Unit) {
    Card(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp).clickable{onWalletClick()}, colors=CardDefaults.cardColors(Color(0xFF111111)), shape=RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            listOf(Triple("Balance","₹${userData.balance}",D11Green), Triple("Winnings","₹${userData.winnings}",D11Yellow), Triple("Bonus","₹${userData.bonusBalance}",Color(0xFF82B1FF))).forEach { (label,value,color) ->
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(value, color=color, fontSize=14.sp, fontWeight=FontWeight.ExtraBold)
                    Text(label, color=Color(0xFF888888), fontSize=10.sp)
                }
            }
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(D11Red).padding(horizontal=12.dp,vertical=6.dp)) {
                Text("Add Cash", color=D11White, fontSize=12.sp, fontWeight=FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun DynamicBannerCarousel(banners: List<BannerItem>, currentIndex: Int) {
    val idx = currentIndex % banners.size
    val item = banners[idx]
    val color = try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch(_:Exception){ D11Red }
    Box(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp).shadow(6.dp,RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(color,color.copy(alpha=0.7f)))).height(96.dp), Alignment.Center) {
        Box(Modifier.align(Alignment.CenterEnd).offset(x=30.dp).size(130.dp).background(Color.White.copy(alpha=0.07f),CircleShape))
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text(item.title, color=D11White, fontSize=19.sp, fontWeight=FontWeight.ExtraBold, letterSpacing=0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.subtitle, color=Color(0xDDFFFFFF), fontSize=13.sp)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom=10.dp), horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            banners.indices.forEach { i -> Box(Modifier.size(if(i==idx)22.dp else 6.dp,5.dp).clip(CircleShape).background(if(i==idx)D11White else Color(0x55FFFFFF))) }
        }
    }
}

@Composable
fun StaticBannerCarousel(currentIndex: Int) {
    val items = listOf(Triple("MEGA CONTEST","Win ₹50 Crores",D11Red),Triple("IPL 2026","Play & Win Big",Color(0xFF1565C0)),Triple("FREE CONTEST","No Entry Fee",D11Green))
    val idx = currentIndex % items.size
    val (title,sub,color) = items[idx]
    Box(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp).shadow(6.dp,RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(color,color.copy(alpha=0.7f)))).height(96.dp), Alignment.Center) {
        Box(Modifier.align(Alignment.CenterEnd).offset(x=30.dp).size(130.dp).background(Color.White.copy(alpha=0.07f),CircleShape))
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text(title, color=D11White, fontSize=19.sp, fontWeight=FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(sub, color=Color(0xDDFFFFFF), fontSize=13.sp)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom=10.dp), horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            items.indices.forEach { i -> Box(Modifier.size(if(i==idx)22.dp else 6.dp,5.dp).clip(CircleShape).background(if(i==idx)D11White else Color(0x55FFFFFF))) }
        }
    }
}

@Composable
fun SearchSection(query: String, onChange: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp), RoundedCornerShape(12.dp), D11White, shadowElevation=2.dp) {
        Row(Modifier.padding(horizontal=14.dp,vertical=12.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint=Color(0xFFAAAAAA), modifier=Modifier.size(20.dp))
            BasicTextField(value=query, onValueChange=onChange, modifier=Modifier.weight(1f), textStyle=TextStyle(color=Color(0xFF111111),fontSize=14.sp), singleLine=true,
                decorationBox={inner->if(query.isEmpty())Text(stringResource(R.string.search_hint),color=Color(0xFFBBBBBB),fontSize=14.sp);inner()})
            AnimatedVisibility(visible=query.isNotEmpty()){Icon(Icons.Filled.Close,null,tint=Color(0xFFAAAAAA),modifier=Modifier.size(18.dp).clickable{onClear()})}
        }
    }
}

@Composable
fun FilterChips(items: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(contentPadding=PaddingValues(horizontal=16.dp), horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=modifier) {
        items(items) { item ->
            val isSel = item == selected
            Surface(shape=RoundedCornerShape(20.dp), color=if(isSel)D11Red else D11White, shadowElevation=if(isSel)3.dp else 1.dp,
                modifier=Modifier.clickable{onSelect(item)}.then(if(!isSel)Modifier.border(1.dp,Color(0xFFDDDDDD),RoundedCornerShape(20.dp)) else Modifier)) {
                Text(item, color=if(isSel)D11White else Color(0xFF444444), fontSize=13.sp, fontWeight=if(isSel)FontWeight.ExtraBold else FontWeight.Medium, modifier=Modifier.padding(horizontal=18.dp,vertical=8.dp))
            }
        }
    }
}

@Composable
fun StatusFilterTabs(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color=D11White, modifier=modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp), horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                StatusFilters.forEach { f ->
                    val isSel = f == selected
                    Column(Modifier.clickable(indication=null,interactionSource=remember{MutableInteractionSource()},onClick={onSelect(f)}), horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(f, color=if(isSel)D11Red else Color(0xFF777777), fontSize=13.sp, fontWeight=if(isSel)FontWeight.ExtraBold else FontWeight.Normal)
                        Spacer(Modifier.height(4.dp))
                        AnimatedVisibility(visible=isSel){Box(Modifier.width(30.dp).height(2.dp).clip(CircleShape).background(D11Red))}
                    }
                }
            }
            HorizontalDivider(color=Color(0xFFEEEEEE))
        }
    }
}

@Composable
fun MatchCard(match: CricMatch, isJoined: Boolean = false, onClick: () -> Unit) {
    val t1Info  = match.teamInfo?.getOrNull(0)
    val t2Info  = match.teamInfo?.getOrNull(1)
    val t1Short = t1Info?.shortname?.ifEmpty{match.teams.getOrElse(0){"T1"}.take(3)} ?: match.teams.getOrElse(0){"T1"}.take(3)
    val t2Short = t2Info?.shortname?.ifEmpty{match.teams.getOrElse(1){"T2"}.take(3)} ?: match.teams.getOrElse(1){"T2"}.take(3)
    val isLive      = match.matchStarted && !match.matchEnded
    val isCompleted = match.matchEnded
    val hoursLeft   = getHoursLeft(match.date)
    val minutesLeft = getMinutesLeft(match.date)
    val inf = rememberInfiniteTransition(label="live_${match.id}")
    val dotAlpha by inf.animateFloat(0.25f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="dot_${match.id}")
    val badgeColor = when(match.badge){"FREE"->D11Green;"GUARANTEED"->Color(0xFF7B1FA2);else->D11Red}

    Card(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp).clickable(onClick=onClick),
        colors=CardDefaults.cardColors(D11White), shape=RoundedCornerShape(16.dp), elevation=CardDefaults.cardElevation(3.dp),
        border=if(isLive)BorderStroke(1.dp,Color(0xFFFFCDD2)) else null) {
        Column {
            Row(Modifier.fillMaxWidth().background(if(isLive)Color(0xFFFFF5F5) else Color(0xFFF7F8FA)).padding(horizontal=14.dp,vertical=9.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.EmojiEvents, null, tint=D11Yellow, modifier=Modifier.size(14.dp))
                    Text("${match.matchType} • ${match.name.take(25)}", color=Color(0xFF555555), fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                }
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    if(isJoined) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF003300)).padding(horizontal=6.dp,vertical=2.dp)){Text("Joined",color=D11Green,fontSize=9.sp,fontWeight=FontWeight.ExtraBold)}
                    when {
                        isLive      -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){Box(Modifier.size(7.dp).clip(CircleShape).alpha(dotAlpha).background(D11Red));Text("LIVE",color=D11Red,fontSize=12.sp,fontWeight=FontWeight.ExtraBold)}
                        isCompleted -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){Icon(Icons.Filled.CheckCircle,null,tint=Color(0xFF888888),modifier=Modifier.size(13.dp));Text("Completed",color=Color(0xFF888888),fontSize=12.sp)}
                        hoursLeft > 0 || minutesLeft > 0 -> CountdownTimer(hoursLeft=hoursLeft, minutesLeft=minutesLeft)
                        else        -> Text("Starting soon",color=D11Yellow,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                HomeTeamBlock(t1Short,match.teams.getOrElse(0){t1Short},match.t1LogoUrl,if(isLive)match.score?.getOrNull(0) else null,Brush.radialGradient(listOf(Color(0xFF1E88E5),Color(0xFF003366))),false)
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF0F0F0)),Alignment.Center){Text("vs",color=Color(0xFF999999),fontSize=11.sp,fontWeight=FontWeight.ExtraBold)}
                HomeTeamBlock(t2Short,match.teams.getOrElse(1){t2Short},match.t2LogoUrl,if(isLive)match.score?.getOrNull(1) else null,Brush.radialGradient(listOf(Color(0xFF43A047),Color(0xFF006600))),true)
            }
            if(match.status.isNotEmpty()) Text(match.status,color=if(isLive)D11Red else Color(0xFF777777),fontSize=11.sp,fontWeight=if(isLive)FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=16.dp).padding(bottom=10.dp))
            HorizontalDivider(color=Color(0xFFF0F0F0))
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(!match.badge.isNullOrEmpty()) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(badgeColor.copy(alpha=0.12f)).padding(horizontal=7.dp,vertical=3.dp)){Text(match.badge!!,color=badgeColor,fontSize=10.sp,fontWeight=FontWeight.ExtraBold)}
                    Column {
                        Text(match.prizePool ?: "₹50 Crores",color=Color(0xFF111111),fontSize=15.sp,fontWeight=FontWeight.ExtraBold)
                        Text(stringResource(R.string.label_prize_pool),color=Color(0xFF999999),fontSize=10.sp)
                    }
                }
                when {
                    isCompleted -> OutlinedButton(onClick=onClick,border=BorderStroke(1.dp,Color(0xFFCCCCCC)),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=18.dp,vertical=9.dp)){Text(stringResource(R.string.btn_view_results),color=Color(0xFF666666),fontSize=13.sp)}
                    else        -> Button(onClick=onClick,colors=ButtonDefaults.buttonColors(D11Red),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=22.dp,vertical=9.dp),elevation=ButtonDefaults.buttonElevation(3.dp)){Text(if(isLive)stringResource(R.string.btn_join_now) else stringResource(R.string.btn_play),color=D11White,fontWeight=FontWeight.ExtraBold,fontSize=14.sp)}
                }
            }
            val fillFraction = (match.filledSpots ?: 0) / 100f
            val fillColor = when{fillFraction>0.85f->Color(0xFFE53935);fillFraction>0.6f->D11Yellow;else->D11Green}
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFEEEEEE))){Box(Modifier.fillMaxWidth(fillFraction).height(4.dp).background(Brush.horizontalGradient(listOf(fillColor,fillColor.copy(alpha=0.7f)))))}
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp), Arrangement.SpaceBetween) {
                Text("${match.filledSpots ?: 0}% ${stringResource(R.string.label_full)}",color=Color(0xFF999999),fontSize=11.sp)
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(3.dp)){Icon(Icons.Filled.Group,null,tint=Color(0xFFBBBBBB),modifier=Modifier.size(12.dp));Text(match.totalSpots ?: "50,000",color=Color(0xFF999999),fontSize=11.sp)}
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFFF7F8FA)).padding(horizontal=16.dp,vertical=6.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                Icon(Icons.Filled.ConfirmationNumber,null,tint=Color(0xFF999999),modifier=Modifier.size(12.dp))
                Text("${stringResource(R.string.label_entry)}: ${match.entryFee ?: "₹49"}",color=Color(0xFF777777),fontSize=11.sp)
            }
        }
    }
}

@Composable
private fun HomeTeamBlock(shortName:String, fullName:String, logoUrl:String, score:Score?, gradient:Brush, rtl:Boolean) {
    Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        if(!rtl){HomeTeamLogo(shortName,logoUrl,gradient);HomeTeamText(shortName,fullName,score,Alignment.Start)}
        else{HomeTeamText(shortName,fullName,score,Alignment.End);HomeTeamLogo(shortName,logoUrl,gradient)}
    }
}

@Composable
private fun HomeTeamLogo(shortName:String, logoUrl:String, gradient:Brush) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(gradient).border(2.dp,Color(0xFFEEEEEE),CircleShape), Alignment.Center) {
        if(logoUrl.isNotBlank()) SubcomposeAsyncImage(model=logoUrl,contentDescription=shortName,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize().clip(CircleShape),
            loading={Text(shortName.take(3).uppercase(),color=D11White,fontSize=11.sp,fontWeight=FontWeight.ExtraBold)},
            error={Text(shortName.take(3).uppercase(),color=D11White,fontSize=11.sp,fontWeight=FontWeight.ExtraBold)})
        else Text(shortName.take(3).uppercase(),color=D11White,fontSize=11.sp,fontWeight=FontWeight.ExtraBold)
    }
}

@Composable
private fun HomeTeamText(shortName:String, fullName:String, score:Score?, alignment:Alignment.Horizontal) {
    Column(horizontalAlignment=alignment) {
        Text(shortName.uppercase(),color=Color(0xFF111111),fontSize=20.sp,fontWeight=FontWeight.ExtraBold)
        Text(fullName.take(18),color=Color(0xFF999999),fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        score?.let{s->Text("${s.r}/${s.w} (${s.o})",color=Color(0xFF111111),fontSize=13.sp,fontWeight=FontWeight.Bold)}
    }
}

@Composable
fun BottomNav(currentTab:String, onTabChange:(String)->Unit, modifier:Modifier=Modifier) {
    Surface(color=D11White, shadowElevation=16.dp, modifier=modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color=Color(0xFFEEEEEE))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical=8.dp), Arrangement.SpaceEvenly) {
                BottomNavItems.forEach { item ->
                    val isSel = currentTab==item.key
                    Column(Modifier.clickable(indication=null,interactionSource=remember{MutableInteractionSource()},onClick={onTabChange(item.key)}).padding(horizontal=14.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if(isSel)D11Red.copy(alpha=0.12f) else Color.Transparent), Alignment.Center) {
                            Icon(if(isSel)item.selectedIcon else item.unselectedIcon,stringResource(item.labelRes),tint=if(isSel)D11Red else Color(0xFF888888),modifier=Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(item.labelRes),color=if(isSel)D11Red else Color(0xFF999999),fontSize=10.sp,fontWeight=if(isSel)FontWeight.ExtraBold else FontWeight.Normal,textAlign=TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun CardSkeletonList() {
    val transition = rememberInfiniteTransition(label="skeleton")
    val shimmerX by transition.animateFloat(-300f,1000f,infiniteRepeatable(tween(1200,easing=LinearEasing)),label="shimmerX")
    val brush = Brush.horizontalGradient(listOf(Color(0xFFE0E0E0),Color(0xFFF5F5F5),Color(0xFFE0E0E0)),startX=shimmerX,endX=shimmerX+600f)
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),userScrollEnabled=false) {
        items(3) {
            Card(modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(D11White), elevation=CardDefaults.cardElevation(3.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(36.dp).background(brush))
                    Spacer(Modifier.height(1.dp))
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).clip(CircleShape).background(brush));Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Box(Modifier.width(48.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush));Box(Modifier.width(80.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))}}
                        Box(Modifier.size(36.dp).clip(CircleShape).background(brush))
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(6.dp)){Box(Modifier.width(48.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush));Box(Modifier.width(80.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))};Box(Modifier.size(48.dp).clip(CircleShape).background(brush))}
                    }
                    HorizontalDivider(color=Color(0xFFF0F0F0))
                    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),Arrangement.SpaceBetween,Alignment.CenterVertically){Column(verticalArrangement=Arrangement.spacedBy(5.dp)){Box(Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush));Box(Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))};Box(Modifier.width(80.dp).height(36.dp).clip(RoundedCornerShape(10.dp)).background(brush))}
                    Box(Modifier.fillMaxWidth().height(4.dp).background(brush))
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyState(onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical=64.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("🏏",fontSize=48.sp)
        Text("No matches available",color=Color(0xFF888888),fontSize=16.sp,fontWeight=FontWeight.Bold)
        Text("Pull down to refresh",color=Color(0xFFBBBBBB),fontSize=13.sp)
        Spacer(Modifier.height(4.dp))
        Button(onClick=onRefresh,colors=ButtonDefaults.buttonColors(D11Red),shape=RoundedCornerShape(10.dp)){
            Icon(Icons.Filled.Refresh,null,modifier=Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.btn_refresh),color=D11White,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminButton(onClick: () -> Unit) {
    Button(onClick=onClick,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),colors=ButtonDefaults.buttonColors(Color(0xFF212121)),shape=RoundedCornerShape(10.dp)) {
        Icon(Icons.Filled.AdminPanelSettings,null,tint=D11White,modifier=Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.btn_admin),color=D11White,fontWeight=FontWeight.Bold)
    }
}
