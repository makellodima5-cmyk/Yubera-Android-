package com.yubera.app.features.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.R
import com.yubera.app.core.ui.theme.*

internal data class FrameChoice(val id: String, val title: String, val resource: Int?, val requirement: String)
private val supportFrames = listOf(
    FrameChoice("YUBERA_Contribution_Avatar_Frame_01_Soyuznik.webp.webp","Союзник",R.drawable.yubera_contribution_avatar_frame_01_soyuznik,"Поддержка проекта"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_02_Storonnik.webp.webp","Сторонник",R.drawable.yubera_contribution_avatar_frame_02_storonnik,"Поддержка проекта"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_03_Predanniy_Uchastnik.webp.webp","Преданный участник",R.drawable.yubera_contribution_avatar_frame_03_predanniy_uchastnik,"Поддержка проекта"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_04_Pochetnyy_Pokrovitel.webp","Почётный покровитель",R.drawable.yubera_contribution_avatar_frame_04_pochetnyy_pokrovitel,"Поддержка проекта"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_05_Legenda_YUBERA.webp","Легенда YUBERA",R.drawable.yubera_contribution_avatar_frame_05_legenda_yubera,"Поддержка проекта")
)
private val plusFrames = listOf(
    FrameChoice("YUBERA_Plus_Avatar_Frame_01_Sakura_Gold.webp","Sakura Gold",R.drawable.yubera_plus_avatar_frame_01_sakura_gold,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_02_Mystic_Kitsune.webp","Mystic Kitsune",R.drawable.yubera_plus_avatar_frame_02_mystic_kitsune,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_03_Samurai_Steel.webp","Samurai Steel",R.drawable.yubera_plus_avatar_frame_03_samurai_steel,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_04_Celestial_Archive.webp","Celestial Archive",R.drawable.yubera_plus_avatar_frame_04_celestial_archive,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_05_Cyberpunk_Neon.webp","Cyberpunk Neon",R.drawable.yubera_plus_avatar_frame_05_cyberpunk_neon,"YUBERA Plus")
)
internal val adminFrames = (1..8).map { n ->
    val res = when(n) {
        1 -> R.drawable.admin_frame_01; 2 -> R.drawable.admin_frame_02; 3 -> R.drawable.admin_frame_03; 4 -> R.drawable.admin_frame_04
        5 -> R.drawable.admin_frame_05; 6 -> R.drawable.admin_frame_06; 7 -> R.drawable.admin_frame_07; else -> R.drawable.admin_frame_08
    }
    FrameChoice("Admin_Frame_%02d.webp".format(n),"Административная рамка %d".format(n),res,"Администратор")
} + FrameChoice("Ramka_Admin_01.webp", "Административная рамка 01", R.drawable.ramka_admin_01, "Администратор")

@Composable
fun ProfileEditScreen(onBack: () -> Unit, viewModel: ProfileEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val profile = state.profile
    var confirmDiscard by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setAvatarUri(uri.toString())
    }
    val original = state.profile?.let { it.bio == state.bio && it.visibility == state.visibility && it.avatarFrame == state.selectedFrame && state.selectedAvatarUri == null } ?: true
    fun leave() { if (original) onBack() else confirmDiscard = true }
    BackHandler(enabled = !state.saving) { leave() }
    LaunchedEffect(Unit) { viewModel.load() }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Несохранённые изменения") },
        text = { Text("Выйти без сохранения изменений?") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Выйти") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Продолжить") } }
    )

    Column(Modifier.fillMaxSize().background(YuberaBackground)) {
        Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clickable { leave() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowBack, "Назад", tint = YuberaOnBackground, modifier = Modifier.size(24.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.yubera_logo_transparent), "YUBERA", Modifier.height(32.dp), contentScale = ContentScale.Fit) }
            Spacer(Modifier.width(24.dp))
        }
        Text("Редактирование профиля", color = YuberaOnBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = YuberaPrimary) }
        else if (profile == null) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.error ?: "Профиль недоступен", color = YuberaMuted)
                Button(onClick = viewModel::load) { Text("Повторить") }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                            val avatar = state.selectedAvatarUri ?: profile.avatarDataUrl?.takeIf(String::isNotBlank) ?: profile.avatarUrl?.takeIf(String::isNotBlank) ?: profile.photoUrl
                            if (!avatar.isNullOrBlank()) AsyncImage(avatar, "Аватар пользователя", Modifier.size(76.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.AccountCircle, null, tint = YuberaMuted, modifier = Modifier.size(76.dp))
                            state.selectedFrame?.let { selected -> frameResource(selected)?.let { Image(painterResource(it), "Выбранная рамка", Modifier.fillMaxSize()) } }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(profile.displayName, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            profile.username?.let { Text("@$it", color = YuberaMuted, style = MaterialTheme.typography.bodySmall) }
                            Text("Аватар", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                            OutlinedButton(onClick = { avatarPicker.launch("image/*") }) { Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Загрузить") }
                        }
                    }
                    Text("Максимальный размер файла — 2 МБ.", color = YuberaMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
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
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).background(YuberaSurfaceElevated, RoundedCornerShape(12.dp)).padding(12.dp),
                            decorationBox = { inner -> Box { if (state.bio.isEmpty()) Text("Расскажите о себе", color = YuberaMuted); inner() } }
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (state.visibility == "public") "Публичный профиль" else "Закрытый профиль",
                                color = YuberaOnBackground,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.visibility == "public",
                                onCheckedChange = { viewModel.setVisibility(if (it) "public" else "private") }
                            )
                        }
                        Text(
                            if (state.visibility == "public") {
                                "Ваш профиль виден другим пользователям и доступен в поиске."
                            } else {
                                "Ваш профиль не отображается в общем поиске и недоступен для общего просмотра."
                            },
                            color = YuberaMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Рамка аватара", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                        Text("Выберите рамку для своего аватара.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryChip("За поддержку", state.category == "support") { viewModel.setCategory("support") }
                            CategoryChip("YUBERA Plus", state.category == "plus") { viewModel.setCategory("plus") }
                            if (profile.isAdmin) CategoryChip("Админ", state.category == "admin") { viewModel.setCategory("admin") }
                        }
                        val choices = when (state.category) { "plus" -> plusFrames; "admin" -> adminFrames; else -> supportFrames }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FrameOption(null, "Без рамки", null, "", true, state.selectedFrame == null) { viewModel.setFrame(null) }
                            choices.forEach { frame ->
                                val available = when (state.category) {
                                    "plus" -> profile.isPlus
                                    "admin" -> frame.id in profile.availableAdminFrames
                                    else -> frame.id in profile.availableSupportFrames
                                }
                                FrameOption(frame.id, frame.title, frame.resource, frame.requirement, available, state.selectedFrame == frame.id) { if (available) viewModel.setFrame(frame.id) }
                            }
                        }
                        val anyAvailable = when (state.category) {
                            "plus" -> profile.isPlus
                            "admin" -> profile.availableAdminFrames.isNotEmpty()
                            else -> profile.availableSupportFrames.isNotEmpty()
                        }
                        if (!anyAvailable) Text(if (state.category == "plus") "Рамки доступны при активной подписке YUBERA Plus." else "Рамки открываются по мере поддержки проекта.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { leave() }, modifier = Modifier.weight(1f), enabled = !state.saving) { Text("Отменить") }
                Button(onClick = { viewModel.save(onBack) }, modifier = Modifier.weight(1f), enabled = !state.saving, colors = ButtonDefaults.buttonColors(containerColor = YuberaPrimary)) {
                    if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Сохранить изменения")
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun FrameOption(id: String?, title: String, resource: Int?, requirement: String, available: Boolean, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(112.dp).clip(RoundedCornerShape(14.dp)).background(YuberaSurfaceElevated).clickable(enabled = available, onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountCircle, null, tint = YuberaMuted, modifier = Modifier.size(64.dp))
            if (resource != null) Image(painterResource(resource), null, Modifier.fillMaxSize())
            if (!available) Icon(Icons.Default.Lock, null, tint = YuberaGold, modifier = Modifier.align(Alignment.TopEnd).size(18.dp).background(YuberaSurface, CircleShape))
            if (selected) Icon(Icons.Default.Check, null, tint = YuberaPrimary, modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).background(YuberaSurface, CircleShape))
        }
        Text(title, color = YuberaOnBackground, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        if (requirement.isNotBlank()) Text(requirement, color = YuberaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
    }
}

private fun frameResource(frame: String): Int? = when(frame) {
    "Admin_Frame_01.webp" -> R.drawable.admin_frame_01
    "Admin_Frame_02.webp" -> R.drawable.admin_frame_02
    "Admin_Frame_03.webp" -> R.drawable.admin_frame_03
    "Admin_Frame_04.webp" -> R.drawable.admin_frame_04
    "Admin_Frame_05.webp" -> R.drawable.admin_frame_05
    "Admin_Frame_06.webp" -> R.drawable.admin_frame_06
    "Admin_Frame_07.webp" -> R.drawable.admin_frame_07
    "Admin_Frame_08.webp" -> R.drawable.admin_frame_08
    "Ramka_Admin_01.webp" -> R.drawable.ramka_admin_01
    "YUBERA_Contribution_Avatar_Frame_01_Soyuznik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_01_soyuznik
    "YUBERA_Contribution_Avatar_Frame_02_Storonnik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_02_storonnik
    "YUBERA_Contribution_Avatar_Frame_03_Predanniy_Uchastnik.webp.webp" -> R.drawable.yubera_contribution_avatar_frame_03_predanniy_uchastnik
    "YUBERA_Contribution_Avatar_Frame_04_Pochetnyy_Pokrovitel.webp" -> R.drawable.yubera_contribution_avatar_frame_04_pochetnyy_pokrovitel
    "YUBERA_Contribution_Avatar_Frame_05_Legenda_YUBERA.webp" -> R.drawable.yubera_contribution_avatar_frame_05_legenda_yubera
    "YUBERA_Plus_Avatar_Frame_01_Sakura_Gold.webp" -> R.drawable.yubera_plus_avatar_frame_01_sakura_gold
    "YUBERA_Plus_Avatar_Frame_02_Mystic_Kitsune.webp" -> R.drawable.yubera_plus_avatar_frame_02_mystic_kitsune
    "YUBERA_Plus_Avatar_Frame_03_Samurai_Steel.webp" -> R.drawable.yubera_plus_avatar_frame_03_samurai_steel
    "YUBERA_Plus_Avatar_Frame_04_Celestial_Archive.webp" -> R.drawable.yubera_plus_avatar_frame_04_celestial_archive
    "YUBERA_Plus_Avatar_Frame_05_Cyberpunk_Neon.webp" -> R.drawable.yubera_plus_avatar_frame_05_cyberpunk_neon
    else -> null
}
