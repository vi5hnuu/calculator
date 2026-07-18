package com.vi5hnu.calculator.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The visual vocabulary shared by all nine modes.
 *
 * Keeping these here rather than in each feature is what stops the modes drifting apart
 * visually, and it means an accent or radius change lands everywhere at once.
 */

/**
 * Android's minimum comfortable touch target.
 *
 * The design mock was drawn for a mouse at 412px, so several of its controls are 30–36px.
 * Those sizes are fine to *look* at and too small to *hit* — this is the floor for anything
 * tappable.
 */
val MinTouchTarget: Dp = 48.dp

/** How a key is filled. Mirrors the mock's four button treatments. */
enum class KeyTone { Neutral, Accent, Emphasis, Danger }

/**
 * A pressable key.
 *
 * The press scale (0.95) and the corner radius come straight from the mock's
 * `style-active="transform:scale(.95)"`.
 */
@Composable
fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: KeyTone = KeyTone.Neutral,
    height: Dp = 58.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    cornerRadius: Dp = 20.dp,
    enabled: Boolean = true,
    /**
     * Spoken name, where the visible label is a glyph. "⌫" and "±" mean nothing read aloud,
     * and screen readers pronounce them inconsistently or skip them entirely.
     */
    contentDescription: String? = null,
) {
    val colors = MathProTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "keyPress")

    // A keypad with no tactile response feels dead, and the 0.95 press-scale alone is easy to
    // miss under a thumb. performHapticFeedback already honours the system haptics setting,
    // so users who turn vibration off still get silence.
    val haptics = LocalHapticFeedback.current

    val background: Brush = when (tone) {
        KeyTone.Neutral -> colors.keyBrush
        KeyTone.Accent -> Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.18f), colors.accent.copy(alpha = 0.08f)),
        )
        KeyTone.Emphasis -> colors.accentBrush
        KeyTone.Danger -> Brush.verticalGradient(
            listOf(colors.dangerSurface, colors.dangerSurface),
        )
    }
    val contentColor = when (tone) {
        KeyTone.Neutral -> colors.textPrimary
        KeyTone.Accent -> colors.accent
        KeyTone.Emphasis -> colors.onAccent
        KeyTone.Danger -> colors.danger
    }
    val borderColor = when (tone) {
        KeyTone.Neutral -> colors.border
        KeyTone.Accent -> colors.accent.copy(alpha = 0.16f)
        KeyTone.Emphasis -> Color.Transparent
        KeyTone.Danger -> colors.dangerBorder
    }

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onClick()
                },
            )
            .semantics {
                // Replaces the glyph the Text would otherwise expose, and marks a greyed-out
                // key as disabled rather than merely unresponsive.
                contentDescription?.let { this.contentDescription = it }
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.35f),
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = SpaceGrotesk,
            maxLines = 1,
        )
    }
}

/** A pill in the mode dock, or any selectable chip. */
@Composable
fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            // 35dp in the mock; raised to 44 because the dock is the app's main navigation
            // and gets hit constantly.
            .height(44.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(colors.accentBrush)
                else Modifier
                    .background(colors.surface)
                    .border(BorderStroke(1.dp, colors.border), shape),
            )
            // Tab, not Button: these switch between modes, and the accent fill that marks the
            // current one is invisible to a screen reader without this.
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) colors.onAccent else colors.textMuted,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpaceGrotesk,
            maxLines = 1,
        )
    }
}

/** A bordered container — the mock's `rgba(255,255,255,.03)` card. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), shape),
    ) { content() }
}

/** The filled accent card used for headline answers (EMI, per-person, duration). */
@Composable
fun HeroResultCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accentBrush)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column {
            Text(
                text = label,
                color = colors.onAccent.copy(alpha = 0.75f),
                style = MathProTextStyles.FieldLabel.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                color = colors.onAccent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A small labelled figure, used in rows beneath hero cards. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    center: Boolean = false,
) {
    val colors = MathProTheme.colors
    SurfaceCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            if (center) {
                MonoValue(value, 20.sp, colors.textPrimary)
                Text(label, color = colors.textMuted, style = MathProTextStyles.FieldLabel)
            } else {
                Text(label, color = colors.textMuted, style = MathProTextStyles.FieldLabel)
                MonoValue(value, 18.sp, colors.textPrimary)
            }
        }
    }
}

@Composable
private fun MonoValue(value: String, size: androidx.compose.ui.unit.TextUnit, color: Color) {
    Text(
        text = value,
        color = color,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = size,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** An uppercase section caption. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = MathProTheme.colors.textMuted,
        style = MathProTextStyles.SectionLabel,
    )
}

/** The amber advisory strip — currently the converter's stale-rates disclaimer. */
@Composable
fun WarningNote(text: String, modifier: Modifier = Modifier) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.warningSurface)
            .border(BorderStroke(1.dp, colors.warningBorder), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = text, color = colors.warning, fontSize = 10.sp, fontFamily = SpaceGrotesk)
    }
}

/** A labelled row of equal-width chips (tip presets, percent ops, bases). */
@Composable
fun ChipRow(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { option ->
            SmallChip(
                label = option,
                selected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun SmallChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(colors.accent.copy(alpha = 0.18f))
                else Modifier.background(colors.surface),
            )
            .border(
                BorderStroke(1.dp, if (selected) colors.accent.copy(alpha = 0.45f) else colors.border),
                shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            // Without this a chip that is not weighted by its parent shrink-wraps to the glyph
            // and clips it — which is what the converter's unit dropdowns were doing.
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) colors.accent else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpaceGrotesk,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shared text style for editable numeric fields inside cards. */
@Composable
fun fieldTextStyle(fontSize: androidx.compose.ui.unit.TextUnit = 22.sp): TextStyle =
    TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        color = MathProTheme.colors.textPrimary,
    )
