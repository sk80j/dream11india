package com.example.dream11india

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// ===== FCM SERVICE =====
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenSafe(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"] ?: "Dream11 India"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"] ?: ""
        val type = remoteMessage.data["type"] ?: "general"
        val notifId = remoteMessage.data["notificationId"]?.hashCode()
            ?: System.currentTimeMillis().toInt()
        NotificationHelper.showNotification(this, title, body, type, notifId)
    }

    // FIX: Use merge to avoid crash if doc missing
    private fun saveTokenSafe(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }
}

// ===== NOTIFICATION HELPER =====
object NotificationHelper {
    const val CHANNEL_MATCH = "match_channel"
    const val CHANNEL_CONTEST = "contest_channel"
    const val CHANNEL_WALLET = "wallet_channel"
    const val CHANNEL_GENERAL = "general_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            listOf(
                Triple(CHANNEL_MATCH, "Match Alerts", NotificationManager.IMPORTANCE_HIGH),
                Triple(CHANNEL_CONTEST, "Contest Alerts", NotificationManager.IMPORTANCE_HIGH),
                Triple(CHANNEL_WALLET, "Wallet Alerts", NotificationManager.IMPORTANCE_DEFAULT),
                Triple(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_LOW)
            ).forEach { (id, name, importance) ->
                val channel = NotificationChannel(id, name, importance).apply {
                    description = "Dream11 India $name"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    // FIX: Check Android 13+ permission
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: String = "general",
        notifId: Int = System.currentTimeMillis().toInt()
    ) {
        if (!hasNotificationPermission(context)) return

        val channelId = when(type) {
            "match" -> CHANNEL_MATCH
            "contest" -> CHANNEL_CONTEST
            "wallet" -> CHANNEL_WALLET
            else -> CHANNEL_GENERAL
        }

        // FIX: Proper navigation intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
            putExtra("notification_id", notifId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.notify(notifId, notification)
    }

    // FIX: Match-specific topic (not spam all)
    fun initFCM(userId: String, joinedMatchIds: List<String> = emptyList()) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            // FIX: merge instead of update
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
        // Subscribe only to relevant topics
        FirebaseMessaging.getInstance().subscribeToTopic("ipl_2026")
        // Subscribe to joined matches only
        joinedMatchIds.forEach { matchId ->
            FirebaseMessaging.getInstance().subscribeToTopic("match_$matchId")
        }
    }
}

// ===== IN-APP NOTIFICATION MANAGER =====
object InAppNotificationManager {
    private val db = FirebaseFirestore.getInstance()
    private val shownIds = mutableSetOf<String>() // FIX: track shown notifications

    // FIX: Only show new notifications using sentAt filter
    fun listenForNotifications(
        context: Context,
        userId: String,
        onNotification: (String, String) -> Unit
    ) {
        val startTime = System.currentTimeMillis()

        // Global notifications - only NEW ones
        db.collection("notifications")
            .whereEqualTo("target", "all")
            .whereGreaterThan("sentAt", startTime)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        // FIX: Duplicate check
                        if (shownIds.contains(doc.id)) return@forEach
                        shownIds.add(doc.id)

                        val title = doc.getString("title") ?: return@forEach
                        val body = doc.getString("body") ?: return@forEach
                        val type = doc.getString("type") ?: "general"
                        val notifId = doc.id.hashCode()

                        onNotification(title, body)
                        NotificationHelper.showNotification(context, title, body, type, notifId)

                        // Save to notification history
                        saveToHistory(userId, doc.id, title, body, type)
                    }
                }
            }

        // User-specific notifications - only NEW ones
        db.collection("notifications")
            .whereEqualTo("target", userId)
            .whereGreaterThan("sentAt", startTime)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        if (shownIds.contains(doc.id)) return@forEach
                        shownIds.add(doc.id)

                        val title = doc.getString("title") ?: return@forEach
                        val body = doc.getString("body") ?: return@forEach
                        val type = doc.getString("type") ?: "general"
                        val notifId = doc.id.hashCode()

                        onNotification(title, body)
                        NotificationHelper.showNotification(context, title, body, type, notifId)
                        saveToHistory(userId, doc.id, title, body, type)
                    }
                }
            }
    }

    // FIX: Save notification history
    private fun saveToHistory(
        userId: String, notifId: String,
        title: String, body: String, type: String
    ) {
        db.collection("user_notifications")
            .document(userId)
            .collection("history")
            .document(notifId)
            .set(mapOf(
                "title" to title,
                "body" to body,
                "type" to type,
                "isRead" to false,
                "receivedAt" to System.currentTimeMillis()
            ), SetOptions.merge())
    }

    // FIX: Admin only send (security handled by Firestore rules)
    fun sendNotification(
        target: String = "all",
        title: String,
        body: String,
        type: String = "general",
        matchId: String = ""
    ) {
        val data = mutableMapOf(
            "target" to target,
            "title" to title,
            "body" to body,
            "type" to type,
            "sentAt" to System.currentTimeMillis(),
            "notificationId" to "${System.currentTimeMillis()}_${target}"
        )
        if (matchId.isNotEmpty()) data["matchId"] = matchId
        db.collection("notifications").add(data)
    }

    // Pre-built types
    fun sendMatchReminder(matchTitle: String, minutesLeft: Int, matchId: String = "") {
        sendNotification(
            target = "all",
            title = "Match Starting Soon!",
            body = "$matchTitle starts in $minutesLeft minutes. Create your team now!",
            type = "match",
            matchId = matchId
        )
    }

    fun sendContestFillingAlert(contestName: String, percentFilled: Int) {
        sendNotification(
            target = "all",
            title = "Contest Filling Fast!",
            body = "$contestName is $percentFilled% full. Join now before it closes!",
            type = "contest"
        )
    }

    fun sendWinningAlert(userId: String, amount: Long, rank: Int) {
        sendNotification(
            target = userId,
            title = "Congratulations! You Won!",
            body = "You won Rs.$amount! Rank: #$rank. Check your wallet now.",
            type = "wallet"
        )
    }

    fun sendWithdrawApproved(userId: String, amount: Int) {
        sendNotification(
            target = userId,
            title = "Withdrawal Approved!",
            body = "Rs.$amount transferred to your UPI. May take 24 hours to reflect.",
            type = "wallet"
        )
    }

    fun sendLiveAlert(userId: String, playerName: String, event: String) {
        sendNotification(
            target = userId,
            title = "Live Update!",
            body = "$playerName just $event! Points updating...",
            type = "match"
        )
    }
}
