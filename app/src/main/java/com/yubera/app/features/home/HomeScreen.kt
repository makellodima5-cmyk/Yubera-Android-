package com.yubera.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary

@Composable
fun HomeScreen(
    onAnimeClick:(String)->Unit,
    onNotificationsClick:()->Unit={},
    onCollectionsClick:()->Unit={},
    viewModel:HomeViewModel=hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            YuberaLogo(width=124.dp)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            IconButton(onClick=onNotificationsClick) { Icon(Icons.Default.Notifications,contentDescription="Уведомления",tint=YuberaOnBackground) }
        }
        LazyColumn(
            modifier=Modifier.fillMaxSize(),
            contentPadding=PaddingValues(bottom=24.dp),
            verticalArrangement=Arrangement.spacedBy(22.dp)
        ) {
            if(state.errorMessage!=null) item { Text(state.errorMessage.orEmpty(),color=YuberaMuted,modifier=Modifier.padding(horizontal=18.dp)) }
            if(state.recommended.isNotEmpty()) item {
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    HomeSectionHeader("Рекомендуем")
                    LazyRow(contentPadding=PaddingValues(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        items(state.recommended,key=AnimeCatalogItem::id) { anime -> RecommendedCard(anime){onAnimeClick(anime.id)} }
                    }
                }
            }
            item {
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    HomeSectionHeader("Продолжить просмотр")
                    if(state.continueWatching.isEmpty()) Text("Здесь появятся тайтлы, просмотр которых не завершён.",color=YuberaMuted,modifier=Modifier.padding(horizontal=18.dp))
                    else LazyRow(contentPadding=PaddingValues(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        items(state.continueWatching,key={it.anime.id}) { item -> HomeProgressCard(item.anime,item.progress){onAnimeClick(item.anime.id)} }
                    }
                }
            }
            if(state.popular.isNotEmpty()) item { AnimeStrip("Популярное",state.popular,onAnimeClick) }
            if(state.newReleases.isNotEmpty()) item { AnimeStrip("Новинки",state.newReleases,onAnimeClick) }
            if(state.announcements.isNotEmpty()) item { AnimeStrip("Анонсы",state.announcements,onAnimeClick) }
            if(state.ongoing.isNotEmpty()) item { AnimeStrip("Онгоинги",state.ongoing,onAnimeClick) }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.Card(onClick={state.randomPick?.let{onAnimeClick(it.id)}},modifier=Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("⚄ Случайное аниме",fontWeight=FontWeight.Bold,color=YuberaOnBackground)
                            Text("Подобрать тайтл",color=YuberaMuted,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    androidx.compose.material3.Card(onClick=onCollectionsClick,modifier=Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("♧ Подборки",fontWeight=FontWeight.Bold,color=YuberaOnBackground)
                            Text("Тематические коллекции",color=YuberaMuted,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeStrip(title:String,items:List<AnimeCatalogItem>,onAnimeClick:(String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        HomeSectionHeader(title)
        LazyRow(contentPadding=PaddingValues(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(items,key=AnimeCatalogItem::id) { anime -> HomeAnimeTile(anime){onAnimeClick(anime.id)} }
        }
    }
}
