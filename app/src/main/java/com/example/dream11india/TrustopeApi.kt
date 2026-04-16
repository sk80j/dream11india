package com.example.dream11india

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

object TrustopeConfig {
    const val USER_TOKEN   = "283206e338b8dd3574fc09f5464f1d35"
    const val REDIRECT_URL = "https://dream11india.page.link/payment-return"
    const val BASE_CREATE  = "https://a.trustope.com/"
    const val BASE_CHECK   = "https://trustope.com/"
}

data class TrustopeOrderResponse(
    val status: String? = null,
    val message: String? = null,
    val payment_url: String? = null,
    val order_id: String? = null,
    val txn_id: String? = null
)

data class TrustopeStatusResponse(
    val status: String? = null,
    val message: String? = null,
    val order_id: String? = null,
    val amount: String? = null,
    val txn_id: String? = null,
    val payment_status: String? = null
)

data class TrustopeStatusRequest(
    val user_token: String,
    val order_id: String
)

interface TrustopeCreateApi {
    @FormUrlEncoded
    @POST("api/create-order")
    suspend fun createOrder(
        @Field("customer_mobile") customerMobile: String,
        @Field("user_token")      userToken: String,
        @Field("amount")          amount: String,
        @Field("order_id")        orderId: String,
        @Field("redirect_url")    redirectUrl: String,
        @Field("remark1")         remark1: String,
        @Field("remark2")         remark2: String
    ): Response<TrustopeOrderResponse>
}

interface TrustopeCheckApi {
    @POST("api/check-order-status")
    suspend fun checkStatus(
        @Body request: TrustopeStatusRequest
    ): Response<TrustopeStatusResponse>
}

object TrustopeRetrofit {
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logger)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    val createApi: TrustopeCreateApi by lazy {
        Retrofit.Builder()
            .baseUrl(TrustopeConfig.BASE_CREATE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TrustopeCreateApi::class.java)
    }
    val checkApi: TrustopeCheckApi by lazy {
        Retrofit.Builder()
            .baseUrl(TrustopeConfig.BASE_CHECK)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TrustopeCheckApi::class.java)
    }
}
