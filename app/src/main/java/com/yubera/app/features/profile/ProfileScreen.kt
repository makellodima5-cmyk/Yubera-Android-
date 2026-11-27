package com.yubera.app.features.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
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
import androidx.compose.material3.TextButton
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
import com.yubera.app.R
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface
import com.yubera.app.data.settings.ThemeMode

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val accountProfile by viewModel.accountProfile.collectAsState()
    val communityProfile by viewModel.communityProfile.collectAsState()
    val appUpdate by viewModel.appUpdate.collectAsState()
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
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            val photo = accountProfile.user?.photoUrl
                            if (!photo.isNullOrBlank()) AsyncImage(model = photo, contentDescription = "Аватар профиля", modifier = Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape))
                            else Icon(Icons.Default.AccountCircle, contentDescription = "Аватар профиля", tint = YuberaMuted, modifier = Modifier.size(52.dp))
                            communityProfile?.avatarFrame?.let { frame -> profileFrameResource(frame)?.let { Image(painterResource(it), contentDescription = "Рамка аватара", modifier = Modifier.fillMaxSize()) } }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Аккаунт YUBERA", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                            communityProfile?.let { p -> if (p.isPlus) Text("PLUS", color = YuberaPrimary, style = MaterialTheme.typography.labelMedium); if (p.bio.isNotBlank()) Text(p.bio, color = YuberaMuted, style = MaterialTheme.typography.bodySmall, maxLines = 3); p.xp?.let { Text("Уровень ${it.level} · ${it.title}", color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }; p.support?.let { Text("Поддержка: ${it.title}", color = YuberaMuted, style = MaterialTheme.typography.bodySmall) } } ?: Text("Профиль и рамки синхронизируются с Website", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = onEditProfile) { Text("Редактировать профиль") }; TextButton(onClick = { confirmLogout = true }) { Text("Выйти из аккаунта") } }
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
                        title = "Push-уведомления (не подключены)",
                        checked = false,
                        enabled = false,
                        onCheckedChange = { }
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
                        "Push через Firebase пока не подключён. Сейчас приложение проверяет ваши подписки в фоновом режиме примерно раз в 8 часов; Android может переносить запуск для экономии батареи.",
                        color = YuberaMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=YuberaSurface),shape=RoundedCornerShape(18.dp)){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("Обновления YUBERA",color=YuberaOnBackground,style=MaterialTheme.typography.titleMedium)
                    when(val result=appUpdate.checkResult){
                        is com.yubera.app.features.updates.AppUpdateCheckResult.Available->{Text("Доступна версия ${result.update.versionName} (код ${result.update.versionCode}).",color=YuberaOnBackground);if(result.isMandatory)Text("Это обязательное обновление.",color=YuberaOnBackground);result.update.releaseNotes.takeIf{it.isNotBlank()}?.let{Text(it,color=YuberaMuted,style=MaterialTheme.typography.bodySmall)}}
                        com.yubera.app.features.updates.AppUpdateCheckResult.UpToDate->Text("Установлена актуальная версия.",color=YuberaMuted)
                        is com.yubera.app.features.updates.AppUpdateCheckResult.Failed->Text(result.reason,color=YuberaMuted)
                        null->appUpdate.availableUpdate?.let{Text("Доступна версия ${it.versionName} (код ${it.versionCode}).",color=YuberaOnBackground)}?:Text("Проверка обновлений ещё не выполнена.",color=YuberaMuted)
                    }
                    when(appUpdate.downloadWorkInfo?.state){
                        androidx.work.WorkInfo.State.ENQUEUED->Text("Загрузка ожидает подключения к сети.",color=YuberaMuted)
                        androidx.work.WorkInfo.State.RUNNING->{val progress=appUpdate.downloadWorkInfo?.progress?.getInt("progress",-1)?:-1;Text(if(progress>=0)"Скачивание: $progress%" else "Скачивание APK…",color=YuberaMuted)}
                        androidx.work.WorkInfo.State.SUCCEEDED->Text("APK скачан и проверен. Установка требует подтверждения Android.",color=YuberaMuted)
                        androidx.work.WorkInfo.State.FAILED->Text("Не удалось скачать или проверить APK.",color=YuberaMuted)
                        androidx.work.WorkInfo.State.CANCELLED->Text("Загрузка отменена.",color=YuberaMuted)
                        else->Unit
                    }
                    Button(onClick=viewModel::checkForUpdates,enabled=!appUpdate.isChecking){Text(if(appUpdate.isChecking)"Проверяем…" else "Проверить обновления")}
                    if(appUpdate.availableUpdate!=null&&appUpdate.downloadWorkInfo?.state!=androidx.work.WorkInfo.State.SUCCEEDED)Button(onClick=viewModel::downloadUpdate,enabled=appUpdate.downloadWorkInfo?.state!=androidx.work.WorkInfo.State.RUNNING){Text(if(appUpdate.isMandatory)"Скачать обязательное обновление" else "Скачать обновление")}
                    if(appUpdate.availableUpdate!=null&&appUpdate.downloadWorkInfo?.state==androidx.work.WorkInfo.State.SUCCEEDED)Button(onClick={val install=viewModel.createInstallIntent();if(install!=null)runCatching{context.startActivity(install)}}){Text("Установить / разрешить установку")}
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


@Composable
private fun profileFrameResource(frame: String): Int? = when(frame) {
    "Admin_Frame_01.webp" -> R.drawable.admin_frame_01
    "Admin_Frame_02.webp" -> R.drawable.admin_frame_02
    "Admin_Frame_03.webp" -> R.drawable.admin_frame_03
    "Admin_Frame_04.webp" -> R.drawable.admin_frame_04
    "Admin_Frame_05.webp" -> R.drawable.admin_frame_05
    "Admin_Frame_06.webp" -> R.drawable.admin_frame_06
    "Admin_Frame_07.webp" -> R.drawable.admin_frame_07
    "Admin_Frame_08.webp" -> R.drawable.admin_frame_08
    "YUBERA_Contribution_Avatar_Frame_01_Soyuznik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_01_soyuznik_webp
    "YUBERA_Contribution_Avatar_Frame_02_Storonnik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_02_storonnik_webp
    "YUBERA_Contribution_Avatar_Frame_03_Predanniy_Uchastnik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_03_predanniy_uchastnik_webp
    "YUBERA_Contribution_Avatar_Frame_04_Pochetnyy_Pokrovitel.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_04_pochetnyy_pokrovitel
    "YUBERA_Contribution_Avatar_Frame_05_Legenda_YUBERA.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_05_legenda_yubera
    "YUBERA_Plus_Avatar_Frame_01_Sakura_Gold.webp" -> R.drawable.yubera_plus_avatar_frame_01_sakura_gold
    "YUBERA_Plus_Avatar_Frame_02_Mystic_Kitsune.webp" -> R.drawable.yubera_plus_avatar_frame_02_mystic_kitsune
    "YUBERA_Plus_Avatar_Frame_03_Samurai_Steel.webp" -> R.drawable.yubera_plus_avatar_frame_03_samurai_steel
    "YUBERA_Plus_Avatar_Frame_04_Celestial_Archive.webp" -> R.drawable.yubera_plus_avatar_frame_04_celestial_archive
    "YUBERA_Plus_Avatar_Frame_05_Cyberpunk_Neon.webp" -> R.drawable.yubera_plus_avatar_frame_05_cyberpunk_neon
    else -> null
}
