package app.declutty.ui

import android.app.Activity
import android.content.Context
import android.graphics.ImageDecoder
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.declutty.PhotoCleaner
import app.declutty.data.DeleteMode
import app.declutty.data.Photo
import app.declutty.data.SimilarGroup
import app.declutty.data.TimeWindow
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.Metro
import androidx.compose.ui.graphics.SolidColor
import app.declutty.ui.theme.Motion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ---------- Immagini ----------

/** Miniature già decodificate, per non ricaricarle scorrendo. */
private val thumbs = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

@RequiresApi(Build.VERSION_CODES.R)
private fun decode(context: Context, photo: Photo, px: Int): ImageBitmap {
    val resolver = context.contentResolver
    // Le miniature di sistema bastano per le griglie; per lo schermo intero serve la foto vera, ridotta.
    if (px <= 512) return resolver.loadThumbnail(photo.uri, Size(px, px), null).asImageBitmap()
    return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, photo.uri)) { decoder, info, _ ->
        val long = maxOf(info.size.width, info.size.height)
        if (long > px) decoder.setTargetSize(info.size.width * px / long, info.size.height * px / long)
    }.asImageBitmap()
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PhotoImage(photo: Photo, px: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val key = "${photo.id}:${photo.dateModified}:$px"
    val bitmap by produceState(thumbs.get(key), key) {
        if (value == null) value = withContext(Dispatchers.IO) { runCatching { decode(context, photo, px) }.getOrNull() }
            ?.also { if (px <= 512) thumbs.put(key, it) }
    }
    Crossfade(bitmap, modifier, tween(220), label = "photo") { b ->
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = contentScale)
        else Box(Modifier.fillMaxSize().shimmer())
    }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.ITALIAN).format(Date(millis))

// ---------- Scheda Foto ----------

@Composable
fun PhotosUnsupported() {
    Column(Modifier.fillMaxSize().padding(bottom = TabBarSpace), verticalArrangement = Arrangement.Center) {
        EmptyState("Serve Android 11", "La ricerca delle foto simili usa il cestino di sistema, disponibile da Android 11 in poi.")
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PhotosScreen(p: PhotoCleaner, listState: LazyListState, openGroup: (Long) -> Unit, openSettings: () -> Unit, openSwipe: () -> Unit) {
    var access by remember { mutableStateOf(p.hasAccess()) }
    var denied by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) { access = p.hasAccess(); onPauseOrDispose { } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = p.hasAccess(); denied = !access
        if (access) p.refreshCount()
    }
    val context = LocalContext.current
    AnimatedContent(
        when { !access -> 0; p.progress != null -> 1; p.groups == null -> 2; else -> 3 },
        transitionSpec = { (fadeIn(tween(300)) + scaleIn(tween(360, easing = Motion.Reveal), 0.96f)) togetherWith fadeOut(tween(180)) },
        label = "photos",
    ) { state ->
        when (state) {
            0 -> PhotoPermission(denied) {
                if (denied) runCatching {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } else permission.launch(p.permissions())
            }
            1 -> ScanView(p)
            2 -> PhotosIntro(p, openSettings, openSwipe)
            else -> PhotoResults(p, listState, openGroup, openSettings, openSwipe)
        }
    }
}

@Composable
private fun PhotoPermission(denied: Boolean, onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = TabBarSpace),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.reveal(0).size(110.dp).clip(CircleShape).background(DF.Primary.copy(alpha = 0.12f)).border(1.dp, DF.LineStrong, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PhotoLibrary, null, tint = DF.Primary, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Via i doppioni dalla galleria", Modifier.reveal(1), style = MaterialTheme.typography.displaySmall, color = DF.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Trovo le foto identiche o quasi uguali, scelgo quella venuta meglio e ti aiuto a eliminare le altre. L'analisi avviene sul telefono: le foto non lasciano mai il dispositivo.",
            Modifier.reveal(2), style = MaterialTheme.typography.bodyLarge, color = DF.Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        MintButton(if (denied) "Apri impostazioni di sistema" else "Consenti accesso alle foto", Icons.Rounded.PhotoLibrary, Modifier.reveal(3), onClick = onGrant)
        if (denied) {
            Spacer(Modifier.height(10.dp))
            Text("Scegli \"Consenti tutto\" per analizzare l'intera galleria.", style = MaterialTheme.typography.bodySmall, color = DF.Muted, textAlign = TextAlign.Center)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun PhotosIntro(p: PhotoCleaner, openSettings: () -> Unit, openSwipe: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = TabBarSpace + 24.dp)) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item {
            ScreenHeader(
                "Foto", "Foto simili e copie identiche", Modifier.padding(top = 12.dp),
                trailing = { RoundIconButton(Icons.Rounded.Tune, "Impostazioni foto", tint = DF.Primary, onClick = openSettings) },
            )
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp).reveal(1)) {
                Text(
                    buildAnnotatedString {
                        append("Una galleria\n")
                        withStyle(SpanStyle(color = DF.Primary)) { append("senza doppioni") }
                        append(".")
                    },
                    style = MaterialTheme.typography.displaySmall, color = DF.Text,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Confronto ogni foto con quelle scattate nello stesso momento, tengo la più nitida e ti propongo di eliminare le altre.",
                    style = MaterialTheme.typography.bodyMedium, color = DF.Muted,
                )
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoStat(Icons.Rounded.PhotoLibrary, p.galleryCount?.toString() ?: "—", "Foto in galleria", 2, Modifier.weight(1f))
                PhotoStat(Icons.Rounded.Savings, formatSize(p.freedBytes), "Liberati finora", 3, Modifier.weight(1f))
            }
        }
        item {
            CardSurface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).reveal(4)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = DF.Primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (p.lastScanAt > 0) "Ultima scansione ${relativeDate(p.lastScanAt)}" else "Mai analizzata", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                        Text(
                            if (p.lastScanAt > 0) "Analizzo solo le foto nuove: ci vuole un attimo." else "La prima scansione richiede qualche minuto.",
                            style = MaterialTheme.typography.bodySmall, color = DF.Muted,
                        )
                    }
                }
            }
        }
        item {
            MintButton("Avvia scansione", Icons.Rounded.AutoAwesome, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).reveal(5)) { p.startScan() }
        }
        item {
            // Non serve la scansione: scorre tutta la galleria, foto per foto.
            MintButton("Scorri e scegli", Icons.Rounded.Swipe, Modifier.fillMaxWidth().padding(horizontal = 20.dp).reveal(5), filled = false, onClick = openSwipe)
        }
        item {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).reveal(6), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = DF.Muted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tutto avviene sul telefono. Nessuna foto viene caricata online.", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
            }
        }
    }
}

@Composable
private fun PhotoStat(icon: ImageVector, value: String, label: String, index: Int, modifier: Modifier) {
    CardSurface(modifier.reveal(index), padding = PaddingValues(16.dp)) {
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

/** Anello che si riempie con la foto in analisi al centro e un satellite che gira. */
@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun ScanView(p: PhotoCleaner) {
    val progress = p.progress ?: return
    val fraction = if (progress.total > 0) progress.done.toFloat() / progress.total else 0f
    val animated by animateFloatAsState(fraction, tween(500, easing = Motion.Reveal), label = "scan")
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "angle")
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = TabBarSpace),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(DF.SurfaceHigher, 0f, 360f, false, style = Stroke(10.dp.toPx()))
                drawArc(
                    if (Metro.on) SolidColor(DF.Primary) else Brush.sweepGradient(listOf(DF.PrimaryStrong, DF.Primary, DF.Secondary, DF.PrimaryStrong)), -90f, 360f * animated, false,
                    style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Canvas(Modifier.size(176.dp).rotate(angle)) { drawCircle(DF.Secondary.copy(alpha = 0.7f), 4.dp.toPx(), Offset(size.width / 2, 0f)) }
            Box(Modifier.size(150.dp).clip(CircleShape).background(DF.Surface), contentAlignment = Alignment.Center) {
                AnimatedContent(progress.current, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) }, label = "current") { photo ->
                    if (photo != null) PhotoImage(photo, 256, Modifier.fillMaxSize())
                }
                Box(Modifier.fillMaxSize().background(DF.Bg.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = DF.Text)
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        Text(if (progress.grouping) "Raggruppo le foto simili…" else "Analizzo le tue foto", style = MaterialTheme.typography.headlineSmall, color = DF.Text)
        Spacer(Modifier.height(6.dp))
        Text(if (progress.total > 0) "${progress.done} di ${progress.total} foto" else "Leggo la galleria…", style = MaterialTheme.typography.bodyLarge, color = DF.Muted)
        Spacer(Modifier.height(6.dp))
        Text(
            "La prima scansione richiede più tempo. Le successive analizzano solo le foto nuove.",
            style = MaterialTheme.typography.bodySmall, color = DF.Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        MintButton("Annulla", null, filled = false) { p.cancelScan() }
    }
}

// ---------- Risultati ----------

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun PhotoResults(p: PhotoCleaner, listState: LazyListState, openGroup: (Long) -> Unit, openSettings: () -> Unit, openSwipe: () -> Unit) {
    val groups = p.groups.orEmpty()
    val count = p.toDelete.size
    val bytes = p.toDeleteBytes
    var confirm by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        p.onDeleteResult(it.resultCode == Activity.RESULT_OK)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), listState, contentPadding = PaddingValues(bottom = TabBarSpace + 120.dp)) {
            item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    ScreenHeader(
                        "Foto",
                        if (groups.isEmpty()) "Nessun doppione trovato" else "${plural(groups.size, "gruppo", "gruppi")} · ${plural(count, "da eliminare", "da eliminare")}",
                        trailing = { RoundIconButton(Icons.Rounded.Tune, "Impostazioni foto", tint = DF.Primary, onClick = openSettings) },
                    )
                    Spacer(Modifier.height(14.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { DfChip("Rianalizza", false, icon = Icons.Rounded.Refresh) { p.startScan() } }
                        item { DfChip("Scorri e scegli", false, icon = Icons.Rounded.Swipe, onClick = openSwipe) }
                        if (groups.isNotEmpty()) item {
                            DfChip(if (count == 0) "Seleziona tutte" else "Deseleziona", false, icon = Icons.Rounded.DoneAll) { p.selectAll(count == 0) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (groups.isEmpty()) {
                item { EmptyState("Galleria già in ordine", "Non ho trovato foto simili. Puoi abbassare il grado di somiglianza dalle impostazioni.") }
            } else {
                item { FreeableCard(bytes, count, groups.size) }
                item {
                    Text(
                        "Tocca una foto per tenerla o eliminarla, tienila premuta per vederla in grande.",
                        Modifier.padding(horizontal = 20.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = DF.Muted,
                    )
                }
                itemsIndexed(groups, key = { _, g -> g.id }) { i, g ->
                    GroupCard(g, p.toDelete, i, Modifier.animateItem(), onToggle = p::toggle, onOpen = { openGroup(g.id) }) { viewing = g.id to it }
                }
            }
        }

        SelectionBar(
            visible = count > 0,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TabBarSpace),
            label = "$count · ${formatSize(bytes)}",
            onClear = { p.selectAll(false) },
        ) { MintButton("Elimina", Icons.Rounded.DeleteSweep, danger = true) { confirm = true } }
    }

    viewing?.let { (id, index) ->
        groups.firstOrNull { it.id == id }?.let { PhotoViewer(it, index, p.toDelete, p::toggle) { viewing = null } }
    }

    if (confirm) ConfirmDialog(
        "Eliminare ${plural(count, "foto", "foto")}?",
        (if (p.settings.deleteMode == DeleteMode.TRASH) "Finiscono nel cestino e potrai recuperarle per 30 giorni."
        else "Vengono eliminate definitivamente e non si possono recuperare.") +
            " In ogni gruppo resta almeno una foto. Libererai ${formatSize(bytes)}.",
        "Elimina", danger = true, onDismiss = { confirm = false },
    ) { p.deleteRequest()?.let { deleteLauncher.launch(IntentSenderRequest.Builder(it).build()) } }
}

@Composable
private fun FreeableCard(bytes: Long, count: Int, groups: Int) {
    CardSurface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).reveal(0)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Puoi liberare", style = MaterialTheme.typography.bodyMedium, color = DF.Muted)
                AnimatedNumber(formatSize(bytes), MaterialTheme.typography.displaySmall, DF.Primary)
                Text("${plural(count, "foto", "foto")} in ${plural(groups, "gruppo", "gruppi")}", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
            }
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(Brush.linearGradient(listOf(DF.Primary, DF.PrimaryStrong))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = DF.OnPrimary, modifier = Modifier.size(28.dp)) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun GroupCard(
    group: SimilarGroup, toDelete: Set<Long>, index: Int, modifier: Modifier,
    onToggle: (Long) -> Unit, onOpen: () -> Unit, onPreview: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val selected = group.members.count { it.photo.id in toDelete }
    // Tutte grandi uguali, quasi due per schermata con la successiva che spunta: si sceglie già scorrendo l'elenco, senza aprire il gruppo.
    val thumb = ((LocalConfiguration.current.screenWidthDp - 80) / 1.8f).dp
    CardSurface(modifier.reveal(index).fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), padding = PaddingValues(vertical = 14.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().pressScale(0.97f, onClick = onOpen).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${group.members.size} foto", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                        Spacer(Modifier.width(8.dp))
                        if (group.isExactDuplicate) Pill("Identiche")
                        else Pill("Simili ${(group.members.minOf { it.similarityToBest } * 100).toInt()}%", color = DF.Secondary)
                    }
                    Text(
                        formatDate(group.best.photo.dateTaken) + if (selected > 0) " · $selected da eliminare" else "",
                        style = MaterialTheme.typography.bodySmall, color = DF.Muted,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Dettagli", tint = DF.Muted, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(group.members, key = { _, m -> m.photo.id }) { i, m ->
                    SelectableThumb(
                        m.photo, m.photo.id == group.bestId, m.photo.id in toDelete, thumb,
                        onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onPreview(i) },
                    ) { onToggle(m.photo.id) }
                }
            }
        }
    }
}

/** Miniatura con bordo menta se è da tenere, rossa e spuntata se è da eliminare. */
@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun SelectableThumb(photo: Photo, isBest: Boolean, marked: Boolean, size: Dp, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val shape = rounded(18.dp)
    val border by animateColorAsState(if (isBest) DF.Primary else if (marked) DF.Error else DF.Line, tween(240), label = "border")
    val scale by animateFloatAsState(if (marked) 0.92f else 1f, spring(0.5f, 400f), label = "scale")
    Box(Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }.pressScale(0.92f, onLongClick, onClick).clip(shape).border(2.dp, border, shape)) {
        PhotoImage(photo, if (size > 100.dp) 512 else 320, Modifier.fillMaxSize())
        AnimatedVisibility(marked, enter = fadeIn(), exit = fadeOut()) { Box(Modifier.fillMaxSize().background(DF.Error.copy(alpha = 0.26f))) }
        when {
            isBest && size < 80.dp -> Box(
                Modifier.align(Alignment.BottomStart).padding(4.dp).size(20.dp).clip(CircleShape).background(DF.Primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.WorkspacePremium, null, tint = DF.OnPrimary, modifier = Modifier.size(13.dp)) }
            isBest -> PhotoBadge(Icons.Rounded.WorkspacePremium, "Migliore", DF.Primary, Modifier.align(Alignment.BottomStart).padding(6.dp))
            photo.isFavorite -> Icon(Icons.Rounded.Star, "Preferita", tint = DF.Primary, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(18.dp))
        }
        DeleteCheck(marked, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

@Composable
private fun DeleteCheck(checked: Boolean, modifier: Modifier) {
    val bg by animateColorAsState(if (checked) DF.Error else Color.Black.copy(alpha = 0.3f), tween(200), label = "check")
    Box(modifier.size(24.dp).clip(CircleShape).background(bg).border(2.dp, DF.Text, CircleShape), contentAlignment = Alignment.Center) {
        if (checked) Icon(Icons.Rounded.Check, null, tint = DF.OnPrimary, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun PhotoBadge(icon: ImageVector?, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.clip(if (Metro.on) androidx.compose.ui.graphics.RectangleShape else CircleShape).background(color).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = DF.OnPrimary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = DF.OnPrimary)
    }
}

// ---------- Dettaglio gruppo ----------

@OptIn(ExperimentalLayoutApi::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PhotoGroupScreen(p: PhotoCleaner, groupId: Long, onBack: () -> Unit) {
    val group = p.groups.orEmpty().firstOrNull { it.id == groupId }
    if (group == null) { LaunchedEffect(Unit) { onBack() }; return }
    val pager = rememberPagerState { group.members.size }
    val strip = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var viewer by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pager.currentPage) { strip.animateScrollToItem(pager.currentPage) }
    val current = group.members[pager.currentPage.coerceIn(0, group.members.lastIndex)]
    val isBest = current.photo.id == group.bestId
    val marked = current.photo.id in p.toDelete

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", onClick = onBack)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                AnimatedNumber("${pager.currentPage + 1} di ${group.members.size}", MaterialTheme.typography.headlineSmall)
                Text(formatDate(current.photo.dateTaken), style = MaterialTheme.typography.bodySmall, color = DF.Muted)
            }
            if (group.isExactDuplicate) Pill("Identiche", color = DF.Primary)
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 12.dp) { page ->
            val m = group.members[page]
            Box(Modifier.fillMaxSize().clip(rounded(28.dp)).background(DF.Surface).border(1.dp, DF.Line, rounded(28.dp)).clickable { viewer = page }) {
                PhotoImage(m.photo, 1600, Modifier.fillMaxSize(), ContentScale.Fit)
                Row(Modifier.align(Alignment.TopStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (m.photo.id == group.bestId) PhotoBadge(Icons.Rounded.WorkspacePremium, "Da tenere", DF.Primary)
                    if (m.photo.id in p.toDelete) PhotoBadge(Icons.Rounded.Delete, "Da eliminare", DF.Error)
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Qualità ${current.score}/100", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = DF.Text)
                if (!isBest) Text("Somiglianza ${(current.similarityToBest * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = DF.Muted)
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                current.reasons.forEach { Pill(it) }
                Pill("${current.photo.width}×${current.photo.height}", color = DF.Muted)
                Pill(String.format(Locale.ITALY, "%.1f MP", current.photo.megapixels), color = DF.Muted)
                Pill(formatSize(current.photo.sizeBytes), color = DF.Muted)
                if (current.photo.bucket.isNotBlank()) Pill(current.photo.bucket, color = DF.Muted)
            }
        }
        LazyRow(state = strip, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(group.members, key = { _, m -> m.photo.id }) { i, m ->
                SelectableThumb(
                    m.photo, m.photo.id == group.bestId, m.photo.id in p.toDelete, 64.dp,
                    onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); viewer = i },
                ) { scope.launch { pager.animateScrollToPage(i) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MintButton(
                if (marked) "Tieni" else "Elimina", if (marked) Icons.Rounded.RestoreFromTrash else Icons.Rounded.Delete,
                Modifier.weight(1f), filled = false,
            ) { p.toggle(current.photo.id) }
            MintButton("Tieni solo questa", Icons.Rounded.WorkspacePremium, Modifier.weight(1.4f)) { p.keepOnly(group.id, current.photo.id) }
        }
    }

    viewer?.let { start -> PhotoViewer(group, start, p.toDelete, p::toggle) { page ->
        viewer = null
        scope.launch { pager.scrollToPage(page) }
    } }
}

// ---------- Schermo intero ----------

/** Foto del gruppo a tutto schermo: scorri, pizzica o tocca due volte per lo zoom, segna da tenere o eliminare. */
@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun PhotoViewer(group: SimilarGroup, start: Int, toDelete: Set<Long>, onToggle: (Long) -> Unit, onDismiss: (Int) -> Unit) {
    val pager = rememberPagerState(start.coerceIn(0, group.members.lastIndex)) { group.members.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    Dialog({ onDismiss(pager.currentPage) }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize(), pageSpacing = 16.dp, userScrollEnabled = !zoomed) { page ->
                ZoomablePhoto(group.members[page].photo, pager.settledPage == page, { if (pager.settledPage == page) zoomed = it }) { chrome = !chrome }
            }
            val current = group.members[pager.currentPage.coerceIn(0, group.members.lastIndex)]
            val marked = current.photo.id in toDelete
            AnimatedVisibility(chrome, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
                Row(
                    Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                        .statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundIconButton(Icons.Rounded.Close, "Chiudi") { onDismiss(pager.currentPage) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${pager.currentPage + 1} di ${group.members.size}", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                        Text(formatDate(current.photo.dateTaken), style = MaterialTheme.typography.bodySmall, color = DF.Muted)
                    }
                    if (current.photo.id == group.bestId) PhotoBadge(Icons.Rounded.WorkspacePremium, "Migliore", DF.Primary)
                    if (marked) PhotoBadge(Icons.Rounded.Delete, "Da eliminare", DF.Error, Modifier.padding(start = 6.dp))
                }
            }
            AnimatedVisibility(chrome, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
                Column(
                    Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                        .navigationBarsPadding().padding(20.dp),
                ) {
                    Text(
                        "Qualità ${current.score}/100 · ${current.photo.width}×${current.photo.height} · ${formatSize(current.photo.sizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium, color = DF.Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    MintButton(
                        if (marked) "Tieni" else "Elimina", if (marked) Icons.Rounded.RestoreFromTrash else Icons.Rounded.Delete,
                        Modifier.fillMaxWidth(), danger = !marked,
                    ) { onToggle(current.photo.id) }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun ZoomablePhoto(photo: Photo, isCurrent: Boolean, onZoom: (Boolean) -> Unit, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // Torna a 1x quando si scorre via da questa foto.
    LaunchedEffect(isCurrent) { if (!isCurrent) { scale = 1f; offset = Offset.Zero } }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                fun clamp(o: Offset, s: Float) = Offset(
                    o.x.coerceIn(-size.width * (s - 1f) / 2f, size.width * (s - 1f) / 2f),
                    o.y.coerceIn(-size.height * (s - 1f) / 2f, size.height * (s - 1f) / 2f),
                )
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        // Solo con due dita o già ingrandita: a 1x lo scorrimento con un dito resta al pager.
                        if (event.changes.size > 1 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 6f)
                            offset = clamp(offset + event.calculatePan(), scale)
                            onZoom(scale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale < 1.05f) { scale = 1f; offset = Offset.Zero; onZoom(false) }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onDoubleTap = { tap ->
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero; onZoom(false) } else {
                        scale = 2.5f
                        val target = (Offset(size.width / 2f, size.height / 2f) - tap) * (scale - 1f)
                        val maxX = size.width * (scale - 1f) / 2f
                        val maxY = size.height * (scale - 1f) / 2f
                        offset = Offset(target.x.coerceIn(-maxX, maxX), target.y.coerceIn(-maxY, maxY))
                        onZoom(true)
                    }
                })
            },
    ) {
        PhotoImage(
            photo, 3072,
            Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
            ContentScale.Fit,
        )
    }
}

// ---------- Fine eliminazione ----------

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PhotoDoneOverlay(p: PhotoCleaner, onReview: () -> Unit) {
    val done = p.lastDelete
    var last by remember { mutableStateOf(done) }
    if (done != null) last = done
    AnimatedVisibility(done != null, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
        val d = last ?: return@AnimatedVisibility
        val remaining = p.groups.orEmpty().size
        Box(
            Modifier.fillMaxSize().background(DF.Bg.copy(alpha = 0.9f)).clickable(remember { MutableInteractionSource() }, null) { },
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Burst(Modifier.size(180.dp)) {
                    Box(Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(DF.Primary, DF.PrimaryStrong))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PhotoLibrary, null, tint = DF.OnPrimary, modifier = Modifier.size(46.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Galleria più leggera!", style = MaterialTheme.typography.displaySmall, color = DF.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${plural(d.count, "foto eliminata", "foto eliminate")} · ${formatSize(d.bytes)} liberati",
                    style = MaterialTheme.typography.bodyLarge, color = DF.Muted, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                if (remaining > 0) {
                    MintButton("Rivedi ${plural(remaining, "altro gruppo", "altri gruppi")}", null) { p.lastDelete = null; onReview() }
                    Spacer(Modifier.height(10.dp))
                }
                MintButton("Fatto", null, filled = remaining == 0) { p.lastDelete = null }
            }
        }
    }
}

// ---------- Impostazioni foto ----------

private fun thresholdLabel(t: Float): Pair<String, String> = when {
    t >= 0.95f -> "Solo quasi identiche" to "Raggruppa solo foto praticamente uguali, come raffiche o copie."
    t >= 0.89f -> "Molto simili" to "Stessa scena e inquadratura, piccole differenze di posa o movimento."
    t >= 0.83f -> "Simili" to "Stessa scena con leggere variazioni di inquadratura o zoom."
    else -> "Più permissivo" to "Raggruppa anche foto della stessa scena con differenze evidenti. Controlla bene prima di eliminare."
}

@OptIn(ExperimentalLayoutApi::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PhotoSettingsScreen(p: PhotoCleaner, onBack: () -> Unit) {
    val s = p.settings
    // Il cursore resta locale mentre lo trascini: si riraggruppa solo al rilascio.
    var slider by remember(s.threshold) { mutableFloatStateOf(s.threshold) }
    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item {
            Column(Modifier.padding(top = 12.dp)) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", Modifier.padding(start = 20.dp, bottom = 12.dp), onClick = onBack)
                ScreenHeader("Foto simili", "Come cerco i doppioni")
                Spacer(Modifier.height(16.dp))
            }
        }
        item {
            SettingsCard("Somiglianza", 0) {
                val (title, body) = thresholdLabel(slider)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = DF.Text)
                    AnimatedNumber("${(slider * 100).roundToInt()}%", MaterialTheme.typography.headlineSmall, DF.Primary)
                }
                Text(body, style = MaterialTheme.typography.bodySmall, color = DF.Muted)
                Slider(
                    slider, { slider = (it * 100).roundToInt() / 100f },
                    Modifier.padding(top = 6.dp), valueRange = 0.75f..0.98f,
                    onValueChangeFinished = { p.updateSettings { it.copy(threshold = slider) } },
                    colors = SliderDefaults.colors(thumbColor = DF.Primary, activeTrackColor = DF.Primary, inactiveTrackColor = DF.SurfaceHigher),
                )
                Row {
                    Text("Più foto", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = DF.Muted)
                    Text("Più precisione", style = MaterialTheme.typography.labelSmall, color = DF.Muted)
                }
            }
        }
        item {
            SettingsCard("Periodo", 1) {
                Text("Confronta le foto scattate entro", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeWindow.entries.forEach { w -> DfChip(w.label, s.timeWindow == w) { p.updateSettings { it.copy(timeWindow = w) } } }
                }
                Spacer(Modifier.height(8.dp))
                Text("Un intervallo breve è più veloce e preciso. \"Sempre\" confronta tutto il rullino.", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
            }
        }
        item {
            SettingsCard("Analisi", 2) {
                SwitchRow("Copie identiche ovunque", "Trova i doppioni esatti anche se salvati in date diverse, per esempio da WhatsApp.", s.exactEverywhere) { v ->
                    p.updateSettings { it.copy(exactEverywhere = v) }
                }
                SwitchRow("Includi screenshot", "Analizza anche le catture dello schermo.", s.includeScreenshots) { v -> p.updateSettings { it.copy(includeScreenshots = v) } }
                SwitchRow("Proteggi le preferite", "Le foto preferite non vengono mai selezionate per l'eliminazione.", s.protectFavorites) { v ->
                    p.updateSettings { it.copy(protectFavorites = v) }
                }
            }
        }
        item {
            SettingsCard("Eliminazione", 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DfChip("Cestino", s.deleteMode == DeleteMode.TRASH, icon = Icons.Rounded.RestoreFromTrash) { p.updateSettings { it.copy(deleteMode = DeleteMode.TRASH) } }
                    DfChip("Definitiva", s.deleteMode == DeleteMode.PERMANENT, icon = Icons.Rounded.Delete) { p.updateSettings { it.copy(deleteMode = DeleteMode.PERMANENT) } }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (s.deleteMode == DeleteMode.TRASH) "Consigliato: le foto restano recuperabili nel cestino per 30 giorni."
                    else "Le foto vengono eliminate subito e non si possono recuperare.",
                    style = MaterialTheme.typography.bodySmall, color = if (s.deleteMode == DeleteMode.TRASH) DF.Muted else DF.Error,
                )
            }
        }
        item {
            SettingsCard("Dati", 4) {
                Text("Memoria dell'analisi", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                Text("Declutty ricorda le foto già analizzate per rendere più rapide le scansioni successive.", style = MaterialTheme.typography.bodySmall, color = DF.Muted)
                Spacer(Modifier.height(12.dp))
                MintButton("Svuota memoria", null, filled = false) { p.clearCache() }
            }
        }
    }
}

@Composable
private fun SettingsCard(label: String, index: Int, content: @Composable () -> Unit) {
    Column(Modifier.reveal(index).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(label.uppercase(), Modifier.padding(start = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, color = DF.Primary)
        CardSurface(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

@Composable
private fun SwitchRow(title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).pressScale(0.98f) { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = DF.Text)
            Text(text, style = MaterialTheme.typography.bodySmall, color = DF.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DF.OnPrimary, checkedTrackColor = DF.Primary, checkedBorderColor = DF.Primary,
                uncheckedThumbColor = DF.Muted, uncheckedTrackColor = DF.SurfaceHigher, uncheckedBorderColor = DF.LineStrong,
            ),
        )
    }
}
