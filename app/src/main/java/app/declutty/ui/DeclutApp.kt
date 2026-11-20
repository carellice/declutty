package app.declutty.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.declutty.MainViewModel
import app.declutty.data.Category
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.Skeu
import app.declutty.ui.theme.Metro
import app.declutty.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Panoramica", Icons.Rounded.AutoAwesome),
    APPS("App", Icons.Rounded.Apps),
    CATEGORIES("Categorie", Icons.Rounded.Category),
    PHOTOS("Foto", Icons.Rounded.PhotoLibrary),
    CONTACTS("Contatti", Icons.Rounded.Contacts),
}

/** Schermate che si aprono sopra le schede. */
sealed interface Page {
    data class CategoryApps(val category: Category) : Page
    data object Settings : Page
    data object Hidden : Page
    data class PhotoGroup(val id: Long) : Page
    data class PhotoSettings(val fromSettings: Boolean) : Page
}

/** Profondità di una schermata: decide se la transizione va avanti o indietro. */
private fun Page?.depth() = when (this) {
    null -> 0
    Page.Hidden -> 2
    is Page.PhotoSettings -> if (fromSettings) 2 else 1
    else -> 1
}

@Composable
fun DeclutApp(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var page by remember { mutableStateOf<Page?>(null) }
    var unusedFilter by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) { vm.onResume(); onPauseOrDispose { } }

    BackHandler(page != null || tab != Tab.HOME) {
        page = when (val p = page) {
            Page.Hidden -> Page.Settings
            is Page.PhotoSettings -> if (p.fromSettings) Page.Settings else null
            null -> { tab = Tab.HOME; null }
            else -> null
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        AnimatedContent(
            page to tab,
            transitionSpec = {
                val (fromPage, fromTab) = initialState
                val (toPage, toTab) = targetState
                when {
                    // Metro: la rotazione turnstile la fa il contenuto, qui resta solo la dissolvenza a tempo.
                    Metro.on && toPage.depth() != fromPage.depth() ->
                        fadeIn(tween(200, delayMillis = 160)) togetherWith fadeOut(tween(200))
                    toPage.depth() > fromPage.depth() ->
                        (slideInHorizontally(tween(420, easing = Motion.Reveal)) { it / 3 } + fadeIn(tween(300))) togetherWith
                            (fadeOut(tween(200)) + scaleOut(tween(300), 0.94f))
                    toPage.depth() < fromPage.depth() ->
                        (fadeIn(tween(300)) + scaleIn(tween(360, easing = Motion.Reveal), 0.94f)) togetherWith
                            (slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(220)))
                    else -> {
                        val dir = if (toTab.ordinal > fromTab.ordinal) 1 else -1
                        (slideInHorizontally(tween(380, easing = Motion.Reveal)) { dir * it / 5 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(300)) { -dir * it / 5 } + fadeOut(tween(180)))
                    }
                }
            },
            label = "screens",
        ) { (p, t) ->
            Box(turnstile()) {
            when (p) {
                is Page.CategoryApps -> CategoryAppsScreen(vm, p.category) { page = null }
                Page.Settings -> SettingsScreen(vm, onHidden = { page = Page.Hidden }, onPhotos = { page = Page.PhotoSettings(true) }) { page = null }
                Page.Hidden -> HiddenScreen(vm) { page = Page.Settings }
                is Page.PhotoGroup -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) vm.photos?.let { PhotoGroupScreen(it, p.id) { page = null } }
                is Page.PhotoSettings -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) vm.photos?.let {
                    PhotoSettingsScreen(it) { page = if (p.fromSettings) Page.Settings else null }
                }
                null -> when (t) {
                    Tab.HOME -> HomeScreen(
                        vm,
                        openSettings = { page = Page.Settings },
                        openApps = { sortBySize -> unusedFilter = !sortBySize; tab = Tab.APPS },
                        openContacts = { tab = Tab.CONTACTS },
                        openPhotos = { tab = Tab.PHOTOS },
                        openCategory = { page = Page.CategoryApps(it) },
                    )
                    Tab.APPS -> AppsScreen(vm, unusedFilter) { unusedFilter = it }
                    Tab.CATEGORIES -> CategoriesScreen(vm) { page = Page.CategoryApps(it) }
                    Tab.PHOTOS -> {
                        val photos = vm.photos
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && photos != null) {
                            PhotosScreen(photos, openGroup = { page = Page.PhotoGroup(it) }, openSettings = { page = Page.PhotoSettings(false) })
                        } else PhotosUnsupported()
                    }
                    Tab.CONTACTS -> ContactsScreen(vm)
                }
            }
            }
        }

        AnimatedVisibility(
            page == null,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(380, easing = Motion.Reveal)) { it } + fadeIn(),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(),
        ) { BottomBar(tab) { tab = it } }

        Toast(vm.message, Modifier.align(Alignment.TopCenter)) { vm.message = null }
        UninstallOverlay(vm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) vm.photos?.let { PhotoDoneOverlay(it) { page = null; tab = Tab.PHOTOS } }
    }
}

// ---------- Barra in basso con indicatore che scivola ----------

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    if (Metro.on) return MetroAppBar(selected, onSelect)
    val shape = rounded(30.dp)
    BoxWithConstraints(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)
            .panel(shape, DF.Nav, DF.Line).padding(6.dp),
    ) {
        val itemWidth = maxWidth / Tab.entries.size
        val x by animateDpAsState(itemWidth * selected.ordinal, spring(dampingRatio = 0.72f, stiffness = 420f), label = "navX")
        val pill = rounded(24.dp)
        Box(
            Modifier.offset(x = x).width(itemWidth).height(58.dp).then(
                if (Skeu.on) Modifier.inset(pill, Color.Black.copy(alpha = 0.45f))
                else Modifier.clip(pill).background(Brush.verticalGradient(listOf(DF.Primary.copy(alpha = 0.2f), DF.Primary.copy(alpha = 0.08f))))
                    .border(1.dp, DF.Primary.copy(alpha = 0.35f), pill),
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Tab.entries.forEach { t ->
                val active = t == selected
                val lift by animateDpAsState(if (active) (-2).dp else 0.dp, spring(0.5f, 500f), label = "lift")
                Column(
                    Modifier.weight(1f).height(58.dp).pressScale(0.9f) { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, t.label, tint = if (active) DF.Primary else DF.Muted, modifier = Modifier.size(23.dp).offset(y = lift))
                    Spacer(Modifier.height(3.dp))
                    Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (active) DF.Text else DF.Muted, maxLines = 1)
                }
            }
        }
    }
}

/** Application bar di Windows Phone: fascia piena, icone dentro cerchi, etichette minuscole. */
@Composable
private fun MetroAppBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(DF.Nav).navigationBarsPadding().padding(top = 10.dp, bottom = 8.dp)) {
        Tab.entries.forEach { t ->
            val active = t == selected
            val fill by animateColorAsState(if (active) DF.Text else Color.Transparent, tween(160), label = "fill")
            Column(Modifier.weight(1f).pressScale { onSelect(t) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(fill).border(2.dp, DF.Text, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(t.icon, t.label, tint = if (active) DF.Bg else DF.Text, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(t.label.lowercase(), style = MaterialTheme.typography.labelSmall, color = if (active) DF.Text else DF.Muted, maxLines = 1)
            }
        }
    }
}

// ---------- Messaggi brevi ----------

@Composable
private fun Toast(text: String?, modifier: Modifier, onDone: () -> Unit) {
    LaunchedEffect(text) { if (text != null) { delay(2400); onDone() } }
    var last by remember { mutableStateOf("") }
    if (text != null) last = text
    if (Metro.on) {
        // Toast di Windows Phone: fascia accento a tutta larghezza che scende dall'alto.
        AnimatedVisibility(
            text != null, modifier,
            enter = slideInVertically(tween(260, easing = Motion.Reveal)) { -it },
            exit = slideOutVertically(tween(200)) { -it },
        ) {
            Text(
                last,
                Modifier.fillMaxWidth().background(DF.Primary).statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleSmall, color = DF.OnPrimary,
            )
        }
        return
    }
    AnimatedVisibility(
        text != null, modifier.padding(top = 52.dp),
        enter = slideInVertically(spring(0.6f, 380f)) { -it * 2 } + fadeIn(),
        exit = slideOutVertically(tween(240)) { -it * 2 } + fadeOut(),
    ) {
        Text(
            last,
            Modifier.panel(CircleShape, DF.SurfaceHigher, DF.LineStrong, stitched = false).padding(horizontal = 18.dp, vertical = 11.dp),
            style = MaterialTheme.typography.labelLarge, color = DF.Text,
        )
    }
}

// ---------- Disinstallazione in serie ----------

@Composable
private fun UninstallOverlay(vm: MainViewModel) {
    val run = vm.uninstall
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.onUninstallResult() }
    val current = run?.current
    LaunchedEffect(current?.pkg, run?.index) {
        if (current != null) {
            delay(350) // lascia vedere l'icona dell'app prima della finestra di sistema
            runCatching { launcher.launch(vm.apps.uninstallIntent(current.pkg)) }.onFailure { vm.onUninstallResult() }
        }
    }
    AnimatedVisibility(run != null, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
        val r = run ?: return@AnimatedVisibility
        Box(
            Modifier.fillMaxSize().background(Color(0xE6030504))
                .clickable(remember { MutableInteractionSource() }, null) { },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(r.finished, transitionSpec = { (scaleIn(spring(0.6f, 300f), 0.8f) + fadeIn()) togetherWith fadeOut(tween(150)) }, label = "run") { done ->
                if (!done) UninstallProgress(vm, r) else UninstallDone(r) { vm.closeUninstall() }
            }
        }
    }
}

@Composable
private fun UninstallProgress(vm: MainViewModel, run: app.declutty.UninstallRun) {
    val progress by androidx.compose.animation.core.animateFloatAsState(run.index.toFloat() / run.queue.size, tween(600, easing = Motion.Reveal), label = "p")
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(DF.SurfaceHigher, 0f, 360f, false, style = Stroke(10.dp.toPx()))
                drawArc(
                    Brush.sweepGradient(listOf(DF.PrimaryStrong, DF.Primary, DF.PrimaryStrong)), -90f, 360f * progress, false,
                    style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            val app = run.current
            AnimatedContent(app?.pkg, transitionSpec = { (scaleIn(spring(0.5f, 350f), 0.5f) + fadeIn()) togetherWith (scaleOut(tween(200), 0.6f) + fadeOut()) }, label = "icon") { pkg ->
                if (pkg != null) AsyncBitmap(pkg, vm.apps.cachedIcon(pkg), 84.dp) { vm.apps.icon(pkg) }
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("Disinstallo ${minOf(run.index + 1, run.queue.size)} di ${run.queue.size}", style = MaterialTheme.typography.headlineSmall, color = DF.Text)
        Text(run.current?.label.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = DF.Muted)
        Spacer(Modifier.height(8.dp))
        Text("Conferma nella finestra di Android", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
        Spacer(Modifier.height(28.dp))
        MintButton("Interrompi", null, filled = false) { vm.stopUninstall() }
    }
}

@Composable
private fun UninstallDone(run: app.declutty.UninstallRun, onClose: () -> Unit) {
    val freed = run.removed.sumOf { it.size }
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Burst(Modifier.size(180.dp)) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(DF.Primary, DF.PrimaryStrong))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = DF.OnPrimary, modifier = Modifier.size(46.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (run.removed.isEmpty()) "Nessuna app rimossa" else "Più leggero!",
            style = MaterialTheme.typography.displaySmall, color = DF.Text, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (run.removed.isEmpty()) "Hai annullato tutte le disinstallazioni."
            else "${plural(run.removed.size, "app rimossa", "app rimosse")} · ${formatSize(freed)} liberati",
            style = MaterialTheme.typography.bodyLarge, color = DF.Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        MintButton("Fatto", null, onClick = onClose)
    }
}

/** Scintille che esplodono dal centro una volta sola. */
@Composable
internal fun Burst(modifier: Modifier, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { p.animateTo(1f, tween(1100, easing = Motion.Reveal)) }
            launch { pop.animateTo(1f, spring(0.4f, 300f)) }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            for (i in 0 until 14) {
                val angle = i * 2 * PI / 14
                val dist = size.minDimension * (0.22f + 0.3f * p.value) * (if (i % 2 == 0) 1f else 0.78f)
                val pos = Offset(c.x + cos(angle).toFloat() * dist, c.y + sin(angle).toFloat() * dist)
                drawCircle((if (i % 3 == 0) DF.Secondary else DF.Primary).copy(alpha = 1f - p.value), (if (i % 2 == 0) 5f else 3.5f) * density, pos)
            }
        }
        Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) { content() }
    }
}
