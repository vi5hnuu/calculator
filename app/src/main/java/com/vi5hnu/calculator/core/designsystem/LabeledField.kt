package com.vi5hnu.calculator.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A captioned text input inside a surface card — the repeated building block of the
 * Converter, Finance, Date and Percent forms.
 *
 * These modes take free-form numeric input, so unlike the calculator keypad they use the
 * system keyboard.
 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    val colors = MathProTheme.colors

    SurfaceCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = label.uppercase(),
                color = colors.textMuted,
                style = MathProTextStyles.FieldLabel,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = fieldTextStyle(fontSize),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}

/** Read-only counterpart used for computed values that sit in the same visual rhythm. */
@Composable
fun ReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentValue: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
) {
    val colors = MathProTheme.colors

    SurfaceCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = label.uppercase(),
                color = colors.textMuted,
                style = MathProTextStyles.FieldLabel,
            )
            Text(
                text = value,
                color = if (accentValue) colors.accent else colors.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = fontSize,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
