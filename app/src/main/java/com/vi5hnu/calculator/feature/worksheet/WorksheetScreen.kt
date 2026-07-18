package com.vi5hnu.calculator.feature.worksheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.vi5hnu.calculator.core.designsystem.JetBrainsMono
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.designsystem.SectionLabel
import com.vi5hnu.calculator.domain.repository.SettingsRepository
import com.vi5hnu.calculator.domain.usecase.WorksheetEvaluator
import com.vi5hnu.calculator.domain.usecase.WorksheetLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class WorksheetViewModel @Inject constructor(
    private val evaluator: WorksheetEvaluator,
    settings: SettingsRepository,
) : ViewModel() {

    private val _text = MutableStateFlow(DEFAULT_SHEET)
    val text: StateFlow<String> = _text.asStateFlow()

    /**
     * Combined with the angle setting rather than defaulting to radians: a worksheet line
     * reading `sin(30)` must agree with the same expression typed on the keypad.
     */
    val lines: StateFlow<List<WorksheetLine>> =
        combine(_text, settings.observeSettings().map { it.angleUnit }) { text, angleUnit ->
            evaluator.evaluate(text, angleUnit)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onTextChange(value: String) {
        _text.value = value
    }

    private companion object {
        /** The design's sample sheet, which doubles as a hint at the syntax. */
        val DEFAULT_SHEET = """
            price = 250
            qty = 4
            subtotal = price*qty
            subtotal*1.08
        """.trimIndent()
    }
}

@Composable
fun WorksheetScreen(
    text: String,
    lines: List<WorksheetLine>,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors
    val editorShape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionLabel("Worksheet · variables + ans")

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontFamily = JetBrainsMono,
                fontSize = 15.sp,
                lineHeight = 25.sp,
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 110.dp)
                .clip(editorShape)
                .background(colors.surface)
                .border(BorderStroke(1.dp, colors.border), editorShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )

        Text(
            text = "Use name = value then reuse it · ans = previous line",
            color = colors.textMuted,
            fontSize = 10.sp,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(lines, key = { it.index }) { line ->
                val shape = RoundedCornerShape(10.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.surface)
                        .border(BorderStroke(1.dp, colors.border), shape)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = line.expression,
                        modifier = Modifier.weight(1f),
                        color = colors.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "= ${line.formatted}",
                        // A failed line reads as muted rather than accented, so a typo is
                        // visible at a glance without shouting.
                        color = if (line.value == null) colors.textFaint else colors.accent,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
