package com.yubera.app.features.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
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
    onNotifications: () -> Unit = {},
    onFriends: () -> Unit = {},
    onLegal: () -> Unit = {},
    onAbout: () -> Unit = {},
    onAllCollections: () -> Unit = {},
    onCollection: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val account by viewModel.accountProfile.collectAsState()
    val response by viewModel.communityData.collectAsState()
    val community = response?.profile
    val collections = response?.collections.orEmpty()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showBio by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.refreshAccountProfile() }

    if (showBio && !community?.bio.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showBio = false },
            title = { Text("Описание профиля") },
            text = { Text(community?.bio.orEmpty()) },
            confirmButton = { TextButton(onClick = { showBio = false }) { Text("Закрыть") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        SectionBannerHeader(title = "Профиль", subtitle = "")
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = YuberaSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                            val photo = community?.photoUrl ?: account.user?.photoUrl
                            if (!photo.isNullOrBlank()) AsyncImage(
                                model = photo, contentDescription = "Аватар профиля",
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                            ) else Icon(Icons.Default.AccountCircle, "Аватар профиля", tint = YuberaMuted, modifier = Modifier.size(72.dp))
                            community?.avatarFrame?.let { frame ->
                                profileFrameResource(frame)?.let { Image(painterResource(it), "Рамка аватара", Modifier.fillMaxSize()) }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    community?.displayName ?: account.user?.let { listOfNotNull(it.firstName, it.lastName).joinToString(" ").ifBlank { "Пользователь YUBERA" } } ?: "Пользователь YUBERA",
                                    color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge
                                )
                                if (community?.isPlus == true) Text("PLUS", color = YuberaPrimary, style = MaterialTheme.typography.labelMedium)
                            }
                            community?.username?.takeIf { it.isNotBlank() }?.let { Text("@$it", color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }
                            val bio = community?.bio.orEmpty()
                            if (bio.isNotBlank()) {
                                Text(bio, color = YuberaMuted, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                if (bio.length > 110) TextButton(onClick = { showBio = true }, contentPadding = PaddingValues(0.dp)) { Text("Читать полностью", color = YuberaPrimary) }
                            }
                        }
                        TextButton(onClick = onEditProfile) {
                            Icon(Icons.Default.Edit, "Редактировать профиль")
                        }
                    }
                    if (account.isLoading || (community == null && account.errorMessage == null)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = YuberaPrimary)
                            Text("Загружаем профиль…", color = YuberaMuted)
                        }
                    }
                    if (community == null && !account.isLoading) {
                        Text(account.errorMessage ?: "Не удалось загрузить данные профиля.", color = YuberaMuted)
                        TextButton(onClick = viewModel::refreshAccountProfile) { Text("Повторить") }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("◈", color = YuberaPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text("Уровень ${community?.xp?.level ?: "—"}", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(community?.xp?.title ?: "Уровень не загружен", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        androidx.compose.material3.HorizontalDivider(Modifier.height(52.dp).width(1.dp), color = YuberaMuted.copy(alpha = .35f))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("♛", color = YuberaPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text("Поддержка проекта", color = YuberaOnBackground, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(community?.support?.title ?: "Без статуса", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    val xp = community?.xp
                    val progress = (xp?.progress ?: 0f).coerceIn(0f, 100f)
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = YuberaPrimary,
                        trackColor = YuberaMuted.copy(alpha = .22f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${xp?.total ?: 0} XP", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (xp?.nextLevel != null) "До ${xp.nextLevel} уровня: ${xp.remaining} XP" else if (xp != null) "Максимальный уровень" else "—",
                            color = YuberaMuted, style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileCounter("Подборки", response?.profile?.personalCollections ?: community?.publicCollections ?: 0, Modifier.weight(1f))
                ProfileCounter("Тайтлы", response?.profile?.personalTitles ?: community?.publicTitles ?: 0, Modifier.weight(1f))
                ProfileCounter("Лайки", response?.profile?.personalCollectionLikes ?: community?.collectionLikes ?: 0, Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Мои подборки", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onAllCollections) { Text("Все подборки", color = YuberaPrimary) }
            }
            if (collections.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp)) {
                    Text("У вас пока нет подборок.", color = YuberaMuted, modifier = Modifier.padding(16.dp))
                }
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    collections.forEach { collection ->
                        Card(
                            modifier = Modifier.width(190.dp).height(220.dp).clickable { onCollection(collection.id) },
                            colors = CardDefaults.cardColors(containerColor = YuberaSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(collection.title, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (collection.is_public == 1) "◎ Публичная" else "▣ Приватная", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${collection.title_count} тайтлов", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                                        Text("♡ ${collection.like_count}", color = YuberaPrimary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ProfileDestinationRow("Друзья", "Подписки, подписчики и поиск", onClick = onFriends)
            ProfileDestinationRow("Правовая информация", "Пользовательское соглашение и политика конфиденциальности", onClick = onLegal)
            ProfileDestinationRow("О проекте", "Версия ${context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()}", onClick = onAbout)

            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { showSettings = !showSettings }) {
                        Text(if (showSettings) "Скрыть настройки приложения" else "Настройки приложения", color = YuberaOnBackground)
                    }
                    if (showSettings) {
                        Text("Оформление", color = YuberaOnBackground)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(selected = settings.themeMode == mode, onClick = { viewModel.setTheme(mode) }, label = {
                                    Text(when (mode) { ThemeMode.SYSTEM -> "Системная"; ThemeMode.LIGHT -> "Светлая"; ThemeMode.DARK -> "Тёмная" })
                                })
                            }
                        }
                        SettingSwitch("Все уведомления", settings.notificationsEnabled, onCheckedChange = viewModel::setNotifications)
                        SettingSwitch("Новые серии", settings.newEpisodeNotificationsEnabled, onCheckedChange = viewModel::setNewEpisodes)
                        SettingSwitch("Изменения тайтлов", settings.animeInfoNotificationsEnabled, onCheckedChange = viewModel::setAnimeInfo)
                        TextButton(onClick = { showSettings = false; onNotifications() }) { Text("Уведомления", color = YuberaPrimary) }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { viewModel.logout { } }) { Text("Выйти из аккаунта", color = YuberaMuted) }
            }
        }
    }
}

@Composable
private fun ProfileCounter(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge)
            Text(label, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileDestinationRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = YuberaSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = YuberaPrimary, style = MaterialTheme.typography.titleLarge)
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
