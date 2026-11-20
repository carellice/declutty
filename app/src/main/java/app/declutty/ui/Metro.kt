package app.declutty.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.Metro
import kotlinx.coroutines.delay

// Primitive dello stile Modern UI (Windows Phone): angoli vivi, piastrelle piatte, tilt e turnstile.

/** Angoli arrotondati negli altri stili, vivi in Metro. */
fun rounded(size: Dp): Shape = if (Metro.on) RectangleShape else RoundedCornerShape(size)

fun rounded(topStart: Dp = 0.dp, topEnd: Dp = 0.dp): Shape =
    if (Metro.on) RectangleShape else RoundedCornerShape(topStart = topStart, topEnd = topEnd)

/** Le tinte accento di Windows Phone, una per categoria. */
val MetroPalette = listOf(
    0xFF1BA1E2, 0xFF60A917, 0xFFD80073, 0xFFFA6800, 0xFF6A00FF, 0xFFE51400, 0xFF00ABA9, 0xFFF0A30A, 0xFF0050EF,
    0xFFA4C400, 0xFFAA00FF, 0xFF825A2C, 0xFFA20025, 0xFF647687, 0xFF008A00, 0xFF76608A, 0xFF87794E,
).map { Color(it) }

/** Turnstile: la pagina ruota come un'anta incernierata a sinistra, prima esce la vecchia poi entra la nuova. */
@Composable
fun AnimatedVisibilityScope.turnstile(): Modifier {
    if (!Metro.on) return Modifier
    val r by transition.animateFloat(
        { if (targetState == EnterExitState.Visible) tween(320, delayMillis = 160, easing = FastOutSlowInEasing) else tween(200) },
        label = "turnstile",
    ) {
        when (it) {
            EnterExitState.PreEnter -> -75f
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> 75f
        }
    }
    return Modifier.graphicsLayer {
        rotationY = r
        transformOrigin = TransformOrigin(0f, 0.5f)
        cameraDistance = 14f * density
    }
}

/** Piastrella viva: si gira ogni tanto sull'asse orizzontale e mostra il numero sul retro. */
@Composable
fun LiveTile(icon: ImageVector, value: String, label: String, index: Int, modifier: Modifier, color: Color = DF.Primary, onClick: () -> Unit) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(1800L + index * 900L)
        while (true) {
            angle.animateTo(angle.value + 180f, tween(650, easing = FastOutSlowInEasing))
            delay(4200L + (index % 3) * 1100L)
        }
    }
    val back = (angle.value % 360f) in 90f..270f
    Box(
        modifier.aspectRatio(1f).pressScale(onClick = onClick)
            .graphicsLayer { rotationX = angle.value; cameraDistance = 16f * density }
            .background(color),
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { if (back) rotationX = 180f }.padding(10.dp)) {
            if (back) {
                AnimatedNumber(value, MaterialTheme.typography.displaySmall, Color.White, Modifier.align(Alignment.TopStart))
            } else {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(44.dp).align(Alignment.Center))
            }
            Text(label, Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Titolo piccolo in maiuscolo sopra la pagina, come il nome dell'app su Windows Phone. */
@Composable
fun MetroAppTitle(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelLarge, color = DF.Text)

