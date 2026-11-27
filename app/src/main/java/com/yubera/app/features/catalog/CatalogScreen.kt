package com.yubera.app.features.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaOnBackground
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface

@Composable
fun CatalogScreen(onAnimeClick:(AnimeCatalogItem)->Unit,onNotificationsClick:()->Unit={},viewModel:CatalogViewModel=hiltViewModel()) {
 val state by viewModel.uiState.collectAsState()
 var panel by rememberSaveable { mutableStateOf(false) }
 var page by rememberSaveable { mutableIntStateOf(0) }
 val grid=rememberLazyGridState()
 val count=maxOf(1,(state.items.size+49)/50)
 LaunchedEffect(state.query,state.filters){page=0;grid.scrollToItem(0)}
 LaunchedEffect(page){grid.scrollToItem(0)}
 Column(Modifier.fillMaxSize()){
  Box(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),contentAlignment=Alignment.Center){ YuberaLogo(width=124.dp); IconButton(onClick=onNotificationsClick,modifier=Modifier.align(Alignment.CenterEnd)){Icon(Icons.Default.Notifications,contentDescription="Уведомления",tint=YuberaOnBackground)} }
  Text("Каталог",Modifier.padding(start=16.dp),color=YuberaOnBackground,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   OutlinedTextField(state.query,viewModel::onQueryChange,Modifier.weight(1f),placeholder={Text("Название аниме")},singleLine=true)
   IconButton(onClick={panel=!panel},modifier=Modifier.padding(start=6.dp).background(YuberaSurface,RoundedCornerShape(12.dp))){Icon(Icons.Default.FilterList,"Фильтры",tint=YuberaPrimary)}
  }
  Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){CatalogSortOrder.entries.forEach{FilterChip(state.sortOrder==it,{viewModel.onSortOrderSelected(it);page=0},label={Text(it.title,maxLines=1)})}}
  Box(Modifier.weight(1f).fillMaxWidth()){
   if(state.items.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(if(state.errorMessage!=null)"Каталог пока недоступен" else "Ничего не найдено",color=YuberaMuted)}
   else LazyVerticalGrid(GridCells.Fixed(2),state=grid,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    items(state.items.drop(page*50).take(50),key=AnimeCatalogItem::id){anime->PosterCard(anime){onAnimeClick(anime)}}
   }
   AnimatedVisibility(panel,modifier=Modifier.align(Alignment.TopEnd).padding(8.dp),enter=fadeIn()+scaleIn(initialScale=.94f),exit=fadeOut()+scaleOut(targetScale=.94f)){ FilterPanel(state,viewModel::updateDraft,viewModel::resetDraft,{viewModel.applyFilters();panel=false;page=0},{panel=false}) }
  }
  if(state.items.isNotEmpty()) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
   TextButton(onClick={page=(page-1).coerceAtLeast(0)},enabled=page>0){Text("‹")}
   PaginationNumbers(page,count){page=it}
   TextButton(onClick={page=(page+1).coerceAtMost(count-1)},enabled=page<count-1){Text("›")}
  }
 }
}

@Composable private fun PosterCard(anime:AnimeCatalogItem,onClick:()->Unit){
 Column(Modifier.fillMaxWidth().clickable(onClick).padding(bottom=4.dp)){
 Box(Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(14.dp)).background(YuberaSurface)){
 if(!anime.posterUrl.isNullOrBlank())AsyncImage(anime.posterUrl,"Постер: "+anime.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
 else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(anime.title.take(1),color=YuberaPrimary)}
 anime.rating?.let{Text("★ "+String.format("%.1f",it),Modifier.align(Alignment.TopStart).padding(5.dp).background(Color.Black.copy(.75f),RoundedCornerShape(6.dp)).padding(5.dp),color=Color(0xFFFFB547))}
 anime.ageRating?.let{Text(it,Modifier.align(Alignment.TopEnd).padding(5.dp).background(Color.Black.copy(.75f),RoundedCornerShape(6.dp)).padding(5.dp),color=Color.White)}
 }
 Text(anime.title,color=YuberaOnBackground,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
 val metadata=listOfNotNull(anime.kind,anime.year.takeIf{it>0}?.toString(),anime.episodes.takeIf{it>0}?.let{it.toString()+" сер."}).joinToString(" · ")
 if(metadata.isNotBlank())Text(metadata,color=YuberaMuted,maxLines=1,style=MaterialTheme.typography.labelSmall)
 }
}

@Composable private fun FilterPanel(state:CatalogUiState,onDraft:(CatalogFilters)->Unit,onReset:()->Unit,onApply:()->Unit,onClose:()->Unit,modifier:Modifier=Modifier){
 val f=state.draftFilters
 var from by remember(f.yearFrom){mutableStateOf(f.yearFrom?.toString().orEmpty())}
 var to by remember(f.yearTo){mutableStateOf(f.yearTo?.toString().orEmpty())}
 var error by remember{mutableStateOf<String?>(null)}
 Surface(modifier.widthIn(max=350.dp).fillMaxWidth(),color=YuberaSurface,shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,YuberaPrimary.copy(.45f)),shadowElevation=12.dp){
 Column(Modifier.padding(12.dp).heightIn(max=560.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Row(verticalAlignment=Alignment.CenterVertically){Text("Фильтры",Modifier.weight(1f),color=YuberaOnBackground,fontWeight=FontWeight.Bold);TextButton(onClick=onClose){Text("Закрыть")}}
 Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
 SelectFilter("Статус",f.status,state.statuses){onDraft(f.copy(status=it))}
 SelectFilter("Тип",f.kind,state.kinds){onDraft(f.copy(kind=it))}
 SelectFilter("Возраст",f.ageRating,state.ageRatings){onDraft(f.copy(ageRating=it))}
 Text("Жанры",color=YuberaMuted)
 state.genres.forEach{g->FilterChip(g in f.genres,{onDraft(f.copy(genres=if(g in f.genres)f.genres-g else f.genres+g))},label={Text(g)})}
 Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(from,{from=it.filter(Char::isDigit);onDraft(f.copy(yearFrom=from.toIntOrNull()))},Modifier.weight(1f),label={Text("Год от")},singleLine=true);OutlinedTextField(to,{to=it.filter(Char::isDigit);onDraft(f.copy(yearTo=to.toIntOrNull()))},Modifier.weight(1f),label={Text("Год до")},singleLine=true)}
 Text("Минимальная оценка: "+String.format("%.1f",f.minRating),color=YuberaOnBackground)
 Slider(f.minRating.toFloat(),{onDraft(f.copy(minRating=it.toDouble()))},valueRange=0f..10f,steps=19)
 error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
 }
 Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
 OutlinedButton(onClick={onReset();from="";to="";error=null},Modifier.weight(1f)){Text("Сброс")}
 Button(onClick={error=f.validate((state.years.maxOrNull()?:2026)+1);if(error==null)onApply()},Modifier.weight(1f)){Text("Применить")}
 }
 }
 }
 }
}

@Composable private fun SelectFilter(title:String,value:String?,options:List<String>,onSelect:(String?)->Unit){
 var expanded by remember{mutableStateOf(false)}
 Box{OutlinedButton(onClick={expanded=true},Modifier.fillMaxWidth()){Text(title+": "+(value?: "Не выбрано"),Modifier.weight(1f),maxLines=1);Text("⌄")}
 DropdownMenu(expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=260.dp)){
 DropdownMenuItem(text={Text("Не выбрано")},onClick={onSelect(null);expanded=false})
 options.forEach{DropdownMenuItem(text={Text(it)},onClick={onSelect(it);expanded=false})}
 }}
}
