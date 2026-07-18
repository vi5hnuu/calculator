package com.vi5hnu.calculator.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The design's palette, as semantic tokens rather than raw hexes.
 *
 * The dark values are lifted verbatim from the mock. The light set is derived from the same
 * roles so the app keeps the light/dark toggle the Flutter version had — the mock itself is
 * dark-only, so light is an extrapolation, not a copy.
 */
@Immutable
data class MathProColors(
    val accent: Color,
    val accentShade: Color,
    val onAccent: Color,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val border: Color,
    val keyTop: Color,
    val keyBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textFaint: Color,
    val danger: Color,
    val dangerSurface: Color,
    val dangerBorder: Color,
    val warning: Color,
    val warningSurface: Color,
    val warningBorder: Color,
    val isDark: Boolean,
) {
    /** The app's page background, matching the mock's 185° vertical gradient. */
    val backgroundBrush: Brush
        get() = Brush.verticalGradient(
            0.0f to backgroundTop,
            0.55f to backgroundMid,
            1.0f to backgroundBottom,
        )

    /** Accent fill used on the equals key, result cards and the selected mode chip. */
    val accentBrush: Brush
        get() = Brush.linearGradient(listOf(accent, accentShade))

    /** The subtle top-lit fill on number keys. */
    val keyBrush: Brush
        get() = Brush.verticalGradient(listOf(keyTop, keyBottom))
}

private fun darkColors(accent: Accent) = MathProColors(
    accent = accent.color,
    accentShade = accent.shade,
    onAccent = Color(0xFF04140B),
    backgroundTop = Color(0xFF0E1712),
    backgroundMid = Color(0xFF0A110C),
    backgroundBottom = Color(0xFF080D0A),
    surface = Color(0xFFFFFFFF).copy(alpha = 0.03f),
    surfaceStrong = Color(0xFFFFFFFF).copy(alpha = 0.06f),
    border = Color(0xFFFFFFFF).copy(alpha = 0.08f),
    keyTop = Color(0xFF212B24),
    keyBottom = Color(0xFF171E19),
    textPrimary = Color(0xFFF4FAF6),
    textSecondary = Color(0xFFAEC4B8),
    textMuted = Color(0xFF5F746A),
    textFaint = Color(0xFF4D5F56),
    danger = Color(0xFFFF9D9D),
    dangerSurface = Color(0xFFFF5A5A).copy(alpha = 0.10f),
    dangerBorder = Color(0xFFFF7878).copy(alpha = 0.22f),
    warning = Color(0xFFC9A24A),
    warningSurface = Color(0xFFFFBE50).copy(alpha = 0.06f),
    warningBorder = Color(0xFFFFBE50).copy(alpha = 0.16f),
    isDark = true,
)

private fun lightColors(accent: Accent) = MathProColors(
    accent = accent.color.shaded(0.86f), // full-brightness accent is glaring on white
    accentShade = accent.shade,
    onAccent = Color(0xFFFFFFFF),
    backgroundTop = Color(0xFFF7FAF8),
    backgroundMid = Color(0xFFEFF4F1),
    backgroundBottom = Color(0xFFE9F0EC),
    surface = Color(0xFF0A110C).copy(alpha = 0.03f),
    surfaceStrong = Color(0xFF0A110C).copy(alpha = 0.06f),
    border = Color(0xFF0A110C).copy(alpha = 0.10f),
    keyTop = Color(0xFFFFFFFF),
    keyBottom = Color(0xFFF0F5F2),
    textPrimary = Color(0xFF0C1611),
    textSecondary = Color(0xFF3B4A42),
    textMuted = Color(0xFF6C7D73),
    textFaint = Color(0xFF98A89F),
    danger = Color(0xFFC0392B),
    dangerSurface = Color(0xFFFF5A5A).copy(alpha = 0.10f),
    dangerBorder = Color(0xFFC0392B).copy(alpha = 0.24f),
    warning = Color(0xFF8A6A1E),
    warningSurface = Color(0xFFFFBE50).copy(alpha = 0.16f),
    warningBorder = Color(0xFFB98A20).copy(alpha = 0.30f),
    isDark = false,
)

val LocalMathProColors: ProvidableCompositionLocal<MathProColors> =
    staticCompositionLocalOf { darkColors(Accent.Default) }

/** Shorthand: `MathProTheme.colors` anywhere inside the theme. */
object MathProTheme {
    val colors: MathProColors
        @Composable get() = LocalMathProColors.current
}

@Composable
fun MathProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Accent = Accent.Default,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkColors(accent) else lightColors(accent)

    // Material 3's scheme is kept in sync so stock components (ripples, text selection,
    // the odd Surface) pick up the right colours without being restyled one by one.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.backgroundMid,
            onBackground = colors.textPrimary,
            surface = colors.backgroundMid,
            onSurface = colors.textPrimary,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.backgroundMid,
            onBackground = colors.textPrimary,
            surface = colors.backgroundMid,
            onSurface = colors.textPrimary,
            error = colors.danger,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalMathProColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MathProTypography,
            content = content,
        )
    }
}
