package app.declutty.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.declutty.PhotoCleaner
import app.declutty.ui.theme.DF
import app.declutty.PhotoSwiper
import app.declutty.data.DeleteMode
import app.declutty.data.Photo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Scorri e scegli: tutta la galleria, a destra tieni, a sinistra elimini. Le scartate aspettano la fine. */
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PhotoSwipeScreen(p: PhotoCleaner, onBack: () -> Unit) {
    val s = p.swiper
    LaunchedEffect(Unit) { s.load() }
    var confirm by remember { mutableStateOf(false) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        s.onDeleteResult(it.resultCode == Activity.RESULT_OK)
    }
    val queue = s.queue
    val deciding = queue != null && !s.summary && s.current != null

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", onClick = onBack)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Scorri e scegli", style = MaterialTheme.typography.headlineSmall, color = DF.Text)
                Text(
                    when {
                        queue == null -> "Leggo la galleria…"
                        s.pending.isEmpty() -> "${plural(s.left, "foto", "foto")} da vedere"
                        else -> "${plural(s.left, "foto", "foto")} da vedere · ${s.pending.size} in sospeso"
                    },
                    style = MaterialTheme.typography.bodySmall, color = DF.Muted,
                )
            }
            if (deciding && s.pending.isNotEmpty()) Pill("Fine", Modifier.clip(CircleShape).pressScale { s.summary = true })
        }
        when {
            queue == null -> Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp).clip(rounded(28.dp)).shimmer())
            deciding -> SwipeDeck(s)
            else -> SwipeSummary(s, onBack) { confirm = true }
        }
    }

    if (confirm) ConfirmDialog(
        "Eliminare ${plural(s.pending.size, "foto", "foto")}?",
        (if (p.settings.deleteMode == DeleteMode.TRASH) "Finiscono nel cestino e potrai recuperarle per 30 giorni."
        else "Vengono eliminate definitivamente e non si possono recuperare.") + " Libererai ${formatSize(s.pendingBytes)}.",
        "Elimina", danger = true, onDismiss = { confirm = false },
    ) { s.deleteRequest()?.let { deleteLauncher.launch(IntentSenderRequest.Builder(it).build()) } }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun ColumnScope.SwipeDeck(s: PhotoSwiper) {
    val photo = s.current ?: return
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var drag by remember(photo.id) { mutableFloatStateOf(0f) }
    var flying by remember(photo.id) { mutableStateOf(false) }
    var width by remember { mutableFloatStateOf(1f) }
    // Swipe e pulsanti volano via con la stessa animazione.
    val fling: (Boolean) -> Unit = { delete ->
        if (!flying) {
            flying = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                animate(drag, if (delete) -width * 1.4f else width * 1.4f, animationSpec = tween(220)) { v, _ -> drag = v }
                if (delete) s.discard() else s.keep()
            }
        }
    }

    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }) {
        val k = (abs(drag) / (width * 0.35f)).coerceIn(0f, 1f)
        // Stesso key per la foto sotto e quella sopra: salendo in cima non ricarica l'immagine.
        listOfNotNull(s.next, photo).forEach { ph ->
            key(ph.id) {
                val top = ph.id == photo.id
                // Una sola chiamata: la foto sotto, salendo in cima, tiene l'immagine già caricata.
                SwipeCard(
                    ph, if (top && drag < 0) k else 0f, if (top && drag > 0) k else 0f,
                    if (!top) Modifier.graphicsLayer { scaleX = 0.93f + 0.07f * k; scaleY = scaleX; alpha = 0.6f + 0.4f * k }
                    else Modifier
                        .graphicsLayer { translationX = drag; rotationZ = drag / width * 10f }
                        .pointerInput(ph.id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (abs(drag) > width * 0.3f) fling(drag < 0)
                                    else scope.launch { animate(drag, 0f, animationSpec = spring(0.6f, 400f)) { v, _ -> drag = v } }
                                },
                                onDragCancel = { scope.launch { animate(drag, 0f) { v, _ -> drag = v } } },
                            ) { change, amount -> if (!flying) { change.consume(); drag += amount } }
                        },
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SwipeButton(Icons.Rounded.Close, "Elimina", DF.Error, 68) { fling(true) }
        Box(Modifier.graphicsLayer { alpha = if (s.canUndo) 1f else 0.35f }) {
            RoundIconButton(Icons.AutoMirrored.Rounded.Undo, "Annulla") { s.undo() }
        }
        SwipeButton(Icons.Rounded.Check, "Tieni", DF.Primary, 68) { fling(false) }
    }
    Text(
        "Scorri a destra per tenerla, a sinistra per metterla tra quelle da eliminare.",
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
        style = MaterialTheme.typography.bodySmall, color = DF.Muted, textAlign = TextAlign.Center,
    )
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun SwipeCard(photo: Photo, deleteHint: Float, keepHint: Float, modifier: Modifier) {
    val shape = rounded(28.dp)
    Box(modifier.fillMaxSize().clip(shape).background(DF.Surface).border(1.dp, DF.Line, shape)) {
        PhotoImage(photo, 1600, Modifier.fillMaxSize(), ContentScale.Fit)
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(SimpleDateFormat("d MMMM yyyy · HH:mm", Locale.ITALIAN).format(Date(photo.dateTaken)), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                listOf(formatSize(photo.sizeBytes), photo.bucket).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f),
            )
        }
        Stamp("TIENI", DF.Primary, keepHint, Modifier.align(Alignment.TopStart).padding(22.dp).rotate(-14f))
        Stamp("ELIMINA", DF.Error, deleteHint, Modifier.align(Alignment.TopEnd).padding(22.dp).rotate(14f))
    }
}

@Composable
private fun Stamp(text: String, color: Color, alpha: Float, modifier: Modifier) {
    if (alpha <= 0f) return
    Text(
        text, modifier.graphicsLayer { this.alpha = alpha }.border(3.dp, color, rounded(10.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.headlineSmall, color = color,
    )
}

@Composable
private fun SwipeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, color: Color, size: Int, onClick: () -> Unit) {
    Box(
        Modifier.pressScale(0.86f, onClick = onClick).size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = color, modifier = Modifier.size((size * 0.45f).dp)) }
}

/** Fine dello scorrimento: elimina le sospese, rivedile una a una o toglile toccandole. */
@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun ColumnScope.SwipeSummary(s: PhotoSwiper, onBack: () -> Unit, onDelete: () -> Unit) {
    if (s.pending.isEmpty()) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            EmptyState(
                if (s.left > 0) "Niente in sospeso" else "Hai visto tutte le foto",
                if (s.left > 0) "Non hai scartato nessuna foto. Puoi continuare a scorrere." else "Le foto che hai tenuto non ti verranno riproposte. Puoi ricominciare da capo quando vuoi.",
            )
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (s.left > 0) MintButton("Continua a scorrere", Icons.Rounded.Swipe, Modifier.fillMaxWidth()) { s.summary = false }
            else MintButton("Ricomincia da capo", Icons.Rounded.Refresh, Modifier.fillMaxWidth(), filled = false) { s.restart() }
            MintButton("Torna alle foto", null, Modifier.fillMaxWidth(), filled = s.left == 0) { onBack() }
        }
        return
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("${plural(s.pending.size, "foto", "foto")} da eliminare", style = MaterialTheme.typography.displaySmall, color = DF.Text)
        Text(
            "Libereresti ${formatSize(s.pendingBytes)}. Tocca una foto per tenerla.",
            style = MaterialTheme.typography.bodyMedium, color = DF.Muted,
        )
    }
    LazyVerticalGrid(
        GridCells.Adaptive(104.dp), Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(s.pending, key = { it.id }) { photo ->
            Box(Modifier.animateItem().aspectRatio(1f).clip(rounded(18.dp)).pressScale(0.92f) { s.rescue(photo) }) {
                PhotoImage(photo, 320, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(DF.Error.copy(alpha = 0.22f)))
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(DF.Error),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Tieni", tint = DF.OnPrimary, modifier = Modifier.size(15.dp)) }
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MintButton("Rivedile", Icons.Rounded.Swipe, Modifier.weight(1f), filled = false) { s.review() }
            MintButton("Elimina", Icons.Rounded.DeleteSweep, Modifier.weight(1f), danger = true, onClick = onDelete)
        }
        if (s.left > 0) MintButton("Continua a scorrere (${s.left})", null, Modifier.fillMaxWidth(), filled = false) { s.summary = false }
    }
}
