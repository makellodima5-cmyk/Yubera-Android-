package com.yubera.app.features.news
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.core.ui.components.SectionBannerHeader
import com.yubera.app.core.ui.theme.*

@Composable
fun NewsDetailScreen(onBack: () -> Unit, viewModel: NewsDetailViewModel = hiltViewModel()) {
 val state by viewModel.uiState.collectAsState()
 val article = state.article
 val context = LocalContext.current
 Column(Modifier.fillMaxSize()) {
  TextButton(onClick = onBack) { Text("← Назад") }
  SectionBannerHeader(title = "Публикация", subtitle = "Материал и первоисточник")
  if (article == null) {
   Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
    Text("Публикация не найдена в сохранённой ленте.", color = YuberaOnBackground)
    Text("Обновите новости и откройте публикацию снова.", color = YuberaMuted)
   }
  } else {
   Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(article.title, color = YuberaOnBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(listOfNotNull(article.authorName.takeIf(String::isNotBlank), article.createdAt.take(10), article.commentsCount.takeIf { it > 0 }?.let { "$it комментариев" }).joinToString(" · "), color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
    article.linkedTitle?.let { Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(14.dp)) {
     Column(Modifier.padding(14.dp)) { Text("Связанное аниме", color = YuberaMuted, style = MaterialTheme.typography.labelMedium); Text(it, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold) }
    } }
    Text(article.summary.ifBlank { "Краткое описание отсутствует. Полный материал доступен на сайте первоисточника." }, color = YuberaOnBackground, style = MaterialTheme.typography.bodyLarge)
    Text("Источник: Shikimori. В приложении сохранён краткий фрагмент публикации, а не полный текст.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl))) }) { Text("Открыть источник") }
     TextButton(onClick = viewModel::toggleBookmark) { Text(if (state.isBookmarked) "В закладках" else "В закладки") }
    }
    TextButton(onClick = {
     val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, article.title); putExtra(Intent.EXTRA_TEXT, "§{article.title}\n§{article.sourceUrl}") }
     context.startActivity(Intent.createChooser(share, "Поделиться публикацией"))
    }) { Text("Поделиться") }
   }
  }
 }
}
