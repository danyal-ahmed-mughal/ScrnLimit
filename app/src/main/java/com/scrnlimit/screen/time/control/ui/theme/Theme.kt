package com.scrnlimit.screen.time.control.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.view.WindowCompat
import com.scrnlimit.screen.time.control.R
import com.scrnlimit.screen.time.control.i18n.Phrases

data class Palette(
    val ink: Color,
    val inkRaised: Color,
    val card: Color,
    val cardHigh: Color,
    val line: Color,
    val paper: Color,
    val mute: Color,
    val faint: Color,
    val signal: Color,
    val onSignal: Color,
    val accent: Color,
    val amber: Color,
    val coral: Color,
    val focus: Color,
    val productive: Color,
    val dark: Boolean,
)

private val DarkPalette = Palette(
    ink = Color(0xFF08090C),
    inkRaised = Color(0xFF10131A),
    card = Color(0xFF171C26),
    cardHigh = Color(0xFF212836),
    line = Color(0xFF2C3442),
    paper = Color(0xFFF7F4EE),
    mute = Color(0xFFA7AEB8),
    faint = Color(0xFF7A828E),
    signal = Color(0xFFD6F25A),
    onSignal = Color(0xFF14161C),
    accent = Color(0xFFD6F25A),
    amber = Color(0xFFFFB703),
    coral = Color(0xFFFF5D73),
    focus = Color(0xFFC4B5FD),
    productive = Color(0xFF5EEAD4),
    dark = true,
)

private val LightPalette = Palette(
    ink = Color(0xFFF6F3EC),
    inkRaised = Color(0xFFEDE8DE),
    card = Color(0xFFFFFFFF),
    cardHigh = Color(0xFFF3EEE4),
    line = Color(0xFFE4DDD0),
    paper = Color(0xFF16181D),
    mute = Color(0xFF5C6570),
    faint = Color(0xFF7A828C),
    signal = Color(0xFFD2EE3E),
    onSignal = Color(0xFF16181D),
    accent = Color(0xFF4C6700),
    amber = Color(0xFFB45309),
    coral = Color(0xFFE11D48),
    focus = Color(0xFF6D28D9),
    productive = Color(0xFF0F766E),
    dark = false,
)

object SL {
    private var palette by mutableStateOf(DarkPalette)

    fun apply(next: Palette) {
        palette = next
    }

    val Ink: Color get() = palette.ink
    val InkRaised: Color get() = palette.inkRaised
    val Card: Color get() = palette.card
    val CardHigh: Color get() = palette.cardHigh
    val Line: Color get() = palette.line
    val Paper: Color get() = palette.paper
    val Mute: Color get() = palette.mute
    val Faint: Color get() = palette.faint
    val Signal: Color get() = palette.signal
    val OnSignal: Color get() = palette.onSignal
    val Accent: Color get() = palette.accent
    val Amber: Color get() = palette.amber
    val Coral: Color get() = palette.coral
    val Focus: Color get() = palette.focus
    val Productive: Color get() = palette.productive
    val Dark: Boolean get() = palette.dark
}

fun meterColor(ratio: Float): Color = when {
    ratio >= 1f -> SL.Coral
    ratio >= 0.8f -> SL.Amber
    else -> SL.Signal
}

val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

private fun type(palette: Palette) = Typography(
    displayLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-0.8).sp, color = palette.paper),
    headlineLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.4).sp, color = palette.paper),
    titleLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.paper),
    titleMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = palette.paper),
    bodyLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp, color = palette.paper),
    bodyMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = palette.mute),
    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = palette.paper),
)

@Composable
fun ScrnLimitTheme(
    theme: String = "dark",
    language: String = "en",
    content: @Composable () -> Unit,
) {
    val resolved = Phrases.resolve(language)
    if (Phrases.code != resolved) Phrases.code = resolved
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPalette else LightPalette
    SL.apply(palette)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    val direction = if (Phrases.rtl(resolved)) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = palette.signal,
                    onPrimary = palette.onSignal,
                    secondary = palette.focus,
                    background = palette.ink,
                    onBackground = palette.paper,
                    surface = palette.card,
                    onSurface = palette.paper,
                    surfaceVariant = palette.cardHigh,
                    onSurfaceVariant = palette.mute,
                    error = palette.coral,
                    outline = palette.line,
                )
            } else {
                lightColorScheme(
                    primary = palette.accent,
                    onPrimary = palette.onSignal,
                    secondary = palette.focus,
                    background = palette.ink,
                    onBackground = palette.paper,
                    surface = palette.card,
                    onSurface = palette.paper,
                    surfaceVariant = palette.cardHigh,
                    onSurfaceVariant = palette.mute,
                    error = palette.coral,
                    outline = palette.line,
                )
            },
            typography = type(palette),
            content = content,
        )
    }
}
