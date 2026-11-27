package com.yubera.app.data.support

import com.yubera.app.core.session.SecureTokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val api: SupportApi,
    private val tokenStore: SecureTokenStore
) {
    fun hasSession(): Boolean = !tokenStore.getAccessToken().isNullOrBlank()

    suspend fun createStarsInvoice(amount: Int): StarsInvoiceResponse {
        require(amount in 1..100_000)
        return api.createStarsInvoice(StarsInvoiceRequest(amount), requiredAuthorization())
    }

    suspend fun getStarsPaymentStatus(payload: String): StarsPaymentStatusResponse =
        api.getStarsPaymentStatus(payload, requiredAuthorization())

    suspend fun getPlusStatus(): YuberaPlusStatusResponse =
        api.getPlusStatus(requiredAuthorization())

    suspend fun createPlusInvoice(months: Int, autoRenewConsent: Boolean): YuberaPlusMutationResponse {
        require(months == 1 || months == 3)
        return api.mutatePlus(
            YuberaPlusMutationRequest(
                action = "create-invoice",
                months = months,
                autoRenewConsent = autoRenewConsent
            ),
            requiredAuthorization()
        )
    }

    suspend fun setAutoRenew(enabled: Boolean): YuberaPlusMutationResponse =
        api.mutatePlus(
            YuberaPlusMutationRequest(action = if (enabled) "enable-auto-renew" else "cancel-auto-renew"),
            requiredAuthorization()
        )

    private fun requiredAuthorization(): String =
        tokenStore.getAccessToken()?.takeIf(String::isNotBlank)?.let { "Bearer $it" }
            ?: throw IllegalStateException("Войдите в аккаунт YUBERA, чтобы продолжить.")
}
