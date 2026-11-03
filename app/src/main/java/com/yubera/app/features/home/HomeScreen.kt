package com.yubera.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground

@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(
            title = "Главная",
            subtitle = "Твой мир аниме начинается здесь",
            showBanner = false
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (state.errorMessage != null) {
                item {
                    Text(
                        state.errorMessage.orEmpty(),
                        color = YuberaMuted,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Продолжить просмотр", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge)
                    if (state.continueWatching.isEmpty()) {
                        Text(
                            "Здесь появятся тайтлы, в которых сохранён прогресс просмотра.",
                            color = YuberaMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        state.continueWatching.forEach { item ->
                            HomeProgressCard(
                                title = item.anime.title,
                                episode = item.progress.episodeNumber,
                                onClick = { onAnimeClick(item.anime.id) }
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Из каталога YUBERA",
                        color = YuberaOnBackground,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.cataloguePicks, key = AnimeCatalogItem::id) { anime ->
                            HomeAnimeTile(anime = anime, onClick = { onAnimeClick(anime.id) })
                        }
                    }
                }
            }
        }
    }
}
