package com.yubera.app.features.news

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.ui.theme.*

@Composable fun NewsDetailScreen(onBack: () -> Unit, onAnimeClick: (String) -> Unit = {}, viewModel: NewsDetailViewModel = hiltViewModel()) {
 val s by viewModel.uiState.collectAsState(); val a = s.article; val context = LocalContext.current
 Column(Modifier.fillMaxSize()) {
  TextButton(onBack) { Text("‹ Назад") }
  if (a == null) Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { if (s.loading) CircularProgressIndicator(); Text(s.error ?: "Загружаем публикацию…", color = YuberaMuted) }
  else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
   a.imageUrl?.let { AsyncImage(it, "Обложка публикации", Modifier.fillMaxWidth().height(210.dp), contentScale = ContentScale.Crop) }
   Text(a.category, color = YuberaPrimary, style = MaterialTheme.typography.labelMedium)
   Text(a.title, color = YuberaOnBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
   Text(listOfNotNull(a.authorName.takeIf(String::isNotBlank), a.createdAt.take(10), a.commentsCount.takeIf { it > 0 }?.let { "$it комментариев" }).joinToString(" · "), color = YuberaMuted)
   a.linkedTitle?.let { linked -> if (a.linkedAnimeId != null) TextButton(onClick = { onAnimeClick(a.linkedAnimeId.toString()) }) { Text("Тайтл: $linked") } else Text("Связанный тайтл: $linked", color = YuberaMuted) }
   Text(a.bodyText?.takeIf(String::isNotBlank) ?: a.summary.ifBlank { "Текст публикации отсутствует." }, color = YuberaOnBackground)
   a.videoUrl?.let { url -> youtubeId(url)?.let { id ->
    AndroidView(factory = { ctx -> WebView(ctx).apply { settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.mediaPlaybackRequiresUserGesture = true; webViewClient = object : WebViewClient() { override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean { val target = runCatching { Uri.parse(url) }.getOrNull(); return target?.scheme != "https" || target.host !in setOf("youtube-nocookie.com", "www.youtube-nocookie.com") } }; loadUrl("https://www.youtube-nocookie.com/embed/" + id) } }, modifier = Modifier.fillMaxWidth().height(220.dp))
   } }
   if (s.loading) CircularProgressIndicator(); s.error?.let { Text(it, color = YuberaMuted) }
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { openSource(context, a.sourceUrl) }) { Text("Оригинал Shikimori") }; TextButton(onClick = viewModel::toggleBookmark) { Icon(Icons.Default.Bookmark, null, tint = if (s.isBookmarked) YuberaPrimary else YuberaMuted); Text(if (s.isBookmarked) "В закладках" else "Сохранить") } }
   TextButton(onClick = { val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, a.title); putExtra(Intent.EXTRA_TEXT, a.title + "\\n" + a.sourceUrl) }; context.startActivity(Intent.createChooser(intent, "Поделиться")) }) { Text("Поделиться") }
  }
 }
}
private fun youtubeId(value: String): String? { val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null; if (uri.scheme != "https") return null; val id = when (uri.host?.lowercase()) { "youtu.be" -> uri.lastPathSegment; "youtube.com", "www.youtube.com" -> if (uri.path == "/watch") uri.getQueryParameter("v") else if (uri.pathSegments.firstOrNull() == "embed") uri.pathSegments.getOrNull(1) else null; else -> null }; return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) } }
private fun openSource(context: android.content.Context, value: String) { val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return; if (uri.scheme == "https" && uri.host in setOf("shikimori.one", "shikimori.io")) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } }