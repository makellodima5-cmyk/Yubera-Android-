package com.yubera.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.*

@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    onNotificationsClick: () -> Unit = {},
    onRandomAnimeClick: () -> Unit = {},
    onCollectionsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var currentHero by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            YuberaLogo(Modifier.align(Alignment.Center), width = 132.dp)
            IconButton(onClick = onNotificationsClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.NotificationsNone, contentDescription = "Уведомления", tint = YuberaOnBackground)
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            state.errorMessage?.let { message -> item { Text(message, color = YuberaMuted, modifier = Modifier.padding(horizontal = 18.dp)) } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeSectionTitle("Рекомендуем")
                    if (state.recommendedPicks.isNotEmpty()) {
                        val position = currentHero.coerceIn(state.recommendedPicks.indices)
                        val hero = state.recommendedPicks[position]
                        Card(onClick = { onAnimeClick(hero.id) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(230.dp), shape = RoundedCornerShape(22.dp)) {
                            Box(Modifier.fillMaxSize()) {
                                AsyncImage(model = hero.posterUrl, contentDescription = hero.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .88f)))))
                                Column(Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(.82f)) {
                                    Text("РЕКОМЕНДУЕМ", color = YuberaPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text(hero.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                                    Text(hero.genres.take(3).joinToString(" · "), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            state.recommendedPicks.forEachIndexed { position, _ ->
                                FilterChip(selected = currentHero == position, onClick = { currentHero = position }, label = { Text((position + 1).toString()) })
                            }
                        }
                    } else { HomeEmptyState("Рекомендации появятся после загрузки каталога") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeSectionTitle("Продолжить просмотр")
                    if (state.continueWatching.isEmpty()) HomeEmptyState("Здесь появятся тайтлы, в которых сохранён прогресс просмотра.")
                    else LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.continueWatching, key = { it.anime.id }) { progress ->
                            HomeProgressCard(progress.anime, progress.progress) { onAnimeClick(progress.anime.id) }
                        }
                    }
                }
            }
            item { HomeAnimeSection("Популярное", state.topRatedPicks, onAnimeClick) }
            item { HomeAnimeSection("Новинки", state.newPicks, onAnimeClick) }
            item { HomeAnimeSection("Анонсы", state.announcedPicks, onAnimeClick) }
            item { HomeAnimeSection("Онгоинги", state.ongoingPicks, onAnimeClick) }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeActionCard("Случайное аниме", "Подобрать тайтл", Icons.Default.Casino, Modifier.weight(1f), onRandomAnimeClick)
                    HomeActionCard("Подборки", "Тематические коллекции", Icons.Default.CollectionsBookmark, Modifier.weight(1f), onCollectionsClick)
                }
            }
        }
    }
    
}

@Composable
private fun HomeSectionTitle(title: String) {
    Text(title, color = YuberaOnBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun HomeEmptyState(message: String) {
    Text(message, color = YuberaMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
}

@Composable
private fun HomeAnimeSection(title: String, itemsList: List<AnimeCatalogItem>, onAnimeClick: (String) -> Unit) {
    if (itemsList.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionTitle(title)
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(itemsList, key = AnimeCatalogItem::id) { anime ->
                Card(onClick = { onAnimeClick(anime.id) }, modifier = Modifier.width(142.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = YuberaSurface)) {
                    Box(Modifier.fillMaxWidth().height(196.dp)) {
                        AsyncImage(model = anime.posterUrl, contentDescription = anime.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .85f)))))
                        anime.rating?.let { Text("★ " + "%.1f".format(it), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(YuberaBackground.copy(alpha = .86f), RoundedCornerShape(8.dp)).padding(6.dp)) }
                        Column(Modifier.align(Alignment.BottomStart).padding(9.dp)) {
                            Text(anime.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(listOfNotNull(anime.year.takeIf { it > 0 }?.toString(), anime.kind?.takeIf { it.isNotBlank() }).joinToString(" · "), color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = YuberaSurface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = YuberaPrimary, modifier = Modifier.size(25.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
        }
    }
}
