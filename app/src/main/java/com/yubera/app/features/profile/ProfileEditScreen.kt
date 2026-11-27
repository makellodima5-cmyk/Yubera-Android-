package com.yubera.app.features.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yubera.app.R
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface

private data class FrameChoice(val id: String?, val title: String, val resource: Int?, val requirement: String)
private val supportFrames = listOf(
    FrameChoice("YUBERA_Contribution_Avatar_Frame_01_Soyuznik.webp.webp","Союзник",R.drawable.yubera_contribution_avatar_frame_01_soyuznik_webp_webp_webp,"За поддержку"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_02_Storonnik.webp.webp","Сторонник",R.drawable.yubera_contribution_avatar_frame_02_storonnik_webp_webp_webp,"За поддержку"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_03_Predanniy_Uchastnik.webp.webp","Преданный участник",R.drawable.yubera_contribution_avatar_frame_03_predanniy_uchastnik_webp_webp_webp,"За поддержку"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_04_Pochetnyy_Pokrovitel.webp.webp","Почётный покровитель",R.drawable.yubera_contribution_avatar_frame_04_pochetnyy_pokrovitel_webp_webp,"За поддержку"),
    FrameChoice("YUBERA_Contribution_Avatar_Frame_05_Legenda_YUBERA.webp.webp","Легенда YUBERA",R.drawable.yubera_contribution_avatar_frame_05_legenda_yubera_webp_webp,"За поддержку")
)
private val plusFrames = listOf(
    FrameChoice("YUBERA_Plus_Avatar_Frame_01_Sakura_Gold.webp","Sakura Gold",R.drawable.yubera_plus_avatar_frame_01_sakura_gold,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_02_Mystic_Kitsune.webp","Mystic Kitsune",R.drawable.yubera_plus_avatar_frame_02_mystic_kitsune,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_03_Samurai_Steel.webp","Samurai Steel",R.drawable.yubera_plus_avatar_frame_03_samurai_steel,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_04_Celestial_Archive.webp","Celestial Archive",R.drawable.yubera_plus_avatar_frame_04_celestial_archive,"YUBERA Plus"),
    FrameChoice("YUBERA_Plus_Avatar_Frame_05_Cyberpunk_Neon.webp","Cyberpunk Neon",R.drawable.yubera_plus_avatar_frame_05_cyberpunk_neon,"YUBERA Plus")
)
private val adminFrames = (1..8).map { n ->
    val res = when(n) {
        1 -> R.drawable.admin_frame_01; 2 -> R.drawable.admin_frame_02; 3 -> R.drawable.admin_frame_03; 4 -> R.drawable.admin_frame_04
        5 -> R.drawable.admin_frame_05; 6 -> R.drawable.admin_frame_06; 7 -> R.drawable.admin_frame_07; else -> R.drawable.admin_frame_08
    }
    FrameChoice("Admin_Frame_%02d.webp".format(n),"Административная рамка %d".format(n),res,"Администратор")
}

@Composable
fun ProfileEditScreen(onBack: () -> Unit, viewModel: ProfileEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    val original = state.profile?.let { it.bio == state.bio && it.visibility == state.visibility && it.avatarFrame == state.selectedFrame } ?: true
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
    Column(Modifier.fillMaxSize().background(YuberaBackground)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White, modifier = Modifier.size(24.dp).clickable { leave() })
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
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountCircle, null, tint = YuberaMuted, modifier = Modifier.size(72.dp))
                            state.selectedFrame?.let { selected ->
                                val frame = (supportFrames + plusFrames + adminFrames).firstOrNull { it.id == selected }
                                frame?.resource?.let { Image(painterResource(it), contentDescription = "Выбранная рамка", modifier = Modifier.fillMaxSize()) }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(state.profile.displayName, color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                            state.profile.username?.let { Text("@$it", color = YuberaMuted) }
                            Text("Аватар", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { /* Avatar file upload requires shared object-storage provisioning. */ }, enabled = false) { Text("Загрузить") }
                            Text("Загрузка пользовательского аватара временно недоступна.", color = YuberaMuted, style = MaterialTheme.typography.labelSmall)
                        }
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
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).background(Color(0xFF202127), RoundedCornerShape(12.dp)).padding(12.dp),
                            decorationBox = { inner -> Box { if (state.bio.isEmpty()) Text("Расскажите о себе", color = YuberaMuted); inner() } }
                        )
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if(state.visibility=="public") "Публичный профиль" else "Закрытый профиль", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                        Text(if(state.visibility=="public") "Ваш профиль виден другим пользователям и доступен в поиске." else "Профиль не виден в общем поиске и недоступен для общего просмотра.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Публичность", color = YuberaOnBackground)
                            Switch(checked = state.visibility=="public", onCheckedChange = { viewModel.setVisibility(if(it) "public" else "private") })
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = YuberaSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Рамка аватара", color = YuberaOnBackground, style = MaterialTheme.typography.titleMedium)
                        Text("Выберите рамку для своего аватара.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryChip("За поддержку",state.category=="support") { viewModel.setCategory("support") }
                            CategoryChip("YUBERA Plus",state.category=="plus") { viewModel.setCategory("plus") }
                            if(state.profile.isAdmin) CategoryChip("Админ",state.category=="admin") { viewModel.setCategory("admin") }
                        }
                        val options = when(state.category) { "plus" -> plusFrames; "admin" -> adminFrames; else -> supportFrames }
                        val unlocked = when(state.category) {
                            "plus" -> state.profile.isPlus
                            "admin" -> state.profile.isAdmin
                            else -> options.filter { it.id in state.profile.availableSupportFrames }.isNotEmpty()
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FrameOption(null,"Без рамки",null,"",true,state.selectedFrame==null) { viewModel.setFrame(null) }
                            options.forEach { frame ->
                                val available = when(state.category) {
                                    "plus" -> state.profile.isPlus
                                    "admin" -> frame.id in state.profile.availableAdminFrames
                                    else -> frame.id in state.profile.availableSupportFrames
                                }
                                FrameOption(frame.id,frame.title,frame.resource,frame.requirement,available,state.selectedFrame==frame.id) { if(available) viewModel.setFrame(frame.id) }
                            }
                        }
                        if(!unlocked) Text(if(state.category=="plus") "Рамки доступны при активной подписке YUBERA Plus." else "Рамки открываются по мере поддержки проекта.", color = YuberaMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { leave() }, modifier = Modifier.weight(1f), enabled = !state.saving) { Text("Отменить") }
                Button(onClick = { viewModel.save(onBack) }, modifier = Modifier.weight(1f), enabled = !state.saving, colors = ButtonDefaults.buttonColors(containerColor = YuberaPrimary)) {
                    if(state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Сохранить изменения")
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
    Column(Modifier.width(112.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF202127)).clickable(enabled = available, onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountCircle, null, tint = YuberaMuted, modifier = Modifier.size(64.dp))
            if(resource!=null) Image(painterResource(resource),null,Modifier.fillMaxSize())
            if(!available) Icon(Icons.Default.Lock,null,tint=Color(0xFFE0A45A),modifier=Modifier.align(Alignment.TopEnd).size(18.dp).background(Color(0xFF16171C),CircleShape))
            if(selected) Icon(Icons.Default.Check,null,tint=YuberaPrimary,modifier=Modifier.align(Alignment.BottomEnd).size(22.dp).background(Color(0xFF16171C),CircleShape))
        }
        Text(title,color=YuberaOnBackground,style=MaterialTheme.typography.labelMedium,maxLines=2)
        if(requirement.isNotBlank()) Text(if(available) "Доступна" else requirement,color=YuberaMuted,style=MaterialTheme.typography.labelSmall,maxLines=2)
    }
}
