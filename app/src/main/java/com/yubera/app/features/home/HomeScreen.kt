package com.yubera.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yubera.app.core.ui.components.FeaturePlaceholder
import com.yubera.app.core.ui.components.SectionBannerHeader

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp)
    ) {
        SectionBannerHeader(
            title = "Главная",
            subtitle = "Твой мир аниме начинается здесь",
            showBanner = false
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeaturePlaceholder(
                title = "Продолжить просмотр",
                description = "Здесь появятся твои последние просмотренные серии."
            )
            FeaturePlaceholder(
                title = "Популярное и рекомендации",
                description = "Подборки будут загружаться из каталога YUBERA."
            )
            FeaturePlaceholder(
                title = "Новинки и сезоны",
                description = "Актуальные релизы появятся после подключения источников данных."
            )
        }
    }
}
