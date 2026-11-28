package com.yubera.app.features.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileEditScreen(onBack: () -> Unit, viewModel: ProfileEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    var avatarPickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val size = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L }.getOrDefault(-1L)
            if (size in 1..(2L * 1024L * 1024L)) viewModel.setAvatarUri(uri.toString())
            else viewModel.setAvatarUri(null)
        }
    }
    val original = state.profile?.let {
        state.bio == it.bio && state.customName == (it.customName?.takeIf(String::isNotBlank) ?: it.displayName) &&
            state.selectedFrame == it.avatarFrame && state.selectedAvatarUri == null && state.selectedAvatarAnimeId == null
    } ?: true
    fun leave() { if (original) onBack() else confirmDiscard = true }
    BackHandler { leave() }
    LaunchedEffect(Unit) { viewModel.load() }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Несохранённые изменения") },
        text = { Text("Выйти без сохранения изменений?") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Выйти") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Продолжить") } }
    )
    if (avatarPickerOpen) AvatarPickerDialog(catalog, state.selectedAvatarAnimeId, { viewModel.selectCatalogAvatar(it); avatarPickerOpen = false }, { avatarPickerOpen = false })

    Column(Modifier.fillMaxSize().background(YuberaBackground)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, "Назад", tint = YuberaOnBackground, modifier = Modifier.size(24.dp).clickable { leave() })
            Spacer(Modifier.width(12.dp))
            Text("Редактирование профиля", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge)
        }
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = YuberaPrimary) }
        } else if (state.profile == null) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.error ?: "Профиль недоступен", color = YuberaMuted)
                Button(onClick = viewModel::load) { Text("Повторить") }
            }
        } else {
            val profile = state.profile!!
            val selectedCatalog = catalog.firstOrNull { it.id == state.selectedAvatarAnimeId }
            val preview = state.selectedAvatarUri ?: selectedCatalog?.posterUrl
                ?: profile.avatarDataUrl ?: profile.avatarUrl ?: profile.photoUrl
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                            if (!preview.isNullOrBlank()) AsyncImage(preview, "Предпросмотр аватара", Modifier.size(104.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.AccountCircle, null, tint = YuberaMuted, modifier = Modifier.size(104.dp))
                            Icon(Icons.Default.Edit, "Изменить аватар", tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).size(30.dp).background(YuberaPrimary, CircleShape).padding(6.dp))
                        }
                        TextButton(onClick = { avatarPickerOpen = true }) { Text("Выбрать аватар", color = YuberaPrimary) }
                        OutlinedButton(onClick = { avatarLauncher.launch("image/*") }) { Text("Загрузить изображение") }
                        Text("Каталог аватаров или своё изображение до 2 МБ", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Имя пользователя", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = state.customName, onValueChange = viewModel::setCustomName,
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            supportingText = { Text("${state.customName.length} / 20") },
                            isError = state.customName.trim().length < 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = YuberaOnBackground, unfocusedTextColor = YuberaOnBackground,
                                focusedBorderColor = YuberaPrimary, unfocusedBorderColor = YuberaMuted,
                                focusedLabelColor = YuberaPrimary, unfocusedLabelColor = YuberaMuted
                            )
                        )
                        Text("Используйте от 3 до 20 букв, цифр, точек, дефисов или подчёркиваний.", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Описание профиля", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                            Text("${state.bio.length} / 250", color = YuberaMuted)
                        }
                        BasicTextField(
                            value = state.bio, onValueChange = viewModel::setBio, maxLines = 5,
                            textStyle = TextStyle(color = YuberaOnBackground, fontSize = 15.sp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).background(Color(0xFF202127), RoundedCornerShape(12.dp)).padding(12.dp),
                            decorationBox = { inner -> Box { if (state.bio.isEmpty()) Text("Расскажите о себе", color = YuberaMuted); inner() } }
                        )
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { leave() }, modifier = Modifier.weight(1f), enabled = !state.saving) { Text("Отменить") }
                Button(
                    onClick = { viewModel.save(onBack) }, modifier = Modifier.weight(1f),
                    enabled = !state.saving && state.customName.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = YuberaPrimary)
                ) {
                    if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Сохранить изменения")
                }
            }
        }
    }
}

@Composable
private fun AvatarPickerDialog(catalog: List<AnimeCatalogItem>, selectedId: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var category by remember { mutableStateOf("Все") }
    val filtered = remember(catalog, category) {
        when (category) {
            "Девушки" -> catalog.filter { it.genres.any { genre -> genre.contains("роман", true) || genre.contains("сёдзё", true) || genre.contains("драма", true) || genre.contains("повседнев", true) } }
            "Парни" -> catalog.filter { it.genres.any { genre -> genre.contains("экшен", true) || genre.contains("сёнен", true) || genre.contains("приключ", true) || genre.contains("фэнтези", true) } }
            "Оригинальные" -> catalog.filter { it.genres.any { genre -> genre.contains("оригин", true) } }
            else -> catalog
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = YuberaBackground, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Выбор аватара", color = YuberaOnBackground, style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Все", "Девушки", "Парни", "Оригинальные").forEach { label ->
                        FilterChip(selected = category == label, onClick = { category = label }, label = { Text(label) })
                    }
                }
                if (filtered.isEmpty()) Text("В этой категории нет изображений каталога.", color = YuberaMuted)
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { item ->
                        Card(onClick = { onSelect(item.id) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = YuberaSurface)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(.8f)) {
                                if (!item.posterUrl.isNullOrBlank()) AsyncImage(item.posterUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                if (selectedId == item.id) Icon(Icons.Default.Check, "Выбран", tint = YuberaPrimary, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).background(YuberaBackground, CircleShape).padding(3.dp))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Закрыть", color = YuberaPrimary) } }
            }
        }
    }
}
