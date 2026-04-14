package com.example.dream11india

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// ===== POINTS RULES =====
object PointsCalculator {

    fun calculateBattingPoints(
        runs: Int, fours: Int = 0, sixes: Int = 0,
        isOut: Boolean = true, ballsFaced: Int = 0
    ): Float {
        var points = 0f
        points += runs * 1f
        points += fours * 1f
        points += sixes * 2f
        if (runs >= 100) points += 16f
        else if (runs >= 50) points += 8f
        else if (runs >= 30) points += 4f
        if (runs == 0 && isOut && ballsFaced > 0) points -= 2f
        if (ballsFaced >= 10) {
            val sr = (runs.toFloat() / ballsFaced) * 100
            when {
                sr >= 170 -> points += 6f
                sr >= 150 -> points += 4f
                sr >= 130 -> points += 2f
                sr in 60f..70f -> points -= 2f
                sr < 60 -> points -= 4f
            }
        }
        return points
    }

    fun calculateBowlingPoints(
        wickets: Int, runsConceded: Int = 0,
        oversBowled: Float = 0f, maidens: Int = 0
    ): Float {
        var points = 0f
        points += wickets * 25f
        if (wickets >= 5) points += 16f
        else if (wickets >= 4) points += 12f
        else if (wickets >= 3) points += 8f
        points += maidens * 4f
        if (oversBowled >= 2f) {
            val economy = runsConceded.toFloat() / oversBowled
            when {
                economy <= 5 -> points += 6f
                economy <= 6 -> points += 4f
                economy <= 7 -> points += 2f
                economy in 10f..11f -> points -= 2f
                economy > 11 -> points -= 4f
            }
        }
        return points
    }

    fun calculateFieldingPoints(
        catches: Int = 0, stumpings: Int = 0, runOuts: Int = 0
    ): Float {
        var points = 0f
        points += catches * 8f
        points += stumpings * 12f
        points += runOuts * 6f
        if (catches >= 3) points += 4f
        return points
    }

    fun calculateTotalPoints(
        runs: Int = 0, fours: Int = 0, sixes: Int = 0,
        isOut: Boolean = true, ballsFaced: Int = 0,
        wickets: Int = 0, runsConceded: Int = 0,
        oversBowled: Float = 0f, maidens: Int = 0,
        catches: Int = 0, stumpings: Int = 0, runOuts: Int = 0
    ): Float {
        return calculateBattingPoints(runs, fours, sixes, isOut, ballsFaced) +
                calculateBowlingPoints(wickets, runsConceded, oversBowled, maidens) +
                calculateFieldingPoints(catches, stumpings, runOuts)
    }

    fun calculateTeamPoints(
        players: List<PlayerPoints>,
        captainId: String,
        viceCaptainId: String
    ): Float {
        var total = 0f
        players.forEach { p ->
            total += when(p.playerId) {
                captainId -> p.totalPoints * 2f
                viceCaptainId -> p.totalPoints * 1.5f
                else -> p.totalPoints
            }
        }
        return total
    }
}

// ===== DATA MODELS =====
data class PlayerPoints(
    val playerId: String = "",
    val playerName: String = "",
    val team: String = "",
    val role: String = "",
    val runs: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val wickets: Int = 0,
    val catches: Int = 0,
    val stumpings: Int = 0,
    val runOuts: Int = 0,
    val ballsFaced: Int = 0,
    val runsConceded: Int = 0,
    val oversBowled: Float = 0f,
    val maidens: Int = 0,
    val isOut: Boolean = true,
    val totalPoints: Float = 0f
)

// ===== SAFE PRIZE DISTRIBUTOR =====
object PrizeDistributor {
    private val db = FirebaseFirestore.getInstance()

    // STEP 1: Check guards
    // STEP 2: Fetch entries sorted
    // STEP 3: Rank + prize batch
    // STEP 4: Wallet transaction (separate loop)
    // STEP 5: Mark distributed
    fun distributeContestPrizes(contestId: String) {
        val contestRef = db.collection("contests").document(contestId)

        // GUARD: check match ended + not already distributed
        contestRef.get().addOnSuccessListener { contestDoc ->
            if (!contestDoc.exists()) return@addOnSuccessListener

            val isDistributed = contestDoc.getBoolean("isDistributed") ?: false
            val isMatchEnded = contestDoc.getBoolean("isMatchEnded") ?: false

            if (isDistributed) return@addOnSuccessListener
            if (!isMatchEnded) return@addOnSuccessListener

            // Parse prize breakup safely
            val rawBreakup = contestDoc.get("prizeBreakup")
            val prizeBreakup: List<Map<String, Any>> = try {
                @Suppress("UNCHECKED_CAST")
                rawBreakup as? List<Map<String, Any>> ?: emptyList()
            } catch (e: Exception) { emptyList() }

            // Fetch entries sorted by points
            db.collection("contest_entries")
                .whereEqualTo("contestId", contestId)
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snap ->
                    if (snap.isEmpty) return@addOnSuccessListener

                    val docs = snap.documents

                    // Handle ties — same points = same rank
                    val rankMap = mutableMapOf<String, Int>()
                    var currentRank = 1
                    var prevPoints = -1.0

                    docs.forEachIndexed { index, doc ->
                        val pts = doc.getDouble("points") ?: 0.0
                        if (pts != prevPoints) {
                            currentRank = index + 1
                            prevPoints = pts
                        }
                        rankMap[doc.id] = currentRank
                    }

                    // Calculate prize per doc
                    val prizeMap = mutableMapOf<String, Long>()
                    rankMap.forEach { (docId, rank) ->
                        val prize = prizeBreakup.find { p ->
                            val from = (p["rankFrom"] as? Long)?.toInt() ?: 0
                            val to = (p["rankTo"] as? Long)?.toInt() ?: 0
                            rank in from..to
                        }
                        prizeMap[docId] = (prize?.get("amount") as? Long) ?: 0L
                    }

                    // STEP 3: Batch update ranks + prizes in contest_entries
                    val batch = db.batch()
                    docs.forEach { doc ->
                        val rank = rankMap[doc.id] ?: 0
                        val prize = prizeMap[doc.id] ?: 0L
                        batch.update(doc.reference, mapOf(
                            "rank" to rank,
                            "winningAmount" to prize
                        ))
                    }
                    // Mark contest as distributed
                    batch.update(contestRef, mapOf(
                        "isDistributed" to true,
                        "distributedAt" to System.currentTimeMillis()
                    ))

                    batch.commit().addOnSuccessListener {
                        // STEP 4: Wallet credit — separate safe transactions
                        docs.forEach { doc ->
                            val prize = prizeMap[doc.id] ?: 0L
                            val userId = doc.getString("userId") ?: ""
                            if (prize > 0 && userId.isNotEmpty()) {
                                creditWalletSafe(userId, prize, "Won prize - Rank #${rankMap[doc.id]}")
                            }
                        }
                    }.addOnFailureListener { e ->
                        android.util.Log.e("PrizeDistributor", "Batch failed: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrizeDistributor", "Fetch failed: ${e.message}")
                }
        }
    }

    // SAFE wallet credit using transaction
    private fun creditWalletSafe(userId: String, amount: Long, description: String) {
        val userRef = db.collection("users").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (!snapshot.exists()) return@runTransaction
            val currentBalance = snapshot.getLong("balance") ?: 0L
            val currentWinnings = snapshot.getLong("winnings") ?: 0L
            transaction.update(userRef, mapOf(
                "balance" to currentBalance + amount,
                "winnings" to currentWinnings + amount
            ))
        }.addOnSuccessListener {
            // Log transaction record
            db.collection("transactions").add(mapOf(
                "userId" to userId,
                "type" to "credit",
                "amount" to amount,
                "description" to description,
                "timestamp" to System.currentTimeMillis(),
                "status" to "completed"
            ))
        }.addOnFailureListener { e ->
            android.util.Log.e("PrizeDistributor", "Wallet credit failed for $userId: ${e.message}")
        }
    }

    // Update team points + contest entries
    fun updateContestPoints(matchId: String, playerStats: List<PlayerPoints>) {
        val pointsMap = playerStats.associateBy { it.playerId }

        // Get all teams for this match
        db.collection("teams")
            .whereEqualTo("matchId", matchId)
            .get()
            .addOnSuccessListener { teamsSnap ->
                teamsSnap.documents.forEach { teamDoc ->
                    val playerIds = try {
                        @Suppress("UNCHECKED_CAST")
                        teamDoc.get("players") as? List<String> ?: emptyList()
                    } catch (e: Exception) { emptyList() }

                    val captainId = teamDoc.getString("captainId") ?: ""
                    val vcId = teamDoc.getString("viceCaptainId") ?: ""
                    val teamPlayers = playerIds.mapNotNull { pointsMap[it] }
                    val totalPts = PointsCalculator.calculateTeamPoints(
                        teamPlayers, captainId, vcId)

                    // Update team points
                    teamDoc.reference.update("totalPoints", totalPts)
                        .addOnFailureListener { e ->
                            android.util.Log.e("Points", "Team update failed: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Points", "Teams fetch failed: ${e.message}")
            }

        // Update contest entries by matchId (more efficient)
        db.collection("contest_entries")
            .whereEqualTo("matchId", matchId)
            .get()
            .addOnSuccessListener { entriesSnap ->
                entriesSnap.documents.forEach { entryDoc ->
                    val teamId = entryDoc.getString("teamId") ?: ""
                    // Get team points
                    db.collection("teams").document(teamId)
                        .get()
                        .addOnSuccessListener { teamDoc ->
                            val pts = teamDoc.getDouble("totalPoints") ?: 0.0
                            entryDoc.reference.update("points", pts)
                                .addOnFailureListener { e ->
                                    android.util.Log.e("Points", "Entry update failed: ${e.message}")
                                }
                        }
                }
            }
    }

    // Sample player points for testing
    fun getSamplePlayerPoints(matchId: String): List<PlayerPoints> {
        return listOf(
            PlayerPoints("4","Virat Kohli","RCB","BAT",runs=72,fours=7,sixes=2,
                ballsFaced=48,isOut=true,
                totalPoints=PointsCalculator.calculateTotalPoints(72,7,2,true,48)),
            PlayerPoints("13","Jasprit Bumrah","MI","BOWL",wickets=3,
                runsConceded=22,oversBowled=4f,maidens=1,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    wickets=3,runsConceded=22,oversBowled=4f,maidens=1)),
            PlayerPoints("1","MS Dhoni","CSK","WK",runs=38,fours=2,sixes=3,
                catches=1,stumpings=1,ballsFaced=22,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    38,2,3,catches=1,stumpings=1,ballsFaced=22)),
            PlayerPoints("9","Ravindra Jadeja","CSK","AR",runs=28,fours=2,
                wickets=2,catches=1,oversBowled=4f,runsConceded=28,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    28,2,0,wickets=2,catches=1,oversBowled=4f,runsConceded=28)),
            PlayerPoints("5","Faf du Plessis","RCB","BAT",runs=45,fours=4,sixes=1,
                ballsFaced=32,
                totalPoints=PointsCalculator.calculateTotalPoints(45,4,1,ballsFaced=32)),
            PlayerPoints("14","Mohammed Siraj","RCB","BOWL",wickets=2,
                runsConceded=32,oversBowled=4f,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    wickets=2,runsConceded=32,oversBowled=4f)),
            PlayerPoints("6","Ruturaj Gaikwad","CSK","BAT",runs=58,fours=5,sixes=2,
                ballsFaced=42,
                totalPoints=PointsCalculator.calculateTotalPoints(58,5,2,ballsFaced=42)),
            PlayerPoints("10","Glenn Maxwell","RCB","AR",runs=32,fours=2,sixes=2,
                wickets=1,oversBowled=2f,runsConceded=18,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    32,2,2,wickets=1,oversBowled=2f,runsConceded=18)),
            PlayerPoints("15","Deepak Chahar","CSK","BOWL",wickets=1,
                runsConceded=28,oversBowled=3f,maidens=1,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    wickets=1,runsConceded=28,oversBowled=3f,maidens=1)),
            PlayerPoints("2","KL Rahul","MI","WK",runs=62,fours=5,sixes=1,
                catches=2,ballsFaced=45,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    62,5,1,catches=2,ballsFaced=45)),
            PlayerPoints("16","Josh Hazlewood","RCB","BOWL",wickets=2,
                runsConceded=24,oversBowled=4f,
                totalPoints=PointsCalculator.calculateTotalPoints(
                    wickets=2,runsConceded=24,oversBowled=4f))
        )
    }
}

// ===== POINTS MANAGER (Firestore) =====
object PointsManager {
    private val db = FirebaseFirestore.getInstance()

    suspend fun savePlayerStats(matchId: String, stats: List<PlayerPoints>) {
        val batch = db.batch()
        stats.forEach { player ->
            val points = PointsCalculator.calculateTotalPoints(
                runs = player.runs, fours = player.fours, sixes = player.sixes,
                isOut = player.isOut, ballsFaced = player.ballsFaced,
                wickets = player.wickets, runsConceded = player.runsConceded,
                oversBowled = player.oversBowled, maidens = player.maidens,
                catches = player.catches, stumpings = player.stumpings,
                runOuts = player.runOuts
            )
            val ref = db.collection("matchStats")
                .document(matchId)
                .collection("players")
                .document(player.playerId)
            batch.set(ref, mapOf(
                "playerId" to player.playerId,
                "playerName" to player.playerName,
                "team" to player.team,
                "role" to player.role,
                "runs" to player.runs,
                "fours" to player.fours,
                "sixes" to player.sixes,
                "wickets" to player.wickets,
                "catches" to player.catches,
                "stumpings" to player.stumpings,
                "runOuts" to player.runOuts,
                "totalPoints" to points
            ))
        }
        batch.commit().await()
    }

    suspend fun getPlayerPoints(matchId: String): List<PlayerPoints> {
        val snap = db.collection("matchStats")
            .document(matchId).collection("players").get().await()
        return snap.documents.map { doc ->
            PlayerPoints(
                playerId = doc.getString("playerId") ?: "",
                playerName = doc.getString("playerName") ?: "",
                team = doc.getString("team") ?: "",
                role = doc.getString("role") ?: "",
                runs = doc.getLong("runs")?.toInt() ?: 0,
                fours = doc.getLong("fours")?.toInt() ?: 0,
                sixes = doc.getLong("sixes")?.toInt() ?: 0,
                wickets = doc.getLong("wickets")?.toInt() ?: 0,
                catches = doc.getLong("catches")?.toInt() ?: 0,
                totalPoints = doc.getDouble("totalPoints")?.toFloat() ?: 0f
            )
        }
    }

    fun getSamplePlayerPoints(matchId: String): List<PlayerPoints> {
        return PrizeDistributor.getSamplePlayerPoints(matchId)
    }
}
