package com.yubera.app.features.news

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
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
import com.yubera.app.core.ui.components.yuberaPressScale
import com.yubera.app.core.ui.components.rememberYuberaPressInteractionSource

@Composable
fun NewsDetailScreen(
    onBack: () -> Unit,
    onAnimeClick: (String) -> Unit = {},
    viewModel: NewsDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val article = state.article
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        val backInteraction = rememberYuberaPressInteractionSource()
        TextButton(onClick = onBack, modifier = Modifier.yuberaPressScale(backInteraction), interactionSource = backInteraction) { Text("‹ Назад") }
        if (article == null) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                if (state.loading) CircularProgressIndicator()
                Text(state.error ?: "Загружаем публикацию…", color = YuberaMuted)
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                article.imageUrl?.let {
                    AsyncImage(it, "Обложка публикации", Modifier.fillMaxWidth().height(210.dp), contentScale = ContentScale.Crop)
                }
                Text(article.category, color = YuberaPrimary, style = MaterialTheme.typography.labelMedium)
                Text(article.title, color = YuberaOnBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(
                        article.authorName.takeIf(String::isNotBlank),
                        article.createdAt.take(10),
                        article.commentsCount.takeIf { it > 0 }?.let { "$it комментариев" }
                    ).joinToString(" · "),
                    color = YuberaMuted
                )
                article.linkedTitle?.let { linked ->
                    if (article.linkedAnimeId != null) {
                        val linkedInteraction = rememberYuberaPressInteractionSource()
                        TextButton(onClick = { onAnimeClick(article.linkedAnimeId.toString()) }, modifier = Modifier.yuberaPressScale(linkedInteraction), interactionSource = linkedInteraction) { Text("Тайтл: $linked") }
                    } else {
                        Text("Связанный тайтл: $linked", color = YuberaMuted)
                    }
                }
                Text(
                    article.bodyText?.takeIf(String::isNotBlank) ?: article.summary.ifBlank { "Текст публикации отсутствует." },
                    color = YuberaOnBackground
                )

                val videoId = article.videoUrl?.let(::youtubeId)
                if (videoId != null) {
                    val player = remember(videoId) {
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = false
                                allowFileAccess = false
                                allowContentAccess = false
                                javaScriptCanOpenWindowsAutomatically = false
                                setSupportMultipleWindows(false)
                                mediaPlaybackRequiresUserGesture = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
                            }
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean =
                                    !isAllowedYoutubeEmbed(request.url)

                                @Suppress("DEPRECATION")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    val target = url?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return true
                                    return !isAllowedYoutubeEmbed(target)
                                }
                            }
                            loadUrl("https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&rel=0")
                        }
                    }
                    DisposableEffect(player) {
                        onDispose {
                            player.stopLoading()
                            player.loadUrl("about:blank")
                            player.destroy()
                        }
                    }
                    AndroidView(factory = { player }, modifier = Modifier.fillMaxWidth().height(220.dp))
                }

                if (state.loading) CircularProgressIndicator()
                state.error?.let { Text(it, color = YuberaMuted) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val originalInteraction = rememberYuberaPressInteractionSource()
                    Button(onClick = { openSource(context, article.sourceUrl) }, modifier = Modifier.yuberaPressScale(originalInteraction), interactionSource = originalInteraction) { Text("Оригинал Shikimori") }
                    val bookmarkInteraction = rememberYuberaPressInteractionSource()
                    TextButton(onClick = viewModel::toggleBookmark, modifier = Modifier.yuberaPressScale(bookmarkInteraction), interactionSource = bookmarkInteraction) {
                        Icon(Icons.Default.Bookmark, null, tint = if (state.isBookmarked) YuberaPrimary else YuberaMuted)
                        Text(if (state.isBookmarked) "В закладках" else "Сохранить")
                    }
                }
                val shareInteraction = rememberYuberaPressInteractionSource()
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, article.title)
                        putExtra(Intent.EXTRA_TEXT, article.title + "\n" + article.sourceUrl)
                    }
                    context.startActivity(Intent.createChooser(intent, "Поделиться"))
                }, modifier = Modifier.yuberaPressScale(shareInteraction), interactionSource = shareInteraction) { Text("Поделиться") }
            }
        }
    }
}

private fun youtubeId(value: String): String? {
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
    if (uri.scheme != "https") return null
    val host = uri.host?.lowercase() ?: return null
    val id = when (host) {
        "youtu.be" -> uri.lastPathSegment
        "youtube.com", "www.youtube.com", "m.youtube.com" ->
            if (uri.path == "/watch") uri.getQueryParameter("v")
            else if (uri.pathSegments.firstOrNull() == "embed") uri.pathSegments.getOrNull(1) else null
        "youtube-nocookie.com", "www.youtube-nocookie.com" ->
            if (uri.pathSegments.firstOrNull() == "embed") uri.pathSegments.getOrNull(1) else null
        else -> null
    }
    return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) }
}

private fun isAllowedYoutubeEmbed(uri: Uri): Boolean =
    uri.scheme == "https" &&
        uri.host?.lowercase() in setOf("youtube-nocookie.com", "www.youtube-nocookie.com") &&
        uri.path?.startsWith("/embed/") == true

private fun openSource(context: android.content.Context, value: String) {
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return
    if (uri.scheme == "https" && uri.host in setOf("shikimori.one", "shikimori.io")) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
