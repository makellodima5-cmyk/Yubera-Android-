package com.yubera.app.data.support

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupportApi {
    @POST("api/telegram/stars-invoice")
    suspend fun createStarsInvoice(
        @Body request: StarsInvoiceRequest,
        @Header("Authorization") authorization: String
    ): StarsInvoiceResponse

    @GET("api/telegram/stars-status")
    suspend fun getStarsPaymentStatus(
        @Query("payload") payload: String,
        @Header("Authorization") authorization: String
    ): StarsPaymentStatusResponse

    @GET("api/yubera/plus")
    suspend fun getPlusStatus(
        @Header("Authorization") authorization: String
    ): YuberaPlusStatusResponse

    @POST("api/yubera/plus")
    suspend fun mutatePlus(
        @Body request: YuberaPlusMutationRequest,
        @Header("Authorization") authorization: String
    ): YuberaPlusMutationResponse
}

@Serializable
data class StarsInvoiceRequest(val amount: Int)

@Serializable
data class StarsInvoiceResponse(
    val invoiceUrl: String? = null,
    val payload: String? = null,
    val error: String? = null
)

@Serializable
data class StarsPaymentStatusResponse(
    val status: String = "pending",
    val error: String? = null
)

@Serializable
data class YuberaPlusMutationRequest(
    val action: String,
    val months: Int? = null,
    val autoRenewConsent: Boolean? = null
)

@Serializable
data class YuberaPlusMutationResponse(
    val invoiceUrl: String? = null,
    val error: String? = null
)

@Serializable
data class YuberaPlusSubscription(
    val active: Boolean = false,
    val expiresAt: Long = 0,
    val autoRenew: Boolean = false,
    val isRecurring: Boolean = false,
    val renewalFailedAt: String? = null
)

@Serializable
data class YuberaPlusStatusResponse(
    val price: Int = 0,
    val threeMonthPrice: Int = 0,
    val threeMonthDiscountPercent: Int = 0,
    val periodDays: Int = 30,
    val isPlus: Boolean = false,
    val renewalFailedAt: String? = null,
    val subscription: YuberaPlusSubscription? = null,
    val error: String? = null
)
