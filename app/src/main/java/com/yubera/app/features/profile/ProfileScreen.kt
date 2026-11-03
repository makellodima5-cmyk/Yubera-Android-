package com.yubera.app.features.profile

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
fun ProfileScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionBannerHeader(
            title = "Профиль",
            subtitle = "Ваш аккаунт YUBERA"
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeaturePlaceholder(
                title = "Профиль пользователя",
                description = "Аватар, имя, описание и дата регистрации появятся после подключения аккаунта."
            )
            FeaturePlaceholder(
                title = "Настройки",
                description = "Тема оформления, язык и параметры уведомлений."
            )
            FeaturePlaceholder(
                title = "О приложении",
                description = "Версия YUBERA, пользовательское соглашение и политика конфиденциальности."
            )
        }
    }
}
