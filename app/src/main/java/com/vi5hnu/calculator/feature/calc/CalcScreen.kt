package com.vi5hnu.calculator.feature.calc

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vi5hnu.calculator.core.designsystem.JetBrainsMono
import com.vi5hnu.calculator.core.designsystem.KeyButton
import com.vi5hnu.calculator.core.designsystem.KeyTone
import com.vi5hnu.calculator.core.designsystem.MathProTextStyles
import com.vi5hnu.calculator.core.designsystem.MathProTheme

/**
 * Standard and Scientific share this screen; Scientific simply adds the function tray.
 */
@Composable
fun CalcWorkArea(
    state: CalcUiState,
    onReuse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors
    val listState = rememberLazyListState()

    // Keep the newest entry in view as the tape grows.
    LaunchedEffect(state.tape.size) {
        if (state.tape.isNotEmpty()) listState.animateScrollToItem(state.tape.lastIndex)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
        ) {
            items(state.tape, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReuse(item.expression) },
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = item.expression,
                        color = colors.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 15.sp,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        text = "= ${item.result}",
                        color = colors.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        CalcDisplay(state)
    }
}

@Composable
private fun CalcDisplay(state: CalcUiState) {
    val colors = MathProTheme.colors
    val scrollState = rememberScrollState()

    // Long expressions scroll horizontally; follow the caret at the right edge.
    LaunchedEffect(state.expression) { scrollState.animateScrollTo(scrollState.maxValue) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (state.hasMemory) {
            Text(
                text = "M = ${state.memoryLabel}",
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = state.expression.ifEmpty { "0" },
                color = if (state.expression.isEmpty()) colors.textMuted else colors.textPrimary,
                style = MathProTextStyles.Expression,
                maxLines = 1,
            )
        }

        Box(modifier = Modifier.height(34.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                state.error != null -> Text(
                    text = state.error,
                    color = colors.danger,
                    fontFamily = JetBrainsMono,
                    fontSize = 15.sp,
                )

                state.preview.isNotEmpty() -> Text(
                    text = state.preview,
                    color = colors.accent,
                    style = MathProTextStyles.Result,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The 5-column scientific tray. The 2nd key swaps each trig entry for its inverse. */
@Composable
fun FunctionTray(
    second: Boolean,
    angleLabel: String,
    onKey: (CalcKey) -> Unit,
    onToggleAngle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        trayItems(second, angleLabel).chunked(TRAY_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { item ->
                    KeyButton(
                        label = item.label,
                        onClick = {
                            when (item.action) {
                                TrayAction.ToggleSecond -> onKey(CalcKey.ToggleSecond)
                                TrayAction.ToggleAngle -> onToggleAngle()
                                is TrayAction.Insert -> onKey(CalcKey.Insert(item.action.text))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        tone = if (item.active) KeyTone.Emphasis else KeyTone.Accent,
                        height = 44.dp,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        cornerRadius = 13.dp,
                        contentDescription = item.spoken,
                    )
                }
                // Keep the last row's keys the same width as every other row's.
                repeat(TRAY_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val TRAY_COLUMNS = 5

private sealed interface TrayAction {
    data object ToggleSecond : TrayAction
    data object ToggleAngle : TrayAction
    data class Insert(val text: String) : TrayAction
}

private data class TrayItem(
    val label: String,
    val action: TrayAction,
    /** Drawn filled rather than outlined, to show a latched toggle. */
    val active: Boolean = false,
    val spoken: String? = null,
)

/** Mirrors the design's `fnTray`, including the 2nd-shifted variants. */
private fun trayItems(second: Boolean, angleLabel: String): List<TrayItem> = buildList {
    add(
        TrayItem(
            label = "2ⁿᵈ",
            action = TrayAction.ToggleSecond,
            active = second,
            spoken = if (second) "Second functions, on" else "Second functions, off",
        ),
    )
    add(
        TrayItem(
            label = angleLabel,
            action = TrayAction.ToggleAngle,
            spoken = "Angle unit, $angleLabel",
        ),
    )

    fun insert(label: String, text: String, spoken: String) =
        add(TrayItem(label, TrayAction.Insert(text), spoken = spoken))

    if (second) insert("sin⁻¹", "asin(", "Arc sine") else insert("sin", "sin(", "Sine")
    if (second) insert("cos⁻¹", "acos(", "Arc cosine") else insert("cos", "cos(", "Cosine")
    if (second) insert("tan⁻¹", "atan(", "Arc tangent") else insert("tan", "tan(", "Tangent")
    insert("x!", "!", "Factorial")
    insert("1/x", "^(-1)", "Reciprocal")
    if (second) insert("x²", "^2", "Squared") else insert("√", "sqrt(", "Square root")
    insert("xʸ", "^", "Power")
    if (second) insert("10ˣ", "10^(", "Ten to the power") else insert("log", "log(", "Log base ten")
    if (second) insert("eˣ", "exp(", "e to the power") else insert("ln", "ln(", "Natural log")
    insert("π", "pi", "Pi")
    insert("e", "e", "Euler's number")
    insert("(", "(", "Open bracket")
    insert(")", ")", "Close bracket")
}

/** Memory row plus the 4x5 numeric pad. */
@Composable
fun CalcKeypad(
    onKey: (CalcKey) -> Unit,
    onMemory: (MemoryKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("MC", MemoryKey.MC, "Memory clear"),
                Triple("MR", MemoryKey.MR, "Memory recall"),
                Triple("M+", MemoryKey.PLUS, "Memory add"),
                Triple("M−", MemoryKey.MINUS, "Memory subtract"),
            ).forEach { (label, key, spoken) ->
                KeyButton(
                    label = label,
                    onClick = { onMemory(key) },
                    modifier = Modifier.weight(1f),
                    tone = KeyTone.Neutral,
                    height = 44.dp,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    cornerRadius = 12.dp,
                    contentDescription = spoken,
                )
            }
        }

        keypadRows().forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { key ->
                    KeyButton(
                        label = key.label,
                        onClick = { onKey(key.action) },
                        modifier = Modifier.weight(1f),
                        tone = key.tone,
                        fontSize = key.fontSize,
                        fontWeight = if (key.tone == KeyTone.Neutral) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Bold
                        },
                        contentDescription = key.spoken,
                    )
                }
            }
        }
    }
}

private data class PadKey(
    val label: String,
    val action: CalcKey,
    val tone: KeyTone = KeyTone.Neutral,
    val fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    /** Spoken name, where the printed label is a symbol rather than a word. */
    val spoken: String? = null,
)

/**
 * The pad inserts display symbols (× ÷ −) rather than ASCII; the engine normalises them back.
 * That keeps what the user sees identical to the design without the parser caring.
 */
private fun keypadRows(): List<List<PadKey>> = listOf(
    listOf(
        PadKey("AC", CalcKey.Clear, KeyTone.Danger, 19.sp, spoken = "All clear"),
        PadKey("⌫", CalcKey.Delete, KeyTone.Neutral, 20.sp, spoken = "Delete"),
        // Named for what it does here, not for the symbol — see RpnEvaluator.
        PadKey("%", CalcKey.Insert("%"), KeyTone.Neutral, 20.sp, spoken = "Modulo"),
        PadKey("÷", CalcKey.Insert("÷"), KeyTone.Accent, spoken = "Divide"),
    ),
    listOf(
        PadKey("7", CalcKey.Insert("7")),
        PadKey("8", CalcKey.Insert("8")),
        PadKey("9", CalcKey.Insert("9")),
        PadKey("×", CalcKey.Insert("×"), KeyTone.Accent, spoken = "Multiply"),
    ),
    listOf(
        PadKey("4", CalcKey.Insert("4")),
        PadKey("5", CalcKey.Insert("5")),
        PadKey("6", CalcKey.Insert("6")),
        PadKey("−", CalcKey.Insert("−"), KeyTone.Accent, 26.sp, spoken = "Minus"),
    ),
    listOf(
        PadKey("1", CalcKey.Insert("1")),
        PadKey("2", CalcKey.Insert("2")),
        PadKey("3", CalcKey.Insert("3")),
        PadKey("+", CalcKey.Insert("+"), KeyTone.Accent, spoken = "Plus"),
    ),
    listOf(
        PadKey("±", CalcKey.Negate, KeyTone.Neutral, 20.sp, spoken = "Toggle sign"),
        PadKey("0", CalcKey.Insert("0")),
        PadKey(".", CalcKey.Insert("."), spoken = "Decimal point"),
        PadKey("=", CalcKey.Equals, KeyTone.Emphasis, 26.sp, spoken = "Equals"),
    ),
)
