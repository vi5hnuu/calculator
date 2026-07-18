package com.vi5hnu.calculator.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.vi5hnu.calculator.core.designsystem.HeroResultCard
import com.vi5hnu.calculator.core.designsystem.LabeledField
import com.vi5hnu.calculator.core.designsystem.SectionLabel
import com.vi5hnu.calculator.core.designsystem.StatTile
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.usecase.LoanCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.round

data class FinanceUiState(
    val principal: String = "500000",
    val ratePercent: String = "8.5",
    val years: String = "20",
    val monthlyPayment: String = "—",
    val totalInterest: String = "—",
    val totalPayable: String = "—",
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val calculator: LoanCalculator,
) : ViewModel() {

    private val _state = MutableStateFlow(FinanceUiState().recomputed())
    val state: StateFlow<FinanceUiState> = _state.asStateFlow()

    fun onPrincipalChange(value: String) = _state.update { it.copy(principal = value).recomputed() }

    fun onRateChange(value: String) = _state.update { it.copy(ratePercent = value).recomputed() }

    fun onYearsChange(value: String) = _state.update { it.copy(years = value).recomputed() }

    private fun FinanceUiState.recomputed(): FinanceUiState {
        val summary = calculator.calculate(
            principal = principal.toDoubleOrNull() ?: return blank(),
            annualRatePercent = ratePercent.toDoubleOrNull() ?: return blank(),
            years = years.toDoubleOrNull() ?: return blank(),
        ) ?: return blank()

        // kotlin.math.round stays in Double. Math.round would return a Long and saturate at
        // Long.MAX_VALUE for an absurd principal, reporting a smaller payment than the truth.
        return copy(
            monthlyPayment = NumberFormatter.format(round(summary.monthlyPayment * 100.0) / 100.0),
            totalInterest = NumberFormatter.format(round(summary.totalInterest)),
            totalPayable = NumberFormatter.format(round(summary.totalPayable)),
        )
    }

    private fun FinanceUiState.blank() =
        copy(monthlyPayment = "—", totalInterest = "—", totalPayable = "—")
}

@Composable
fun FinanceScreen(
    state: FinanceUiState,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel("Loan / EMI Calculator", modifier = Modifier.padding(bottom = 2.dp))

        LabeledField(
            label = "Principal amount",
            value = state.principal,
            onValueChange = viewModel::onPrincipalChange,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledField(
                label = "Rate % / yr",
                value = state.ratePercent,
                onValueChange = viewModel::onRateChange,
                modifier = Modifier.weight(1f),
            )
            LabeledField(
                label = "Tenure (yrs)",
                value = state.years,
                onValueChange = viewModel::onYearsChange,
                modifier = Modifier.weight(1f),
            )
        }

        HeroResultCard(
            label = "MONTHLY PAYMENT",
            value = state.monthlyPayment,
            modifier = Modifier.padding(top = 6.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Total interest", state.totalInterest, Modifier.weight(1f))
            StatTile("Total payable", state.totalPayable, Modifier.weight(1f))
        }
    }
}
