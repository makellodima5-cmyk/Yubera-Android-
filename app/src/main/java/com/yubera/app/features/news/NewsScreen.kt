package com.yubera.app.features.news
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.*
@Composable fun NewsScreen(onArticleClick:(Long)->Unit,onBookmarksClick:()->Unit,onNotificationsClick:()->Unit={},viewModel:NewsViewModel=hiltViewModel()){
 val state by viewModel.uiState.collectAsState()
 LaunchedEffect(viewModel) { viewModel.ensureLoaded() }
 Column(Modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){YuberaLogo(Modifier.weight(1f),124.dp);IconButton(onNotificationsClick){Icon(Icons.Default.Notifications,"Уведомления",tint=YuberaOnBackground)};IconButton(viewModel::refresh){Icon(Icons.Default.Refresh,"Обновить",tint=YuberaPrimary)};IconButton(onBookmarksClick){Icon(Icons.Default.Bookmark,"Закладки",tint=YuberaPrimary)}}
  Text("Новости",Modifier.padding(horizontal=18.dp),color=YuberaOnBackground,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  Text("Главные события мира аниме · Shikimori",Modifier.padding(horizontal=18.dp,vertical=2.dp),color=YuberaMuted,style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(state.query,viewModel::onQueryChange,Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),placeholder={Text("Поиск новостей")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){NewsCategory.entries.forEach{cat->FilterChip(selected=state.selectedCategory==cat,onClick={viewModel.selectCategory(cat)},label={Text(cat.title)})}}
  if(state.isRefreshing&&state.articles.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=YuberaPrimary)}
  else if (state.articles.isEmpty()) Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text((if (state.errorMessage != null) "Не удалось загрузить новости" else "Новостей пока нет"), color = YuberaOnBackground); Text(state.errorMessage ?: "Попробуйте обновить ленту.", color = YuberaMuted, modifier = Modifier.padding(top = 8.dp)); Button(onClick = viewModel::refresh, modifier = Modifier.padding(top = 12.dp)) { Text("Повторить") } }
  else PullToRefreshBox(isRefreshing=state.isRefreshing,onRefresh=viewModel::refresh,modifier=Modifier.weight(1f).fillMaxWidth()){
   LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   state.errorMessage?.let{item{Text(it,color=YuberaMuted)}}
   val featured=state.articles.firstOrNull()
   if(featured!=null)item{Featured(featured,state.bookmarkedIds.contains(featured.id),{onArticleClick(featured.id)},{viewModel.toggleBookmark(featured.id)})}
   items(state.articles.drop(if(featured==null)0 else 1),key=NewsArticleEntity::id){a->NewsRow(a,state.bookmarkedIds.contains(a.id),{onArticleClick(a.id)},{viewModel.toggleBookmark(a.id)})}
   }
  }
 }
}
@Composable private fun Featured(a:NewsArticleEntity,saved:Boolean,open:()->Unit,bookmark:()->Unit){Card(onClick=open,Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=YuberaSurface)){Box(Modifier.fillMaxWidth().height(230.dp)){
 if(!a.imageUrl.isNullOrBlank())AsyncImage(a.imageUrl,"Обложка новости",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Box(Modifier.fillMaxSize().background(YuberaSurfaceElevated))
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent,Color(0xE8000000)))))
 Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp).padding(end=48.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(category(a.category).title.uppercase(),color=YuberaPrimary,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold);Text(a.title,color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,maxLines=3,overflow=TextOverflow.Ellipsis);Text(a.summary,color=Color.White.copy(.82f),style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis);Text(a.createdAt.take(10),color=Color.White.copy(.7f),style=MaterialTheme.typography.labelSmall)}
 IconButton(bookmark,Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(.45f),RoundedCornerShape(50))){Icon(Icons.Default.Bookmark,"Закладка",tint=if(saved)YuberaPrimary else Color.White)}
}}}
@Composable private fun NewsRow(a:NewsArticleEntity,saved:Boolean,open:()->Unit,bookmark:()->Unit){Card(onClick=open,Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=YuberaSurface),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
 Box(Modifier.size(110.dp,102.dp).clip(RoundedCornerShape(12.dp)).background(YuberaSurfaceElevated)){if(!a.imageUrl.isNullOrBlank())AsyncImage(a.imageUrl,"Изображение",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(category(a.category).title.take(1),color=YuberaPrimary,style=MaterialTheme.typography.headlineMedium)};if(a.videoUrl!=null)Text("▶ Трейлер",Modifier.align(Alignment.BottomStart).background(Color.Black.copy(.65f)).padding(4.dp),color=Color.White,style=MaterialTheme.typography.labelSmall)}
 Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(category(a.category).title.uppercase(),color=YuberaPrimary,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);Text(a.title,color=YuberaOnBackground,fontWeight=FontWeight.SemiBold,maxLines=3,overflow=TextOverflow.Ellipsis);a.linkedTitle?.let{Text(it,color=YuberaMuted,style=MaterialTheme.typography.labelSmall,maxLines=1)};Text(a.createdAt.take(10),color=YuberaMuted,style=MaterialTheme.typography.labelSmall)}
 IconButton(bookmark){Icon(Icons.Default.Bookmark,"Закладка",tint=if(saved)YuberaPrimary else YuberaMuted)}
}}}
@Composable fun NewsBookmarksScreen(onBack:()->Unit,onArticleClick:(Long)->Unit,viewModel:NewsViewModel=hiltViewModel()){
 val state by viewModel.uiState.collectAsState();LaunchedEffect(viewModel){viewModel.showBookmarkedOnly(true);viewModel.ensureLoaded()}
 Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onBack){Text("‹ Новости")};Text("Закладки",color=YuberaOnBackground,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
 if(state.articles.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Сохранённых публикаций пока нет",color=YuberaMuted)}
 else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(state.articles,key=NewsArticleEntity::id){a->NewsRow(a,true,{onArticleClick(a.id)},{viewModel.toggleBookmark(a.id)})}}
 }
}
private fun category(value:String)=runCatching{NewsCategory.valueOf(value)}.getOrDefault(NewsCategory.ALL)
