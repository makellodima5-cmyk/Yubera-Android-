package com.yubera.app.features.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.expandIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
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
fun CatalogScreen(
    onAnimeClick: (AnimeCatalogItem) -> Unit,
    onNotificationsClick: () -> Unit = {},
    resetKey: Int = 0,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var panel by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var pendingScrollToTop by rememberSaveable { mutableStateOf(false) }
    var skipInitialQueryFilterReset by remember { mutableStateOf(true) }
    var lastResetKey by rememberSaveable { mutableIntStateOf(resetKey) }
    val grid = rememberLazyGridState()
    val actualPageCount = (state.items.size + 49) / 50
    val count = maxOf(1, actualPageCount)

    LaunchedEffect(state.query, state.filters) {
        if (skipInitialQueryFilterReset) {
            skipInitialQueryFilterReset = false
        } else {
            page = 0
            pendingScrollToTop = true
        }
    }
    LaunchedEffect(page, pendingScrollToTop) {
        panel = false
        if (pendingScrollToTop) {
            grid.scrollToItem(0)
            pendingScrollToTop = false
        }
    }
    LaunchedEffect(actualPageCount) {
        if (actualPageCount > 0 && page >= actualPageCount) {
            page = (actualPageCount - 1).coerceAtLeast(0)
            pendingScrollToTop = true
        }
    }
    LaunchedEffect(resetKey) {
        if (resetKey != lastResetKey) {
            lastResetKey = resetKey
            page = 0
            panel = false
            viewModel.resetCatalog()
            pendingScrollToTop = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            YuberaLogo(width = 124.dp)
            IconButton(onClick = onNotificationsClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Notifications, contentDescription = "Уведомления", tint = YuberaOnBackground)
            }
        }
        Text(
            "Каталог",
            Modifier.padding(start = 16.dp),
            color = YuberaOnBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                state.query,
                viewModel::onQueryChange,
                Modifier.weight(1f),
                placeholder = { Text("Название аниме") },
                singleLine = true
            )
            IconButton(
                onClick = { panel = !panel },
                modifier = Modifier.padding(start = 6.dp).background(if (panel) YuberaPrimary.copy(alpha = 0.18f) else YuberaSurface, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.FilterList, "Фильтры", tint = YuberaPrimary)
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CatalogSortOrder.entries.forEach {
                FilterChip(state.sortOrder == it, { viewModel.onSortOrderSelected(it); page = 0; pendingScrollToTop = true }, label = { Text(it.title, maxLines = 1) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (state.errorMessage != null) "Каталог пока недоступен" else "Ничего не найдено", color = YuberaMuted)
                }
            } else {
                LazyVerticalGrid(
                    GridCells.Fixed(2),
                    state = grid,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.items.drop(page * 50).take(50), key = AnimeCatalogItem::id) { anime ->
                        PosterCard(anime) { onAnimeClick(anime) }
                    }
                }
            }
            if (panel) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable { panel = false }
                )
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Column {
                    AnimatedVisibility(
                        visible = panel,
                        enter = if (rememberCatalogMotionAllowed()) fadeIn() + expandIn(expandFrom = Alignment.TopEnd) + scaleIn(initialScale = .92f) else EnterTransition.None,
                        exit = if (rememberCatalogMotionAllowed()) fadeOut() + shrinkOut(shrinkTowards = Alignment.TopEnd) + scaleOut(targetScale = .92f) else ExitTransition.None
                    ) {
                        FilterPanel(
                            state,
                            viewModel::updateDraft,
                            viewModel::resetFilters,
                            { viewModel.applyFilters(); panel = false; page = 0; pendingScrollToTop = true },
                            { panel = false }
                        )
                    }
                }
            }
        }
        if (state.items.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0) { Text("‹") }
                PaginationNumbers(page, count) { selected -> page = selected; panel = false; pendingScrollToTop = true }
                TextButton(onClick = { page = (page + 1).coerceAtMost(count - 1) }, enabled = page < count - 1) { Text("›") }
            }
        }
    }
}

@Composable private fun PosterCard(anime:AnimeCatalogItem,onClick:()->Unit){
 Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(bottom=4.dp)){
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

@Composable
private fun FilterPanel(
    state: CatalogUiState,
    onDraft: (CatalogFilters) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val f = state.draftFilters
    var from by remember(f.yearFrom) { mutableStateOf(f.yearFrom?.toString().orEmpty()) }
    var to by remember(f.yearTo) { mutableStateOf(f.yearTo?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedFilter by remember { mutableStateOf<String?>(null) }
    val maxYear = (state.years.maxOrNull() ?: 2026) + 1
    val fromYear = from.toIntOrNull()
    val toYear = to.toIntOrNull()
    val fromError = fromYear?.takeIf { it !in 1900..maxYear }?.let { "Год должен быть от 1900 до $maxYear." }
    val toError = toYear?.takeIf { it !in 1900..maxYear }?.let { "Год должен быть от 1900 до $maxYear." }
    val rangeError = if (fromYear != null && toYear != null && fromYear > toYear) "Год «От» не может быть больше года «До»." else null

    Surface(
        modifier.widthIn(max = 350.dp).fillMaxWidth(),
        color = YuberaSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, YuberaPrimary.copy(.45f)),
        shadowElevation = 12.dp
    ) {
        Column(Modifier.padding(12.dp).heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = YuberaPrimary)
                Text("Фильтры", Modifier.weight(1f).padding(start = 8.dp), color = YuberaOnBackground, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть фильтры", tint = YuberaOnBackground)
                }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectFilter("Статус", "status", f.status, state.statuses, expandedFilter, { expandedFilter = it }) { onDraft(f.copy(status = it)) }
                SelectFilter("Тип", "kind", f.kind, state.kinds, expandedFilter, { expandedFilter = it }) { onDraft(f.copy(kind = it)) }
                SelectFilter("Возраст", "age", f.ageRating, state.ageRatings, expandedFilter, { expandedFilter = it }) { onDraft(f.copy(ageRating = it)) }
                GenreFilter(state.genres, f.genres, expandedFilter, { expandedFilter = it }) { selected -> onDraft(f.copy(genres = selected)) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(from, { from = it.filter(Char::isDigit); onDraft(f.copy(yearFrom = from.toIntOrNull())) }, Modifier.weight(1f), label = { Text("Год от") }, singleLine = true, isError = fromError != null)
                    OutlinedTextField(to, { to = it.filter(Char::isDigit); onDraft(f.copy(yearTo = to.toIntOrNull())) }, Modifier.weight(1f), label = { Text("Год до") }, singleLine = true, isError = toError != null)
                }
                fromError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                toError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                rangeError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("Минимальная оценка: " + String.format("%.1f", f.minRating), color = YuberaOnBackground)
                Slider(f.minRating.toFloat(), { onDraft(f.copy(minRating = it.toDouble())) }, valueRange = 0f..10f, steps = 19)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onReset(); from = ""; to = ""; error = null }, Modifier.weight(1f)) { Text("Сброс") }
                Button(onClick = { error = f.validate(maxYear) ?: fromError ?: toError ?: rangeError; if (error == null) onApply() }, Modifier.weight(1f)) { Text("Применить") }
            }
        }
    }
}

@Composable
private fun GenreFilter(options: List<String>, selected: Set<String>, expandedFilter: String?, setExpandedFilter: (String?) -> Unit, onChange: (Set<String>) -> Unit) {
    val expanded = expandedFilter == "genres"
    Box {
        OutlinedButton(onClick = { setExpandedFilter(if (expanded) null else "genres") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (selected.isEmpty()) "Жанры: не выбрано" else "Жанры: ${selected.size}", Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { setExpandedFilter(null) }, modifier = Modifier.heightIn(max = 300.dp)) {
            options.forEach { genre ->
                DropdownMenuItem(
                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(genre in selected, onCheckedChange = null); Text(genre) } },
                    onClick = { onChange(if (genre in selected) selected - genre else selected + genre) }
                )
            }
            DropdownMenuItem(text = { Text("Готово") }, onClick = { setExpandedFilter(null) })
        }
    }
}

@Composable
private fun SelectFilter(
    title: String,
    key: String,
    value: String?,
    options: List<String>,
    expandedFilter: String?,
    setExpandedFilter: (String?) -> Unit,
    onSelect: (String?) -> Unit
) {
    val expanded = expandedFilter == key
    Box {
        OutlinedButton(onClick = { setExpandedFilter(if (expanded) null else key) }, Modifier.fillMaxWidth()) {
            Text("$title: ${value ?: "Не выбрано"}", Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded, onDismissRequest = { setExpandedFilter(null) }, modifier = Modifier.heightIn(max = 260.dp)) {
            DropdownMenuItem(text = { Text("Не выбрано") }, onClick = { onSelect(null); setExpandedFilter(null) })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); setExpandedFilter(null) })
            }
        }
    }
}


@Composable
private fun PaginationNumbers(page: Int, pageCount: Int, onPage: (Int) -> Unit) {
    val numbers = when {
        pageCount <= 5 -> (0 until pageCount).toList()
        page <= 1 -> listOf(0, 1, 2, pageCount - 1).distinct()
        page >= pageCount - 2 -> listOf(0, pageCount - 3, pageCount - 2, pageCount - 1).distinct()
        else -> listOf(0, page - 1, page, page + 1, pageCount - 1).distinct()
    }
    numbers.forEachIndexed { index, number ->
        if (index > 0 && number - numbers[index - 1] > 1) {
            Text("…", color = YuberaMuted)
        }
        TextButton(onClick = { onPage(number) }) {
            Text(
                (number + 1).toString(),
                color = if (number == page) YuberaPrimary else YuberaMuted,
                fontWeight = if (number == page) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}


@Composable
private fun rememberCatalogMotionAllowed(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
}
