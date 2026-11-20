package app.declutty.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.declutty.R

/** Stile grafico scelto dall'utente: false = Dueffe (menta su nero), true = skeuomorfismo (pelle, ottone, gel). */
object Skeu {
    var on by mutableStateOf(false)
}

private fun c(dueffe: Long, skeu: Long) = if (Skeu.on) Color(skeu) else Color(dueffe)

/** Token colore. Dueffe: menta su fondo quasi nero. Skeu: pelle scura, crema e smeraldo lucido. */
object DF {
    val Bg get() = c(0xFF050706, 0xFF24180F)
    val BgElevated get() = c(0xFF0A0D0C, 0xFF1A110B)
    val Surface get() = c(0xFF0F1311, 0xFF5A3F2C)
    val SurfaceHigh get() = c(0xFF161C19, 0xFF684A34)
    val SurfaceHigher get() = c(0xFF1F2723, 0xFF2A1C13)
    val Text get() = c(0xFFF2FFF8, 0xFFF7EBD6)
    val Muted get() = c(0xFF9DB3A8, 0xFFC9B08E)
    val Line get() = c(0x2980FFD0, 0x40E8D2B0)
    val LineStrong get() = c(0x575CF2B8, 0x8CE8D2B0)
    val Primary get() = c(0xFF5CF2B8, 0xFF62D394)
    val PrimaryStrong get() = c(0xFF1FD3A0, 0xFF1F7D4A)
    val Secondary get() = c(0xFFC8FFE9, 0xFFE8C579)
    val OnPrimary get() = c(0xFF03140D, 0xFFFFFFFF)
    val Error get() = c(0xFFFF8F80, 0xFFF0806A)
    val Nav get() = c(0xFF080B0A, 0xFF120C08)
}

object Motion {
    val Reveal = CubicBezierEasing(.22f, .78f, .16f, 1f)
    val Letter = CubicBezierEasing(.16f, .86f, .24f, 1f)
    val Standard = CubicBezierEasing(.4f, 0f, .2f, 1f)
}

@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int) = Font(R.font.bricolage, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(R.font.inter, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val DisplayFont = FontFamily(bricolage(400), bricolage(500), bricolage(600), bricolage(700), bricolage(800))
val BodyFont = FontFamily(inter(400), inter(500), inter(600), inter(700))

private fun typography(display: FontFamily, body: FontFamily) = Typography(
    displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 48.sp, letterSpacing = (-0.035).em),
    displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = (-0.03).em),
    displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em),
)

/** Rilievo da tipografia a caratteri mobili: ombra scura sotto ogni lettera. */
private val letterpress = Shadow(Color(0xB0000000), Offset(0f, 2.5f), 2f)

private fun TextStyle.p() = copy(shadow = letterpress, letterSpacing = if (letterSpacing.value < 0f) 0.em else letterSpacing)

private fun Typography.pressed() = Typography(
    displayLarge.p(), displayMedium.p(), displaySmall.p(), headlineLarge.p(), headlineMedium.p(), headlineSmall.p(),
    titleLarge.p(), titleMedium.p(), titleSmall.p(), bodyLarge.p(), bodyMedium.p(), bodySmall.p(),
    labelLarge.p(), labelMedium.p(), labelSmall.p(),
)

private val dueffeType = typography(DisplayFont, BodyFont)
private val skeuType = typography(FontFamily.Serif, BodyFont).pressed()

private fun colors() = darkColorScheme(
    primary = DF.Primary, onPrimary = DF.OnPrimary,
    primaryContainer = DF.SurfaceHigher, onPrimaryContainer = DF.Secondary,
    secondary = DF.Secondary, onSecondary = DF.OnPrimary,
    background = DF.Bg, onBackground = DF.Text,
    surface = DF.Surface, onSurface = DF.Text,
    surfaceVariant = DF.SurfaceHigh, onSurfaceVariant = DF.Muted,
    surfaceContainer = DF.Surface, surfaceContainerLow = DF.BgElevated,
    surfaceContainerHigh = DF.SurfaceHigh, surfaceContainerHighest = DF.SurfaceHigher,
    outline = DF.LineStrong, outlineVariant = DF.Line, error = DF.Error,
)

@Composable
fun DeclutTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors(), typography = if (Skeu.on) skeuType else dueffeType, content = content)
