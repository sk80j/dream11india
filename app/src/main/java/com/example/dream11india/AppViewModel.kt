package com.example.dream11india

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────
// CONSTANTS
// ─────────────────────────────────────────────

private const val TAG          = "AppViewModel"
private const val OWNER_UID    = "1irz1sRJ3QNeEtUuN70OSWiUBdq2"
private const val CONFIG_DOC   = "app_config/main"

// ─────────────────────────────────────────────
// AUTH STATE
// ─────────────────────────────────────────────

sealed class AuthState {
    object LoggedOut  : AuthState()
    object LoggingIn  : AuthState()
    data class LoggedIn(val uid: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// ─────────────────────────────────────────────
// APP CONFIG MODEL
// ─────────────────────────────────────────────

data class AppConfig(
    val maintenanceMode: Boolean = false,
    val minDeposit:      Int     = 10,
    val minWithdraw:     Int     = 100,
    val appVersion:      String  = "1.0.0",
    val forceUpdate:     Boolean = false,
    val supportText:     String  = "support@akku11.com",
    val gstPercent:      Float   = 28f,
    val platformFee:     Float   = 15f
)

// ─────────────────────────────────────────────
// WALLET STATE
// ─────────────────────────────────────────────

data class AppWalletState(
    val balance:     Int = 0,
    val winnings:    Int = 0,
    val bonusCash:   Int = 0,
    val totalAmount: Int = 0
)

// ─────────────────────────────────────────────
// NETWORK STATE
// ─────────────────────────────────────────────

enum class NetworkState { ONLINE, OFFLINE, UNKNOWN }

// ─────────────────────────────────────────────
// APP VIEW MODEL
// ─────────────────────────────────────────────

class AppViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Listeners (cleanup on onCleared) ──
    private var userListener:         ListenerRegistration? = null
    private var notifListener:        ListenerRegistration? = null
    private var configListener:       ListenerRegistration? = null
    private var authStateListener:    FirebaseAuth.AuthStateListener? = null

    // ── Auth ──
    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = _authState
        .map { it is AuthState.LoggedIn }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── User data ──
    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    // ── Wallet ──
    private val _wallet = MutableStateFlow(AppWalletState())
    val wallet: StateFlow<AppWalletState> = _wallet.asStateFlow()

    // ── Current match ──
    private val _currentMatch = MutableStateFlow<MatchData?>(null)
    val currentMatch: StateFlow<MatchData?> = _currentMatch.asStateFlow()

    // ── App config ──
    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    // ── Notifications ──
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // ── Network ──
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    // ── Global loading ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Global error ──
    private val _globalError = MutableStateFlow<String?>(null)
    val globalError: StateFlow<String?> = _globalError.asStateFlow()

    // ── Admin ──
    val isOwner: StateFlow<Boolean> = _userData
        .map { it.uid == OWNER_UID }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canOpenAdminPanel: StateFlow<Boolean> = _userData
        .map { it.uid == OWNER_UID || it.isAdmin }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ─────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────

    init {
        startAuthListener()
        startConfigListener()
    }

    // ─────────────────────────────────────────
    // AUTH LISTENER
    // ─────────────────────────────────────────

    private fun startAuthListener() {
        authStateListener?.let { auth.removeAuthStateListener(it) }
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.LoggedIn(user.uid)
                startUserListener(user.uid)
                startNotifListener(user.uid)
            } else {
                _authState.value = AuthState.LoggedOut
                stopAllUserListeners()
                _userData.value  = UserData()
                _wallet.value    = AppWalletState()
                _unreadCount.value = 0
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    // ─────────────────────────────────────────
    // USER LISTENER (realtime)
    // ─────────────────────────────────────────

    fun startUserListener(uid: String) {
        userListener?.remove()
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, err ->
                if (err != null) {
                    Log.e(TAG, "User listen failed: ${err.message}")
                    _globalError.value = "Failed to load user data"
                    return@addSnapshotListener
                }
                doc?.let { d ->
                    val balance   = d.getLong("balance")?.toInt()   ?: 0
                    val winnings  = d.getLong("winnings")?.toInt()  ?: 0
                    val bonus     = d.getLong("bonusBalance")?.toInt() ?: 0
                    _userData.value = UserData(
                        uid             = d.id,
                        phone           = d.getString("phone")           ?: "",
                        name            = d.getString("name")            ?: "Player",
                        balance         = balance,
                        winnings        = winnings,
                        bonusBalance    = bonus,
                        matchesPlayed   = d.getLong("matchesPlayed")?.toInt()  ?: 0,
                        teamsCreated    = d.getLong("teamsCreated")?.toInt()   ?: 0,
                        joinedContests  = d.getLong("joinedContests")?.toInt() ?: 0,
                        totalDeposits   = d.getLong("totalDeposits")?.toInt()  ?: 0,
                        totalWithdrawals= d.getLong("totalWithdrawals")?.toInt() ?: 0,
                        referralCode    = d.getString("referralCode")    ?: "",
                        kycStatus       = d.getString("kycStatus")       ?: "none",
                        fcmToken        = d.getString("fcmToken")        ?: "",
                        isAdmin         = d.getBoolean("isAdmin")        ?: false,
                        isBlocked       = d.getBoolean("isBlocked")      ?: false,
                        walletFrozen    = d.getBoolean("walletFrozen")   ?: false
                    )
                    _wallet.value = AppWalletState(
                        balance     = balance,
                        winnings    = winnings,
                        bonusCash   = bonus,
                        totalAmount = balance + winnings + bonus
                    )
                }
            }
    }

    // ─────────────────────────────────────────
    // NOTIFICATIONS LISTENER
    // ─────────────────────────────────────────

    private fun startNotifListener(uid: String) {
        notifListener?.remove()
        notifListener = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                _unreadCount.value = snap?.size() ?: 0
            }
    }

    // ─────────────────────────────────────────
    // APP CONFIG LISTENER
    // ─────────────────────────────────────────

    private fun startConfigListener() {
        configListener?.remove()
        configListener = db.document(CONFIG_DOC)
            .addSnapshotListener { doc, err ->
                if (err != null || doc == null) return@addSnapshotListener
                _appConfig.value = AppConfig(
                    maintenanceMode = doc.getBoolean("maintenanceMode") ?: false,
                    minDeposit      = doc.getLong("minDeposit")?.toInt()  ?: 10,
                    minWithdraw     = doc.getLong("minWithdraw")?.toInt() ?: 100,
                    appVersion      = doc.getString("appVersion")         ?: "1.0.0",
                    forceUpdate     = doc.getBoolean("forceUpdate")       ?: false,
                    supportText     = doc.getString("supportText")        ?: "support@akku11.com",
                    gstPercent      = doc.getDouble("gstPercent")?.toFloat()    ?: 28f,
                    platformFee     = doc.getDouble("platformFee")?.toFloat()   ?: 15f
                )
            }
    }

    // ─────────────────────────────────────────
    // CREATE USER IF NEW
    // ─────────────────────────────────────────

    fun createUserIfNew(uid: String, phone: String) {
        _authState.value = AuthState.LoggingIn
        viewModelScope.launch {
            try {
                val ref  = db.collection("users").document(uid)
                val snap = ref.get().await()
                if (!snap.exists()) {
                    ref.set(mapOf(
                        "phone"            to phone,
                        "name"             to "Player${phone.takeLast(4)}",
                        "balance"          to 0,
                        "winnings"         to 0,
                        "bonusBalance"     to 100, // Welcome bonus
                        "matchesPlayed"    to 0,
                        "teamsCreated"     to 0,
                        "joinedContests"   to 0,
                        "totalDeposits"    to 0,
                        "totalWithdrawals" to 0,
                        "referralCode"     to "AKKU${phone.takeLast(4)}",
                        "kycStatus"        to "none",
                        "fcmToken"         to "",
                        "isAdmin"          to false,
                        "isBlocked"        to false,
                        "walletFrozen"     to false,
                        "createdAt"        to System.currentTimeMillis()
                    ))
                } else {
                    // Merge any missing fields silently
                    ref.set(mapOf(
                        "bonusBalance"     to 0,
                        "joinedContests"   to 0,
                        "totalDeposits"    to 0,
                        "totalWithdrawals" to 0,
                        "referralCode"     to "AKKU${phone.takeLast(4)}",
                        "isBlocked"        to false,
                        "walletFrozen"     to false
                    ), SetOptions.merge())
                }
                _authState.value = AuthState.LoggedIn(uid)
                startUserListener(uid)
                startNotifListener(uid)
            } catch (e: Exception) {
                Log.e(TAG, "createUser failed: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Login failed")
                _globalError.value = "Failed to setup account. Please try again."
            }
        }
    }

    // ─────────────────────────────────────────
    // WALLET FUNCTIONS
    // ─────────────────────────────────────────

    fun refreshWallet() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val balance  = doc.getLong("balance")?.toInt()      ?: 0
                val winnings = doc.getLong("winnings")?.toInt()     ?: 0
                val bonus    = doc.getLong("bonusBalance")?.toInt() ?: 0
                _wallet.value = AppWalletState(balance, winnings, bonus, balance + winnings + bonus)
            } catch (e: Exception) {
                Log.e(TAG, "refreshWallet failed: ${e.message}")
            }
        }
    }

    fun updateAfterPayment(amount: Int) {
        _wallet.value = _wallet.value.copy(
            balance     = _wallet.value.balance + amount,
            totalAmount = _wallet.value.totalAmount + amount
        )
    }

    fun updateAfterJoinContest(entryFee: Int) {
        val newBal = (_wallet.value.balance - entryFee).coerceAtLeast(0)
        _wallet.value = _wallet.value.copy(
            balance     = newBal,
            totalAmount = newBal + _wallet.value.winnings + _wallet.value.bonusCash
        )
    }

    fun updateAfterWinning(amount: Int) {
        _wallet.value = _wallet.value.copy(
            winnings    = _wallet.value.winnings + amount,
            totalAmount = _wallet.value.totalAmount + amount
        )
    }

    // ─────────────────────────────────────────
    // MATCH SELECTION
    // ─────────────────────────────────────────

    fun setCurrentMatch(match: MatchData) {
        _currentMatch.value = match
    }

    fun clearCurrentMatch() {
        _currentMatch.value = null
    }

    // ─────────────────────────────────────────
    // NETWORK STATE
    // ─────────────────────────────────────────

    fun setNetworkState(isOnline: Boolean) {
        _networkState.value = if (isOnline) NetworkState.ONLINE else NetworkState.OFFLINE
    }

    // ─────────────────────────────────────────
    // REFRESH FUNCTIONS
    // ─────────────────────────────────────────

    fun refreshUser() {
        val uid = auth.currentUser?.uid ?: return
        startUserListener(uid)
    }

    fun refreshNotifications() {
        val uid = auth.currentUser?.uid ?: return
        startNotifListener(uid)
    }

    fun refreshAll() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            startUserListener(uid)
            startNotifListener(uid)
            delay(1000L)
            _isLoading.value = false
        }
    }

    // ─────────────────────────────────────────
    // ERROR HANDLING
    // ─────────────────────────────────────────

    fun clearError() {
        _globalError.value = null
    }

    fun showError(message: String) {
        _globalError.value = message
    }

    // ─────────────────────────────────────────
    // MARK NOTIFICATIONS READ
    // ─────────────────────────────────────────

    fun markAllNotificationsRead() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snap = db.collection("notifications")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("isRead", false)
                    .get().await()
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
                _unreadCount.value = 0
            } catch (e: Exception) {
                Log.e(TAG, "markNotifications failed: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────

    fun logout() {
        stopAllUserListeners()
        auth.signOut()
        _userData.value    = UserData()
        _wallet.value      = AppWalletState()
        _unreadCount.value = 0
        _currentMatch.value = null
        _authState.value   = AuthState.LoggedOut
        _globalError.value = null
    }

    // ─────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────

    private fun stopAllUserListeners() {
        userListener?.remove();  userListener  = null
        notifListener?.remove(); notifListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAllUserListeners()
        configListener?.remove(); configListener = null
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}
