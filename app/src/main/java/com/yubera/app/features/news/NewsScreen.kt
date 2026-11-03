package com.yubera.app.features.news

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yubera.app.core.ui.components.FeaturePlaceholder
import com.yubera.app.core.ui.components.SectionBannerHeader

@Composable
fun NewsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(
            title = "Новости",
            subtitle = "Главные события мира аниме"
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Все", "Анонсы", "Новые сезоны", "Индустрия", "События", "Интервью")
                    .forEachIndexed { index, label ->
                        FilterChip(
                            selected = index == 0,
                            onClick = {},
                            enabled = false,
                            label = { Text(label) }
                        )
                    }
            }
            FeaturePlaceholder(
                title = "Новостная лента пока не подключена",
                description = "Здесь будут публикации с датами, ссылками на первоисточники и обязательной атрибуцией."
            )
        }
    }
}
