package com.yubera.app.features.news

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.*

@Composable
fun NewsScreen(
    onArticleClick: (Long) -> Unit,
    viewModel: NewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SectionBannerHeader(title = "Новости", subtitle = "Публикации новостного форума Shikimori")

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.width(4.dp))
            NewsCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category.title) }
                )
            }
            FilterChip(
                selected = state.showBookmarkedOnly,
                onClick = { viewModel.showBookmarkedOnly(!state.showBookmarkedOnly) },
                label = { Text("Закладки") }
            )
            Spacer(Modifier.width(4.dp))
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Поиск по заголовку и тексту") },
            singleLine = true
        )

        Text(
            "Источник: Shikimori. Это лента публикаций одного источника, а не полный обзор всех новостей индустрии.",
            color = YuberaMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (state.isRefreshing && state.articles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.articles.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    when {
                        state.showBookmarkedOnly -> "Закладок пока нет"
                        state.query.isNotBlank() -> "Ничего не найдено"
                        else -> "Публикаций пока нет"
                    },
                    color = YuberaOnBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    state.errorMessage ?: if (state.showBookmarkedOnly) {
                        "Сохраняйте интересные публикации, чтобы вернуться к ним позже."
                    } else {
                        "Измените запрос или выберите другую категорию."
                    },
                    color = YuberaMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (state.errorMessage != null) {
                    Button(
                        onClick = viewModel::refresh,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Повторить")
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.errorMessage != null) {
                    item { Text(state.errorMessage.orEmpty(), color = YuberaMuted) }
                }
                items(state.articles, key = NewsArticleEntity::id) { article ->
                    Card(
                        onClick = { onArticleClick(article.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = YuberaSurface)
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                article.title,
                                color = YuberaOnBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                            article.linkedTitle?.let {
                                Text(it, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            if (article.summary.isNotBlank()) {
                                Text(
                                    article.summary,
                                    color = YuberaOnBackground,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 4
                                )
                            }
                            Text(
                                listOfNotNull(
                                    article.authorName.takeIf(String::isNotBlank),
                                    article.commentsCount.takeIf { it > 0 }?.let { "$it комментариев" },
                                    article.createdAt.take(10),
                                    "Shikimori"
                                ).joinToString(" · "),
                                color = YuberaMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { viewModel.toggleBookmark(article.id) }) {
                                Text(
                                    if (article.id in state.bookmarkedIds) {
                                        "В закладках"
                                    } else {
                                        "Добавить в закладки"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
