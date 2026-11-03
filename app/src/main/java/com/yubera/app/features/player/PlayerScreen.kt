package com.yubera.app.features.player

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.player.KodikLinkPolicy
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = YuberaOnBackground)
            }
            Text(state.animeTitle, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YuberaPrimary)
            }
        } else if (state.errorMessage != null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.errorMessage.orEmpty(), color = YuberaMuted)
                    Button(onClick = viewModel::retry) { Text("Повторить") }
                }
            }
        } else {
            Text("Озвучка", color = YuberaMuted, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(state.sources, key = { it.id }) { source ->
                    FilterChip(
                        selected = state.selectedSourceId == source.id,
                        onClick = { viewModel.selectSource(source.id) },
                        label = { Text(source.title, maxLines = 1) }
                    )
                }
            }
            val selectedSource = state.sources.firstOrNull { it.id == state.selectedSourceId }
            Text("Серия", color = YuberaMuted, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(selectedSource?.episodes.orEmpty(), key = { it.number }) { episode ->
                    FilterChip(
                        selected = state.selectedEpisodeNumber == episode.number,
                        onClick = { viewModel.selectEpisode(episode.number) },
                        label = { Text(episode.title.ifBlank { episode.number.toString() }) }
                    )
                }
            }
            val playerUrl = state.selectedEpisodeUrl
            if (playerUrl != null && KodikLinkPolicy.isTrustedHttpsUrl(playerUrl)) {
                PlayerWebView(url = playerUrl, modifier = Modifier.fillMaxWidth().weight(1f))
            } else {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Ссылка на плеер недоступна.", color = YuberaMuted)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PlayerWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.mediaPlaybackRequiresUserGesture = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.safeBrowsingEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val target = request?.url?.toString() ?: return true
                        return !KodikLinkPolicy.isTrustedHttpsUrl(target)
                    }
                }
                if (KodikLinkPolicy.isTrustedHttpsUrl(url)) {
                    tag = url
                    loadUrl(url)
                }
            }
        },
        update = { webView ->
            if (webView.tag != url && KodikLinkPolicy.isTrustedHttpsUrl(url)) {
                webView.tag = url
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
    )
}
