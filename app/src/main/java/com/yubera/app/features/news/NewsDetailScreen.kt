package com.yubera.app.features.news
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.ui.theme.*
@Composable fun NewsDetailScreen(onBack:()->Unit,viewModel:NewsDetailViewModel=hiltViewModel()){
 val state by viewModel.uiState.collectAsState();val a=state.article;val ctx=LocalContext.current
 Column(Modifier.fillMaxSize()){
 TextButton(onBack){Text("‹ Назад")}
 if(a==null)Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){Text("Публикация недоступна в локальном кэше",color=YuberaMuted)}
 else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 if(!a.imageUrl.isNullOrBlank())AsyncImage(a.imageUrl,"Обложка публикации",Modifier.fillMaxWidth().height(210.dp),contentScale=ContentScale.Crop)
 Text(a.category,color=YuberaPrimary,style=MaterialTheme.typography.labelMedium)
 Text(a.title,color=YuberaOnBackground,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
 Text(listOfNotNull(a.authorName.takeIf(String::isNotBlank),a.createdAt.take(10),a.commentsCount.takeIf{it>0}?.let{"$it комментариев"}).joinToString(" · "),color=YuberaMuted,style=MaterialTheme.typography.bodySmall)
 a.linkedTitle?.let{Text("Связанный тайтл: $it",color=YuberaMuted)}
 Text(a.summary.ifBlank{"Краткое содержание отсутствует."},color=YuberaOnBackground,style=MaterialTheme.typography.bodyLarge)
 a.videoUrl?.let{Button(onClick={safeOpen(ctx,it)}){Text("▶ Смотреть трейлер")}}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={safeOpen(ctx,a.sourceUrl)}){Text("Оригинал Shikimori")};TextButton(onClick=viewModel::toggleBookmark){Icon(Icons.Default.Bookmark,null,tint=if(state.isBookmarked)YuberaPrimary else YuberaMuted);Text(if(state.isBookmarked)"В закладках" else "Сохранить")}}
 TextButton(onClick={val share=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,a.title);putExtra(Intent.EXTRA_TEXT,a.title+"\n"+a.sourceUrl)};ctx.startActivity(Intent.createChooser(share,"Поделиться"))){Text("Поделиться")}
 }
 }
}
private fun safeOpen(ctx:android.content.Context,url:String){val u=runCatching{Uri.parse(url)}.getOrNull()?:return;if(u.scheme=="https"&&u.host!=null)runCatching{ctx.startActivity(Intent(Intent.ACTION_VIEW,u))}}
