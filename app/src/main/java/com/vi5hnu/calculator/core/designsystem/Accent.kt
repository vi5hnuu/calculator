package com.vi5hnu.calculator.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * The five accents the design exposes as a theme option.
 *
 * Each renders as a gradient from [color] to a 72%-shaded companion, which is exactly how the
 * mock derives its `--acc2` from `--acc`.
 */
enum class Accent(val color: Color, val label: String) {
    GREEN(Color(0xFF2FE08A), "Green"),
    BLUE(Color(0xFF2A8DFF), "Blue"),
    ORANGE(Color(0xFFFF9F2F), "Orange"),
    PURPLE(Color(0xFFB06BFF), "Purple"),
    PINK(Color(0xFFFF5C8A), "Pink");

    /** The darker end of the accent gradient. */
    val shade: Color get() = color.shaded(0.72f)

    companion object {
        val Default = GREEN

        fun fromName(name: String?): Accent =
            entries.firstOrNull { it.name == name } ?: Default
    }
}

/** Multiplies each RGB channel, mirroring the mock's `shade()` helper. */
internal fun Color.shaded(factor: Float): Color = Color(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
    alpha = alpha,
)
