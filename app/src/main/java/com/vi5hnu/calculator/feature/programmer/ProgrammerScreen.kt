package com.vi5hnu.calculator.feature.programmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vi5hnu.calculator.core.designsystem.JetBrainsMono
import com.vi5hnu.calculator.core.designsystem.KeyButton
import com.vi5hnu.calculator.core.designsystem.KeyTone
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.designsystem.SpaceGrotesk
import com.vi5hnu.calculator.domain.model.NumberBase
import com.vi5hnu.calculator.domain.usecase.ProgOp

/** The four live base readouts. Tapping one switches the entry base. */
@Composable
fun ProgrammerWorkArea(
    state: ProgrammerUiState,
    onBaseSelected: (NumberBase) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors

    // The four base readouts scroll within the work area, so on a short screen they never get
    // squeezed against the keypad — they slide under the mode dock instead. The bottom padding
    // is the gap the keypad needs to sit clear of the readouts.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.rows.forEach { row ->
            val shape = RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(if (row.selected) colors.accent.copy(alpha = 0.10f) else colors.surface)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (row.selected) colors.accent.copy(alpha = 0.45f) else colors.border,
                        ),
                        shape,
                    )
                    .clickable { onBaseSelected(row.base) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.base.label,
                    modifier = Modifier.width(38.dp),
                    color = if (row.selected) colors.accent else colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = SpaceGrotesk,
                )
                Text(
                    text = row.value,
                    modifier = Modifier.weight(1f),
                    color = if (row.selected) colors.textPrimary else colors.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The programmer keypad.
 *
 * Hex digits stay visible in every base and simply disable when illegal, so the layout never
 * reflows as the base changes.
 */
@Composable
fun ProgrammerKeypad(
    state: ProgrammerUiState,
    viewModel: ProgrammerViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            // Top padding sets the readouts clearly apart from the keypad; bottom clears the
            // gesture bar. Keys are 46dp so all four base readouts fit above them.
            .padding(top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        padRows().forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val enabled = key !is ProgKey.Digit || key.char in state.enabledDigits
                    KeyButton(
                        label = key.label,
                        onClick = { key.dispatch(viewModel) },
                        modifier = Modifier.weight(key.weight),
                        tone = key.tone,
                        height = 46.dp,
                        fontSize = key.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        cornerRadius = 14.dp,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

private sealed interface ProgKey {
    val label: String
    val tone: KeyTone
    val fontSize: androidx.compose.ui.unit.TextUnit

    /** Column span. The equals key claims two so the bottom row still fills four columns. */
    val weight: Float

    fun dispatch(viewModel: ProgrammerViewModel)

    data class Digit(val char: Char) : ProgKey {
        override val label = char.toString()
        override val tone = KeyTone.Neutral
        override val fontSize = 20.sp
        override val weight = 1f
        override fun dispatch(viewModel: ProgrammerViewModel) = viewModel.onDigit(char)
    }

    data class Operator(val op: ProgOp) : ProgKey {
        override val label = op.label
        override val tone = KeyTone.Accent
        override val fontSize = 16.sp
        override val weight = 1f
        override fun dispatch(viewModel: ProgrammerViewModel) = viewModel.onOperator(op)
    }

    data class Action(
        override val label: String,
        override val tone: KeyTone = KeyTone.Neutral,
        override val fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
        override val weight: Float = 1f,
        val onPress: (ProgrammerViewModel) -> Unit,
    ) : ProgKey {
        override fun dispatch(viewModel: ProgrammerViewModel) = onPress(viewModel)
    }
}

private fun padRows(): List<List<ProgKey>> = listOf(
    listOf(
        ProgKey.Action("CE", KeyTone.Danger) { it.onClearEntry() },
        ProgKey.Action("⌫") { it.onDelete() },
        ProgKey.Action("NOT") { it.onNot() },
        ProgKey.Action("±") { it.onNegate() },
    ),
    listOf(ProgKey.Digit('A'), ProgKey.Digit('B'), ProgKey.Operator(ProgOp.AND), ProgKey.Operator(ProgOp.OR)),
    listOf(ProgKey.Digit('C'), ProgKey.Digit('D'), ProgKey.Operator(ProgOp.XOR), ProgKey.Operator(ProgOp.SHL)),
    listOf(ProgKey.Digit('E'), ProgKey.Digit('F'), ProgKey.Operator(ProgOp.SHR), ProgKey.Operator(ProgOp.DIV)),
    listOf(ProgKey.Digit('7'), ProgKey.Digit('8'), ProgKey.Digit('9'), ProgKey.Operator(ProgOp.MUL)),
    listOf(ProgKey.Digit('4'), ProgKey.Digit('5'), ProgKey.Digit('6'), ProgKey.Operator(ProgOp.SUB)),
    listOf(ProgKey.Digit('1'), ProgKey.Digit('2'), ProgKey.Digit('3'), ProgKey.Operator(ProgOp.ADD)),
    listOf(
        ProgKey.Digit('0'),
        ProgKey.Action("00") { it.onDigit('0'); it.onDigit('0') },
        ProgKey.Action("=", KeyTone.Emphasis, 20.sp, weight = 2f) { it.onEquals() },
    ),
)
