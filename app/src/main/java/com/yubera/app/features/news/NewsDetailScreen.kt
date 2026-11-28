package com.yubera.app.features.news

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary

@Composable
fun NewsDetailScreen(onBack: () -> Unit, viewModel: NewsDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val article = state.article
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) { Text("‹ Назад") }
        if (article == null) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                if (state.isLoadingDetails) CircularProgressIndicator()
                Text(state.detailError ?: "Загружаем публикацию…", color = YuberaMuted)
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                article.imageUrl?.takeIf(String::isNotBlank)?.let {
                    AsyncImage(it, "Обложка публикации", Modifier.fillMaxWidth().height(210.dp), contentScale = ContentScale.Crop)
                }
                Text(article.category, color = YuberaPrimary, style = MaterialTheme.typography.labelMedium)
                Text(article.title, color = YuberaOnBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(listOfNotNull(article.authorName.takeIf(String::isNotBlank), article.createdAt.take(10), article.commentsCount.takeIf { it > 0 }?.let { "$it комментариев" }).joinToString(" · "), color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                article.linkedTitle?.let { Text("Связанный тайтл: $it", color = YuberaMuted) }
                Text(article.bodyText?.takeIf(String::isNotBlank) ?: article.summary.ifBlank { "Текст публикации отсутствует." }, color = YuberaOnBackground, style = MaterialTheme.typography.bodyLarge)
                article.videoUrl?.let { url -> youtubeVideoId(url)?.let { YouTubeEmbed(it) } }
                if (state.isLoadingDetails) CircularProgressIndicator()
                state.detailError?.let { Text(it, color = YuberaMuted) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openSafeSource(context, article.sourceUrl) }) { Text("Оригинал Shikimori") }
                    TextButton(onClick = viewModel::toggleBookmark) {
                        Icon(Icons.Default.Bookmark, contentDescription = null)
                        Text(if (state.isBookmarked) "В закладках" else "Сохранить")
                    }
                }
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, article.title)
                        putExtra(Intent.EXTRA_TEXT, article.title + "\\n" + article.sourceUrl)
                    }
                    context.startActivity(Intent.createChooser(share, "Поделиться"))
                }) { Text("Поделиться") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun YouTubeEmbed(videoId: String) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        val uri = runCatching { Uri.parse(url) }.getOrNull()
                        return uri?.scheme != "https" || uri.host !in ALLOWED_VIDEO_HOSTS
                    }
                }
                loadUrl("https://www.youtube-nocookie.com/embed/" + Uri.encode(videoId))
            }
        },
        update = { view -> if (!view.url.orEmpty().contains(videoId)) view.loadUrl("https://www.youtube-nocookie.com/embed/" + Uri.encode(videoId)) }
    )
}

private fun youtubeVideoId(url: String): String? {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    if (uri.scheme != "https") return null
    val id = when (uri.host?.lowercase()) {
        "youtu.be" -> uri.pathSegments.firstOrNull()
        "youtube.com", "www.youtube.com" -> when {
            uri.path == "/watch" -> uri.getQueryParameter("v")
            uri.pathSegments.firstOrNull() == "embed" -> uri.pathSegments.getOrNull(1)
            else -> null
        }
        else -> null
    }
    return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) }
}

private fun openSafeSource(context: android.content.Context, url: String) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    if (uri.scheme == "https" && uri.host in ALLOWED_SOURCE_HOSTS) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

private val ALLOWED_VIDEO_HOSTS = setOf("youtube-nocookie.com", "www.youtube-nocookie.com")
private val ALLOWED_SOURCE_HOSTS = setOf("shikimori.one", "shikimori.io")