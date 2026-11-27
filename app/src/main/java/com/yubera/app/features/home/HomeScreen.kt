package com.yubera.app.features.home

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun HomeScreen(
    onAnimeClick:(String)->Unit,
    onContinueClick:(String,Int?)->Unit={id,_->onAnimeClick(id)},
    onNotificationsClick:()->Unit={},
    onCollectionsClick:()->Unit={},
    onViewAll:(String)->Unit={},
    onSupportClick:()->Unit={},
    viewModel:HomeViewModel=hiltViewModel()
) {
    val motionAllowed=rememberHomeMotionAllowed()
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal=18.dp,vertical=8.dp)
        ) {
            YuberaLogo(modifier=Modifier.align(Alignment.Center),width=124.dp)
            Box(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick=onNotificationsClick) {
                    Icon(Icons.Default.Notifications,contentDescription="Уведомления",tint=YuberaOnBackground)
                }
                if (state.unreadNotificationCount > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top=9.dp,end=10.dp).size(7.dp)
                            .background(Color(0xFFFF4545),CircleShape)
                    )
                }
            }
        }
        LazyColumn(
            modifier=Modifier.fillMaxSize(),
            contentPadding=PaddingValues(bottom=24.dp),
            verticalArrangement=Arrangement.spacedBy(22.dp)
        ) {
            if(state.errorMessage!=null) item { Text(state.errorMessage.orEmpty(),color=YuberaMuted,modifier=Modifier.padding(horizontal=18.dp)) }
            item {
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    HomeSectionHeader("Рекомендуем")
                    if(state.recommended.isEmpty()) {
                        Text("Рекомендации появятся после загрузки каталога.",color=YuberaMuted,modifier=Modifier.padding(horizontal=18.dp))
                    } else {
                        val listState=rememberLazyListState()
                        val activeIndex by remember { derivedStateOf {
                            val visible=listState.layoutInfo.visibleItemsInfo
                            if(visible.isEmpty()) 0 else {
                                val center=listState.layoutInfo.viewportStartOffset+listState.layoutInfo.viewportEndOffset/2
                                visible.minByOrNull { kotlin.math.abs(it.offset+it.size/2-center) }?.index ?: 0
                            }
                        } }
                        LazyRow(
                            state=listState,
                            contentPadding=PaddingValues(horizontal=32.dp),
                            horizontalArrangement=Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.recommended,key=AnimeCatalogItem::id) { anime ->
                                val targetScale=if(anime.id==state.recommended.getOrNull(activeIndex)?.id) 1f else .94f
                                val scale by animateFloatAsState(targetScale,tween(if(motionAllowed) 220 else 0),label="recommendationScale")
                                RecommendedCard(anime,onClick={onAnimeClick(anime.id)},modifier=Modifier.scale(scale))
                            }
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                            state.recommended.indices.forEach { index ->
                                androidx.compose.foundation.layout.Box(Modifier.padding(horizontal=3.dp).size(if(index==activeIndex) 8.dp else 6.dp).background(if(index==activeIndex) YuberaPrimary else YuberaMuted.copy(.45f),CircleShape))
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    HomeSectionHeader("Продолжить просмотр",onAction={onViewAll("continue")})
                    if(state.continueWatching.isEmpty()) Text("Здесь появятся тайтлы, просмотр которых не завершён.",color=YuberaMuted,modifier=Modifier.padding(horizontal=18.dp))
                    else LazyRow(contentPadding=PaddingValues(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        items(state.continueWatching,key={it.anime.id}) { item -> HomeProgressCard(item.anime,item.progress){onContinueClick(item.anime.id,item.progress.episodeNumber)} }
                    }
                }
            }
            item {
                Card(
                    onClick = onSupportClick,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    colors = CardDefaults.cardColors(containerColor = YuberaSurface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF202126), Color(0xFF27282E), Color(0xFF1A1B20))
                            )
                        ).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB59D), modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Поддержка и YUBERA Plus", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Поддержать проект и открыть дополнительные возможности", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("›", color = YuberaPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if(state.popular.isNotEmpty()) item { AnimeStrip("Популярное",state.popular,"popular",onAnimeClick,onViewAll) }
            if(state.newReleases.isNotEmpty()) item { AnimeStrip("Новинки",state.newReleases,"new",onAnimeClick,onViewAll) }
            if(state.announcements.isNotEmpty()) item { AnimeStrip("Анонсы",state.announcements,"announcements",onAnimeClick,onViewAll) }
            if(state.ongoing.isNotEmpty()) item { AnimeStrip("Онгоинги",state.ongoing,"ongoing",onAnimeClick,onViewAll) }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Card(onClick={state.catalogue.filter { it.kodikAvailable == true }.randomOrNull()?.let{onAnimeClick(it.id)}},modifier=Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=YuberaSurface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Casino,contentDescription="Случайный выбор",tint=YuberaPrimary)
                            Text("Случайное аниме",fontWeight=FontWeight.Bold,color=YuberaOnBackground)
                            Text("Подобрать тайтл",color=YuberaMuted,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(onClick=onCollectionsClick,modifier=Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=YuberaSurface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Bookmarks,contentDescription="Подборки",tint=YuberaPrimary)
                            Text("Подборки",fontWeight=FontWeight.Bold,color=YuberaOnBackground)
                            Text("Тематические коллекции",color=YuberaMuted,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeStrip(title:String,items:List<AnimeCatalogItem>,category:String,onAnimeClick:(String)->Unit,onViewAll:(String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        HomeSectionHeader(title,onAction={onViewAll(category)})
        LazyRow(contentPadding=PaddingValues(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(items,key=AnimeCatalogItem::id) { anime -> HomeAnimeTile(anime,onClick={onAnimeClick(anime.id)},badge=if(category=="announcements") "Анонс" else null) }
        }
    }
}

@Composable
fun HomeCategoryScreen(category:String,onAnimeClick:(String)->Unit,onContinueClick:(String,Int?)->Unit,onBack:()->Unit,viewModel:HomeViewModel=hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val title=when(category) {
        "continue"->"Продолжить просмотр"; "popular"->"Популярное"; "new"->"Новинки"; "announcements"->"Анонсы"; "ongoing"->"Онгоинги"; else->"Категория"
    }
    val items=when(category) {
        "continue"->state.continueWatching.map{it.anime}
        "popular"->state.popular
        "new"->state.newReleases
        "announcements"->state.announcements
        "ongoing"->state.ongoing
        else->emptyList()
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("‹",fontSize=30.sp,color=YuberaPrimary,modifier=Modifier.clickable(onClick=onBack).padding(end=14.dp))
            Text(title,color=YuberaOnBackground,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
        }
        if(items.isEmpty()) Text("Здесь пока нет тайтлов.",color=YuberaMuted,modifier=Modifier.padding(18.dp))
        else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(category=="continue") items(state.continueWatching,key={it.anime.id}) { row -> HomeProgressCard(row.anime,row.progress){onContinueClick(row.anime.id,row.progress.episodeNumber)} }
            else items(items,key=AnimeCatalogItem::id) { anime -> HomeAnimeTile(anime,onClick={onAnimeClick(anime.id)}) }
        }
    }
}

@Composable
private fun rememberHomeMotionAllowed():Boolean {
    val context=LocalContext.current
    return remember {
        runCatching { Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f }
            .getOrDefault(true)
    }
}
