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
 val state by viewModel.uiState.collectAsState()
 val article=state.article
 val context=LocalContext.current
 Column(Modifier.fillMaxSize()){
  TextButton(onBack){Text("‹ Назад")}
  if(article==null){
   Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){Text("Публикация недоступна в локальном кэше",color=YuberaMuted)}
  }else{
   Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    if(!article.imageUrl.isNullOrBlank())AsyncImage(article.imageUrl,"Обложка публикации",Modifier.fillMaxWidth().height(210.dp),contentScale=ContentScale.Crop)
    Text(article.category,color=YuberaPrimary,style=MaterialTheme.typography.labelMedium)
    Text(article.title,color=YuberaOnBackground,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    Text(listOfNotNull(article.authorName.takeIf(String::isNotBlank),article.createdAt.take(10),article.commentsCount.takeIf{it>0}?.let{"$it комментариев"}).joinToString(" · "),color=YuberaMuted,style=MaterialTheme.typography.bodySmall)
    article.linkedTitle?.let{Text("Связанный тайтл: $it",color=YuberaMuted)}
    Text(article.summary.ifBlank{"Краткое содержание отсутствует."},color=YuberaOnBackground,style=MaterialTheme.typography.bodyLarge)
    article.videoUrl?.let{url->Button(onClick={safeOpen(context,url)}){Text("▶ Смотреть трейлер")}}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button(onClick={safeOpen(context,article.sourceUrl)}){Text("Оригинал Shikimori")}
     TextButton(onClick=viewModel::toggleBookmark){Icon(Icons.Default.Bookmark,contentDescription=null,tint=if(state.isBookmarked)YuberaPrimary else YuberaMuted);Text(if(state.isBookmarked)"В закладках" else "Сохранить")}
    }
    TextButton(onClick={
     val share=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,article.title);putExtra(Intent.EXTRA_TEXT,article.title+"\n"+article.sourceUrl)}
     context.startActivity(Intent.createChooser(share,"Поделиться"))
    }){Text("Поделиться")}
   }
  }
 }
}
private fun safeOpen(context:android.content.Context,url:String){val uri=runCatching{Uri.parse(url)}.getOrNull()?:return;if(uri.scheme=="https"&&uri.host!=null)runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,uri))}}
