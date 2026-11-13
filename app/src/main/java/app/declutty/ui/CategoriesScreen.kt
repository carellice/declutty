package app.declutty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.declutty.CategorySort
import app.declutty.MainViewModel
import app.declutty.data.AppItem
import app.declutty.data.Category
import app.declutty.ui.theme.DF

@Composable
fun CategoriesScreen(vm: MainViewModel, open: (Category) -> Unit) {
    val groups = vm.visibleApps.groupBy { it.category }.toList().let { list ->
        when (vm.categorySort) {
            CategorySort.DEFAULT -> list.sortedBy { it.first.ordinal }
            CategorySort.NAME -> list.sortedBy { it.first.title }
            CategorySort.COUNT -> list.sortedByDescending { it.second.size }
            CategorySort.SIZE -> list.sortedByDescending { g -> g.second.sumOf { it.size } }
        }
    }
    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = TabBarSpace + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(12.dp))
                ScreenHeader(
                    "Categorie", "${plural(groups.size, "categoria", "categorie")} · divise in automatico",
                    Modifier.offset(x = (-14).dp),
                )
                Spacer(Modifier.height(14.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategorySort.entries.forEach { s ->
                        item { DfChip(s.label, vm.categorySort == s, icon = if (vm.categorySort == s) Icons.AutoMirrored.Rounded.Sort else null) { vm.changeCategorySort(s) } }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (vm.appsLoading) {
            items(6) { Box(Modifier.height(150.dp).clip(RoundedCornerShape(26.dp)).shimmer()) }
        } else {
            itemsIndexed(groups, key = { _, g -> g.first.name }) { i, (category, apps) ->
                CategoryCard(vm, category, apps, Modifier.animateItem().reveal(i)) { open(category) }
            }
        }
        item(span = { GridItemSpan(2) }) {
            Text(
                "Tieni premuta un'app per spostarla in un'altra categoria.",
                Modifier.padding(top = 10.dp, start = 6.dp), style = MaterialTheme.typography.bodySmall, color = DF.Muted,
            )
        }
    }
}

@Composable
private fun CategoryCard(vm: MainViewModel, category: Category, apps: List<AppItem>, modifier: Modifier, onClick: () -> Unit) {
    val color = category.color()
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier.pressScale(0.95f, onClick = onClick).clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), DF.Surface)))
            .border(1.dp, color.copy(alpha = 0.26f), shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(categoryBrush(category)), contentAlignment = Alignment.Center) {
                Icon(category.icon(), null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            AnimatedNumber("${apps.size}", MaterialTheme.typography.headlineMedium, color)
        }
        Spacer(Modifier.height(14.dp))
        Text(category.title, style = MaterialTheme.typography.titleMedium, color = DF.Text, maxLines = 1)
        Text(formatSize(apps.sumOf { it.size }), style = MaterialTheme.typography.bodySmall, color = DF.Muted)
        Spacer(Modifier.height(12.dp))
        // Le prime icone sovrapposte come un piccolo mazzo di carte.
        Row {
            apps.sortedByDescending { it.lastUsed ?: it.updated }.take(4).forEachIndexed { i, app ->
                Box(
                    Modifier.offset(x = (-8 * i).dp).size(28.dp).clip(RoundedCornerShape(9.dp)).border(1.5.dp, DF.Surface, RoundedCornerShape(9.dp)),
                ) { AsyncBitmap(app.pkg, vm.apps.cachedIcon(app.pkg), 28.dp, fallback = app.label) { vm.apps.icon(app.pkg) } }
            }
            if (apps.size > 4) {
                Box(
                    Modifier.offset(x = (-32).dp).size(28.dp).clip(CircleShape).background(DF.SurfaceHigher),
                    contentAlignment = Alignment.Center,
                ) { Text("+${apps.size - 4}", style = MaterialTheme.typography.labelSmall, color = DF.Muted) }
            }
        }
    }
}
