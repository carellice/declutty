package app.declutty.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.declutty.data.Category
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.DisplayFont
import app.declutty.ui.theme.Motion
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ---------- Movimento ----------

/** Click con molla: l'elemento si comprime mentre è premuto e rimbalza al rilascio. */
fun Modifier.pressScale(pressedScale: Float = 0.95f, onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = if (pressed) 1f else 0.42f, stiffness = if (pressed) 900f else 380f),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(interactionSource = source, indication = null, onLongClick = onLongClick, onClick = onClick)
}

/** Entrata a cascata: dissolvenza, salita e leggero zoom con ritardo per indice. */
fun Modifier.reveal(index: Int, step: Long = 45): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(12) * step)
        p.animateTo(1f, tween(560, easing = Motion.Letter))
    }
    graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 36.dp.toPx()
        val s = 0.95f + 0.05f * p.value
        scaleX = s; scaleY = s
    }
}

/** Riflesso che scorre, segnaposto di caricamento. */
fun Modifier.shimmer(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "x")
    drawBehind {
        val w = size.width
        drawRect(Brush.linearGradient(listOf(DF.SurfaceHigh, DF.SurfaceHigher, DF.SurfaceHigh), start = Offset(w * x - w, 0f), end = Offset(w * x, size.height)))
    }
}

/** Fondale vivo: nebulose menta che derivano lentamente e pulviscolo che sale. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(36_000, easing = LinearEasing)), label = "a")
    Canvas(modifier.fillMaxSize()) {
        drawRect(DF.Bg)
        val w = size.width
        val h = size.height
        fun blob(c: Offset, r: Float, color: Color) = drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), c, r), r, c)
        blob(Offset(w * (0.15f + 0.15f * cos(a)), h * 0.05f + 60f * sin(a * 2)), w * 0.95f, Color(0x305CF2B8))
        blob(Offset(w * (0.95f + 0.08f * sin(a)), h * (0.42f + 0.05f * cos(a))), w * 0.7f, Color(0x1F3DD6F5))
        blob(Offset(w * (0.3f + 0.2f * sin(a + 1.3f)), h * (0.95f + 0.03f * sin(a * 3))), w * 1.05f, Color(0x1A1FD3A0))
        val phase = a / (2 * PI).toFloat()
        for (i in 0 until 28) {
            val sx = seed(i * 3 + 1); val sy = seed(i * 7 + 2); val sp = 0.35f + seed(i * 11 + 3)
            val y = ((sy - phase * sp) % 1f + 1f) % 1f
            val tw = 0.5f + 0.5f * sin(a * 6 * sp + sx * 20)
            drawCircle(DF.Secondary.copy(alpha = 0.06f + 0.22f * tw * (1f - y * 0.6f)), (0.6f + seed(i * 5 + 4) * 1.6f) * density, Offset(sx * w, y * h))
        }
    }
}

private fun seed(i: Int): Float = ((sin(i * 12.9898) * 43758.5453) % 1.0).let { abs(it) }.toFloat()

// ---------- Elementi di base ----------

@Composable
fun DfChip(text: String, selected: Boolean, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) DF.Primary else DF.Surface, tween(260), label = "chipBg")
    val fg by animateColorAsState(if (selected) DF.OnPrimary else DF.Text, tween(260), label = "chipFg")
    val border by animateColorAsState(if (selected) DF.Primary else DF.Line, tween(260), label = "chipBorder")
    Row(
        modifier.pressScale(0.92f, onClick = onClick).clip(CircleShape).background(bg).border(1.dp, border, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}

@Composable
fun MintButton(
    text: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val accent = if (danger) DF.Error else DF.Primary
    Row(
        modifier.pressScale(0.95f, onClick = onClick).clip(shape)
            .then(
                if (filled) Modifier.background(Brush.linearGradient(listOf(accent, if (danger) Color(0xFFFF6B5B) else DF.PrimaryStrong)))
                else Modifier.background(DF.Surface).border(1.dp, DF.LineStrong, shape),
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (filled) DF.OnPrimary else DF.Text
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = fg, maxLines = 1)
    }
}

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(DF.SurfaceHigh, DF.Surface))).border(1.dp, DF.Line, shape).padding(padding),
    ) { content() }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = DF.Primary) {
    Text(
        text,
        modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.32f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

/** Titolo grande della schermata con riga secondaria animata. */
@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displayMedium, color = DF.Text)
            AnimatedContent(subtitle, transitionSpec = { (fadeIn(tween(220)) + slideInVertically { it / 2 }) togetherWith fadeOut(tween(150)) }, label = "sub") {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = DF.Muted)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, tint: Color = DF.Text, onClick: () -> Unit) {
    Box(
        modifier.pressScale(0.88f, onClick = onClick).size(44.dp).clip(CircleShape).background(DF.Surface).border(1.dp, DF.Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(21.dp)) }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(shape).background(DF.Surface).border(1.dp, DF.Line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = DF.Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = DF.Muted)
            BasicTextField(
                value, onChange, singleLine = true, cursorBrush = SolidColor(DF.Primary),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = DF.Text), modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) Icon(Icons.Rounded.Close, "Cancella", tint = DF.Muted, modifier = Modifier.size(20.dp).pressScale(0.8f) { onChange("") })
    }
}

/** Cerchio di selezione: si riempie con un rimbalzo e disegna la spunta. */
@Composable
fun SelectCheck(selected: Boolean, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow), label = "check")
    Box(modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).border(1.6.dp, if (selected) DF.Primary else DF.LineStrong, CircleShape))
        Box(
            Modifier.fillMaxSize().graphicsLayer { scaleX = p; scaleY = p; alpha = p.coerceIn(0f, 1f) }.clip(CircleShape)
                .background(Brush.linearGradient(listOf(DF.Primary, DF.PrimaryStrong))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Check, null, tint = DF.OnPrimary, modifier = Modifier.size(17.dp).graphicsLayer { rotationZ = (1f - p) * -40f }) }
    }
}

/** Numero che scorre in verticale quando cambia. */
@Composable
fun AnimatedNumber(text: String, style: androidx.compose.ui.text.TextStyle, color: Color = DF.Text, modifier: Modifier = Modifier) {
    AnimatedContent(
        text, modifier,
        transitionSpec = { (slideInVertically(spring(0.7f, 400f)) { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
        label = "num",
    ) { Text(it, style = style, color = color, maxLines = 1) }
}

/** Icona di un'app caricata in modo asincrono, con segnaposto luccicante. */
@Composable
fun AsyncBitmap(key: String, cached: ImageBitmap?, size: Dp, corner: Dp = size * 0.28f, fallback: String = "", load: suspend () -> ImageBitmap?) {
    val bitmap by produceState(cached, key) { if (value == null) value = load() }
    val shape = RoundedCornerShape(corner)
    Box(Modifier.size(size).clip(shape), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) Image(b, null, Modifier.fillMaxSize())
        else if (fallback.isNotEmpty()) Monogram(fallback, size)
        else Box(Modifier.fillMaxSize().shimmer())
    }
}

@Composable
fun Monogram(name: String, size: Dp) {
    val hue = (abs(name.hashCode()) % 360).toFloat()
    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.hsv(hue, 0.45f, 0.55f), Color.hsv((hue + 40f) % 360f, 0.6f, 0.25f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#",
            fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.44f).sp, color = DF.Text,
        )
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, danger: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DF.SurfaceHigh,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = DF.Text) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = DF.Muted) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm, color = if (danger) DF.Error else DF.Primary, style = MaterialTheme.typography.labelLarge) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = DF.Muted, style = MaterialTheme.typography.labelLarge) } },
    )
}

// ---------- Categorie ----------

fun Category.icon(): ImageVector = when (this) {
    Category.COMUNICAZIONE -> Icons.Rounded.Forum
    Category.SOCIAL -> Icons.Rounded.Groups
    Category.FOTO -> Icons.Rounded.CameraAlt
    Category.VIDEO -> Icons.Rounded.Movie
    Category.MUSICA -> Icons.Rounded.Headphones
    Category.GIOCHI -> Icons.Rounded.SportsEsports
    Category.PRODUTTIVITA -> Icons.Rounded.Checklist
    Category.FINANZA -> Icons.Rounded.AccountBalance
    Category.SHOPPING -> Icons.Rounded.ShoppingBag
    Category.CIBO -> Icons.Rounded.Restaurant
    Category.VIAGGI -> Icons.Rounded.Map
    Category.SALUTE -> Icons.Rounded.FavoriteBorder
    Category.ISTRUZIONE -> Icons.Rounded.School
    Category.NOTIZIE -> Icons.Rounded.AutoStories
    Category.STRUMENTI -> Icons.Rounded.Build
    Category.ACCESSIBILITA -> Icons.Rounded.AccessibilityNew
    Category.ALTRO -> Icons.Rounded.Apps
}

/** Ogni categoria ha una tinta sua, sempre chiara e satura come l'accento. */
fun Category.color(): Color = Color.hsv((152f + ordinal * 360f / Category.entries.size * 7) % 360f, 0.55f, 0.95f)

// ---------- Formati ----------

private val italian = Locale.ITALIAN

fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "0 MB"
    bytes < 1024L * 1024 -> String.format(italian, "%.0f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(italian, "%.0f MB", bytes / 1024.0 / 1024.0)
    else -> String.format(italian, "%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

fun relativeDate(millis: Long): String {
    if (millis <= 0) return "mai"
    val days = Duration.between(Instant.ofEpochMilli(millis), Instant.now()).toDays()
    return when {
        days < 1 -> "oggi"
        days < 2 -> "ieri"
        days < 7 -> "$days giorni fa"
        days < 30 -> "${days / 7} sett. fa"
        days < 365 -> if (days / 30 == 1L) "1 mese fa" else "${days / 30} mesi fa"
        else -> if (days / 365 == 1L) "1 anno fa" else "${days / 365} anni fa"
    }
}

fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"
