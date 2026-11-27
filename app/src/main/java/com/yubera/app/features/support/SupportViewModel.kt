package com.yubera.app.features.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.data.support.SupportRepository
import com.yubera.app.data.support.YuberaPlusStatusResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupportUiState(
    val amount: Int = 100,
    val customAmount: String = "",
    val isAuthenticated: Boolean = false,
    val invoiceCreating: Boolean = false,
    val invoiceUrl: String? = null,
    val invoicePayload: String? = null,
    val paymentStatus: String? = null,
    val paymentMessage: String? = null,
    val paymentChecking: Boolean = false,
    val error: String? = null,
    val plus: YuberaPlusStatusResponse? = null,
    val plusLoading: Boolean = true,
    val plusInvoiceUrl: String? = null,
    val plusInvoiceCreating: Boolean = false,
    val plusChecking: Boolean = false,
    val plusMessage: String? = null,
    val plusError: String? = null,
    val plusMonths: Int = 1,
    val autoRenewConsent: Boolean = false,
    val plusBusy: Boolean = false
) {
    val selectedAmount: Int get() = customAmount.toIntOrNull() ?: amount
    val validAmount: Boolean get() = selectedAmount in 1..100_000
}

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        SupportUiState(isAuthenticated = supportRepository.hasSession())
    )
    val uiState: StateFlow<SupportUiState> = mutableState.asStateFlow()

    init {
        loadPlusStatus()
    }

    fun chooseAmount(value: Int) {
        if (value !in PRESET_AMOUNTS) return
        mutableState.update {
            it.copy(amount = value, customAmount = "", invoiceUrl = null, invoicePayload = null,
                paymentStatus = null, paymentMessage = null, error = null)
        }
    }

    fun chooseCustomAmount(value: String) {
        if (value.length > 6 || value.any { !it.isDigit() }) return
        mutableState.update {
            it.copy(customAmount = value, invoiceUrl = null, invoicePayload = null,
                paymentStatus = null, paymentMessage = null, error = null)
        }
    }

    fun selectPlusMonths(months: Int) {
        if (months != 1 && months != 3) return
        mutableState.update {
            it.copy(plusMonths = months, autoRenewConsent = if (months == 3) false else it.autoRenewConsent,
                plusInvoiceUrl = null, plusMessage = null, plusError = null)
        }
    }

    fun setAutoRenewConsent(consent: Boolean) {
        mutableState.update { it.copy(autoRenewConsent = consent) }
    }

    fun createStarsInvoice() {
        val amount = mutableState.value.selectedAmount
        if (!mutableState.value.isAuthenticated) {
            mutableState.update { it.copy(error = "Войдите в аккаунт YUBERA, чтобы поддержать проект.") }
            return
        }
        if (amount !in 1..100_000 || mutableState.value.invoiceCreating) {
            mutableState.update { it.copy(error = "Укажите целое число от 1 до 100 000 Stars.") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(invoiceCreating = true, error = null, invoiceUrl = null, invoicePayload = null, paymentMessage = null, paymentStatus = null) }
            try {
                val result = supportRepository.createStarsInvoice(amount)
                val url = result.invoiceUrl
                val payload = result.payload
                if (url.isNullOrBlank() || payload.isNullOrBlank()) {
                    throw IllegalStateException("Не удалось создать счёт Telegram Stars.")
                }
                mutableState.update {
                    it.copy(
                        invoiceCreating = false,
                        invoiceUrl = url,
                        invoicePayload = payload,
                        paymentStatus = "pending",
                        paymentMessage = "Счёт создан. Откройте его в Telegram и завершите оплату. Подтверждение проверяется на сервере."
                    )
                }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(invoiceCreating = false, error = "Не удалось создать счёт. Проверьте подключение и войдите в аккаунт YUBERA.")
                }
            }
        }
    }

    fun checkStarsPayment() {
        val payload = mutableState.value.invoicePayload ?: return
        if (mutableState.value.paymentChecking) return
        viewModelScope.launch {
            mutableState.update { it.copy(paymentChecking = true, error = null) }
            try {
                repeat(8) { attempt ->
                    val result = supportRepository.getStarsPaymentStatus(payload)
                    when (result.status) {
                        "paid" -> {
                            mutableState.update {
                                it.copy(
                                    paymentChecking = false,
                                    paymentStatus = "paid",
                                    paymentMessage = "Оплата подтверждена. Спасибо за поддержку YUBERA!"
                                )
                            }
                            return@launch
                        }
                        "failed" -> {
                            mutableState.update {
                                it.copy(
                                    paymentChecking = false,
                                    paymentStatus = "failed",
                                    paymentMessage = "Платёж не завершён. Можно создать новый счёт."
                                )
                            }
                            return@launch
                        }
                    }
                    if (attempt < 7) delay(1_200)
                }
                mutableState.update {
                    it.copy(
                        paymentChecking = false,
                        paymentStatus = "pending",
                        paymentMessage = "Telegram ещё не подтвердил оплату. Нажмите «Проверить статус» позже. Платёж не считается успешным без подтверждения сервера."
                    )
                }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(paymentChecking = false, error = "Не удалось проверить статус оплаты. Повторите попытку позже.")
                }
            }
        }
    }

    fun loadPlusStatus() {
        if (!mutableState.value.isAuthenticated) {
            mutableState.update { it.copy(plusLoading = false) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(plusLoading = true, plusError = null) }
            try {
                val status = supportRepository.getPlusStatus()
                mutableState.update { it.copy(plus = status, plusLoading = false, plusError = null) }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(plusLoading = false, plusError = "Не удалось загрузить статус YUBERA Plus.")
                }
            }
        }
    }

    fun createPlusInvoice() {
        val current = mutableState.value
        if (!current.isAuthenticated || current.plusInvoiceCreating || current.plusBusy) return
        val selectedPrice = if (current.plusMonths == 1) current.plus?.price else current.plus?.threeMonthPrice
        if ((selectedPrice ?: 0) <= 0) {
            mutableState.update { it.copy(plusError = "Оформление YUBERA Plus сейчас недоступно.") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(plusInvoiceCreating = true, plusError = null, plusMessage = null, plusInvoiceUrl = null) }
            try {
                val result = supportRepository.createPlusInvoice(current.plusMonths, current.autoRenewConsent)
                if (result.invoiceUrl.isNullOrBlank()) throw IllegalStateException("Invoice link missing")
                mutableState.update {
                    it.copy(
                        plusInvoiceCreating = false,
                        plusInvoiceUrl = result.invoiceUrl,
                        plusMessage = "Счёт создан. Завершите оплату в Telegram, затем проверьте активацию."
                    )
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(plusInvoiceCreating = false, plusError = "Не удалось создать счёт YUBERA Plus.") }
            }
        }
    }

    fun checkPlusActivation() {
        if (mutableState.value.plusChecking) return
        viewModelScope.launch {
            mutableState.update { it.copy(plusChecking = true, plusError = null) }
            try {
                repeat(12) { attempt ->
                    val status = supportRepository.getPlusStatus()
                    mutableState.update { it.copy(plus = status) }
                    if (status.isPlus) {
                        mutableState.update {
                            it.copy(
                                plusChecking = false,
                                plusMessage = "Оплата подтверждена. YUBERA Plus активирован.",
                                plusInvoiceUrl = null
                            )
                        }
                        return@launch
                    }
                    if (attempt < 11) delay(1_000)
                }
                mutableState.update {
                    it.copy(plusChecking = false, plusMessage = "Платёж отправлен. Telegram ещё подтверждает его. Обновите статус позже.")
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(plusChecking = false, plusError = "Не удалось проверить подписку.") }
            }
        }
    }

    fun changeAutoRenew(enabled: Boolean) {
        if (mutableState.value.plusBusy) return
        viewModelScope.launch {
            mutableState.update { it.copy(plusBusy = true, plusError = null, plusMessage = null) }
            try {
                supportRepository.setAutoRenew(enabled)
                val status = supportRepository.getPlusStatus()
                mutableState.update {
                    it.copy(
                        plus = status,
                        plusBusy = false,
                        plusMessage = if (enabled) "Автопродление включено." else "Автопродление отключено."
                    )
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(plusBusy = false, plusError = "Не удалось изменить автопродление.") }
            }
        }
    }

    fun clearErrors() {
        mutableState.update { it.copy(error = null, plusError = null) }
    }

    private companion object {
        val PRESET_AMOUNTS = setOf(50, 100, 250, 500, 1_000)
    }
}
