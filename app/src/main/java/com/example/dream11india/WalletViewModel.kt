package com.example.dream11india

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WalletState(
    val balance:      Int  = 0,
    val winnings:     Int  = 0,
    val bonusBalance: Int  = 0,
    val transactions: List<Transaction>     = emptyList(),
    val withdrawals:  List<WithdrawRequest> = emptyList(),
    val isLoading:    Boolean     = false,
    val paymentState: PaymentState = PaymentState.Idle
)

sealed class PaymentState {
    object Idle      : PaymentState()
    object Creating  : PaymentState()
    data class AwaitingPayment(val url: String, val orderId: String) : PaymentState()
    object Verifying : PaymentState()
    object Success   : PaymentState()
    object Failed    : PaymentState()
    object Pending   : PaymentState()
    data class Error(val message: String) : PaymentState()
}

class WalletViewModel : ViewModel() {
    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    init { loadData() }

    fun loadData() {
        if (uid.isEmpty()) return
        db.collection("users").document(uid).addSnapshotListener { snap, _ ->
            snap?.let { doc ->
                _state.update { it.copy(
                    balance      = doc.getLong("balance")?.toInt()      ?: 0,
                    winnings     = doc.getLong("winnings")?.toInt()     ?: 0,
                    bonusBalance = doc.getLong("bonusBalance")?.toInt() ?: 0,
                    isLoading    = false
                )}
            }
        }
        db.collection("transactions").whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    _state.update { s -> s.copy(transactions = it.documents.map { doc ->
                        Transaction(
                            id          = doc.id,
                            type        = doc.getString("type") ?: "",
                            amount      = doc.getLong("amount")?.toInt() ?: 0,
                            description = doc.getString("description") ?: "",
                            timestamp   = doc.getLong("timestamp") ?: 0L,
                            status      = doc.getString("status") ?: "completed",
                            orderId     = doc.getString("orderId") ?: ""
                        )
                    })}
                }
            }
        db.collection("withdrawRequests").whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    _state.update { s -> s.copy(withdrawals = it.documents.map { doc ->
                        WithdrawRequest(
                            id        = doc.id,
                            userId    = doc.getString("userId") ?: "",
                            amount    = doc.getLong("amount")?.toInt() ?: 0,
                            upiId     = doc.getString("upiId") ?: "",
                            status    = doc.getString("status") ?: "pending",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    })}
                }
            }
    }

    fun createOrder(amount: Int, phone: String) {
        viewModelScope.launch {
            _state.update { it.copy(paymentState = PaymentState.Creating) }
            val orderId = TrustopeRepository.generateOrderId()
            when (val r = TrustopeRepository.createOrder(amount, phone, orderId)) {
                is PaymentResult.Success -> _state.update { it.copy(paymentState = PaymentState.AwaitingPayment(r.paymentUrl, r.orderId)) }
                is PaymentResult.Error   -> _state.update { it.copy(paymentState = PaymentState.Error(r.message)) }
            }
        }
    }

    fun verifyPayment(orderId: String) {
        viewModelScope.launch {
            _state.update { it.copy(paymentState = PaymentState.Verifying) }
            when (val r = TrustopeRepository.checkOrderStatus(orderId)) {
                StatusResult.Success -> {
                    val snap   = db.collection("paymentOrders").document(orderId).get().await()
                    val amount = snap.getLong("amount")?.toInt() ?: 0
                    val ok     = TrustopeRepository.creditWalletSafe(orderId, amount)
                    _state.update { it.copy(paymentState = if (ok) PaymentState.Success else PaymentState.Error("Credit failed")) }
                }
                StatusResult.Pending -> _state.update { it.copy(paymentState = PaymentState.Pending) }
                StatusResult.Failed  -> _state.update { it.copy(paymentState = PaymentState.Failed) }
                is StatusResult.Error-> _state.update { it.copy(paymentState = PaymentState.Error(r.message)) }
            }
        }
    }

    fun resetPayment() { _state.update { it.copy(paymentState = PaymentState.Idle) } }
}
