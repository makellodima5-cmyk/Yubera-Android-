package com.yubera.app.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.BuildConfig
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
                    Text("Аккаунт YUBERA", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Вход через Telegram будет доступен после подключения безопасной нативной сессии.",
                        color = YuberaMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                        onCheckedChange = { enabled -> viewModel.setNotifications(enabled) }
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
                        "Параметры сохраняются локально. Фактическая отправка уведомлений будет включена после подключения фоновой проверки и системного разрешения Android.",
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
                    Text("YUBERA Android ${BuildConfig.VERSION_NAME}", color = YuberaOnBackground)
                    Text("Язык интерфейса: русский", color = YuberaMuted)
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = if (enabled) YuberaOnBackground else YuberaMuted, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
