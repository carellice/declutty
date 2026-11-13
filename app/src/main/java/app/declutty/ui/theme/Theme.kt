package app.declutty.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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

/** Token del design system Dueffe con l'accento di Declutty: menta su fondo quasi nero. */
object DF {
    val Bg = Color(0xFF050706)
    val BgElevated = Color(0xFF0A0D0C)
    val Surface = Color(0xFF0F1311)
    val SurfaceHigh = Color(0xFF161C19)
    val SurfaceHigher = Color(0xFF1F2723)
    val Text = Color(0xFFF2FFF8)
    val Muted = Color(0xFF9DB3A8)
    val Line = Color(0x2980FFD0)
    val LineStrong = Color(0x575CF2B8)
    val Primary = Color(0xFF5CF2B8)
    val PrimaryStrong = Color(0xFF1FD3A0)
    val Secondary = Color(0xFFC8FFE9)
    val OnPrimary = Color(0xFF03140D)
    val Error = Color(0xFFFF8F80)
    val Nav = Color(0xFF080B0A)
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

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 48.sp, letterSpacing = (-0.035).em),
    displayMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = (-0.03).em),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em),
)

private val colors = darkColorScheme(
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
fun DeclutTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, typography = typography, content = content)
