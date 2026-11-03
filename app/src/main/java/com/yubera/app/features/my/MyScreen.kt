package com.yubera.app.features.my

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
fun MyScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(
            title = "Моё",
            subtitle = "Ваше персональное пространство"
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeaturePlaceholder(title = "Избранное", description = "Сохранённые тайтлы.")
            FeaturePlaceholder(title = "Хочу посмотреть", description = "Аниме, отложенные на потом.")
            FeaturePlaceholder(title = "История просмотра", description = "Просмотренные серии и текущий прогресс.")
            FeaturePlaceholder(
                title = "Подписки на новые серии",
                description = "Только выбранные тобой аниме, отдельно от избранного."
            )
        }
    }
}
