package com.yubera.app.features.my

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary

@Composable
fun MyScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: MyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Избранное", "Хочу посмотреть", "История", "Подписки")
    val lists = listOf(state.favorites, state.watchLater, state.history, state.subscriptions)
    val currentItems = lists[selectedTab]

    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(title = "Моё", subtitle = "Ваше персональное пространство")
        TabRow(selectedTabIndex = selectedTab, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
                    selectedContentColor = YuberaPrimary,
                    unselectedContentColor = YuberaMuted
                )
            }
        }
        if (currentItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Пока здесь пусто", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                Text(
                    when (selectedTab) {
                        0 -> "Добавляйте аниме в избранное на странице тайтла."
                        1 -> "Сохраняйте тайтлы, которые хотите посмотреть позже."
                        2 -> "История появится после сохранения прогресса просмотра."
                        else -> "Подписывайтесь на интересующие тайтлы, чтобы следить за новыми сериями."
                    },
                    color = YuberaMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentItems, key = { it.anime.id }) { item ->
                    MyAnimeCard(item = item, onClick = { onAnimeClick(item.anime.id) })
                }
            }
        }
    }
}
