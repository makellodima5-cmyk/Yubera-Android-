package com.yubera.app.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary

@Composable
fun HomeSimpleDestination(title: String, description: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("‹  Назад",color=YuberaPrimary,fontSize=18.sp,modifier=Modifier.padding(bottom=4.dp).clickable(onClick=onBack))
        Text(title,color=YuberaOnBackground,fontSize=26.sp,fontWeight=FontWeight.Bold)
        Text(description,color=YuberaMuted,fontSize=14.sp)
    }
}
