package app.declutty.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.declutty.MainViewModel
import app.declutty.UpdateState
import app.declutty.data.AppSort
import app.declutty.data.Category
import app.declutty.data.sorted
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.Skeu
import app.declutty.ui.theme.Metro
import app.declutty.ui.theme.MonoFont
import app.declutty.ui.theme.Nd
import app.declutty.ui.theme.NdColor
import app.declutty.ui.theme.Motion
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: MainViewModel,
    openSettings: () -> Unit,
    openApps: (sortBySize: Boolean) -> Unit,
    openContacts: () -> Unit,
    openPhotos: () -> Unit,
    openCategory: (Category) -> Unit,
) {
    val context = LocalContext.current
    val apps = vm.visibleApps
    val now = remember { System.currentTimeMillis() }
    val unused = apps.count { it.unused(now) }
    val heavy = apps.sorted(AppSort.SIZE, true).take(8)
    val topCategories = apps.groupBy { it.category }.toList().sortedByDescending { it.second.size }.take(6)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = TabBarSpace + 24.dp)) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (Metro.on) MetroAppTitle("Declutty", Modifier.weight(1f).reveal(0))
                else if (Nd.on) Text("DECLUTTY", Modifier.weight(1f).reveal(0), style = MaterialTheme.typography.labelLarge, color = DF.Muted)
                else Text("Declutty", Modifier.weight(1f).reveal(0), style = MaterialTheme.typography.titleLarge, color = DF.Primary)
                RoundIconButton(if (Nd.on) Icons.Outlined.Settings else Icons.Rounded.Settings, "Impostazioni", onClick = openSettings)
            }
        }
        item { if (Nd.on) NdHero(apps.size, unused, vm.usageAccess) else Hero(apps.size, unused, vm.usageAccess) }

        item {
            AnimatedVisibility(vm.update is UpdateState.Available) {
                val info = (vm.update as? UpdateState.Available)?.info
                CardSurface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).pressScale(0.97f, onClick = openSettings)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SystemUpdate, null, tint = DF.Primary, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Declutty ${info?.version.orEmpty()} è disponibile", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                            Text("Tocca per aggiornare", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = DF.Primary)
                    }
                }
            }
        }

        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = if (Metro.on) 4.dp else 6.dp), horizontalArrangement = Arrangement.spacedBy(if (Metro.on) 8.dp else 10.dp)) {
                StatCard(Icons.Rounded.Apps, "${apps.size}", "App installate", 1, Modifier.weight(1f)) { openApps(true) }
                StatCard(Icons.Rounded.Storage, formatSize(apps.sumOf { it.size }), "Spazio delle app", 2, Modifier.weight(1f)) { openApps(true) }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(if (Metro.on) 8.dp else 10.dp)) {
                StatCard(Icons.Rounded.Bedtime, if (vm.usageAccess) "$unused" else "—", "Non usate da 30 giorni", 3, Modifier.weight(1f)) { openApps(false) }
                StatCard(
                    Icons.Rounded.People, vm.contacts?.let { "${vm.duplicates.size}" } ?: "—",
                    if (vm.contacts == null) "Doppioni: tocca per cercarli" else "Gruppi di doppioni", 4, Modifier.weight(1f), onClick = openContacts,
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) vm.photos?.let { p ->
            item {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(if (Metro.on) 8.dp else 10.dp)) {
                    StatCard(
                        Icons.Rounded.PhotoLibrary, p.groups?.size?.toString() ?: "—",
                        "Gruppi di foto simili", 5, Modifier.weight(1f), onClick = openPhotos,
                    )
                    StatCard(Icons.Rounded.Savings, formatSize(p.freedBytes), "Liberati dalle foto", 6, Modifier.weight(1f), onClick = openPhotos)
                }
            }
        }
        if (!vm.usageAccess) item { UsageAccessCard(context, vm, Modifier.padding(horizontal = 20.dp, vertical = 8.dp).reveal(5)) }

        if (heavy.isNotEmpty()) {
            item { SectionTitle("Le più pesanti", "Vedi tutte") { openApps(true) } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(heavy, key = { _, a -> a.pkg }) { i, app ->
                        Column(
                            Modifier.reveal(i).width(78.dp).pressScale(0.9f) { openApps(true) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncBitmap(app.pkg, vm.apps.cachedIcon(app.pkg), 60.dp, fallback = app.label) { vm.apps.icon(app.pkg) }
                            Spacer(Modifier.height(8.dp))
                            Text(app.label, style = MaterialTheme.typography.labelMedium, color = DF.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatSize(app.size), style = MaterialTheme.typography.labelSmall, color = if (Nd.on) DF.Muted else DF.Primary)
                        }
                    }
                }
            }
        }

        if (topCategories.isNotEmpty()) {
            item { SectionTitle("Categorie principali", null, null) }
            itemsIndexed(topCategories, key = { _, g -> g.first.name }) { i, (category, list) ->
                CategoryBar(category, list.size, topCategories.first().second.size, i) { openCategory(category) }
            }
        }
    }
}

/** Anello che si riempie con la quota di app usate di recente, e titolo con la parola chiave in evidenza. */
@Composable
private fun Hero(total: Int, unused: Int, usageAccess: Boolean) {
    val target = if (!usageAccess || total == 0) 1f else (total - unused).toFloat() / total
    val ring = remember { Animatable(0f) }
    LaunchedEffect(target) { delay(150); ring.animateTo(target, tween(1400, easing = Motion.Reveal)) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).reveal(1)) {
            Text(
                buildAnnotatedString {
                    append("Meno app,\n")
                    withStyle(SpanStyle(color = DF.Primary)) { append("più spazio") }
                    append(" per te.")
                },
                style = MaterialTheme.typography.displaySmall, color = DF.Text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (!usageAccess) "Seleziona, ordina e libera il telefono in pochi tocchi."
                else if (unused == 0) "Usi tutte le tue app: il telefono è in ordine."
                else "${plural(unused, "app non la apri", "app non le apri")} da più di un mese.",
                style = MaterialTheme.typography.bodyMedium, color = DF.Muted,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(104.dp).reveal(2), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                if (Skeu.on) { // quadrante in ottone intorno all'anello
                    drawCircle(Brush.sweepGradient(listOf(Color(0xFFF6DFA2), Color(0xFF8A6427), Color(0xFFE8C579), Color(0xFF6E4E1E), Color(0xFFF6DFA2))), size.minDimension / 2 + 8.dp.toPx(), style = Stroke(5.dp.toPx()))
                    drawCircle(Color.Black.copy(alpha = 0.45f), size.minDimension / 2 + 5.5.dp.toPx())
                }
                drawArc(DF.SurfaceHigher, 0f, 360f, false, style = Stroke(9.dp.toPx()))
                drawArc(
                    if (Metro.on) SolidColor(DF.Primary) else Brush.sweepGradient(listOf(DF.PrimaryStrong, DF.Primary, DF.Secondary, DF.PrimaryStrong)), -90f, 360f * ring.value, false,
                    style = Stroke(9.dp.toPx(), cap = if (Metro.on) StrokeCap.Butt else StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (usageAccess) "${(ring.value * 100).toInt()}%" else "$total", style = MaterialTheme.typography.headlineSmall, color = DF.Text)
                Text(if (usageAccess) "in uso" else "app", style = MaterialTheme.typography.labelSmall, color = DF.Muted)
            }
        }
    }
}

/** Nothing: il dato è la grafica. Percentuale enorme in Doto, barra a segmenti sotto, il resto piccolo ai bordi. */
@Composable
private fun NdHero(total: Int, unused: Int, usageAccess: Boolean) {
    val target = if (!usageAccess || total == 0) 1f else (total - unused).toFloat() / total
    val p = remember { Animatable(0f) }
    LaunchedEffect(target) { delay(150); p.animateTo(target, tween(900, easing = Motion.Nd)) }
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 32.dp).reveal(1)) {
        Text((if (usageAccess) "App in uso" else "App installate").uppercase(), style = MaterialTheme.typography.labelMedium, color = DF.Muted)
        Spacer(Modifier.height(8.dp))
        if (usageAccess) NdReadout("${(p.value * 100).toInt()}", "%") else NdReadout("$total", "app")
        Spacer(Modifier.height(16.dp))
        SegmentedBar(p.value, height = 18.dp, segments = 20, color = if (usageAccess && target < 0.5f) NdColor.Warning else NdColor.Display)
        Spacer(Modifier.height(16.dp))
        Text(
            if (!usageAccess) "Seleziona, ordina e libera il telefono in pochi tocchi."
            else if (unused == 0) "Usi tutte le tue app: il telefono è in ordine."
            else "${plural(unused, "app non la apri", "app non le apri")} da più di un mese.",
            style = MaterialTheme.typography.bodyMedium, color = DF.Text,
        )
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, index: Int, modifier: Modifier, onClick: () -> Unit) {
    if (Metro.on) return LiveTile(icon, value, label.lowercase(), index, modifier.reveal(index), onClick = onClick)
    if (Nd.on) return CardSurface(modifier.reveal(index).pressScale(onClick = onClick), padding = PaddingValues(16.dp)) {
        // Widget Nothing: categoria in alto a sinistra, valore in Space Mono. Niente icona: il dato basta.
        Column {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = DF.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(20.dp))
            AnimatedNumber(value, MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFont), NdColor.Display)
        }
    }
    CardSurface(modifier.reveal(index).pressScale(0.95f, onClick = onClick), padding = PaddingValues(16.dp)) {
        Column {
            Box(Modifier.size(36.dp).clip(rounded(12.dp)).background(DF.Primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = DF.Primary, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(14.dp))
            AnimatedNumber(value, MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall, color = DF.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onAction: (() -> Unit)?) {
    if (Nd.on) return Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 40.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = DF.Muted)
        if (action != null && onAction != null) Text("[ ${action.uppercase()} ]", Modifier.pressScale(onClick = onAction).padding(8.dp), style = MaterialTheme.typography.labelMedium, color = DF.Text)
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (Metro.on) title.lowercase() else title, Modifier.weight(1f), style = if (Metro.on) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, color = DF.Text)
        if (action != null && onAction != null) {
            Row(Modifier.pressScale(0.9f, onClick = onAction).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (Metro.on) action.lowercase() else action, style = MaterialTheme.typography.labelLarge, color = DF.Primary)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = DF.Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Barra orizzontale che cresce fino alla quota della categoria rispetto alla più grande. */
@Composable
private fun CategoryBar(category: Category, count: Int, max: Int, index: Int, onClick: () -> Unit) {
    val color = category.color()
    val p = remember { Animatable(0f) }
    LaunchedEffect(count, max) { delay(200L + index * 70); p.animateTo(count.toFloat() / max.coerceAtLeast(1), tween(900, easing = Motion.Reveal)) }
    if (Nd.on) return Column(Modifier.reveal(index).fillMaxWidth().pressScale(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp)) {
        // Riga dati Nothing: etichetta a sinistra, valore a destra, barra compatta a segmenti sotto.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(category.icon(), null, tint = DF.Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(category.title.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = DF.Muted)
            Text("$count", style = MaterialTheme.typography.labelLarge, color = DF.Text)
        }
        Spacer(Modifier.height(8.dp))
        SegmentedBar(p.value, height = 6.dp, segments = 32)
    }
    Row(
        Modifier.reveal(index).fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).pressScale(0.97f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(rounded(12.dp)).background(categoryBrush(category)), contentAlignment = Alignment.Center) {
            Icon(category.icon(), null, tint = if (Metro.on) Color.White else color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(category.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = DF.Text)
                Text("$count", style = MaterialTheme.typography.labelLarge, color = color)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(rounded(3.dp)).background(DF.SurfaceHigher)) {
                Box(
                    Modifier.fillMaxWidth(p.value.coerceAtLeast(0.02f)).height(6.dp).clip(rounded(3.dp))
                        .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color))),
                )
            }
        }
    }
}

