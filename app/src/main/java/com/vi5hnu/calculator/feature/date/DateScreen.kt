package com.vi5hnu.calculator.feature.date

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.vi5hnu.calculator.core.designsystem.HeroResultCard
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.designsystem.ReadOnlyField
import com.vi5hnu.calculator.core.designsystem.SectionLabel
import com.vi5hnu.calculator.core.designsystem.StatTile
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.usecase.DateDifference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DateUiState(
    val from: LocalDate = LocalDate.now(),
    val to: LocalDate = LocalDate.now().plusDays(30),
    val days: String = "—",
    val weeks: String = "—",
    val months: String = "—",
    val ymd: String = "—",
)

@HiltViewModel
class DateViewModel @Inject constructor(
    private val dates: DateDifference,
) : ViewModel() {

    private val _state = MutableStateFlow(DateUiState().recomputed())
    val state: StateFlow<DateUiState> = _state.asStateFlow()

    fun onFromChange(date: LocalDate) = _state.update { it.copy(from = date).recomputed() }

    fun onToChange(date: LocalDate) = _state.update { it.copy(to = date).recomputed() }

    private fun DateUiState.recomputed(): DateUiState {
        val span = dates.between(from, to)
        return copy(
            days = NumberFormatter.format(span.days.toDouble()),
            weeks = String.format(java.util.Locale.US, "%.1f", span.weeks),
            months = String.format(java.util.Locale.US, "%.1f", span.months),
            ymd = span.ymd,
        )
    }
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun DateScreen(
    state: DateUiState,
    viewModel: DateViewModel,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf<DateField?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel("Date Difference", modifier = Modifier.padding(bottom = 2.dp))

        ReadOnlyField(
            label = "From date",
            value = state.from.format(DATE_FORMAT),
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { picking = DateField.FROM },
        )
        ReadOnlyField(
            label = "To date",
            value = state.to.format(DATE_FORMAT),
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { picking = DateField.TO },
        )

        HeroResultCard(
            label = "DURATION",
            value = "${state.days} days",
            modifier = Modifier.padding(top = 6.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Weeks", state.weeks, Modifier.weight(1f), center = true)
            StatTile("Months", state.months, Modifier.weight(1f), center = true)
            StatTile("Y·M·D", state.ymd, Modifier.weight(1f), center = true)
        }
    }

    picking?.let { field ->
        DateFieldPicker(
            initial = if (field == DateField.FROM) state.from else state.to,
            onDismiss = { picking = null },
            onConfirm = { date ->
                if (field == DateField.FROM) viewModel.onFromChange(date)
                else viewModel.onToChange(date)
                picking = null
            },
        )
    }
}

private enum class DateField { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFieldPicker(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val colors = MathProTheme.colors
    // DatePicker speaks UTC millis in both directions. Seeding it from the system zone would
    // land a day early for anyone east of Greenwich (IST would open on the 14th for the 15th).
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text("OK", color = colors.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMuted) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
