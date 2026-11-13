package app.declutty.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.declutty.MainViewModel
import app.declutty.data.AppItem
import app.declutty.data.AppSort
import app.declutty.data.Category
import app.declutty.data.sorted
import app.declutty.ui.theme.DF

private const val UNUSED_DAYS = 30
private const val DAY = 24L * 60 * 60 * 1000

fun AppItem.unused(now: Long = System.currentTimeMillis()) = lastUsed != null && now - lastUsed > UNUSED_DAYS * DAY

/** Spazio sotto la lista lasciato alla barra delle schede. */
val TabBarSpace = 104.dp

@Composable
fun AppsScreen(vm: MainViewModel, unusedOnly: Boolean, onUnused: (Boolean) -> Unit) {
    AppListScreen(vm, "App", vm.visibleApps, bottomSpace = TabBarSpace, unusedOnly = unusedOnly, onUnused = onUnused)
}

@Composable
fun CategoryAppsScreen(vm: MainViewModel, category: Category, onBack: () -> Unit) {
    AppListScreen(vm, category.title, vm.visibleApps.filter { it.category == category }, bottomSpace = 0.dp, onBack = onBack, accent = category.color())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListScreen(
    vm: MainViewModel,
    title: String,
    apps: List<AppItem>,
    bottomSpace: Dp,
    onBack: (() -> Unit)? = null,
    unusedOnly: Boolean = false,
    onUnused: ((Boolean) -> Unit)? = null,
    accent: Color = DF.Primary,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var sortSheet by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<AppItem?>(null) }
    var moving by remember { mutableStateOf<Set<String>?>(null) }
    var confirmHide by remember { mutableStateOf(false) }

    val now = remember { System.currentTimeMillis() }
    val shown = apps
        .filter { query.isBlank() || it.label.contains(query.trim(), true) || it.pkg.contains(query.trim(), true) }
        .filter { !unusedOnly || it.unused(now) }
        .sorted(vm.appSort, vm.appDesc)
    // La selezione segue le app ancora presenti: quelle disinstallate o nascoste escono da sole.
    val live = selected.filter { pkg -> apps.any { it.pkg == pkg } }.toSet()
    val selectedApps = apps.filter { it.pkg in live }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomSpace + 120.dp)) {
            item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    if (onBack != null) {
                        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", Modifier.padding(start = 20.dp, bottom = 12.dp), onClick = onBack)
                    }
                    ScreenHeader(
                        title,
                        if (live.isEmpty()) "${plural(apps.size, "app", "app")} · ${formatSize(apps.sumOf { it.size })}"
                        else "${plural(live.size, "selezionata", "selezionate")} · ${formatSize(selectedApps.sumOf { it.size })}",
                        trailing = { RoundIconButton(Icons.AutoMirrored.Rounded.Sort, "Ordina", tint = accent) { sortSheet = true } },
                    )
                    Spacer(Modifier.height(16.dp))
                    SearchField(query, { query = it }, "Cerca un'app")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            DfChip(vm.appSort.label, true, icon = if (vm.appDesc) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward) { sortSheet = true }
                        }
                        if (onUnused != null) item {
                            DfChip("Inutilizzate da $UNUSED_DAYS giorni", unusedOnly, icon = Icons.Rounded.Bedtime) { onUnused(!unusedOnly) }
                        }
                        item {
                            val all = shown.isNotEmpty() && shown.all { it.pkg in live }
                            DfChip(if (all) "Deseleziona" else "Seleziona tutte", false, icon = Icons.Rounded.DoneAll) {
                                selected = if (all) live - shown.map { it.pkg }.toSet() else live + shown.map { it.pkg }
                            }
                        }
                    }
                    if (!vm.usageAccess && (unusedOnly || vm.appSort == AppSort.LAST_USED)) {
                        UsageAccessCard(context, vm, Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            when {
                vm.appsLoading -> items(8) { LoadingRow() }
                shown.isEmpty() -> item {
                    EmptyState(
                        if (unusedOnly && vm.usageAccess) "Nessuna app dimenticata" else "Nessuna app",
                        if (unusedOnly && vm.usageAccess) "Usi tutte le app che hai installato. Ottimo lavoro!" else "Prova a cambiare ricerca o filtri.",
                    )
                }
                else -> itemsIndexed(shown, key = { _, it -> it.pkg }) { i, app ->
                    AppRow(
                        vm, app, app.pkg in live, accent, i,
                        Modifier.animateItem(),
                        onLongClick = { actionsFor = app },
                    ) { selected = if (app.pkg in live) live - app.pkg else live + app.pkg }
                }
            }
        }

        SelectionBar(
            visible = live.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomSpace),
            label = "${live.size} · ${formatSize(selectedApps.sumOf { it.size })}",
            onClear = { selected = emptySet() },
        ) {
            BarAction(Icons.Rounded.VisibilityOff, "Nascondi") { confirmHide = true }
            BarAction(Icons.AutoMirrored.Rounded.DriveFileMove, "Sposta") { moving = live }
            MintButton("Disinstalla", Icons.Rounded.DeleteSweep, danger = true) {
                vm.startUninstall(selectedApps.sorted(vm.appSort, vm.appDesc)); selected = emptySet()
            }
        }
    }

    if (confirmHide) ConfirmDialog(
        "Nascondere ${plural(live.size, "app", "app")}?",
        "Non compariranno più in Declutty, ma restano installate sul telefono. Le ritrovi in Impostazioni → App nascoste.",
        "Nascondi", onDismiss = { confirmHide = false },
    ) { vm.hide(live); selected = emptySet() }

    if (sortSheet) SortSheet(vm) { sortSheet = false }
    actionsFor?.let { app -> AppActionsSheet(vm, context, app, onMove = { moving = setOf(app.pkg) }) { actionsFor = null } }
    moving?.let { pkgs ->
        MoveSheet(vm.allApps.firstOrNull { it.pkg in pkgs }?.category, onDismiss = { moving = null }) { cat ->
            vm.moveTo(pkgs, cat); selected = emptySet()
            vm.message = "Spostate in ${cat.title}"
        }
    }
}

@Composable
private fun AppRow(
    vm: MainViewModel, app: AppItem, selected: Boolean, accent: Color, index: Int, modifier: Modifier,
    onLongClick: () -> Unit, onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent, tween(240), label = "rowBg")
    val border by animateColorAsState(if (selected) accent.copy(alpha = 0.4f) else Color.Transparent, tween(240), label = "rowBorder")
    val iconScale by animateFloatAsState(if (selected) 0.88f else 1f, spring(0.45f, 420f), label = "iconScale")
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.reveal(index).fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            .pressScale(0.97f, onLongClick, onClick).clip(shape).background(bg).border(1.dp, border, shape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) {
            AsyncBitmap(app.pkg, vm.apps.cachedIcon(app.pkg), 48.dp, fallback = app.label) { vm.apps.icon(app.pkg) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium, color = DF.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail(vm.appSort, app), style = MaterialTheme.typography.bodySmall, color = DF.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        SelectCheck(selected)
    }
}

private fun detail(sort: AppSort, app: AppItem): String = when (sort) {
    AppSort.SIZE, AppSort.NAME -> "${formatSize(app.size)} · ${app.category.title}"
    AppSort.INSTALLED -> "Installata ${relativeDate(app.installed)} · ${formatSize(app.size)}"
    AppSort.UPDATED -> "Aggiornata ${relativeDate(app.updated)} · ${formatSize(app.size)}"
    AppSort.LAST_USED -> app.lastUsed?.let { "Usata ${relativeDate(it)} · ${formatSize(app.size)}" } ?: "Ultimo utilizzo non disponibile"
}

@Composable
fun LoadingRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).shimmer())
        Spacer(Modifier.width(14.dp))
        Column {
            Box(Modifier.width(150.dp).height(14.dp).clip(CircleShape).shimmer())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(90.dp).height(10.dp).clip(CircleShape).shimmer())
        }
    }
}

@Composable
fun EmptyState(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 60.dp).reveal(0), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(DF.Primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = DF.Primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = DF.Text)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = DF.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun UsageAccessCard(context: Context, vm: MainViewModel, modifier: Modifier = Modifier) {
    CardSurface(modifier.fillMaxWidth().pressScale(0.97f) { runCatching { context.startActivity(vm.apps.usageAccessIntent()) } }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Insights, null, tint = DF.Primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Attiva l'accesso all'utilizzo", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                Text("Serve per sapere quando hai usato ogni app e quanto spazio occupa davvero. Resta tutto sul telefono.", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
            }
        }
    }
}

// ---------- Barra della selezione ----------

@Composable
fun SelectionBar(visible: Boolean, modifier: Modifier, label: String, onClear: () -> Unit, actions: @Composable () -> Unit) {
    AnimatedVisibility(
        visible, modifier,
        enter = slideInVertically(spring(0.75f, 380f)) { it } + fadeIn(),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(),
    ) {
        val shape = RoundedCornerShape(28.dp)
        Row(
            Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(shape).background(DF.SurfaceHigher).border(1.dp, DF.LineStrong, shape).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.weight(1f).pressScale(0.95f, onClick = onClear).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, tint = DF.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                AnimatedNumber(label, MaterialTheme.typography.labelLarge)
            }
            actions()
        }
    }
}

@Composable
fun BarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.pressScale(0.88f, onClick = onClick).clip(RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = DF.Text, modifier = Modifier.size(21.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = DF.Muted)
    }
}

// ---------- Fogli ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DfSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DF.SurfaceHigh,
        scrimColor = Color(0xB3000000),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp), content = content)
    }
}

@Composable
fun SheetTitle(text: String) = Text(text, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.headlineSmall, color = DF.Text)

@Composable
fun SheetOption(icon: ImageVector, text: String, selected: Boolean = false, tint: Color = DF.Text, index: Int = 0, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) DF.Primary.copy(alpha = 0.14f) else Color.Transparent, label = "opt")
    Row(
        Modifier.reveal(index, 30).fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).pressScale(0.97f, onClick = onClick)
            .clip(RoundedCornerShape(18.dp)).background(bg).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) DF.Primary else tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (selected) DF.Primary else tint)
        if (selected) Icon(Icons.Rounded.Check, null, tint = DF.Primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SortSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    DfSheet(onDismiss) {
        SheetTitle("Ordina per")
        AppSort.entries.forEachIndexed { i, s ->
            SheetOption(sortIcon(s), s.label, vm.appSort == s, index = i) { vm.setAppSort(s, vm.appDesc) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DfChip("Crescente", !vm.appDesc, icon = Icons.Rounded.ArrowUpward) { vm.setAppSort(vm.appSort, false) }
            DfChip("Decrescente", vm.appDesc, icon = Icons.Rounded.ArrowDownward) { vm.setAppSort(vm.appSort, true) }
        }
    }
}

private fun sortIcon(s: AppSort): ImageVector = when (s) {
    AppSort.NAME -> Icons.AutoMirrored.Rounded.Sort
    AppSort.SIZE -> Icons.Rounded.DeleteSweep
    AppSort.INSTALLED -> Icons.Rounded.ArrowDownward
    AppSort.UPDATED -> Icons.Rounded.ArrowUpward
    AppSort.LAST_USED -> Icons.Rounded.Bedtime
}

@Composable
private fun AppActionsSheet(vm: MainViewModel, context: Context, app: AppItem, onMove: () -> Unit, onDismiss: () -> Unit) {
    fun go(intent: Intent?) { onDismiss(); intent?.let { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }
    DfSheet(onDismiss) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncBitmap(app.pkg, vm.apps.cachedIcon(app.pkg), 56.dp, fallback = app.label) { vm.apps.icon(app.pkg) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(app.label, style = MaterialTheme.typography.titleLarge, color = DF.Text)
                Text("${app.version.ifBlank { "—" }} · ${formatSize(app.size)}", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
            }
        }
        Row(Modifier.padding(horizontal = 24.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(app.category.title, color = app.category.color())
            Pill("Installata ${relativeDate(app.installed)}", color = DF.Muted)
            app.lastUsed?.let { Pill("Usata ${relativeDate(it)}", color = DF.Muted) }
        }
        Spacer(Modifier.height(8.dp))
        vm.apps.launchIntent(app.pkg)?.let { SheetOption(Icons.Rounded.OpenInNew, "Apri", index = 0) { go(it) } }
        SheetOption(Icons.Rounded.Info, "Informazioni app", index = 1) { go(vm.apps.detailsIntent(app.pkg)) }
        SheetOption(Icons.AutoMirrored.Rounded.DriveFileMove, "Sposta in un'altra categoria", index = 2) { onDismiss(); onMove() }
        SheetOption(Icons.Rounded.VisibilityOff, "Nascondi da Declutty", index = 3) { onDismiss(); vm.hide(setOf(app.pkg)) }
        SheetOption(Icons.Rounded.DeleteSweep, "Disinstalla", tint = DF.Error, index = 4) { onDismiss(); vm.startUninstall(listOf(app)) }
    }
}

@Composable
private fun MoveSheet(current: Category?, onDismiss: () -> Unit, onPick: (Category) -> Unit) {
    DfSheet(onDismiss) {
        SheetTitle("Sposta in")
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Category.entries.forEachIndexed { i, c ->
                SheetOption(c.icon(), c.title, c == current, tint = c.color(), index = i) { onDismiss(); onPick(c) }
            }
        }
    }
}

/** Gradiente dell'icona di categoria, usato in più schermate. */
fun categoryBrush(c: Category) = Brush.linearGradient(listOf(c.color().copy(alpha = 0.32f), c.color().copy(alpha = 0.1f)))
