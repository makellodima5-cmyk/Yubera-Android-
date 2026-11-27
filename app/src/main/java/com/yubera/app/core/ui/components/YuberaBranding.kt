package com.yubera.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yubera.app.R
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun YuberaLogo(modifier: Modifier = Modifier, width: Dp = 132.dp) {
    Image(
        painter = painterResource(R.drawable.yubera_logo_transparent),
        contentDescription = "YUBERA",
        modifier = modifier.width(width).height(width / 3f),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun SectionBannerHeader(
    title: String,
    subtitle: String,
    showBanner: Boolean = false,
    modifier: Modifier = Modifier,
    onNotificationsClick: (() -> Unit)? = null,
    showBack: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    Column(modifier.fillMaxWidth().background(YuberaBackground)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack && onBackClick != null) {
                androidx.compose.material3.IconButton(onClick = onBackClick) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = YuberaOnBackground
                    )
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                YuberaLogo(width = 132.dp)
            }
            if (onNotificationsClick != null) {
                androidx.compose.material3.IconButton(onClick = onNotificationsClick) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.NotificationsNone,
                        contentDescription = "Уведомления",
                        tint = YuberaOnBackground
                    )
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }
        if (showBanner) {
            Box(Modifier.fillMaxWidth().height(110.dp)) {
                Image(
                    painter = painterResource(R.drawable.yubera_about_project_banner),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterEnd
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(YuberaBackground.copy(alpha = .96f), YuberaBackground.copy(alpha = .45f), Color.Transparent)
                        )
                    )
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(title, color = YuberaOnBackground, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = YuberaMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FeaturePlaceholder(title: String, description: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = YuberaSurface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(description, color = YuberaMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
