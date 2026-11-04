package com.yubera.app.features.news

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(title = "Новости", subtitle = "Публикации новостного форума Shikimori")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 4.dp))
            NewsCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category.title) }
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(end = 4.dp))
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
        } else if (state.articles.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(
                    if (state.query.isNotBlank()) "Ничего не найдено" else "Публикаций пока нет",
                    color = YuberaOnBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    state.errorMessage ?: "Измените запрос или выберите другую категорию.",
                    color = YuberaMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (state.errorMessage != null) {
                    Button(onClick = viewModel::refresh, modifier = Modifier.padding(top = 12.dp)) { Text("Повторить") }
                }
            }
        } else {
            val uriHandler = LocalUriHandler.current
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.errorMessage != null) {
                    item { Text(state.errorMessage.orEmpty(), color = YuberaMuted) }
                }
                items(state.articles, key = NewsArticleEntity::id) { article ->
                    NewsArticleCard(article) { uriHandler.openUri(article.sourceUrl) }
                }
            }
        }
    }
}

@Composable
private fun NewsArticleCard(article: NewsArticleEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = YuberaSurface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(article.title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold)
            article.linkedTitle?.let {
                Text(it, color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (article.summary.isNotBlank()) {
                Text(article.summary, color = YuberaOnBackground, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
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
        }
    }
}
