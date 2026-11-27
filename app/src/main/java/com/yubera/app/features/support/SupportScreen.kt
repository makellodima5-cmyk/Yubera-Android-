package com.yubera.app.features.support

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class SupportTab { Support, Plus }

@Composable
fun SupportScreen(onBack: () -> Unit, viewModel: SupportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(SupportTab.Support) }
    var customDialogOpen by remember { mutableStateOf(false) }
    var customDraft by remember { mutableStateOf("") }
    var pendingStarsResumeCheck by remember { mutableStateOf(false) }
    var pendingPlusResumeCheck by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestPaymentStatus by rememberUpdatedState(state.paymentStatus)
    val latestPlusStatus by rememberUpdatedState(state.plus)

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (pendingStarsResumeCheck) {
                    pendingStarsResumeCheck = false
                    if (latestPaymentStatus == "pending") viewModel.checkStarsPayment()
                }
                if (pendingPlusResumeCheck) {
                    pendingPlusResumeCheck = false
                    if (latestPlusStatus?.isPlus == false) viewModel.checkPlusActivation()
                    else viewModel.loadPlusStatus()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openTelegramInvoice(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "Не удалось открыть Telegram. Установите приложение Telegram.", Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = YuberaOnBackground)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { YuberaLogo(width = 122.dp) }
            Spacer(Modifier.width(44.dp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF351E24), Color(0xFF281E2C), Color(0xFF43251F)))).padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ПОДДЕРЖКА ПРОЕКТА", color = Color(0xFFFFB59D), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Поддержка и YUBERA Plus", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Поддержите развитие YUBERA и откройте дополнительные возможности.", color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeTab == SupportTab.Support,
                    onClick = { activeTab = SupportTab.Support },
                    label = { Text("Поддержка") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = activeTab == SupportTab.Plus,
                    onClick = { activeTab = SupportTab.Plus; viewModel.loadPlusStatus() },
                    label = { Text("YUBERA Plus") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (activeTab == SupportTab.Support) {
                SupportDonationContent(
                    amount = state.selectedAmount,
                    customAmount = state.customAmount,
                    authenticated = state.isAuthenticated,
                    invoiceCreating = state.invoiceCreating,
                    invoiceUrl = state.invoiceUrl,
                    paymentStatus = state.paymentStatus,
                    paymentMessage = state.paymentMessage,
                    paymentChecking = state.paymentChecking,
                    error = state.error,
                    onChoose = viewModel::chooseAmount,
                    onCustom = {
                        customDraft = state.customAmount.ifBlank { state.amount.toString() }
                        customDialogOpen = true
                    },
                    onCreateInvoice = viewModel::createStarsInvoice,
                    onOpenInvoice = { state.invoiceUrl?.let { pendingStarsResumeCheck = true; openTelegramInvoice(it) } },
                    onCheckStatus = viewModel::checkStarsPayment
                )
            } else {
                YuberaPlusContent(
                    state = state,
                    onSelectMonths = viewModel::selectPlusMonths,
                    onAutoRenewConsent = viewModel::setAutoRenewConsent,
                    onCreateInvoice = viewModel::createPlusInvoice,
                    onOpenInvoice = { state.plusInvoiceUrl?.let { pendingPlusResumeCheck = true; openTelegramInvoice(it) } },
                    onCheckActivation = viewModel::checkPlusActivation,
                    onChangeRenewal = viewModel::changeAutoRenew
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (customDialogOpen) {
        AlertDialog(
            onDismissRequest = { customDialogOpen = false },
            title = { Text("Другая сумма") },
            text = {
                OutlinedTextField(
                    value = customDraft,
                    onValueChange = { value -> if (value.length <= 6 && value.all(Char::isDigit)) customDraft = value },
                    label = { Text("Количество Stars") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = customDraft.toIntOrNull()
                        if (value != null && value in 1..100_000) {
                            viewModel.chooseCustomAmount(value.toString())
                            customDialogOpen = false
                        }
                    },
                    enabled = customDraft.toIntOrNull()?.let { it in 1..100_000 } == true
                ) { Text("Применить") }
            },
            dismissButton = { TextButton(onClick = { customDialogOpen = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SupportDonationContent(
    amount: Int,
    customAmount: String,
    authenticated: Boolean,
    invoiceCreating: Boolean,
    invoiceUrl: String?,
    paymentStatus: String?,
    paymentMessage: String?,
    paymentChecking: Boolean,
    error: String?,
    onChoose: (Int) -> Unit,
    onCustom: () -> Unit,
    onCreateInvoice: () -> Unit,
    onOpenInvoice: () -> Unit,
    onCheckStatus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SupportSectionTitle("Поддержать проект")
        Text("Единовременный вклад в развитие YUBERA. Выберите сумму в Telegram Stars.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
        val presets = listOf(50, 100, 250, 500, 1_000)
        presets.chunked(3).forEach { rowValues ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    val selected = customAmount.isBlank() && amount == value
                    Surface(
                        modifier = Modifier.weight(1f).height(58.dp).clickable { onChoose(value) },
                        color = if (selected) Color(0xFF51271F) else YuberaSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) YuberaPrimary else Color(0xFF3A3B44))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC044), modifier = Modifier.size(18.dp))
                            Text(value.toString(), color = YuberaOnBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                if (rowValues.size < 3) {
                    Surface(
                        modifier = Modifier.weight(1f).height(58.dp).clickable(onClick = onCustom),
                        color = if (customAmount.isNotBlank()) Color(0xFF51271F) else YuberaSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (customAmount.isNotBlank()) YuberaPrimary else Color(0xFF3A3B44))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (customAmount.isNotBlank()) customAmount else "Другая сумма", color = YuberaOnBackground, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Text("Выбрано: " + amount + " Stars", color = YuberaPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (!authenticated) SupportNotice("Войдите в аккаунт YUBERA, чтобы создать счёт на поддержку.")
        Button(onClick = onCreateInvoice, enabled = authenticated && amount in 1..100_000 && !invoiceCreating, modifier = Modifier.fillMaxWidth()) {
            if (invoiceCreating) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Создать счёт — " + amount + " Stars")
        }
        if (!invoiceUrl.isNullOrBlank()) {
            Button(onClick = onOpenInvoice, modifier = Modifier.fillMaxWidth()) { Text("Открыть счёт в Telegram") }
            OutlinedButton(onClick = onCheckStatus, enabled = !paymentChecking, modifier = Modifier.fillMaxWidth()) {
                if (paymentChecking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Проверить статус оплаты")
            }
        }
        if (paymentMessage != null) SupportNotice(paymentMessage, success = paymentStatus == "paid")
        if (error != null) SupportNotice(error, isError = true)
        Surface(color = YuberaSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("На что идёт поддержка", color = YuberaOnBackground, fontWeight = FontWeight.SemiBold)
                listOf(
                    "Развитие Android-приложения и плеера",
                    "Работа каталога, подборок и обновлений",
                    "Серверная инфраструктура и стабильность сервиса"
                ).forEach { purpose ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = YuberaPrimary, modifier = Modifier.size(16.dp))
                        Text(purpose, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Surface(color = YuberaSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Оплата через Telegram Stars", color = YuberaOnBackground, fontWeight = FontWeight.SemiBold)
                Text("Счёт открывается в Telegram. После оплаты YUBERA ждёт подтверждение от сервера; открытие или закрытие счёта само по себе не считается успешным платежом.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun YuberaPlusContent(
    state: SupportUiState,
    onSelectMonths: (Int) -> Unit,
    onAutoRenewConsent: (Boolean) -> Unit,
    onCreateInvoice: () -> Unit,
    onOpenInvoice: () -> Unit,
    onCheckActivation: () -> Unit,
    onChangeRenewal: (Boolean) -> Unit
) {
    val plus = state.plus
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC044), modifier = Modifier.size(34.dp))
            Column(Modifier.weight(1f)) {
                SupportSectionTitle("YUBERA Plus")
                Text("Подписка для ещё большего комфорта.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (plus?.isPlus == true) Icon(Icons.Default.CheckCircle, contentDescription = "Подписка активна", tint = YuberaPrimary)
        }
        val benefits = listOf(
            Triple(Icons.Default.Headphones, "AI-помощник", "Персональные рекомендации, подборки и помощь в поиске аниме"),
            Triple(Icons.Default.Layers, "Расширенные подборки", "Больше лимитов и гибкие настройки"),
            Triple(Icons.Default.Image, "Эксклюзивные рамки аватара", "Специальные рамки для подписчиков Plus"),
            Triple(Icons.Default.AutoAwesome, "Эксклюзивный статус", "Особый статус YUBERA Plus в профиле")
        )
        benefits.forEach { item ->
            Surface(color = YuberaSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(item.first, contentDescription = null, tint = YuberaPrimary, modifier = Modifier.size(26.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.second, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold)
                        Text(item.third, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (state.plusLoading) {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = YuberaPrimary) }
        } else if (plus?.isPlus == true && plus.subscription != null) {
            val sub = plus.subscription
            Surface(color = Color(0xFF273328), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Подписка активна", color = Color(0xFF9FE0A6), fontWeight = FontWeight.Bold)
                    Text("Действует до " + formatSupportDate(sub.expiresAt), color = YuberaOnBackground)
                    Text(if (sub.autoRenew) "Автопродление включено" else "Автопродление выключено", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (sub.isRecurring) {
                if (sub.autoRenew) {
                    OutlinedButton(onClick = { onChangeRenewal(false) }, enabled = !state.plusBusy, modifier = Modifier.fillMaxWidth()) { Text("Отключить автопродление") }
                } else {
                    OutlinedButton(onClick = { onChangeRenewal(true) }, enabled = !state.plusBusy, modifier = Modifier.fillMaxWidth()) { Text("Включить автопродление") }
                }
            } else {
                Text("Автопродление недоступно для этой подписки. Продлить её можно после окончания текущего периода.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onCheckActivation, enabled = !state.plusChecking, modifier = Modifier.fillMaxWidth()) {
                if (state.plusChecking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Обновить статус")
            }
        } else {
            Text("Выберите период подписки", color = YuberaOnBackground, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.plusMonths == 1, onClick = { onSelectMonths(1) },
                    label = { Text("1 месяц · " + (plus?.price?.toString() ?: "—") + " Stars") }, modifier = Modifier.weight(1f))
                FilterChip(selected = state.plusMonths == 3, onClick = { onSelectMonths(3) },
                    label = { Text("3 месяца · " + (plus?.threeMonthPrice?.toString() ?: "—") + " Stars") }, modifier = Modifier.weight(1f))
            }
            if (state.plusMonths == 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.autoRenewConsent, onCheckedChange = onAutoRenewConsent)
                    Text("Разрешить автопродление через Telegram", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onCreateInvoice, enabled = state.isAuthenticated && plus != null && !state.plusInvoiceCreating, modifier = Modifier.fillMaxWidth()) {
                if (state.plusInvoiceCreating) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Оформить YUBERA Plus")
            }
            if (!state.isAuthenticated) SupportNotice("Войдите в аккаунт YUBERA, чтобы оформить подписку.")
            if (!state.plusInvoiceUrl.isNullOrBlank()) {
                Button(onClick = onOpenInvoice, modifier = Modifier.fillMaxWidth()) { Text("Открыть счёт в Telegram") }
                OutlinedButton(onClick = onCheckActivation, enabled = !state.plusChecking, modifier = Modifier.fillMaxWidth()) {
                    if (state.plusChecking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Проверить активацию")
                }
            }
        }
        state.plusMessage?.let { SupportNotice(it, success = it.contains("подтверждена", true) || it.contains("включено", true) || it.contains("отключено", true)) }
        state.plusError?.let { SupportNotice(it, isError = true) }
    }
}

@Composable
private fun SupportSectionTitle(title: String) {
    Text(title, color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun SupportNotice(message: String, success: Boolean = false, isError: Boolean = false) {
    Surface(
        color = when { isError -> Color(0xFF3A2024); success -> Color(0xFF223529); else -> YuberaSurface },
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            message,
            color = when { isError -> Color(0xFFFFB8B8); success -> Color(0xFFB2E6B8); else -> YuberaMuted },
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatSupportDate(epochMillis: Long): String =
    runCatching {
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }.getOrDefault("—")


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
