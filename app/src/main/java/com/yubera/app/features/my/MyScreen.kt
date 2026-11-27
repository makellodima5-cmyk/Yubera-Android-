package com.yubera.app.features.my
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
@Composable fun MyScreen(onAnimeClick:(String)->Unit,onOpenSection:(String)->Unit={},section:String?=null,onNotificationsClick:()->Unit={},viewModel:MyViewModel=hiltViewModel()){
 val s by viewModel.uiState.collectAsState();var centerIndex by rememberSaveable{mutableIntStateOf(0)}
 val rows=when(section){"watch-later"->s.watchLater;"history"->s.history;"subscriptions"->s.subscriptions;else->emptyList()}
 val sectionTitle=when(section){"watch-later"->"Смотреть позже";"history"->"История просмотра";"subscriptions"->"Подписки";else->"Моё"}
 Column(Modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){YuberaLogo(Modifier.weight(1f),124.dp);IconButton(onNotificationsClick){Icon(Icons.Default.Notifications,"Уведомления",tint=YuberaOnBackground)}}
  if(section!=null){Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){TextButton({onOpenSection("")}){Text("‹ Моё")};Text(sectionTitle,color=YuberaOnBackground,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
   if(rows.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Здесь пока нет тайтлов.",color=YuberaMuted)}else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(rows,key={it.anime.id}){r->MyAnimeCard(r){onAnimeClick(r.anime.id)}}}
  }else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   item{Text("Моё",Modifier.padding(horizontal=18.dp),color=YuberaOnBackground,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
   item{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Default.Favorite,null,tint=YuberaPrimary);Text("Избранное",color=YuberaOnBackground,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
    if(s.favorites.isEmpty())Card(Modifier.fillMaxWidth().padding(horizontal=18.dp),colors=CardDefaults.cardColors(containerColor=YuberaSurface)){Column(Modifier.padding(18.dp)){Text("Избранное пока пусто",color=YuberaOnBackground,fontWeight=FontWeight.SemiBold);Text("Добавляйте тайтлы с помощью сердечка на странице аниме.",color=YuberaMuted)}}else Carousel(s.favorites,centerIndex.coerceIn(0,s.favorites.lastIndex),{centerIndex=it},onAnimeClick,viewModel::removeFavorite)
   }}
   item{Tile(Icons.Default.Bookmark,"Смотреть позже","Тайтлы, которые вы отложили",s.watchLater.size){onOpenSection("watch-later")}}
   item{Tile(Icons.Default.Notifications,"Подписки","Тайтлы, за которыми вы следите",s.subscriptions.size){onOpenSection("subscriptions")}}
   item{Tile(Icons.Default.History,"История просмотра","Ранее просмотренные тайтлы",s.history.size){onOpenSection("history")}}
  }
 }
}
@Composable private fun Carousel(fav:List<MyLibraryItem>,initial:Int,onCenter:(Int)->Unit,onOpen:(String)->Unit,onRemove:(String)->Unit){
 val scope=rememberCoroutineScope();var center by rememberSaveable{mutableIntStateOf(initial)};var drag by remember{mutableFloatStateOf(0f)};val n=fav.size
 LaunchedEffect(n,initial){center=if(n==0)0 else initial.coerceIn(0,n-1)}
 val cards=remember(center,fav){if(n==0)emptyList()else(-2..2).map{off->val i=((center+off)%n+n)%n;Triple(i,off,fav[i])}.distinctBy{it.third.anime.id}}
 Box(Modifier.fillMaxWidth().height(350.dp).pointerInput(n){detectHorizontalDragGestures(onHorizontalDrag={c,d->c.consume();drag+=d},onDragEnd={if(abs(drag)>48&&n>1){center=(center+(if(drag<0)1 else -1)+n)%n;onCenter(center)};drag=0f},onDragCancel={drag=0f})},contentAlignment=Alignment.Center){
  cards.forEach{(idx,pos,item)->val scale by animateFloatAsState(if(pos==0)1f else if(abs(pos)==1).78f else .58f,tween(260),label="scale");val rotation by animateFloatAsState(pos*-18f,tween(260),label="rotation");val blur by animateFloatAsState(if(abs(pos)>1)2.2f else 0f,tween(260),label="blur")
   Poster(item.anime,Modifier.width(214.dp).height(318.dp).graphicsLayer{translationX=pos*118f;scaleX=scale;scaleY=scale;rotationY=rotation;cameraDistance=12*density;alpha=if(abs(pos)>1).56f else 1f}.blur(blur.dp),pos==0,{if(pos==0)onOpen(item.anime.id)else{center=idx;onCenter(idx);scope.launch{delay(260);onOpen(item.anime.id)}}},{onRemove(item.anime.id)})
  }
 }
}
@Composable private fun Poster(a:AnimeCatalogItem,modifier:Modifier,central:Boolean,click:()->Unit,remove:()->Unit){
 Card(onClick=click,modifier=modifier,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=YuberaSurface),elevation=CardDefaults.cardElevation(if(central)8.dp else 2.dp)){Box(Modifier.fillMaxSize()){
 if(!a.posterUrl.isNullOrBlank())AsyncImage(a.posterUrl,"Постер: \${a.title}",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Box(Modifier.fillMaxSize().background(YuberaSurfaceElevated),contentAlignment=Alignment.Center){Text(a.title.take(1),color=YuberaPrimary,style=MaterialTheme.typography.displayLarge)}
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent,Color(0xE8000000)))))
 Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){a.rating?.let{Badge("★ %.1f".format(it),Color(0xFFFFB547))};a.ageRating?.let{Badge(it,Color.White)}}
 Column(Modifier.align(Alignment.BottomStart).padding(start=12.dp,end=48.dp,bottom=12.dp)){Text(a.title,color=Color.White,fontWeight=FontWeight.Bold,maxLines=3,overflow=TextOverflow.Ellipsis);if(central)Text(listOfNotNull(a.kind,a.year.takeIf{it>0}?.toString()).joinToString(" · "),color=Color.White.copy(.7f),style=MaterialTheme.typography.labelSmall)}
 IconButton(remove,Modifier.align(Alignment.BottomEnd).padding(8.dp).size(38.dp).background(Color.Black.copy(.5f),RoundedCornerShape(50))){Icon(Icons.Default.Favorite,"Удалить из избранного",tint=Color(0xFFFF3B45))}
 }}
}
@Composable private fun Badge(text:String,color:Color){Box(Modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(.65f)).padding(horizontal=7.dp,vertical=4.dp)){Text(text,color=color,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold)}}
@Composable private fun Tile(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,count:Int,onClick:()->Unit){Card(onClick,Modifier.fillMaxWidth().padding(horizontal=18.dp),colors=CardDefaults.cardColors(containerColor=YuberaSurface),shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,YuberaSurfaceElevated)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Icon(icon,null,tint=YuberaPrimary,modifier=Modifier.size(28.dp));Column(Modifier.weight(1f)){Text(title,color=YuberaOnBackground,fontWeight=FontWeight.Bold);Text(subtitle,color=YuberaMuted,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)}Text(count.toString(),color=YuberaMuted);Icon(Icons.Default.ArrowForward,null,tint=YuberaMuted)}}}
