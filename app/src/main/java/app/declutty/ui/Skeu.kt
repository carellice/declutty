package app.declutty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.declutty.R
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.Skeu
import androidx.compose.ui.graphics.ImageBitmap

// Primitive dello stile skeuomorfico: pelle cucita, gel lucido, pozzetti incassati, ottone.

/** Pelle vera (texture generata), ripetuta a specchio così le giunture non si vedono. */
fun Modifier.leatherTexture(tint: Color, tintAlpha: Float = 0.45f): Modifier = composed {
    val img = ImageBitmap.imageResource(R.drawable.skeu_leather)
    val brush = remember(img) { ShaderBrush(ImageShader(img, TileMode.Mirror, TileMode.Mirror)) }
    drawBehind {
        drawRect(brush)
        drawRect(tint.copy(alpha = tintAlpha))
    }
}

private fun DrawScope.dropShadow(shape: Shape, depth: Dp) {
    val o = shape.createOutline(size, layoutDirection, this)
    translate(top = depth.toPx()) { drawOutline(o, Color.Black.copy(alpha = 0.35f)) }
    translate(top = depth.toPx() * 0.45f) { drawOutline(o, Color.Black.copy(alpha = 0.4f)) }
}

/** Cucitura tratteggiata a [inset] dal bordo. */
private fun DrawScope.stitch(shape: Shape, inset: Dp, color: Color) {
    val i = inset.toPx()
    val o = shape.createOutline(Size(size.width - 2 * i, size.height - 2 * i), layoutDirection, this)
    val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.5.dp.toPx()))
    translate(i, i + 1f) { drawOutline(o, Color.Black.copy(alpha = 0.55f), style = Stroke(1.4.dp.toPx(), pathEffect = dash)) }
    translate(i, i) { drawOutline(o, color, style = Stroke(1.4.dp.toPx(), pathEffect = dash)) }
}

/** Bordo smussato: luce sul lato alto, ombra su quello basso, filo scuro intorno. */
private fun DrawScope.bevel(shape: Shape) {
    val o = shape.createOutline(size, layoutDirection, this)
    drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.16f), 0.06f to Color.Transparent, 0.9f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f)))
    drawOutline(o, Color.Black.copy(alpha = 0.7f), style = Stroke(1.dp.toPx()))
}

/** Pannello in pelle cucita, rialzato con ombra portata. */
fun Modifier.leatherPanel(shape: Shape, tint: Color = DF.Surface, stitched: Boolean = true, depth: Dp = 3.dp): Modifier =
    drawBehind { dropShadow(shape, depth) }
        .clip(shape)
        .leatherTexture(tint)
        .drawWithContent {
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.18f))))
            bevel(shape)
            if (stitched) stitch(shape, 5.dp, Color(0xA6E8D2B0))
            drawContent()
        }

/** Bottone in gel lucido: gradiente, riflesso a mezzaluna, bordo scuro. */
fun Modifier.gel(shape: Shape, color: Color, depth: Dp = 2.5.dp): Modifier =
    drawBehind { dropShadow(shape, depth) }
        .clip(shape)
        .drawWithContent {
            drawRect(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.18f), color, lerp(color, Color.Black, 0.35f))))
            drawRect(
                Brush.verticalGradient(0f to Color.White.copy(alpha = 0.45f), 0.48f to Color.White.copy(alpha = 0.12f), 0.5f to Color.Transparent),
                size = Size(size.width, size.height * 0.5f),
            )
            drawOutline(shape.createOutline(size, layoutDirection, this), Color.Black.copy(alpha = 0.65f), style = Stroke(1.dp.toPx()))
            drawContent()
        }

/** Pozzetto incassato nella pelle: fondo scuro, ombra interna in alto, luce sul bordo basso. */
fun Modifier.inset(shape: Shape, color: Color = DF.BgElevated): Modifier =
    drawBehind {
        val o = shape.createOutline(size, layoutDirection, this)
        translate(top = 1.dp.toPx()) { drawOutline(o, Color.White.copy(alpha = 0.12f)) }
    }
        .clip(shape)
        .background(color)
        .drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.6f), 0.18f to Color.Transparent), size = Size(size.width, minOf(size.height, 18.dp.toPx())))
            drawOutline(shape.createOutline(size, layoutDirection, this), Color.Black.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
        }

/** Manopola in ottone spazzolato. */
fun Modifier.brass(shape: Shape): Modifier =
    drawBehind { dropShadow(shape, 2.5.dp) }
        .clip(shape)
        .drawWithContent {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFF6DFA2), Color(0xFFC79A4A), Color(0xFF8A6427))))
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), Offset(size.width * 0.35f, size.height * 0.2f), size.minDimension * 0.6f))
            drawOutline(shape.createOutline(size, layoutDirection, this), Color(0xFF3A2610), style = Stroke(1.dp.toPx()))
            drawContent()
        }

/** Superficie generica: pannello in pelle nello stile skeu, fondo+bordo piatti altrimenti. */
fun Modifier.panel(shape: Shape, bg: Color, border: Color, stitched: Boolean = true): Modifier =
    if (Skeu.on) leatherPanel(shape, bg, stitched) else clip(shape).background(bg).border(1.dp, border, shape)
