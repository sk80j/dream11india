package com.example.dream11india

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ===== POINTS RULES =====
object PointsCalculator {

    fun calculateBattingPoints(
        runs: Int,
        fours: Int = 0,
        sixes: Int = 0,
        isOut: Boolean = true,
        ballsFaced: Int = 0
    ): Float {
        var points = 0f
        points += runs * 1f
        points += fours * 1f
        points += sixes * 2f
        if (runs >= 100) points += 16f
        else if (runs >= 50) points += 8f
        else if (runs >= 30) points += 4f
        if (runs == 0 && isOut && ballsFaced > 0) points -= 2f
        // Strike rate bonus (if balls faced > 10)
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
        wickets: Int,
        runsConceded: Int = 0,
        oversBowled: Float = 0f,
        maidens: Int = 0
    ): Float {
        var points = 0f
        points += wickets * 25f
        if (wickets >= 5) points += 16f
        else if (wickets >= 4) points += 12f
        else if (wickets >= 3) points += 8f
        points += maidens * 4f
        // Economy rate bonus (if overs >= 2)
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
        catches: Int = 0,
        stumpings: Int = 0,
        runOuts: Int = 0
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
            val pts = p.totalPoints
            total += when (p.playerId) {
                captainId -> pts * 2f
                viceCaptainId -> pts * 1.5f
                else -> pts
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

data class TeamResult(
    val teamId: String = "",
    val userId: String = "",
    val userName: String = "",
    val totalPoints: Float = 0f,
    val rank: Int = 0,
    val captainId: String = "",
    val viceCaptainId: String = ""
)

// ===== FIRESTORE POINTS MANAGER =====
object PointsManager {
    private val db = FirebaseFirestore.getInstance()

    // Save player stats to Firestore
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
            val updatedPlayer = player.copy(totalPoints = points)
            val ref = db.collection("matchStats")
                .document(matchId)
                .collection("players")
                .document(player.playerId)
            batch.set(ref, mapOf(
                "playerId" to updatedPlayer.playerId,
                "playerName" to updatedPlayer.playerName,
                "team" to updatedPlayer.team,
                "role" to updatedPlayer.role,
                "runs" to updatedPlayer.runs,
                "fours" to updatedPlayer.fours,
                "sixes" to updatedPlayer.sixes,
                "wickets" to updatedPlayer.wickets,
                "catches" to updatedPlayer.catches,
                "stumpings" to updatedPlayer.stumpings,
                "runOuts" to updatedPlayer.runOuts,
                "totalPoints" to updatedPlayer.totalPoints
            ))
        }
        batch.commit().await()
    }

    // Get player points from Firestore
    suspend fun getPlayerPoints(matchId: String): List<PlayerPoints> {
        val snap = db.collection("matchStats")
            .document(matchId)
            .collection("players")
            .get().await()
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

    // Update team points
    suspend fun updateTeamPoints(matchId: String, contestId: String) {
        val playerStats = getPlayerPoints(matchId)
        val playerPointsMap = playerStats.associateBy { it.playerId }

        val joinedSnap = db.collection("joined_contests")
            .whereEqualTo("matchId", matchId)
            .whereEqualTo("contestId", contestId)
            .get().await()

        val batch = db.batch()
        joinedSnap.documents.forEach { doc ->
            val captainId = doc.getString("captainId") ?: ""
            val viceCaptainId = doc.getString("viceCaptainId") ?: ""
            val playerIds = (doc.get("playerIds") as? List<String>) ?: emptyList()

            val teamPlayers = playerIds.mapNotNull { playerPointsMap[it] }
            val totalPoints = PointsCalculator.calculateTeamPoints(
                teamPlayers, captainId, viceCaptainId)

            batch.update(doc.reference, "points", totalPoints)
        }
        batch.commit().await()

        // Update rankings
        updateRankings(contestId)
    }

    // Update rankings
    private suspend fun updateRankings(contestId: String) {
        val snap = db.collection("joined_contests")
            .whereEqualTo("contestId", contestId)
            .get().await()

        val sorted = snap.documents
            .sortedByDescending { it.getDouble("points") ?: 0.0 }

        val batch = db.batch()
        sorted.forEachIndexed { index, doc ->
            batch.update(doc.reference, "rank", index + 1)
        }
        batch.commit().await()
    }

    // Get sample points for testing
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
