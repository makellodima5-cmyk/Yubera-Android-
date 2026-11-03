package com.yubera.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
fun YuberaLogo(
    modifier: Modifier = Modifier,
    width: Dp = 132.dp
) {
    Image(
        painter = painterResource(R.drawable.yubera_logo),
        contentDescription = "YUBERA",
        modifier = modifier.width(width).height(width / 3f),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun SectionBannerHeader(
    title: String,
    subtitle: String,
    showBanner: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (showBanner) 184.dp else 142.dp)
            .background(YuberaBackground)
    ) {
        if (showBanner) {
            Image(
                painter = painterResource(R.drawable.yubera_banner),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterEnd
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            YuberaBackground.copy(alpha = 0.96f),
                            YuberaBackground.copy(alpha = 0.68f),
                            YuberaBackground.copy(alpha = 0.08f)
                        )
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, YuberaBackground.copy(alpha = 0.58f))
                    )
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            YuberaLogo(width = 126.dp)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                color = YuberaOnBackground,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = YuberaMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun FeaturePlaceholder(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = YuberaSurface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Text(
                text = description,
                color = YuberaMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
