package com.yubera.app.features.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var isFullscreen by remember { mutableStateOf(false) }
    var retryNonce by remember { mutableStateOf(0) }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isFullscreen) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = YuberaOnBackground)
                }
                Text(state.animeTitle, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
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
            if (!isFullscreen) {
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
            }
            val playerUrl = state.selectedEpisodeUrl
            if (playerUrl != null && KodikLinkPolicy.isTrustedHttpsUrl(playerUrl)) {
                key(playerUrl, retryNonce) {
                    PlayerWebView(
                        url = playerUrl,
                        retryNonce = retryNonce,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        onFullscreenChanged = { isFullscreen = it }
                    )
                }
                if (!isFullscreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { retryNonce += 1 }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Перезагрузить плеер", tint = YuberaOnBackground)
                        }
                    }
                }
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
private fun PlayerWebView(
    url: String,
    retryNonce: Int,
    modifier: Modifier = Modifier,
    onFullscreenChanged: (Boolean) -> Unit
) {
    var loading by remember(url, retryNonce) { mutableStateOf(true) }
    var loadError by remember(url, retryNonce) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val chromeClient = remember(context) { FullscreenKodikWebChromeClient(context, onFullscreenChanged) }

    DisposableEffect(chromeClient) {
        onDispose { chromeClient.hideCustomView() }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.safeBrowsingEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.userAgentString = WebSettings.getDefaultUserAgent(viewContext)
                    webChromeClient = chromeClient
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val target = request?.url?.toString() ?: return true
                            return !KodikLinkPolicy.isTrustedHttpsUrl(target)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                            !KodikLinkPolicy.isTrustedHttpsUrl(url.orEmpty())

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                            loadError = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                loading = false
                                loadError = error?.description?.toString() ?: "Не удалось загрузить страницу плеера."
                            }
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                loading = false
                                loadError = "Сервис плеера вернул HTTP ${errorResponse?.statusCode}."
                            }
                        }
                    }
                    tag = url
                    if (KodikLinkPolicy.isTrustedHttpsUrl(url)) loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.tag != url && KodikLinkPolicy.isTrustedHttpsUrl(url)) {
                    webView.tag = url
                    loading = true
                    loadError = null
                    webView.stopLoading()
                    webView.loadUrl(url)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                chromeClient.hideCustomView()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.removeAllViews()
                webView.destroy()
            }
        )
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = YuberaPrimary)
        }
        loadError?.let { message ->
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(message, color = YuberaMuted)
                Text("Используйте кнопку перезагрузки, чтобы повторить попытку.", color = YuberaMuted)
            }
        }
    }
}

private class FullscreenKodikWebChromeClient(
    private val context: Context,
    private val onFullscreenChanged: (Boolean) -> Unit
) : WebChromeClient() {
    private var fullscreenDialog: Dialog? = null
    private var customViewCallback: CustomViewCallback? = null
    private var originalOrientation: Int? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view == null || fullscreenDialog != null) {
            callback?.onCustomViewHidden()
            return
        }

        val activity = context as? android.app.Activity
        originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(view)
        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        customViewCallback = callback
        fullscreenDialog = dialog
        dialog.setOnDismissListener {
            fullscreenDialog = null
            restoreOrientation()
            val activeCallback = customViewCallback
            customViewCallback = null
            onFullscreenChanged(false)
            activeCallback?.onCustomViewHidden()
        }
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        onFullscreenChanged(true)
    }

    override fun onHideCustomView() = hideCustomView()

    fun hideCustomView() {
        val dialog = fullscreenDialog ?: return
        fullscreenDialog = null
        dialog.setOnDismissListener(null)
        dialog.dismiss()
        restoreOrientation()
        val callback = customViewCallback
        customViewCallback = null
        onFullscreenChanged(false)
        callback?.onCustomViewHidden()
    }

    private fun restoreOrientation() {
        val activity = context as? android.app.Activity ?: return
        originalOrientation?.let { activity.requestedOrientation = it }
        originalOrientation = null
    }
}
