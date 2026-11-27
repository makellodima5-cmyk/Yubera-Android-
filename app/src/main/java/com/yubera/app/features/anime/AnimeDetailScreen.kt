package com.yubera.app.features.anime

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.AnimeScreenshot
import com.yubera.app.core.player.KodikLinkPolicy
import com.yubera.app.core.player.KodikEpisode
import com.yubera.app.features.player.KodikEmbedWebView
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface
import java.text.DateFormat
import java.util.Date

@Composable
fun AnimeDetailScreen(
    onBack: () -> Unit,
    onWatchClick: () -> Unit,
    onAnimeClick: (String) -> Unit = {},
    viewModel: AnimeDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val related by viewModel.relatedAnime.collectAsState()
    var expandedDescription by remember { mutableStateOf(false) }
    var selectedFrame by remember { mutableIntStateOf(-1) }
    var showSubscribeDialog by remember { mutableStateOf(false) }
    var showDeleteDialogFor by remember { mutableStateOf<String?>(null) }
    var commentDraft by remember { mutableStateOf("") }
    var trailerMode by remember { mutableStateOf(false) }
    var selectedVoiceId by remember { mutableStateOf<String?>(null) }
    var playerVoiceId by remember { mutableStateOf<String?>(null) }
    var playerEpisodeNumber by remember { mutableIntStateOf(1) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = YuberaOnBackground)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { YuberaLogo(width = 122.dp) }
            Spacer(Modifier.width(44.dp))
        }

        val anime = state.anime
        if (anime == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator(color = YuberaPrimary)
                else Text("Тайтл не найден в каталоге.", color = YuberaMuted)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TitleHero(anime)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { viewModel.toggleFavorite(!state.isFavorite) }, modifier = Modifier.weight(1f)) {
                        Text(if (state.isFavorite) "♥ В избранном" else "♡ В избранное", maxLines = 1)
                    }
                    OutlinedButton(onClick = { viewModel.toggleWatchLater(!state.isInWatchLater) }, modifier = Modifier.weight(1f)) {
                        Text(if (state.isInWatchLater) "✓ В моём списке" else "+ Смотреть позже", maxLines = 1)
                    }
                }
                if (anime.status.contains("ongoing", true) || anime.status.contains("онгоинг", true) || anime.nextEpisodeAt != null) {
                    Button(onClick = { viewModel.loadSubscriptionSources(); showSubscribeDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.isSubscribed) "Подписка включена" else "Подписаться на серии")
                    }
                }
                RatingSection(anime.rating, state.localRating, viewModel::saveRating)

                if (anime.description.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Описание")
                        Text(anime.description, color = YuberaOnBackground.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (expandedDescription) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis)
                        if (anime.description.length > 420) TextButton(onClick = { expandedDescription = !expandedDescription }) {
                            Text(if (expandedDescription) "Свернуть" else "Развернуть")
                        }
                    }
                }

                if (anime.screenshots.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Кадры из аниме")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
                            items(anime.screenshots.take(10), key = { it.previewUrl + it.originalUrl }) { frame ->
                                AsyncImage(model = frame.previewUrl.ifBlank { frame.originalUrl }, contentDescription = "Кадр из ${anime.title}",
                                    modifier = Modifier.width(230.dp).height(132.dp).clip(RoundedCornerShape(14.dp)).clickable { selectedFrame = anime.screenshots.indexOf(frame) },
                                    contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }

                LaunchedEffect(anime.id, anime.kodikAvailable) {
                    if (anime.kodikAvailable == true) viewModel.loadSubscriptionSources()
                }
                LaunchedEffect(anime.id) {
                    playerVoiceId = null
                    playerEpisodeNumber = 1
                }
                val hasTrailer = !anime.youtubeTrailerId.isNullOrBlank()
                LaunchedEffect(anime.id, state.availableSources) {
                    val selected = state.availableSources.firstOrNull { it.id == playerVoiceId }
                        ?: state.availableSources.firstOrNull()
                    if (selected != null && selected.id != playerVoiceId) {
                        playerVoiceId = selected.id
                        playerEpisodeNumber = selected.episodes.firstOrNull()?.number ?: 1
                    } else if (selected != null && selected.episodes.none { it.number == playerEpisodeNumber }) {
                        playerEpisodeNumber = selected.episodes.firstOrNull()?.number ?: 1
                    }
                }
                if (anime.kodikAvailable == true || hasTrailer) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Видео")
                        if (anime.kodikAvailable == true && hasTrailer) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !trailerMode, onClick = { trailerMode = false }, label = { Text("Смотреть аниме") })
                                FilterChip(selected = trailerMode, onClick = { trailerMode = true }, label = { Text("Трейлер") })
                            }
                        }
                        if (trailerMode && hasTrailer) {
                            YoutubeTrailer(anime.youtubeTrailerId.orEmpty())
                        } else if (anime.kodikAvailable == true) {
                            val selectedSource = state.availableSources.firstOrNull { it.id == playerVoiceId }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Озвучка", color = YuberaMuted, style = MaterialTheme.typography.labelMedium)
                                if (state.sourcesLoading && state.availableSources.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = YuberaPrimary, modifier = Modifier.size(24.dp))
                                    }
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
                                        items(state.availableSources, key = { it.id }) { source ->
                                            FilterChip(
                                                selected = playerVoiceId == source.id,
                                                onClick = {
                                                    playerVoiceId = source.id
                                                    playerEpisodeNumber = source.episodes.firstOrNull()?.number ?: 1
                                                },
                                                label = { Text(source.title, maxLines = 1) }
                                            )
                                        }
                                    }
                                }
                                Text("Серия", color = YuberaMuted, style = MaterialTheme.typography.labelMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
                                    items(selectedSource?.episodes.orEmpty(), key = { it.number }) { episode ->
                                        FilterChip(
                                            selected = playerEpisodeNumber == episode.number,
                                            onClick = { playerEpisodeNumber = episode.number },
                                            label = { Text(episode.number.toString()) }
                                        )
                                    }
                                }
                                val selectedEpisode = selectedSource?.episodes?.firstOrNull { it.number == playerEpisodeNumber }
                                if (selectedEpisode != null && KodikLinkPolicy.isTrustedHttpsUrl(selectedEpisode.link)) {
                                    KodikEmbedWebView(
                                        url = selectedEpisode.link,
                                        retryNonce = 0,
                                        modifier = Modifier.fillMaxWidth().height(250.dp),
                                        onFullscreenChanged = {}
                                    )
                                } else {
                                    Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
                                        Text(
                                            if (state.sourcesLoading) "Загружаем доступные озвучки и серии…" else "Для выбранной озвучки нет доступных серий.",
                                            color = YuberaMuted,
                                            modifier = Modifier.fillMaxWidth().padding(18.dp)
                                        )
                                    }
                                }
                            }
                        } else if (hasTrailer) {
                            YoutubeTrailer(anime.youtubeTrailerId.orEmpty())
                        }
                    }
                }

                if (related.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Похожие тайтлы")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 4.dp)) {
                            items(related, key = AnimeCatalogItem::id) { item -> RelatedAnimeCard(item, onClick = { onAnimeClick(item.id) }) }
                        }
                    }
                }
                CommentsSection(comments = state.localComments, draft = commentDraft, onDraftChange = { commentDraft = it },
                    onSend = { viewModel.addComment(commentDraft); commentDraft = "" }, onLike = viewModel::toggleCommentLike,
                    onDelete = { showDeleteDialogFor = it })
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (selectedFrame >= 0) state.anime?.let { loadedAnime -> FrameViewer(loadedAnime.screenshots, selectedFrame) { selectedFrame = -1 } }

    if (showSubscribeDialog) {
        AlertDialog(onDismissRequest = { showSubscribeDialog = false }, title = { Text("Подписка на серии") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.sourcesLoading) CircularProgressIndicator(color = YuberaPrimary)
                    else if (state.availableSources.isEmpty()) Text("Озвучки не удалось получить. Можно включить общую подписку на новые серии.")
                    else {
                        Text("Выберите озвучку. Android пока сохраняет общую подписку на тайтл; выбранная озвучка не передаётся серверу.")
                        state.availableSources.forEach { source ->
                            FilterChip(selected = selectedVoiceId == source.id, onClick = { selectedVoiceId = source.id }, label = { Text(source.title) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.toggleSubscription(true); showSubscribeDialog = false }) { Text(if (state.isSubscribed) "Оставить подписку" else "Подписаться") } },
            dismissButton = { TextButton(onClick = { viewModel.toggleSubscription(false); showSubscribeDialog = false }) { Text("Отменить подписку") } }
        )
    }

    showDeleteDialogFor?.let { id ->
        AlertDialog(onDismissRequest = { showDeleteDialogFor = null }, title = { Text("Удалить комментарий?") },
            text = { Text("Комментарий будет удалён с этого устройства.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteComment(id); showDeleteDialogFor = null }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { showDeleteDialogFor = null }) { Text("Отмена") } })
    }
}

@Composable
private fun TitleHero(anime: AnimeCatalogItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box {
            AsyncImage(model = anime.posterUrl, contentDescription = "Постер ${anime.title}", modifier = Modifier.width(142.dp).height(205.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            anime.rating?.let { Surface(color = YuberaBackground.copy(alpha = 0.92f), shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.TopStart).padding(7.dp)) {
                Text("★ %.1f".format(it), color = YuberaPrimary, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
            } }
            anime.ageRating?.takeIf(String::isNotBlank)?.let { Surface(color = YuberaBackground.copy(alpha = 0.92f), shape = RoundedCornerShape(7.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp)) {
                Text(it, color = YuberaOnBackground, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
            } }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(anime.title, color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            anime.originalTitle.takeIf(String::isNotBlank)?.let { Text(it, color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }
            anime.englishTitle?.takeIf(String::isNotBlank)?.let { Text(it, color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }
            listOfNotNull(anime.status.takeIf(String::isNotBlank), anime.kind?.takeIf(String::isNotBlank), anime.year.takeIf { it > 0 }?.toString(),
                anime.episodes.takeIf { it > 0 }?.let { "$it сер." }, anime.episodesAired?.takeIf { it > 0 }?.let { "Вышло $it" }).forEach { MetaPill(it) }
            anime.genres.forEach { MetaPill(it) }
        }
    }
}

@Composable
private fun MetaPill(text: String) { Surface(color = YuberaSurface, shape = RoundedCornerShape(8.dp)) {
    Text(text, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
} }

@Composable
private fun RatingSection(globalRating: Double?, localRating: Int?, onRate: (Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Оценка")
                globalRating?.let { Text("★ %.1f".format(it), color = YuberaPrimary, fontWeight = FontWeight.Bold) }
            }
            Text("Ваш рейтинг на устройстве", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..10).forEach { value -> IconButton(onClick = { onRate(value) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "Оценка $value из 10", tint = if (value <= (localRating ?: 0)) YuberaPrimary else YuberaMuted.copy(alpha = 0.45f))
                } }
            }
            localRating?.let { Text("Вы поставили $it/10", color = YuberaPrimary, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) { Text(title, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }

@Composable
private fun RelatedAnimeCard(anime: AnimeCatalogItem, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp), modifier = Modifier.width(146.dp)) {
        Box {
            AsyncImage(model = anime.posterUrl, contentDescription = anime.title, modifier = Modifier.fillMaxWidth().height(206.dp), contentScale = ContentScale.Crop)
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(YuberaBackground.copy(alpha = 0.92f)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                anime.rating?.let { Text("★ %.1f".format(it), color = YuberaPrimary, style = MaterialTheme.typography.labelSmall) }
                anime.ageRating?.let { Text(it, color = YuberaMuted, style = MaterialTheme.typography.labelSmall) }
                Text(anime.title, color = YuberaOnBackground, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CommentsSection(comments: List<LocalTitleComment>, draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit, onLike: (String) -> Unit, onDelete: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Комментарии · ${comments.size}")
        Text("Комментарии и лайки сохраняются локально на этом устройстве, пока серверная система комментариев не подключена.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = draft, onValueChange = onDraftChange, modifier = Modifier.weight(1f), placeholder = { Text("Написать комментарий…") }, maxLines = 4)
            IconButton(onClick = onSend, enabled = draft.isNotBlank()) { Icon(Icons.Default.Send, contentDescription = "Отправить", tint = YuberaPrimary) }
        }
        if (comments.isEmpty()) Text("Пока нет комментариев. Будьте первым.", color = YuberaMuted)
        comments.forEach { comment ->
            Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.author, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(comment.createdAtEpochMillis)), color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = { onDelete(comment.id) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Delete, contentDescription = "Удалить собственный комментарий", tint = YuberaMuted) }
                    }
                    Text(comment.text, color = YuberaOnBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onLike(comment.id) }, modifier = Modifier.size(34.dp)) {
                            Icon(if (comment.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Лайк", tint = if (comment.likedByMe) YuberaPrimary else YuberaMuted)
                        }
                        Text(comment.likes.toString(), color = YuberaMuted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameViewer(frames: List<AnimeScreenshot>, initialIndex: Int, onClose: () -> Unit) {
    var index by remember { mutableIntStateOf(initialIndex.coerceIn(frames.indices)) }
    Dialog(onDismissRequest = onClose) {
        Surface(color = YuberaBackground, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Кадр ${index + 1} из ${frames.size}", color = YuberaOnBackground, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = YuberaOnBackground) }
                }
                AsyncImage(model = frames[index].originalUrl.ifBlank { frames[index].previewUrl }, contentDescription = null, modifier = Modifier.fillMaxWidth().height(240.dp), contentScale = ContentScale.Fit)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { index = (index - 1).coerceAtLeast(0) }, enabled = index > 0) { Text("←") }
                    TextButton(onClick = { index = (index + 1).coerceAtMost(frames.lastIndex) }, enabled = index < frames.lastIndex) { Text("→") }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeTrailer(videoId: String) {
    AndroidView(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp)),
        factory = { context -> WebView(context).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.mediaPlaybackRequiresUserGesture = true
            webViewClient = WebViewClient(); loadUrl("https://www.youtube.com/embed/${videoId}?playsinline=1")
        } },
        update = { view -> if (view.url.isNullOrBlank()) view.loadUrl("https://www.youtube.com/embed/${videoId}?playsinline=1") },
        onRelease = { view -> view.stopLoading(); view.destroy() })
}
