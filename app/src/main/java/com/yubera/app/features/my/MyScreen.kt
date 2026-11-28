package com.yubera.app.features.my

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.components.yuberaPressScale
import com.yubera.app.core.ui.components.rememberYuberaPressInteractionSource
import com.yubera.app.core.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MyScreen(
    onAnimeClick: (String) -> Unit,
    onOpenSection: (String) -> Unit = {},
    section: String? = null,
    onNotificationsClick: () -> Unit = {},
    resetKey: Int = 0,
    viewModel: MyViewModel = hiltViewModel()
) {
    val s by viewModel.uiState.collectAsState()
    var centerIndex by rememberSaveable { mutableIntStateOf(0) }
    val pageListState = rememberLazyListState()
    LaunchedEffect(resetKey, section) { if (section == null) { pageListState.scrollToItem(0); centerIndex = 0 } }
    val rows = when (section) { "watch-later" -> s.watchLater; "history" -> s.history; "subscriptions" -> s.subscriptions; else -> emptyList() }
    val sectionTitle = when (section) { "watch-later" -> "Смотреть позже"; "history" -> "История просмотра"; "subscriptions" -> "Подписки"; else -> "Моё" }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            YuberaLogo(Modifier.weight(1f), 124.dp)
            Box {
                IconButton(onNotificationsClick) { Icon(Icons.Default.Notifications, "Уведомления", tint = YuberaOnBackground) }
                if (s.unreadNotificationCount > 0) Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(8.dp).background(YuberaRed, CircleShape))
            }
        }
        if (section != null) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton({ onOpenSection("") }) { Text("‹ Моё") }
                Text(sectionTitle, color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (rows.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Здесь пока нет тайтлов.", color = YuberaMuted) }
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.anime.id }) { row -> MyAnimeCard(row) { onAnimeClick(row.anime.id) } }
            }
        } else {
            LazyColumn(state = pageListState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("Моё", Modifier.padding(horizontal = 18.dp), color = YuberaOnBackground, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Favorite, null, tint = YuberaPrimary)
                            Text("Избранное", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        if (s.favorites.isEmpty()) Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp), colors = CardDefaults.cardColors(containerColor = YuberaSurface)) {
                            Column(Modifier.padding(18.dp)) { Text("Избранное пока пусто", color = YuberaOnBackground, fontWeight = FontWeight.SemiBold); Text("Добавляйте тайтлы с помощью сердечка на странице аниме.", color = YuberaMuted) }
                        } else Carousel(s.favorites, centerIndex.coerceIn(0, s.favorites.lastIndex), { centerIndex = it }, onAnimeClick, viewModel::removeFavorite)
                    }
                }
                item { Tile(Icons.Default.Bookmark, "Смотреть позже", "Тайтлы, которые вы отложили", s.watchLater.size) { onOpenSection("watch-later") } }
                item { Tile(Icons.Default.Notifications, "Подписки", "Тайтлы, за которыми вы следите", s.subscriptions.size) { onOpenSection("subscriptions") } }
                item { Tile(Icons.Default.History, "История просмотра", "Ранее просмотренные тайтлы", s.history.size) { onOpenSection("history") } }
            }
        }
    }
}

@Composable
private fun Carousel(fav: List<MyLibraryItem>, initial: Int, onCenter: (Int) -> Unit, onOpen: (String) -> Unit, onRemove: (String) -> Unit) {
    val context = LocalContext.current
    val motionAllowed = remember { runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }.getOrDefault(true) }
    val duration = if (motionAllowed) 260 else 0
    val scope = rememberCoroutineScope()
    var center by rememberSaveable { mutableIntStateOf(initial) }
    var drag by remember { mutableFloatStateOf(0f) }
    val n = fav.size
    LaunchedEffect(n, initial, fav.map { it.anime.id }) { center = if (n == 0) 0 else initial.coerceIn(0, n - 1) }
    val offsets = favoriteCarouselOffsets(n)
    val cards = offsets.mapNotNull { offset -> if (n == 0) null else { val index = ((center + offset) % n + n) % n; Triple(index, offset, fav[index]) } }
    Box(
        Modifier.fillMaxWidth().height(350.dp).pointerInput(n) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, delta -> change.consume(); drag += delta },
                onDragEnd = { if (abs(drag) > 48 && n > 1) { center = (center + (if (drag < 0) 1 else -1) + n) % n; onCenter(center) }; drag = 0f },
                onDragCancel = { drag = 0f }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        cards.forEach { (index, offset, item) ->
            val visualOffset = offset + (drag / 120f).coerceIn(-0.9f, 0.9f)
            val scale by animateFloatAsState((1f - abs(visualOffset) * .18f).coerceIn(.55f, 1f), tween(duration), label = "carousel-scale")
            val rotation by animateFloatAsState(visualOffset * -18f, tween(duration), label = "carousel-rotation")
            val blur by animateFloatAsState((abs(visualOffset) - 1f).coerceAtLeast(0f) * 1.6f, tween(duration), label = "carousel-blur")
            val alpha by animateFloatAsState((1f - abs(visualOffset) * .15f).coerceIn(.4f, 1f), tween(duration), label = "carousel-alpha")
            val animatedTranslationX by animateFloatAsState(visualOffset * 120f, tween(duration), label = "carousel-translation")
            Poster(
                item.anime,
                Modifier.width(214.dp).height(318.dp).graphicsLayer {
                    this.translationX = animatedTranslationX.dp.toPx()
                    translationY = (1f - kotlin.math.cos(visualOffset * .58f)) * 16.dp.toPx()
                    
                    scaleX = scale; scaleY = scale; rotationY = rotation; cameraDistance = 18 * density; this.alpha = alpha
                }.blur(blur.dp).zIndex(-abs(visualOffset).toFloat()),
                offset == 0,
                click = {
                    if (offset == 0) onOpen(item.anime.id)
                    else { center = index; onCenter(index); scope.launch { delay(duration.toLong()); onOpen(item.anime.id) } }
                },
                remove = {
                    if (item.anime.id == fav.getOrNull(center)?.anime?.id) {
                        val next = if (center >= fav.lastIndex) 0 else center
                        center = next; onCenter(next)
                    }
                    onRemove(item.anime.id)
                }
            )
        }
    }
}

@Composable
private fun Poster(a: AnimeCatalogItem, modifier: Modifier, central: Boolean, click: () -> Unit, remove: () -> Unit) {
    val interaction = rememberYuberaPressInteractionSource()
    Card(onClick = click, modifier = modifier.yuberaPressScale(interaction), interactionSource = interaction, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = YuberaSurface), border = BorderStroke(1.dp, YuberaBorder), elevation = CardDefaults.cardElevation(if (central) 8.dp else 2.dp)) {
        Box(Modifier.fillMaxSize()) {
            if (!a.posterUrl.isNullOrBlank()) AsyncImage(a.posterUrl, "Постер: ${a.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize().background(YuberaSurfaceElevated), contentAlignment = Alignment.Center) { Text(a.title.take(1), color = YuberaPrimary, style = MaterialTheme.typography.displayLarge) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, YuberaScrim))))
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) { a.rating?.let { Badge("★ %.1f".format(it), YuberaGold) }; a.ageRating?.let { Badge(it, Color.White) } }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 48.dp, bottom = 12.dp)) {
                Text(a.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (central) Text(listOfNotNull(a.kind, a.year.takeIf { it > 0 }?.toString()).joinToString(" · "), color = Color.White.copy(.7f), style = MaterialTheme.typography.labelSmall)
            }
            val removeInteraction = rememberYuberaPressInteractionSource()
            IconButton(remove, Modifier.align(Alignment.BottomEnd).padding(8.dp).size(38.dp).background(Color.Black.copy(.5f), RoundedCornerShape(50)).yuberaPressScale(removeInteraction), interactionSource = removeInteraction) { Icon(Icons.Default.Favorite, "Удалить из избранного", tint = YuberaRed) }
        }
    }
}

@Composable private fun Badge(text: String, color: Color) { Box(Modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(.65f)).padding(horizontal = 7.dp, vertical = 4.dp)) { Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun Tile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, count: Int, onClick: () -> Unit) {
    val interaction = rememberYuberaPressInteractionSource()
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).yuberaPressScale(interaction), interactionSource = interaction, colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, YuberaBorder)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = YuberaPrimary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) { Text(title, color = YuberaOnBackground, fontWeight = FontWeight.Bold); Text(subtitle, color = YuberaMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text(count.toString(), color = YuberaMuted)
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = YuberaMuted)
        }
    }
}

internal fun favoriteCarouselOffsets(itemCount: Int): List<Int> = when {
    itemCount <= 0 -> emptyList()
    itemCount == 1 -> listOf(0)
    itemCount == 2 -> listOf(0, 1)
    itemCount <= 4 -> listOf(-1, 0, 1)
    else -> listOf(-2, -1, 0, 1, 2)
}
