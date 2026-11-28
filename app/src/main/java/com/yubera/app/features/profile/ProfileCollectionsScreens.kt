package com.yubera.app.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface
import com.yubera.app.core.ui.components.yuberaPressScale
import com.yubera.app.core.ui.components.rememberYuberaPressInteractionSource
import com.yubera.app.data.auth.YuberaCollectionSummary

@Composable
fun ProfileCollectionsScreen(
    onBack: () -> Unit,
    onCollectionClick: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val response by viewModel.communityData.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refreshAccountProfile() }
    Column(Modifier.fillMaxSize()) {
        TextButton(onBack) { Text("‹ Профиль") }
        Text("Мои подборки", color = YuberaOnBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        val collections = response?.collections.orEmpty()
        when {
            response == null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(color = YuberaPrimary)
                Text("Загружаем подборки…", color = YuberaMuted)
            }
            collections.isEmpty() -> Text("Подборок пока нет.", color = YuberaMuted, modifier = Modifier.padding(18.dp))
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(collections, key = YuberaCollectionSummary::id) { collection ->
                    val cover = catalog.firstOrNull { it.id in collection.animeIds }?.posterUrl
                    val interaction = rememberYuberaPressInteractionSource()
                    Card(onClick = { onCollectionClick(collection.id) }, modifier = Modifier.fillMaxWidth().yuberaPressScale(interaction), interactionSource = interaction, colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!cover.isNullOrBlank()) AsyncImage(cover, "Обложка подборки", Modifier.height(112.dp).weight(0.7f), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(collection.title, color = YuberaOnBackground, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(collection.description, color = YuberaMuted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Text(collection.title_count.toString() + " тайтлов · " + collection.like_count + " лайков", color = YuberaPrimary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCollectionDetailScreen(
    collectionId: String,
    onBack: () -> Unit,
    onAnimeClick: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val response by viewModel.communityData.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refreshAccountProfile() }
    val collection = response?.collections?.firstOrNull { it.id == collectionId }
    val titles = collection?.animeIds.orEmpty().mapNotNull { id -> catalog.firstOrNull { it.id == id } }
    Column(Modifier.fillMaxSize()) {
        TextButton(onBack) { Text("‹ Подборки") }
        Text(collection?.title ?: "Подборка", color = YuberaOnBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        when {
            response == null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp) ) {
                CircularProgressIndicator(color = YuberaPrimary)
                Text("Загружаем подборку…", color = YuberaMuted)
            }
            collection == null -> Text("Подборка не найдена.", color = YuberaMuted, modifier = Modifier.padding(18.dp))
            titles.isEmpty() -> Text("В подборке пока нет доступных тайтлов.", color = YuberaMuted, modifier = Modifier.padding(18.dp))
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(titles, key = AnimeCatalogItem::id) { anime ->
                    val interaction = rememberYuberaPressInteractionSource()
                    Card(onClick = { onAnimeClick(anime.id) }, modifier = Modifier.fillMaxWidth().yuberaPressScale(interaction), interactionSource = interaction, colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!anime.posterUrl.isNullOrBlank()) AsyncImage(anime.posterUrl, "Постер: " + anime.title, Modifier.height(96.dp).weight(0.7f), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(anime.title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(listOfNotNull(anime.kind, anime.year.takeIf { it > 0 }?.toString()).joinToString(" · "), color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                                anime.rating?.let { Text("★ %.1f".format(it), color = YuberaPrimary, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}
