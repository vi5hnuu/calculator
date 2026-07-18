package com.vi5hnu.calculator.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vi5hnu.calculator.R

/**
 * The design's two typefaces, both shipped as single variable-weight files.
 *
 * One variable font covers every weight we need, which costs less than the four static cuts
 * it replaces. `FontVariation` needs API 26, which matches the module's minSdk.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Space Grotesk — UI text, numerals on keys, the primary expression display. */
val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Normal),
    variableFont(R.font.space_grotesk, FontWeight.Medium),
    variableFont(R.font.space_grotesk, FontWeight.SemiBold),
    variableFont(R.font.space_grotesk, FontWeight.Bold),
)

/** JetBrains Mono — anywhere digits must line up: results, tape, readouts, worksheet. */
val JetBrainsMono = FontFamily(
    variableFont(R.font.jetbrains_mono, FontWeight.Normal),
    variableFont(R.font.jetbrains_mono, FontWeight.Medium),
    variableFont(R.font.jetbrains_mono, FontWeight.SemiBold),
)

val MathProTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = SpaceGrotesk),
        displayMedium = displayMedium.copy(fontFamily = SpaceGrotesk),
        displaySmall = displaySmall.copy(fontFamily = SpaceGrotesk),
        headlineLarge = headlineLarge.copy(fontFamily = SpaceGrotesk),
        headlineMedium = headlineMedium.copy(fontFamily = SpaceGrotesk),
        headlineSmall = headlineSmall.copy(fontFamily = SpaceGrotesk),
        titleLarge = titleLarge.copy(fontFamily = SpaceGrotesk),
        titleMedium = titleMedium.copy(fontFamily = SpaceGrotesk),
        titleSmall = titleSmall.copy(fontFamily = SpaceGrotesk),
        bodyLarge = bodyLarge.copy(fontFamily = SpaceGrotesk),
        bodyMedium = bodyMedium.copy(fontFamily = SpaceGrotesk),
        bodySmall = bodySmall.copy(fontFamily = SpaceGrotesk),
        labelLarge = labelLarge.copy(fontFamily = SpaceGrotesk),
        labelMedium = labelMedium.copy(fontFamily = SpaceGrotesk),
        labelSmall = labelSmall.copy(fontFamily = SpaceGrotesk),
    )
}

/** Named styles the design leans on repeatedly. */
object MathProTextStyles {

    /** Section captions: "UNIT CONVERTER", "PRINCIPAL AMOUNT". */
    val SectionLabel = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    )

    /** Small captions inside cards. */
    val FieldLabel = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
    )

    /** The main editable expression. */
    val Expression = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp,
    )

    /** The live "= result" under the expression. */
    val Result = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
    )

    /** Monospaced numeric readouts. */
    val Mono = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
    )
}
