package com.yubera.app.features.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun MyAnimeCard(item: MyLibraryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = YuberaSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.anime.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.anime.posterUrl,
                    contentDescription = "Постер: ${item.anime.title}",
                    modifier = Modifier.size(width = 76.dp, height = 108.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(width = 76.dp, height = 108.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(YuberaPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.anime.title.take(1), color = YuberaPrimary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.anime.title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(5.dp))
                Text(
                    listOfNotNull(item.anime.year.takeIf { it > 0 }?.toString(), item.anime.status).joinToString(" · "),
                    color = YuberaMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                item.progress?.let { progress ->
                    Text(
                        progress.episodeNumber?.let { "Эпизод $it" } ?: "Есть прогресс просмотра",
                        color = YuberaPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
