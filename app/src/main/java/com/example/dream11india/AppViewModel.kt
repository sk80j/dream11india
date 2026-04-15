package com.example.dream11india

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ---- User State ----
    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    // ---- Current Match ----
    private val _currentMatch = MutableStateFlow(sampleMatches[0])
    val currentMatch: StateFlow<MatchData> = _currentMatch.asStateFlow()

    // ---- Auth State ----
    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // ---- Listener reference for cleanup ----
    private var userListener: ListenerRegistration? = null

    // ---- Start listening to user data ----
    fun startUserListener(uid: String) {
        // Remove old listener first
        userListener?.remove()

        userListener = db.collection("users")
            .document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    android.util.Log.e("AppVM", "User listen failed: ${error.message}")
                    return@addSnapshotListener
                }
                doc?.let {
                    _userData.value = UserData(
                        uid = it.id,
                        phone = it.getString("phone") ?: "",
                        name = it.getString("name") ?: "Player",
                        balance = it.getLong("balance")?.toInt() ?: 0,
                        winnings = it.getLong("winnings")?.toInt() ?: 0,
                        matchesPlayed = it.getLong("matchesPlayed")?.toInt() ?: 0,
                        teamsCreated = it.getLong("teamsCreated")?.toInt() ?: 0,
                        isAdmin = it.getBoolean("isAdmin") ?: false,
                        kycStatus = it.getString("kycStatus") ?: "none",
                        fcmToken = it.getString("fcmToken") ?: ""
                    )
                }
            }
    }

    // ---- Stop listener (called on logout) ----
    fun stopUserListener() {
        userListener?.remove()
        userListener = null
    }

    // ---- Create new user in Firestore ----
    fun createUserIfNew(uid: String, phone: String) {
        val userRef = db.collection("users").document(uid)
        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                userRef.set(mapOf(
                    "phone" to phone,
                    "name" to "Player${phone.takeLast(4)}",
                    "balance" to 0,
                    "winnings" to 0,
                    "matchesPlayed" to 0,
                    "teamsCreated" to 0,
                    "isAdmin" to false,
                    "kycStatus" to "none",
                    "fcmToken" to "",
                    "createdAt" to System.currentTimeMillis()
                ))
            }
            // Merge missing fields for existing users
            userRef.set(mapOf(
                "kycStatus" to "none",
                "fcmToken" to ""
            ), SetOptions.merge())

            _isLoggedIn.value = true
            startUserListener(uid)
        }
    }

    // ---- Set current match ----
    fun setCurrentMatch(match: MatchData) {
        _currentMatch.value = match
    }

    // ---- Logout ----
    fun logout() {
        stopUserListener()
        auth.signOut()
        _userData.value = UserData()
        _isLoggedIn.value = false
    }

    // ---- Cleanup on ViewModel destroy ----
    override fun onCleared() {
        super.onCleared()
        stopUserListener()
    }
}
