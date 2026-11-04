package com.yubera.app.features.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun CatalogScreen(
    onAnimeClick: (AnimeCatalogItem) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(title = "Каталог", subtitle = "Аниме, которое стоит открыть")
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Название, оригинальное или английское") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Text(
                "Сортировка",
                color = YuberaMuted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(CatalogSortOrder.entries) { order ->
                    FilterChip(
                        selected = state.sortOrder == order,
                        onClick = { viewModel.onSortOrderSelected(order) },
                        label = { Text(order.title) }
                    )
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.width(8.dp)) }
                item {
                    FilterChip(
                        selected = state.selectedGenre == null,
                        onClick = { viewModel.onGenreSelected(null) },
                        label = { Text("Все жанры") }
                    )
                }
                items(state.genres) { genre ->
                    FilterChip(
                        selected = state.selectedGenre == genre,
                        onClick = { viewModel.onGenreSelected(if (state.selectedGenre == genre) null else genre) },
                        label = { Text(genre) }
                    )
                }
                item { Spacer(Modifier.width(8.dp)) }
            }

            if (state.isRefreshing && state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = YuberaPrimary)
                }
            } else if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.errorMessage != null) "Каталог пока недоступен" else "Ничего не найдено",
                            color = YuberaOnBackground,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage ?: "Попробуйте изменить запрос или снять фильтр жанра.",
                            color = YuberaMuted
                        )
                        if (state.errorMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = viewModel::refresh) { Text("Повторить") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Найдено: ${state.items.size}",
                            color = YuberaMuted,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                    if (state.errorMessage != null) {
                        item {
                            Text(
                                text = "Показан сохранённый каталог. Обновление не удалось.",
                                color = YuberaMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(state.items, key = AnimeCatalogItem::id) { anime ->
                        CatalogAnimeCard(
                            anime = anime,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = { onAnimeClick(anime) }
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CatalogAnimeCard(
    anime: AnimeCatalogItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = YuberaSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!anime.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = anime.posterUrl,
                    contentDescription = "Постер: ${anime.title}",
                    modifier = Modifier.size(width = 104.dp, height = 146.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(width = 104.dp, height = 146.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(YuberaPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(anime.title.take(1), color = YuberaPrimary, style = MaterialTheme.typography.headlineLarge)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(anime.title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold, maxLines = 2)
                if (anime.originalTitle.isNotBlank()) {
                    Text(anime.originalTitle, color = YuberaMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = listOfNotNull(
                        anime.year.takeIf { it > 0 }?.toString(),
                        anime.status,
                        anime.episodes.takeIf { it > 0 }?.let { "$it эп." }
                    ).joinToString(" · "),
                    color = YuberaMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                anime.rating?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("★ ${"%.1f".format(it)}", color = YuberaPrimary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = anime.genres.take(3).joinToString(" · "),
                    color = YuberaMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = anime.description.ifBlank { "Описание пока не добавлено." },
                    color = YuberaOnBackground.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }
        }
    }
}
