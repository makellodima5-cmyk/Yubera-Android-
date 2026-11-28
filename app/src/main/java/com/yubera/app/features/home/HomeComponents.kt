package com.yubera.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface
import com.yubera.app.core.ui.theme.YuberaScrim
import com.yubera.app.core.ui.theme.YuberaGold

@Composable
fun HomeAnimeTile(anime: AnimeCatalogItem, onClick: () -> Unit, badge: String? = null) {
    val interaction = rememberYuberaPressInteractionSource()\n    Column(Modifier.width(150.dp).yuberaPressScale(interaction).clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(12.dp)).background(YuberaSurface)
        ) {
            AnimeImage(anime, Modifier.fillMaxSize())
            Row(
                Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                anime.rating?.let { Pill("★ ${"%.1f".format(it)}") }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    anime.ageRating?.takeIf(String::isNotBlank)?.let { Pill(it) }
                    badge?.let { Pill(it) }
                }
            }
        }
        Text(
            anime.title,
            color = YuberaOnBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp)
        )
        val metadata = listOfNotNull(
            anime.kind,
            anime.year.takeIf { it > 0 }?.toString(),
            anime.episodes.takeIf { it > 0 }?.let { "${it} сер." }
        ).joinToString(" · ")
        if (metadata.isNotBlank()) {
            Text(metadata, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun HomeAnnouncementTile(anime: AnimeCatalogItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(320.dp).height(174.dp),
        colors = CardDefaults.cardColors(containerColor = YuberaSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(122.dp).fillMaxHeight().background(YuberaSurface)) {
                AnimeImage(anime, Modifier.fillMaxSize(), preferBanner = true)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Анонс")
                Text(anime.title, color = YuberaOnBackground, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (anime.genres.isNotEmpty()) Text(anime.genres.take(2).joinToString(" · "), color = YuberaMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(anime.year.takeIf { it > 0 }?.toString(), anime.kind).joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun HomeProgressCard(anime: AnimeCatalogItem, progress: AnimeProgressEntity, onClick: () -> Unit) {
    val fraction=if(progress.durationMillis>0L)(progress.positionMillis.toFloat()/progress.durationMillis.toFloat()).coerceIn(0f,1f) else 0f
    val interaction = rememberYuberaPressInteractionSource()\n    Card(onClick=onClick,modifier=Modifier.width(292.dp).yuberaPressScale(interaction), interactionSource=interaction,colors=CardDefaults.cardColors(containerColor=YuberaSurface),shape=RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(width=104.dp,height=76.dp).clip(RoundedCornerShape(10.dp)).background(YuberaSurface)) {
                AnimeImage(anime,Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.Center).size(34.dp).background(Color.Black.copy(.68f),CircleShape),contentAlignment=Alignment.Center){Text("▶",color=Color.White)}
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(anime.title,color=YuberaOnBackground,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(progress.episodeNumber?.let { episode -> progress.seasonNumber?.let { season -> "Сезон $season · Серия $episode" } ?: "Серия $episode" } ?: "Продолжить просмотр",color=YuberaMuted,style=MaterialTheme.typography.bodySmall,maxLines=1)
                if(progress.positionMillis>0L) Text(formatPlaybackTime(progress.positionMillis),color=YuberaMuted,style=MaterialTheme.typography.labelSmall)
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(YuberaMuted.copy(.22f))) {
                    Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(CircleShape).background(YuberaPrimary))
                }
            }
        }
    }
}

@Composable
fun RecommendedCard(anime: AnimeCatalogItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.width(270.dp).height(218.dp).clip(RoundedCornerShape(20.dp)).background(YuberaSurface).clickable(onClick=onClick)) {
        AnimeImage(anime,Modifier.fillMaxSize(),preferBanner=true)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,YuberaScrim))))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp).fillMaxWidth(.84f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Pill("Рекомендуем")
            Text(anime.title,color=Color.White,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(anime.genres.take(3).joinToString(" · "),color=YuberaMuted,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        androidx.compose.material3.IconButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).size(42.dp)
                .background(YuberaPrimary, CircleShape)
        ) {
            Text("▶", color = Color.White)
        }
    }
}

@Composable
fun HomeSectionHeader(title: String, actionLabel: String = "Смотреть все", onAction: (() -> Unit)? = null) {
    val icon=when(title) {
        "Популярное" -> Icons.Default.Whatshot
        "Новинки" -> Icons.Default.NewReleases
        "Анонсы" -> Icons.Default.Campaign
        "Онгоинги" -> Icons.Default.Wifi
        else -> Icons.Default.Star
    }
    Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Icon(icon,contentDescription=null,tint=YuberaPrimary,modifier=Modifier.size(20.dp))
            Text(title,color=YuberaOnBackground,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
        }
        if(onAction!=null) { val interaction = rememberYuberaPressInteractionSource(); Text("$actionLabel  ›",color=YuberaMuted,modifier=Modifier.yuberaPressScale(interaction).clickable(interactionSource=interaction,indication=LocalIndication.current,onClick=onAction).padding(6.dp)) }
    }
}

@Composable
private fun AnimeImage(anime: AnimeCatalogItem, modifier: Modifier, preferBanner: Boolean = false) {
    val image=if(preferBanner) anime.screenshots.firstOrNull()?.originalUrl?.takeIf(String::isNotBlank) ?: anime.bannerUrl?.takeIf(String::isNotBlank) ?: anime.posterUrl else anime.posterUrl
    Box(modifier.background(YuberaSurface),contentAlignment=Alignment.Center) {
        Text(anime.title.take(1),color=YuberaPrimary,style=MaterialTheme.typography.displayMedium)
        if(!image.isNullOrBlank()) AsyncImage(model=image,contentDescription="Изображение: ${anime.title}",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    }
}

@Composable
private fun Pill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(YuberaScrim.copy(alpha = 0.7f)).padding(horizontal=8.dp,vertical=4.dp)) {
        Text(text,color=Color.White,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold)
    }
}

private fun formatPlaybackTime(millis: Long): String {
    val total=millis.coerceAtLeast(0L)/1_000L
    val h=total/3_600L; val m=(total%3_600L)/60L; val s=total%60L
    return if(h>0L) "%d:%02d:%02d".format(h,m,s) else "%d:%02d".format(m,s)
}
