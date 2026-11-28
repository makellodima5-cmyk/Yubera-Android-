package com.yubera.app.features.profile

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.BuildConfig
import com.yubera.app.R
import com.yubera.app.core.ui.theme.*
import com.yubera.app.core.ui.components.yuberaPressScale
import com.yubera.app.core.ui.components.rememberYuberaPressInteractionSource
import com.yubera.app.data.auth.YuberaXpStatus
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onFriends: () -> Unit = {},
    onLegal: () -> Unit = {},
    onAbout: () -> Unit = {},
    onAllCollections: () -> Unit = {},
    onCollection: (String) -> Unit = {},
    resetKey: Int = 0,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val account by viewModel.accountProfile.collectAsState()
    val response by viewModel.communityData.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val profile = response?.profile
    val context = androidx.compose.ui.platform.LocalContext.current
    var fullBio by remember { mutableStateOf(false) }
    var coverTick by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(viewModel) { viewModel.refreshAccountProfile() }
    LaunchedEffect(resetKey) { scroll.scrollTo(0); viewModel.refreshAccountProfile() }
    LaunchedEffect(Unit) { while (true) { delay(60 * 60 * 1_000L); coverTick++ } }
    DisposableEffect(viewModel, lifecycleOwner) {
        val owner = lifecycleOwner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refreshAccountProfile()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (fullBio && !profile?.bio.isNullOrBlank()) {
        ModalBottomSheet(onDismissRequest = { fullBio = false }, sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Описание профиля", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    val closeBioInteraction = rememberYuberaPressInteractionSource()
            IconButton(onClick = { fullBio = false }, modifier = Modifier.yuberaPressScale(closeBioInteraction), interactionSource = closeBioInteraction) { Text("×", color = YuberaMuted, style = MaterialTheme.typography.titleLarge) }
                }
                Text(profile?.bio.orEmpty(), color = YuberaOnBackground, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(42.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.yubera_logo_transparent), "YUBERA", Modifier.height(34.dp), contentScale = ContentScale.Fit)
            }
            Box {
                val notificationInteraction = rememberYuberaPressInteractionSource()
                IconButton(onClick = onNotifications, modifier = Modifier.size(42.dp).yuberaPressScale(notificationInteraction), interactionSource = notificationInteraction) { Icon(Icons.Default.Notifications, "Уведомления", tint = YuberaOnBackground) }
                val unread by viewModel.unreadNotificationCount.collectAsState()
                if (unread > 0) Box(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 5.dp).size(8.dp).background(YuberaRed, CircleShape))
            }
        }
        Text("Профиль", color = YuberaOnBackground, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                        val avatar = profile?.avatarDataUrl?.takeIf(String::isNotBlank)
                            ?: profile?.avatarUrl?.takeIf(String::isNotBlank)
                            ?: profile?.photoUrl?.takeIf(String::isNotBlank)
                        if (!avatar.isNullOrBlank()) AsyncImage(avatar, "Аватар пользователя", Modifier.size(78.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.AccountCircle, null, tint = YuberaMuted, modifier = Modifier.size(78.dp))
                        profile?.avatarFrame?.let { frame -> profileFrameResource(frame)?.let { Image(painterResource(it), "Рамка аватара", Modifier.fillMaxSize()) } }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                profile?.customName?.takeIf(String::isNotBlank)
                                    ?: profile?.displayName?.takeIf(String::isNotBlank)
                                    ?: account.user?.let { u -> listOfNotNull(u.firstName, u.lastName).joinToString(" ").ifBlank { u.username?.let { "@$it" } ?: "Пользователь" } }
                                    ?: "Пользователь",
                                color = YuberaOnBackground,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile?.isPlus == true) Text("PLUS", color = YuberaPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        profile?.username?.takeIf(String::isNotBlank)?.let { Text("@$it", color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }
                        profile?.let { p -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(if (p.visibility == "public") Icons.Default.Public else Icons.Default.Lock, null, tint = YuberaMuted, modifier = Modifier.size(14.dp)); Text(if (p.visibility == "public") "Публичный профиль" else "Приватный профиль", color = YuberaMuted, style = MaterialTheme.typography.labelSmall) } }
                        val bio = profile?.bio.orEmpty()
                        if (bio.isNotBlank()) {
                            val bioInteraction = rememberYuberaPressInteractionSource()
                            Text(bio, color = YuberaMuted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.yuberaPressScale(bioInteraction).clickable(interactionSource = bioInteraction, indication = androidx.compose.foundation.LocalIndication.current, enabled = bio.length > 100) { fullBio = true })
                            if (bio.length > 100) { val readBioInteraction = rememberYuberaPressInteractionSource(); Text("Читать полностью", color = YuberaPrimary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.yuberaPressScale(readBioInteraction).clickable(interactionSource = readBioInteraction, indication = androidx.compose.foundation.LocalIndication.current) { fullBio = true }) }
                        }
                    }
                    val editInteraction = rememberYuberaPressInteractionSource()
                    IconButton(onClick = onEditProfile, modifier = Modifier.size(44.dp).yuberaPressScale(editInteraction), interactionSource = editInteraction) { Icon(Icons.Default.Edit, "Редактировать профиль", tint = YuberaPrimary) }
                }
            }

            if (account.isLoading && profile == null) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = YuberaPrimary)
                        Text("Загружаем профиль…", color = YuberaMuted)
                    }
                }
            }
            if (profile == null && !account.isLoading) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(account.errorMessage ?: "Не удалось загрузить профиль.", color = YuberaMuted)
                        val retryProfileInteraction = rememberYuberaPressInteractionSource()
                        TextButton(onClick = viewModel::refreshAccountProfile, modifier = Modifier.yuberaPressScale(retryProfileInteraction), interactionSource = retryProfileInteraction) { Text("Повторить", color = YuberaPrimary) }
                    }
                }
            }

            ProfileLevelSupportCard(profile?.xp, profile?.support?.title)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileCounter(Icons.Default.BookmarkBorder, profile?.personalCollections, "Подборки", Modifier.weight(1f))
                ProfileCounter(Icons.Default.Movie, profile?.personalTitles, "Тайтлы", Modifier.weight(1f))
                ProfileCounter(Icons.Default.FavoriteBorder, profile?.personalCollectionLikes, "Лайки", Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Мои подборки", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                val allCollectionsInteraction = rememberYuberaPressInteractionSource()
                TextButton(onClick = onAllCollections, modifier = Modifier.yuberaPressScale(allCollectionsInteraction), interactionSource = allCollectionsInteraction) {
                    Text("Все подборки", color = YuberaPrimary)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.ArrowForward, null, tint = YuberaPrimary, modifier = Modifier.size(16.dp))
                }
            }
            val collections = response?.collections.orEmpty()
            if (collections.isEmpty() && response == null && account.isLoading) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = YuberaPrimary); Text("Загружаем подборки…", color = YuberaMuted) }
                }
            } else if (collections.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("У вас пока нет подборок.", color = YuberaMuted, modifier = Modifier.padding(16.dp))
                }
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    collections.forEach { collection ->
                        val chosenId = remember(collection.id, collection.animeIds, coverTick) { chooseCollectionCover(context, collection.id, collection.animeIds) }
                        val poster = catalog.firstOrNull { it.id == chosenId }?.posterUrl
                        val interaction = rememberYuberaPressInteractionSource()
                        Card(
                            onClick = { onCollection(collection.id) },
                            modifier = Modifier.width(210.dp).height(265.dp).yuberaPressScale(interaction),
                            interactionSource = interaction,
                            colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                if (!poster.isNullOrBlank()) AsyncImage(poster, collection.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Box(Modifier.fillMaxSize().background(YuberaSurfaceElevated), contentAlignment = Alignment.Center) { Icon(Icons.Default.BookmarkBorder, null, tint = YuberaMuted, modifier = Modifier.size(52.dp)) }
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .15f), Color.Black.copy(alpha = .9f)), startY = 0f, endY = 900f)))
                                Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(collection.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(if (collection.is_public == 1) Icons.Default.Public else Icons.Default.Lock, null, tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(14.dp))
                                        Text(if (collection.is_public == 1) "Публичная" else "Приватная", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.FavoriteBorder, null, tint = YuberaPrimary, modifier = Modifier.size(15.dp))
                                        Text(collection.like_count.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileDestinationCard(Icons.Default.Group, "Друзья", "Подписки, подписчики и поиск", onFriends)
                ProfileDestinationCard(Icons.Default.ReceiptLong, "Правовая информация", "Пользовательское соглашение и политика конфиденциальности", onLegal)
                ProfileDestinationCard(
                    Icons.Default.Info,
                    "О проекте",
                    "Версия ${BuildConfig.VERSION_NAME} · Описание и цели проекта",
                    onAbout
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ProfileLevelSupportCard(xp: YuberaXpStatus?, supportTitle: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Layers, null, tint = YuberaPrimary, modifier = Modifier.size(18.dp))
                        Text(if (xp != null) "Уровень ${xp.level}" else "Уровень —", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(xp?.title ?: "Название уровня", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                }
                Divider(Modifier.height(52.dp).width(1.dp), color = YuberaMuted.copy(alpha = .25f))
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Star, null, tint = YuberaPrimary, modifier = Modifier.size(18.dp))
                        Text("Поддержка проекта", color = YuberaOnBackground, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(supportTitle ?: "Без статуса", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            val progressTarget = (xp?.progress ?: 0f).coerceIn(0f,100f)/100f
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(progressTarget, androidx.compose.animation.core.tween(280), label = "profileXpProgress")
    LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = YuberaPrimary, trackColor = YuberaSurfaceElevated)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (xp != null && xp.nextThreshold != null) "${xp.total} / ${xp.nextThreshold} XP" else "${xp?.total ?: 0} XP", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                Text(if (xp?.nextLevel != null) "До ${xp.nextLevel} уровня: ${xp.remaining} XP" else if (xp != null) "Максимальный уровень" else "—", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ProfileCounter(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int?, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, tint = YuberaPrimary, modifier = Modifier.size(20.dp))
            Text(count?.toString() ?: "—", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileDestinationCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val interaction = rememberYuberaPressInteractionSource()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).yuberaPressScale(interaction),
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = YuberaSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = YuberaPrimary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = YuberaOnBackground, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = YuberaMuted, modifier = Modifier.size(18.dp))
        }
    }
}

private fun chooseCollectionCover(context: Context, collectionId: String, animeIds: List<String>): String? {
    if (animeIds.isEmpty()) return null
    val prefs = context.getSharedPreferences("yubera_profile_collection_covers", Context.MODE_PRIVATE)
    val idKey = "id_$collectionId"
    val timeKey = "time_$collectionId"
    val now = System.currentTimeMillis()
    val oldId = prefs.getString(idKey, null)
    val oldTime = prefs.getLong(timeKey, 0L)
    if (oldId in animeIds && now - oldTime < 3_600_000L) return oldId
    val candidates = if (animeIds.size > 1) animeIds.filter { it != oldId } else animeIds
    val next = candidates.randomOrNull() ?: oldId?.takeIf { it in animeIds } ?: animeIds.first()
    prefs.edit().putString(idKey, next).putLong(timeKey, now).apply()
    return next
}

private fun profileFrameResource(frame: String): Int? = when(frame) {
    "Admin_Frame_01.webp" -> R.drawable.admin_frame_01
    "Admin_Frame_02.webp" -> R.drawable.admin_frame_02
    "Admin_Frame_03.webp" -> R.drawable.admin_frame_03
    "Admin_Frame_04.webp" -> R.drawable.admin_frame_04
    "Admin_Frame_05.webp" -> R.drawable.admin_frame_05
    "Admin_Frame_06.webp" -> R.drawable.admin_frame_06
    "Admin_Frame_07.webp" -> R.drawable.admin_frame_07
    "Admin_Frame_08.webp" -> R.drawable.admin_frame_08
    "Ramka_Admin_01.webp" -> R.drawable.ramka_admin_01
    "YUBERA_Contribution_Avatar_Frame_01_Soyuznik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_01_soyuznik
    "YUBERA_Contribution_Avatar_Frame_02_Storonnik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_02_storonnik
    "YUBERA_Contribution_Avatar_Frame_03_Predanniy_Uchastnik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_03_predanniy_uchastnik
    "YUBERA_Contribution_Avatar_Frame_04_Pochetnyy_Pokrovitel.webp" -> R.drawable.yubera_contribution_avatar_frame_04_pochetnyy_pokrovitel
    "YUBERA_Contribution_Avatar_Frame_05_Legenda_YUBERA.webp" -> R.drawable.yubera_contribution_avatar_frame_05_legenda_yubera
    "YUBERA_Plus_Avatar_Frame_01_Sakura_Gold.webp" -> R.drawable.yubera_plus_avatar_frame_01_sakura_gold
    "YUBERA_Plus_Avatar_Frame_02_Mystic_Kitsune.webp" -> R.drawable.yubera_plus_avatar_frame_02_mystic_kitsune
    "YUBERA_Plus_Avatar_Frame_03_Samurai_Steel.webp" -> R.drawable.yubera_plus_avatar_frame_03_samurai_steel
    "YUBERA_Plus_Avatar_Frame_04_Celestial_Archive.webp" -> R.drawable.yubera_plus_avatar_frame_04_celestial_archive
    "YUBERA_Plus_Avatar_Frame_05_Cyberpunk_Neon.webp" -> R.drawable.yubera_plus_avatar_frame_05_cyberpunk_neon
    else -> null
}
