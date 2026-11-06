package com.yubera.app.features.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.MainActivity
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaSurface
import com.yubera.app.data.settings.ThemeMode

@Composable
fun ProfileScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val accountProfile by viewModel.accountProfile.collectAsState()
    val context = LocalContext.current
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    var confirmLogout by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(viewModel) {
        viewModel.refreshAccountProfile()
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Сессия будет удалена с устройства. Личная библиотека сохранится отдельно для каждого аккаунта.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout {
                        context.startActivity(
                            Intent(context, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                    }
                }) { Text("Выйти") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Отмена") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(title = "Профиль", subtitle = "Настройки YUBERA")
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = YuberaSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Аватар профиля",
                            tint = YuberaMuted,
                            modifier = Modifier.size(52.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Аккаунт YUBERA", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                            Text("Настройка аватара появится позже", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    when {
                        accountProfile.isLoading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Загружаем данные Telegram…", color = YuberaMuted)
                            }
                        }
                        accountProfile.user != null -> {
                            val user = accountProfile.user!!
                            val displayName = listOfNotNull(
                                user.firstName?.takeIf(String::isNotBlank),
                                user.lastName?.takeIf(String::isNotBlank)
                            ).joinToString(" ").ifBlank { user.username?.let { "@$it" } ?: "Пользователь YUBERA" }
                            Text(displayName, color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge)
                            user.username?.takeIf(String::isNotBlank)?.let {
                                Text("@$it", color = YuberaMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            formatRegistrationDate(user.createdAt)?.let { date ->
                                Text("Дата регистрации: $date", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Вход выполнен через Telegram. Сессия хранится в зашифрованном виде с использованием Android Keystore.",
                                color = YuberaMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {
                            Text(
                                accountProfile.errorMessage ?: "Данные аккаунта пока не загружены.",
                                color = YuberaMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = viewModel::refreshAccountProfile) { Text("Повторить") }
                        }
                    }
                    TextButton(onClick = { confirmLogout = true }) { Text("Выйти из аккаунта") }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = YuberaSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Оформление", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setTheme(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "Системная"
                                            ThemeMode.LIGHT -> "Светлая"
                                            ThemeMode.DARK -> "Тёмная"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = YuberaSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Уведомления", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    SettingSwitch(
                        title = "Все уведомления",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setNotifications(enabled)
                            if (enabled && Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                    SettingSwitch(
                        title = "Новые серии",
                        checked = settings.newEpisodeNotificationsEnabled,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = { enabled -> viewModel.setNewEpisodes(enabled) }
                    )
                    SettingSwitch(
                        title = "Изменения отслеживаемых тайтлов",
                        checked = settings.animeInfoNotificationsEnabled,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = { enabled -> viewModel.setAnimeInfo(enabled) }
                    )
                    Text(
                        "Приложение проверяет только ваши подписки в фоновом режиме примерно раз в 6 часов. Android может переносить запуск для экономии батареи.",
                        color = YuberaMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = YuberaSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("О приложении", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    Text("YUBERA Android ${versionName}", color = YuberaOnBackground)
                    Text("Язык интерфейса: русский", color = YuberaMuted)
                    Text("Пользовательское соглашение · скоро", color = YuberaMuted)
                    Text("Политика конфиденциальности · скоро", color = YuberaMuted)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = if (enabled) YuberaOnBackground else YuberaMuted, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
