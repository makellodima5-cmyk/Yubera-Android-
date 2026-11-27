package com.yubera.app.features.catalog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
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
@OptIn(ExperimentalMaterial3Api::class)
fun CatalogScreen(
    onAnimeClick: (AnimeCatalogItem) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(title = "Каталог", subtitle = "Найди своё следующее аниме")
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = state.query, onValueChange = viewModel::onQueryChange, modifier = Modifier.weight(1f), placeholder = { Text("Поиск аниме") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, shape = RoundedCornerShape(16.dp))
            FilledTonalIconButton(onClick = { showFilters = true }, modifier = Modifier.size(54.dp)) { Icon(Icons.Default.FilterList, contentDescription = "Открыть фильтры") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Все тайтлы", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(state.items.size.toString(), color = YuberaMuted, style = MaterialTheme.typography.labelMedium)
        }
        if (state.isRefreshing && state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = YuberaPrimary) }
        } else if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.errorMessage != null) "Каталог пока недоступен" else "Ничего не найдено", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(state.errorMessage ?: "Измените поисковый запрос или параметры фильтрации.", color = YuberaMuted)
                    if (state.errorMessage != null) Button(onClick = viewModel::refresh) { Text("Повторить") }
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(state.items, key = AnimeCatalogItem::id) { anime -> CatalogPosterTile(anime) { onAnimeClick(anime) } }
            }
        }
    }
    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }, containerColor = YuberaSurface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Фильтры", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.onGenreSelected(null); viewModel.onYearSelected(null); viewModel.onStatusSelected(null); viewModel.onSortOrderSelected(CatalogSortOrder.CATALOG) }) { Text("Сбросить") }
                }
                Text("Сортировка", color = YuberaMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CatalogSortOrder.entries.forEach { order -> FilterChip(selected = state.sortOrder == order, onClick = { viewModel.onSortOrderSelected(order) }, label = { Text(order.title) }) } }
                Text("Жанр", color = YuberaMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.selectedGenre == null, onClick = { viewModel.onGenreSelected(null) }, label = { Text("Все") })
                    state.genres.forEach { genre -> FilterChip(selected = state.selectedGenre == genre, onClick = { viewModel.onGenreSelected(if (state.selectedGenre == genre) null else genre) }, label = { Text(genre) }) }
                }
                Text("Год", color = YuberaMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.selectedYear == null, onClick = { viewModel.onYearSelected(null) }, label = { Text("Любой") })
                    state.years.take(15).forEach { year -> FilterChip(selected = state.selectedYear == year, onClick = { viewModel.onYearSelected(if (state.selectedYear == year) null else year) }, label = { Text(year.toString()) }) }
                }
                Text("Статус", color = YuberaMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.selectedStatus == null, onClick = { viewModel.onStatusSelected(null) }, label = { Text("Любой") })
                    state.statuses.forEach { status -> FilterChip(selected = state.selectedStatus == status, onClick = { viewModel.onStatusSelected(if (state.selectedStatus == status) null else status) }, label = { Text(status) }) }
                }
                Button(onClick = { showFilters = false }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Показать результаты") }
            }
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            color = YuberaMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            content = content
        )
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


@Composable
private fun CatalogPosterTile(anime: AnimeCatalogItem, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = YuberaSurface)) {
            Box(Modifier.fillMaxWidth().aspectRatio(.70f)) {
                if (!anime.posterUrl.isNullOrBlank()) {
                    AsyncImage(model = anime.posterUrl, contentDescription = "Постер: " + anime.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(YuberaPrimary.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text(anime.title.take(1), color = YuberaPrimary, style = MaterialTheme.typography.displayMedium) }
                }
                anime.rating?.let { rating ->
                    Surface(color = YuberaBackground.copy(alpha = .88f), shape = RoundedCornerShape(bottomEnd = 10.dp), modifier = Modifier.align(Alignment.TopStart)) {
                        Text("★ " + "%.1f".format(rating), color = YuberaPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                    }
                }
                anime.ageRating?.takeIf { it.isNotBlank() }?.let { age ->
                    Surface(color = YuberaBackground.copy(alpha = .88f), shape = RoundedCornerShape(topStart = 8.dp), modifier = Modifier.align(Alignment.BottomEnd)) {
                        Text(age, color = YuberaOnBackground, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                }
            }
        }
        Text(anime.title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        Text(listOfNotNull(anime.year.takeIf { it > 0 }?.toString(), anime.kind?.takeIf { it.isNotBlank() }, anime.status.takeIf { it.isNotBlank() }).joinToString(" · "), color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
