package com.vi5hnu.calculator.feature.converter

import androidx.lifecycle.ViewModel
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.usecase.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ConverterUiState(
    val categories: List<String> = emptyList(),
    val category: String = "Length",
    val units: List<String> = emptyList(),
    val from: String = "m",
    val to: String = "ft",
    val input: String = "1",
    val result: String = "—",
    val note: String? = null,
)

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val converter: UnitConverter,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    private fun initialState(): ConverterUiState {
        val category = converter.categories.first()
        val units = category.units.keys.toList()
        return ConverterUiState(
            categories = converter.categories.map { it.name },
            category = category.name,
            units = units,
            from = units.first(),
            to = units.getOrElse(1) { units.first() },
            input = "1",
            note = category.note,
        ).recomputed()
    }

    fun onCategorySelected(name: String) = _state.update { current ->
        val category = converter.category(name)
        val units = category.units.keys.toList()
        current.copy(
            category = category.name,
            units = units,
            from = units.first(),
            to = units.getOrElse(1) { units.first() },
            note = category.note,
        ).recomputed()
    }

    fun onFromSelected(unit: String) = _state.update { it.copy(from = unit).recomputed() }

    fun onToSelected(unit: String) = _state.update { it.copy(to = unit).recomputed() }

    fun onInputChange(value: String) = _state.update { it.copy(input = value).recomputed() }

    /** Swaps the two units, keeping the typed amount in place. */
    fun onSwap() = _state.update { it.copy(from = it.to, to = it.from).recomputed() }

    private fun ConverterUiState.recomputed(): ConverterUiState {
        val value = input.toDoubleOrNull()
            ?: return copy(result = "—")
        val converted = converter.convert(category, from, to, value)
            ?: return copy(result = "—")
        return copy(result = NumberFormatter.format(converted))
    }
}
