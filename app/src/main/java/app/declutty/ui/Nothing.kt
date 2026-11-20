package app.declutty.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.Nd
import app.declutty.ui.theme.NdColor
import kotlin.math.roundToInt

// Primitive dello stile Nothing: nero OLED, griglia a punti, barre a segmenti, etichette in Space Mono maiuscolo.

/** Etichette e azioni: maiuscole in Nothing, invariate negli altri stili. */
fun String.caps(): String = if (Nd.on) uppercase() else this

/** Fondale: nero pieno con una griglia di punti appena visibile, come la matrice del Glyph. */
@Composable
fun DotGrid(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(DF.Bg)
        val step = 16.dp.toPx()
        val r = 1.dp.toPx()
        val dot = Color(0xFF333333).copy(alpha = 0.55f)
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) { drawCircle(dot, r, Offset(x, y)); x += step }
            y += step
        }
    }
}

/**
 * Barra a segmenti, la visualizzazione firma di Nothing: rettangoli a spigolo vivo separati da 2dp.
 * [fraction] oltre 1 resta piena: il superamento lo segnala il colore.
 */
@Composable
fun SegmentedBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 10.dp, segments: Int = 24, color: Color = NdColor.Display) {
    val filled = (fraction.coerceIn(0f, 1f) * segments).roundToInt().let { if (fraction > 0f && it == 0) 1 else it }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (segments - 1)) / segments
        for (i in 0 until segments) {
            drawRect(if (i < filled) color else DF.SurfaceHigher, Offset(i * (w + gap), 0f), Size(w, size.height))
        }
    }
}

/** Numero grande con l'unità piccola accanto, alzata: "72" + "%". */
@Composable
fun NdReadout(value: String, unit: String, modifier: Modifier = Modifier, color: Color = NdColor.Display) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.displayLarge, color = color, maxLines = 1)
        if (unit.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(unit.uppercase(), Modifier.padding(bottom = 40.dp), style = MaterialTheme.typography.labelMedium, color = DF.Muted)
        }
    }
}
