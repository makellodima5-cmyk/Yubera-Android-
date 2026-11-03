package com.yubera.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun HomeAnimeTile(anime: AnimeCatalogItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(116.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!anime.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = "Постер: ${anime.title}",
                modifier = Modifier.size(width = 116.dp, height = 164.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(width = 116.dp, height = 164.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(YuberaPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(anime.title.take(1), color = YuberaPrimary, style = MaterialTheme.typography.displayMedium)
            }
        }
        Text(anime.title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold, maxLines = 2)
        anime.rating?.let {
            Text("★ ${"%.1f".format(it)}", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HomeProgressCard(title: String, episode: Int?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = YuberaSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = YuberaOnBackground, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    episode?.let { "Продолжить с эпизода $it" } ?: "Продолжить просмотр",
                    color = YuberaMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("›", color = YuberaPrimary, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
