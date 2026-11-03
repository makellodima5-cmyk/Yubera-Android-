package com.yubera.app.features.anime

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun AnimeDetailScreen(
    onBack: () -> Unit,
    onWatchClick: () -> Unit,
    viewModel: AnimeDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = YuberaOnBackground)
            }
            Text("Об аниме", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
        }

        val anime = state.anime
        if (anime == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = YuberaPrimary)
                } else {
                    Text("Тайтл не найден в сохранённом каталоге.", color = YuberaMuted)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    if (!anime.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = anime.posterUrl,
                            contentDescription = "Постер: ${anime.title}",
                            modifier = Modifier.size(width = 148.dp, height = 210.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(width = 148.dp, height = 210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(YuberaPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(anime.title.take(1), color = YuberaPrimary, style = MaterialTheme.typography.displayLarge)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(anime.title, color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        anime.englishTitle?.takeIf(String::isNotBlank)?.let {
                            Text(it, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        anime.originalTitle.takeIf(String::isNotBlank)?.let {
                            Text(it, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        anime.rating?.let {
                            Text("★ ${"%.1f".format(it)}", color = YuberaPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            listOfNotNull(
                                anime.year.takeIf { it > 0 }?.toString(),
                                anime.kind,
                                anime.ageRating
                            ).joinToString(" · "),
                            color = YuberaMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Информация", color = YuberaOnBackground, fontWeight = FontWeight.SemiBold)
                        DetailLine("Статус", anime.status)
                        DetailLine("Эпизоды", if (anime.episodes > 0) "${anime.episodes}" else "Не указано")
                        anime.episodesAired?.let { DetailLine("Вышло", it.toString()) }
                        DetailLine("Жанры", anime.genres.joinToString(", ").ifBlank { "Не указаны" })
                        DetailLine("Студии", anime.studios.joinToString(", ").ifBlank { "Не указаны" })
                        anime.nextEpisodeAt?.let { DetailLine("Следующий эпизод", it) }
                    }
                }

                Text("Описание", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    anime.description.ifBlank { "Описание пока не добавлено." },
                    color = YuberaOnBackground.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (anime.kodikAvailable == false) {
                    Text("Просмотр через Kodik сейчас недоступен.", color = YuberaMuted)
                } else {
                    Button(onClick = onWatchClick, modifier = Modifier.fillMaxWidth()) { Text("Смотреть") }
                }

                Text("Моя библиотека", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { viewModel.toggleFavorite(!state.isFavorite) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.isFavorite) "♥  В избранном" else "♡  В избранное") }
                OutlinedButton(
                    onClick = { viewModel.toggleWatchLater(!state.isInWatchLater) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.isInWatchLater) "✓  В списке «Хочу посмотреть»" else "+  Хочу посмотреть") }
                Button(
                    onClick = { viewModel.toggleSubscription(!state.isSubscribed) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.isSubscribed) "Подписка включена" else "Следить за новыми сериями") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = YuberaMuted, modifier = Modifier.weight(0.8f))
            Text(value, color = YuberaOnBackground, modifier = Modifier.weight(1.2f))
        }
    }
}
