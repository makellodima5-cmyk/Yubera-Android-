package com.yubera.app.features.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.R
import com.yubera.app.core.ui.theme.*
import com.yubera.app.core.network.YuberaEndpoints
import com.yubera.app.data.settings.ThemeMode
import com.yubera.app.features.my.MyViewModel

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onSettings: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLegal: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    libraryViewModel: MyViewModel = hiltViewModel()
) {
    val account by viewModel.accountProfile.collectAsState()
    val response by viewModel.communityData.collectAsState()
    val library by libraryViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val profile = response?.profile
    val name = profile?.displayName?.takeIf(String::isNotBlank)
        ?: account.user?.let { user -> listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { user.username ?: "Пользователь" } }
        ?: "Пользователь"
    val avatar = profile?.avatarDataUrl?.takeIf(String::isNotBlank)
        ?: profile?.avatarUrl?.takeIf(String::isNotBlank)
        ?: profile?.photoUrl?.takeIf(String::isNotBlank)
        ?: account.user?.photoUrl
    LaunchedEffect(viewModel) { viewModel.refreshAccountProfile() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("YUBERA", color = YuberaOnBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onNotifications) { Icon(Icons.Default.Notifications, "Уведомления", tint = YuberaOnBackground) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                Image(painterResource(R.drawable.yubera_about_project_banner), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Text("Профиль", Modifier.align(Alignment.BottomStart).padding(18.dp), color = YuberaOnBackground, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                        if (!avatar.isNullOrBlank()) AsyncImage(avatar, "Аватар профиля", Modifier.size(94.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.AccountCircle, "Аватар", tint = YuberaMuted, modifier = Modifier.size(94.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(name, color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        profile?.username?.takeIf(String::isNotBlank)?.let { Text("@$it", color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }
                        account.user?.createdAt?.let { formatRegistrationDate(it) }?.let { Text("Зарегистрирован: $it", color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }
                        profile?.bio?.takeIf(String::isNotBlank)?.let { Text(it, color = YuberaOnBackground, style = MaterialTheme.typography.bodySmall, maxLines = 4) }
                    }
                    IconButton(onClick = onEditProfile) { Icon(Icons.Default.Edit, "Редактировать профиль", tint = YuberaPrimary) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat(Icons.Default.FavoriteBorder, library.favorites.size, "в избранном", Modifier.weight(1f))
                ProfileStat(Icons.Default.BookmarkBorder, library.watchLater.size, "хочу посмотреть", Modifier.weight(1f))
                ProfileStat(Icons.Default.History, library.history.size, "в истории", Modifier.weight(1f))
            }
            ProfileAction(Icons.Default.Edit, "Редактировать профиль", "Аватар, имя и описание", onEditProfile)
            ProfileAction(Icons.Default.Settings, "Настройки", "Внешний вид и настройки приложения", onSettings)
            ProfileAction(Icons.Default.Notifications, "Уведомления", "Новые серии и новости", onNotifications)
            ProfileAction(Icons.Default.Info, "Об аккаунте", "Версия ${appVersion(context)}, документы и информация", onAbout)
            ProfileAction(Icons.Default.Info, "Правовая информация", "Пользовательское соглашение и политика конфиденциальности", onLegal)
        }
    }
}

@Composable
private fun ProfileStat(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, tint = YuberaPrimary, modifier = Modifier.size(22.dp))
            Text(count.toString(), color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = YuberaMuted, modifier = Modifier.size(23.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ArrowForward, null, tint = YuberaMuted)
        }
    }
}

@Composable
fun ProfileAboutScreen(onBack: () -> Unit, onLogout: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val account by viewModel.accountProfile.collectAsState()
    val response by viewModel.communityData.collectAsState()
    val context = LocalContext.current
    val name = response?.profile?.displayName?.takeIf(String::isNotBlank)
        ?: account.user?.let { user -> listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { user.username ?: "Пользователь" } }
        ?: "Пользователь"
    LaunchedEffect(viewModel) { viewModel.refreshAccountProfile() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Назад", color = YuberaPrimary) }
            Text("Об аккаунте", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Имя пользователя", color = YuberaMuted, modifier = Modifier.weight(1f))
                        Text(name, color = YuberaOnBackground)
                    }
                    account.user?.createdAt?.let { formatRegistrationDate(it) }?.let { date ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Дата создания аккаунта", color = YuberaMuted, modifier = Modifier.weight(1f))
                            Text(date, color = YuberaOnBackground)
                        }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("YUBERA", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Версия ${appVersion(context)}", color = YuberaMuted)
                    }
                    Text("YUBERA — ваше личное пространство для аниме, каталога и истории просмотра.", color = YuberaMuted)
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(YuberaEndpoints.SITE_ORIGIN))) }
                    }) { Text("Открыть Website", color = YuberaPrimary) }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Правовая информация", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    Text("Пользовательское соглашение и политика конфиденциальности пока не подключены как отдельные ссылки в Android. Открыть официальный Website можно выше.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { viewModel.logout(onLogout) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsScreen(onBack: () -> Unit, onNotifications: () -> Unit, onLogout: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Назад", color = YuberaPrimary) }
            Text("Настройки", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Внешний вид", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
            ThemeMode.entries.forEach { mode ->
                val label = when(mode) { ThemeMode.SYSTEM -> "Системная тема"; ThemeMode.LIGHT -> "Светлая тема"; ThemeMode.DARK -> "Тёмная тема" }
                Row(Modifier.fillMaxWidth().clickable { viewModel.setTheme(mode) }, verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = YuberaOnBackground, modifier = Modifier.weight(1f))
                    RadioButton(settings.themeMode == mode, onClick = { viewModel.setTheme(mode) })
                }
            }
            Divider(color = YuberaMuted.copy(alpha = .25f))
            Text("Уведомления", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
            SettingSwitch("Все уведомления", settings.notificationsEnabled, onCheckedChange = viewModel::setNotifications)
            SettingSwitch("Новые серии", settings.newEpisodeNotificationsEnabled, onCheckedChange = viewModel::setNewEpisodes)
            SettingSwitch("Изменения тайтлов", settings.animeInfoNotificationsEnabled, onCheckedChange = viewModel::setAnimeInfo)
            SettingSwitch("Push-уведомления", settings.pushNotificationsEnabled, onCheckedChange = viewModel::setPushNotifications)
            TextButton(onClick = onNotifications) { Text("Управление уведомлениями", color = YuberaPrimary) }
            Divider(color = YuberaMuted.copy(alpha = .25f))
            TextButton(onClick = { viewModel.logout(onLogout) }) { Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = YuberaOnBackground, modifier = Modifier.weight(1f))
        Switch(checked, onCheckedChange)
    }
}


private fun appVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
}.getOrDefault("—")

@Composable
fun ProfileNotificationSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Назад", color = YuberaPrimary) }
            Text("Уведомления", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Уведомления", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    Text("Получать уведомления от YUBERA", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                    SettingSwitch("Все уведомления", settings.notificationsEnabled, onCheckedChange = viewModel::setNotifications)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Типы уведомлений", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    SettingSwitch("Новые серии", settings.newEpisodeNotificationsEnabled, onCheckedChange = viewModel::setNewEpisodes)
                    SettingSwitch("Изменения в аниме", settings.animeInfoNotificationsEnabled, onCheckedChange = viewModel::setAnimeInfo)
                    SettingSwitch("Push-уведомления", settings.pushNotificationsEnabled, onCheckedChange = viewModel::setPushNotifications)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Настройки системы", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    Text("Разрешения Android для уведомлений", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        }
                    }) { Text("Открыть настройки Android", color = YuberaPrimary) }
                }
            }
        }
    }
}
