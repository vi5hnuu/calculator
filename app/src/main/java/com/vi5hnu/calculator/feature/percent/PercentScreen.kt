package com.vi5hnu.calculator.feature.percent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.vi5hnu.calculator.core.designsystem.ChipRow
import com.vi5hnu.calculator.core.designsystem.HeroResultCard
import com.vi5hnu.calculator.core.designsystem.JetBrainsMono
import com.vi5hnu.calculator.core.designsystem.LabeledField
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.designsystem.SectionLabel
import com.vi5hnu.calculator.core.designsystem.SmallChip
import com.vi5hnu.calculator.core.designsystem.StatTile
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.usecase.PercentOp
import com.vi5hnu.calculator.domain.usecase.PercentTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.ui.text.input.KeyboardType
import javax.inject.Inject

data class PercentUiState(
    val bill: String = "1200",
    val tipPercent: String = "15",
    val split: String = "2",
    val tipAmount: String = "—",
    val total: String = "—",
    val perPerson: String = "—",
    val value: String = "200",
    val percent: String = "10",
    val op: PercentOp = PercentOp.Default,
    val toolResult: String = "—",
)

@HiltViewModel
class PercentViewModel @Inject constructor(
    private val tools: PercentTools,
) : ViewModel() {

    private val _state = MutableStateFlow(PercentUiState().recomputed())
    val state: StateFlow<PercentUiState> = _state.asStateFlow()

    fun onBillChange(v: String) = _state.update { it.copy(bill = v).recomputed() }

    fun onTipChange(v: String) = _state.update { it.copy(tipPercent = v).recomputed() }

    fun onSplitChange(v: String) = _state.update { it.copy(split = v).recomputed() }

    fun onValueChange(v: String) = _state.update { it.copy(value = v).recomputed() }

    fun onPercentChange(v: String) = _state.update { it.copy(percent = v).recomputed() }

    fun onOpSelected(op: PercentOp) = _state.update { it.copy(op = op).recomputed() }

    private fun PercentUiState.recomputed(): PercentUiState {
        val summary = tools.tip(
            bill = bill.toDoubleOrNull() ?: Double.NaN,
            tipPercent = tipPercent.toDoubleOrNull() ?: Double.NaN,
            split = split.toIntOrNull() ?: 1,
        )
        val tool = tools.apply(
            op = op,
            value = value.toDoubleOrNull() ?: Double.NaN,
            percent = percent.toDoubleOrNull() ?: Double.NaN,
        )
        return copy(
            tipAmount = summary?.tipAmount?.let(NumberFormatter::format) ?: "—",
            total = summary?.total?.let(NumberFormatter::format) ?: "—",
            perPerson = summary?.perPerson?.let(NumberFormatter::format) ?: "—",
            toolResult = tool?.let(NumberFormatter::format) ?: "—",
        )
    }
}

/** The tip presets the design offers as quick chips. */
private val TIP_PRESETS = listOf("5", "10", "15", "20")

@Composable
fun PercentScreen(
    state: PercentUiState,
    viewModel: PercentViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = MathProTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        SectionLabel("Tip & Bill Split")

        LabeledField(
            label = "Bill amount",
            value = state.bill,
            onValueChange = viewModel::onBillChange,
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        ChipRow(
            options = TIP_PRESETS.map { "$it%" },
            selected = "${state.tipPercent}%",
            onSelect = { viewModel.onTipChange(it.removeSuffix("%")) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledField(
                label = "Tip %",
                value = state.tipPercent,
                onValueChange = viewModel::onTipChange,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            LabeledField(
                label = "Split",
                value = state.split,
                onValueChange = viewModel::onSplitChange,
                fontSize = 20.sp,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }

        HeroResultCard(label = "PER PERSON", value = state.perPerson)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Tip", state.tipAmount, Modifier.weight(1f))
            StatTile("Total", state.total, Modifier.weight(1f))
        }

        SectionLabel("Percent Tools", modifier = Modifier.padding(top = 10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PercentOp.entries.forEach { op ->
                SmallChip(
                    label = op.label,
                    selected = op == state.op,
                    onClick = { viewModel.onOpSelected(op) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledField(
                label = "Value",
                value = state.value,
                onValueChange = viewModel::onValueChange,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            LabeledField(
                label = "Percent",
                value = state.percent,
                onValueChange = viewModel::onPercentChange,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
        }

        val shape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.accent.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, colors.accent.copy(alpha = 0.16f)), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = state.toolResult,
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                maxLines = 1,
            )
        }
    }
}
