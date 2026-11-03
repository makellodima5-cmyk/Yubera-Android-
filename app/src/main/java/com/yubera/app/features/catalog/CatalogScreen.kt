package com.yubera.app.features.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yubera.app.core.ui.components.FeaturePlaceholder
import com.yubera.app.core.ui.components.SectionBannerHeader

@Composable
fun CatalogScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(
            title = "Каталог",
            subtitle = "Весь каталог аниме"
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск аниме…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Жанры", "Годы", "Статусы", "Сортировка").forEach { label ->
                    FilterChip(
                        selected = false,
                        onClick = {},
                        enabled = false,
                        label = { Text(label) }
                    )
                }
            }
            FeaturePlaceholder(
                title = "Каталог пока не подключён",
                description = "После проверки API сайта здесь появятся реальные тайтлы, поиск и фильтры."
            )
        }
    }
}
