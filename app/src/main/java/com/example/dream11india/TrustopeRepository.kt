package com.example.dream11india

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class PaymentResult {
    data class Success(val paymentUrl: String, val orderId: String) : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}

sealed class StatusResult {
    object Success : StatusResult()
    object Pending : StatusResult()
    object Failed  : StatusResult()
    data class Error(val message: String) : StatusResult()
}

object TrustopeRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun generateOrderId(): String {
        val ts   = System.currentTimeMillis()
        val rand = (1000..9999).random()
        return "D11_${ts}_$rand"
    }

    suspend fun createOrder(amount: Int, phone: String, orderId: String): PaymentResult {
        return try {
            db.collection("paymentOrders").document(orderId).set(mapOf(
                "orderId"   to orderId,
                "userId"    to uid,
                "amount"    to amount,
                "status"    to "created",
                "phone"     to phone,
                "createdAt" to System.currentTimeMillis()
            )).await()
            val response = TrustopeRetrofit.createApi.createOrder(
                customerMobile = phone,
                userToken      = TrustopeConfig.USER_TOKEN,
                amount         = amount.toString(),
                orderId        = orderId,
                redirectUrl    = TrustopeConfig.REDIRECT_URL,
                remark1        = "Dream11India Deposit",
                remark2        = "UID:$uid"
            )
            if (response.isSuccessful) {
                val url = response.body()?.payment_url
                if (!url.isNullOrBlank()) {
                    db.collection("paymentOrders").document(orderId)
                        .update("status", "initiated", "paymentUrl", url).await()
                    PaymentResult.Success(url, orderId)
                } else {
                    PaymentResult.Error(response.body()?.message ?: "No payment URL")
                }
            } else {
                PaymentResult.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            PaymentResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun checkOrderStatus(orderId: String): StatusResult {
        return try {
            val response = TrustopeRetrofit.checkApi.checkStatus(
                TrustopeStatusRequest(TrustopeConfig.USER_TOKEN, orderId)
            )
            if (response.isSuccessful) {
                when (response.body()?.payment_status?.uppercase()) {
                    "SUCCESS" -> {
                        db.collection("paymentOrders").document(orderId)
                            .update("status", "success", "verifiedAt", System.currentTimeMillis()).await()
                        StatusResult.Success
                    }
                    "FAILED" -> {
                        db.collection("paymentOrders").document(orderId).update("status", "failed").await()
                        StatusResult.Failed
                    }
                    else -> StatusResult.Pending
                }
            } else StatusResult.Error("Status check failed: ${response.code()}")
        } catch (e: Exception) {
            StatusResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun creditWalletSafe(orderId: String, amount: Int): Boolean {
        return try {
            val orderRef = db.collection("paymentOrders").document(orderId)
            val userRef  = db.collection("users").document(uid)
            val orderSnap = orderRef.get().await()
            if (orderSnap.getString("status") == "credited") return true
            db.runTransaction { tx ->
                val snap   = tx.get(userRef)
                val curBal = snap.getLong("balance")?.toInt() ?: 0
                tx.update(userRef, "balance", curBal + amount)
                tx.update(orderRef, "status", "credited", "creditedAt", System.currentTimeMillis())
            }.await()
            db.collection("transactions").add(mapOf(
                "userId"      to uid,
                "type"        to "credit",
                "amount"      to amount,
                "orderId"     to orderId,
                "description" to "Deposit via Trustope",
                "status"      to "completed",
                "timestamp"   to System.currentTimeMillis()
            )).await()
            true
        } catch (e: Exception) { false }
    }
}
