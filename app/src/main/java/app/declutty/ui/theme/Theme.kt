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

/** Stili grafici selezionabili dalle impostazioni. */
enum class UiStyle(val label: String, val hint: String) {
    MODERN("Moderno", "Menta su fondo scuro"),
    SKEU("Skeuomorfico", "Pelle, ottone e bottoni in gel"),
    METRO("Modern UI", "Piastrelle e tipografia, come Windows Phone"),
    NOTHING("Nothing", "Nero OLED, matrice a punti e un solo rosso"),
    DUEFFE("Dueffe", "Avorio e oro su fondo quasi nero, aurora ambra"),
}

/** Stile in uso, letto all'avvio e cambiato dalle impostazioni. */
object Ui {
    var style by mutableStateOf(UiStyle.MODERN)
}

object Skeu {
    val on get() = Ui.style == UiStyle.SKEU
}

object Metro {
    val on get() = Ui.style == UiStyle.METRO
}

/** Stile Dueffe (design system DS.md di dueffe-home): stesse forme e movimenti di Moderno, palette oro. */
object Dueffe {
    val on get() = Ui.style == UiStyle.DUEFFE
}

/** Stile Nothing (design system di dominikmartn/nothing-design-skill). `Nothing` è un tipo Kotlin, da qui Nd. */
object Nd {
    val on get() = Ui.style == UiStyle.NOTHING
}

private fun c(dueffe: Long, skeu: Long, metro: Long, nd: Long, gold: Long) = Color(
    when (Ui.style) {
        UiStyle.MODERN -> dueffe
        UiStyle.DUEFFE -> gold
        UiStyle.SKEU -> skeu
        UiStyle.METRO -> metro
        UiStyle.NOTHING -> nd
    },
)

/** Token colore. Dueffe: menta su fondo quasi nero. Skeu: pelle scura, crema e smeraldo lucido. Metro: nero puro e accento teal piatto. Dueffe: avorio e oro su quasi nero. Nothing: monocromo OLED, rosso #D71921 solo per gli allarmi. */
object DF {
    val Bg get() = c(0xFF050706, 0xFF24180F, 0xFF000000, 0xFF000000, 0xFF050505)
    val BgElevated get() = c(0xFF0A0D0C, 0xFF1A110B, 0xFF0A0A0A, 0xFF000000, 0xFF0B0A08)
    val Surface get() = c(0xFF0F1311, 0xFF5A3F2C, 0xFF1F1F1F, 0xFF111111, 0xFF11100D)
    val SurfaceHigh get() = c(0xFF161C19, 0xFF684A34, 0xFF262626, 0xFF1A1A1A, 0xFF1A1711)
    val SurfaceHigher get() = c(0xFF1F2723, 0xFF2A1C13, 0xFF333333, 0xFF222222, 0xFF242016)
    val Text get() = c(0xFFF2FFF8, 0xFFF7EBD6, 0xFFFFFFFF, 0xFFE8E8E8, 0xFFFFFAF0)
    val Muted get() = c(0xFF9DB3A8, 0xFFC9B08E, 0xFF9A9A9A, 0xFF999999, 0xFFB8AA8D)
    val Line get() = c(0x2980FFD0, 0x40E8D2B0, 0x33FFFFFF, 0xFF222222, 0x29FFDC80)
    val LineStrong get() = c(0x575CF2B8, 0x8CE8D2B0, 0xFFFFFFFF, 0xFF333333, 0x57FFD84D)
    val Primary get() = c(0xFF5CF2B8, 0xFF62D394, 0xFF00ABA9, 0xFFFFFFFF, 0xFFFFD84D)
    val PrimaryStrong get() = c(0xFF1FD3A0, 0xFF1F7D4A, 0xFF00ABA9, 0xFFFFFFFF, 0xFFFFB800)
    val Secondary get() = c(0xFFC8FFE9, 0xFFE8C579, 0xFF7FD5D4, 0xFF999999, 0xFFFFF0AD)
    val OnPrimary get() = c(0xFF03140D, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF161006)
    val Error get() = c(0xFFFF8F80, 0xFFF0806A, 0xFFE51400, 0xFFD71921, 0xFFFF9B85)
    val Nav get() = c(0xFF080B0A, 0xFF120C08, 0xFF1F1F1F, 0xFF000000, 0xFF080705)
}

/** Nothing: bianco pieno per il dato principale, rosso segnale per ciò che è urgente. */
object NdColor {
    val Display = Color(0xFFFFFFFF)
    val Disabled = Color(0xFF666666)
    val Accent = Color(0xFFD71921)
    val Success = Color(0xFF4A9E5C)
    val Warning = Color(0xFFD4A843)
}

object Motion {
    val Reveal = CubicBezierEasing(.22f, .78f, .16f, 1f)
    val Letter = CubicBezierEasing(.16f, .86f, .24f, 1f)
    val Standard = CubicBezierEasing(.4f, 0f, .2f, 1f)
    /** Nothing: ease-out sobrio, niente molle né rimbalzi. */
    val Nd = CubicBezierEasing(.25f, .1f, .25f, 1f)
}

@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int) = Font(R.font.bricolage, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(R.font.inter, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

@OptIn(ExperimentalTextApi::class)
private fun grotesk(weight: Int) = Font(R.font.spacegrotesk, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

@OptIn(ExperimentalTextApi::class)
private fun doto(weight: Int) = Font(R.font.doto, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Nothing: Doto (matrice a punti) solo per i numeri grandi, Space Grotesk per il testo, Space Mono per etichette e dati. */
val DotoFont = FontFamily(doto(400), doto(700))
val GroteskFont = FontFamily(grotesk(300), grotesk(400), grotesk(500), grotesk(700))
val MonoFont = FontFamily(Font(R.font.spacemono, FontWeight.Normal), Font(R.font.spacemono_bold, FontWeight.Bold))

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

/** Metro: sans-serif di sistema, titoli sottili e grandi come Segoe WP Light. */
private fun TextStyle.m(weight: FontWeight) = copy(fontFamily = FontFamily.SansSerif, fontWeight = weight, letterSpacing = if (letterSpacing.value < 0f) (-0.01).em else letterSpacing)

private val metroType = dueffeType.run {
    Typography(
        displayLarge.m(FontWeight.Light).copy(fontSize = 56.sp, lineHeight = 58.sp),
        displayMedium.m(FontWeight.Light).copy(fontSize = 48.sp, lineHeight = 52.sp),
        displaySmall.m(FontWeight.Light), headlineLarge.m(FontWeight.Light), headlineMedium.m(FontWeight.Light), headlineSmall.m(FontWeight.Light),
        titleLarge.m(FontWeight.Normal), titleMedium.m(FontWeight.Normal), titleSmall.m(FontWeight.Normal),
        bodyLarge.m(FontWeight.Normal), bodyMedium.m(FontWeight.Normal), bodySmall.m(FontWeight.Normal),
        labelLarge.m(FontWeight.Medium), labelMedium.m(FontWeight.Medium), labelSmall.m(FontWeight.Normal),
    )
}

private fun ts(f: FontFamily, size: Int, line: Float, track: Double, w: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = f, fontWeight = w, fontSize = size.sp, lineHeight = (size * line).sp, letterSpacing = track.em)

/** Scala tipografica del design system Nothing: Doto da 36 in su, etichette Space Mono spaziate. */
private val ndType = Typography(
    displayLarge = ts(DotoFont, 72, 1f, -0.03, FontWeight.Bold),
    displayMedium = ts(DotoFont, 44, 1.05f, -0.02, FontWeight.Bold),
    displaySmall = ts(GroteskFont, 30, 1.15f, -0.02),
    headlineLarge = ts(GroteskFont, 32, 1.1f, -0.02),
    headlineMedium = ts(GroteskFont, 24, 1.2f, -0.01),
    headlineSmall = ts(GroteskFont, 20, 1.25f, -0.01),
    titleLarge = ts(GroteskFont, 18, 1.3f, 0.0, FontWeight.Medium),
    titleMedium = ts(GroteskFont, 16, 1.3f, 0.0, FontWeight.Medium),
    titleSmall = ts(GroteskFont, 14, 1.35f, 0.0, FontWeight.Medium),
    bodyLarge = ts(GroteskFont, 16, 1.5f, 0.0),
    bodyMedium = ts(GroteskFont, 14, 1.5f, 0.01),
    bodySmall = ts(GroteskFont, 12, 1.4f, 0.02),
    labelLarge = ts(MonoFont, 13, 1.25f, 0.06),
    labelMedium = ts(MonoFont, 11, 1.25f, 0.08),
    labelSmall = ts(MonoFont, 10, 1.2f, 0.06),
)

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
fun DeclutTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors(), typography = when (Ui.style) { UiStyle.MODERN, UiStyle.DUEFFE -> dueffeType; UiStyle.SKEU -> skeuType; UiStyle.METRO -> metroType; UiStyle.NOTHING -> ndType }, content = content)
