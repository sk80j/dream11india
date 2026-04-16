package com.example.dream11india

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@Composable
fun LeaderboardScreen(
    matchTitle: String = "CSK vs RCB",
    matchId:    String = "",
    onBack:     () -> Unit = {}
) {
    val db  = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var entries   by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var prizeMap  by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    LaunchedEffect(matchId) {
        try {
            // Find contest for this match
            val contestSnap = db.collection("contests")
                .whereEqualTo("matchId", matchId).limit(1).get().await()

            if (!contestSnap.isEmpty) {
                val cid = contestSnap.documents.first().id
                // Load prize structure
                val contestDoc = contestSnap.documents.first()
                @Suppress("UNCHECKED_CAST")
                val prizes = contestDoc.get("prizes") as? Map<String, String> ?: emptyMap()
                prizeMap = prizes.mapKeys { it.key.toIntOrNull() ?: 0 }

                // Real-time leaderboard
                db.collection("contests").document(cid)
                    .collection("leaderboard")
                    .orderBy("points", Query.Direction.DESCENDING)
                    .limit(100)
                    .addSnapshotListener { snap, _ ->
                        snap?.let {
                            entries = it.documents.mapIndexed { idx, doc ->
                                LeaderboardEntry(
                                    uid           = doc.id,
                                    name          = doc.getString("name") ?: "Player ${idx+1}",
                                    points        = doc.getDouble("points")?.toFloat() ?: 0f,
                                    rank          = idx + 1,
                                    isCurrentUser = doc.id == uid
                                )
                            }
                            isLoading = false
                        }
                    }
            } else {
                isLoading = false
            }
        } catch (_: Exception) { isLoading = false }
    }

    val myEntry = entries.find { it.isCurrentUser }
    val top3    = entries.take(3)

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        // Top bar
        Box(Modifier.fillMaxWidth().background(D11Red).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = D11White, modifier = Modifier.size(17.dp))
                    }
                    Column {
                        Text("Leaderboard", color = D11White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(matchTitle, color = Color(0xFFFFCDD2), fontSize = 12.sp)
                    }
                }
                Text("${entries.size} Teams", color = D11White, fontSize = 13.sp)
            }
        }

        // My rank bar
        myEntry?.let { me ->
            Box(Modifier.fillMaxWidth().background(Color(0xFF1A1500)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(D11Red), Alignment.Center) {
                            Text("#${me.rank}", color = D11White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Your Rank", color = D11Gray, fontSize = 11.sp)
                            Text(String.format("%.1f", me.points) + " pts", color = D11Yellow, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Prize", color = D11Gray, fontSize = 11.sp)
                        Text(prizeMap[me.rank] ?: "—", color = D11Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = D11Red) }
            return@Column
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏆", fontSize = 40.sp)
                    Text("No entries yet", color = Color(0xFF888888), fontSize = 14.sp)
                    Text("Be the first to join!", color = Color(0xFF666666), fontSize = 12.sp)
                }
            }
            return@Column
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            // Podium
            if (top3.isNotEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().background(Color(0xFF0D0D2E)).padding(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Top Winners", color = D11White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.Bottom) {
                                if (top3.size > 1) PodiumCard(top3[1], 100.dp, Color(0xFFC0C0C0), prizeMap[2] ?: "")
                                PodiumCard(top3[0], 130.dp, D11Yellow, prizeMap[1] ?: "")
                                if (top3.size > 2) PodiumCard(top3[2], 80.dp, Color(0xFFCD7F32), prizeMap[3] ?: "")
                            }
                        }
                    }
                }
            }
            // Header
            item {
                Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 8.dp), Arrangement.SpaceBetween) {
                    Text("All Teams (${entries.size})", color = D11White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Text("Points", color = D11Gray, fontSize = 12.sp)
                        Text("Rank", color = D11Gray, fontSize = 12.sp)
                    }
                }
            }
            // List
            items(entries, key = { it.uid }) { entry ->
                LeaderboardRow(entry = entry, prize = prizeMap[entry.rank] ?: "")
                HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun PodiumCard(entry: LeaderboardEntry, height: Dp, color: Color, prize: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(color), Alignment.Center) {
            Text(entry.name.take(2).uppercase(), color = D11Black, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(4.dp))
        Text(entry.name.take(10), color = D11White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(String.format("%.1f", entry.points), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (prize.isNotEmpty()) Text(prize, color = D11Green, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(80.dp).height(height).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(color.copy(alpha = 0.3f)), Alignment.Center) {
            Text("#${entry.rank}", color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, prize: String) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (entry.isCurrentUser) Color(0xFF1A1500) else Color(0xFF0A0A0A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(
                when (entry.rank) { 1 -> D11Yellow; 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> if (entry.isCurrentUser) D11Red else Color(0xFF2A2A2A) }
            ), Alignment.Center) {
                Text(if (entry.rank <= 3) "${entry.rank}" else entry.name.take(2).uppercase(),
                    color = if (entry.rank <= 3) D11Black else D11White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (entry.isCurrentUser) "You" else entry.name, color = if (entry.isCurrentUser) D11Yellow else D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (entry.isCurrentUser) Box(Modifier.clip(RoundedCornerShape(4.dp)).background(D11Red).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("YOU", color = D11White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                }
                if (prize.isNotEmpty()) Text(prize, color = D11Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(String.format("%.1f", entry.points), color = D11White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("#${entry.rank}", color = if (entry.isCurrentUser) D11Yellow else D11Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
