package com.vi5hnu.calculator.feature.calc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.core.math.EvalError
import com.vi5hnu.calculator.core.math.EvalOutcome
import com.vi5hnu.calculator.core.math.ExpressionEngine
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.repository.HistoryRepository
import com.vi5hnu.calculator.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One committed calculation as shown on the tape above the keypad. */
data class TapeItem(
    val id: Long,
    val expression: String,
    val result: String,
)

data class CalcUiState(
    val expression: String = "",
    /** The live "= 42" under the input. Blank while the expression is incomplete. */
    val preview: String = "",
    /** Set only for expressions that are wrong rather than merely unfinished. */
    val error: String? = null,
    val tape: List<TapeItem> = emptyList(),
    val memory: Double = 0.0,
    val hasMemory: Boolean = false,
    val memoryLabel: String = "",
    /** The 2nd key: swaps the function tray to inverse functions. */
    val second: Boolean = false,
    val angleUnit: AngleUnit = AngleUnit.DEG,
)

/** Everything the keypad and function tray can emit. */
sealed interface CalcKey {
    /** Append literal text — digits, operators, `sin(`, `pi`, … */
    data class Insert(val text: String) : CalcKey
    data object Clear : CalcKey
    data object Delete : CalcKey
    data object Negate : CalcKey
    data object Equals : CalcKey
    data object ToggleSecond : CalcKey
}

/** Memory keys. */
enum class MemoryKey { MC, MR, PLUS, MINUS }

@HiltViewModel
class CalcViewModel @Inject constructor(
    private val engine: ExpressionEngine,
    private val history: HistoryRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private data class Editable(
        val expression: String = "",
        val memory: Double = 0.0,
        val hasMemory: Boolean = false,
        val second: Boolean = false,
        val lastResult: Double = 0.0,
    )

    private val editable = MutableStateFlow(Editable())

    /**
     * Held separately from [state] so that every path which evaluates an expression can reach
     * it. Reading it off [state] would be wrong for M+/M−: `state` only tracks settings while
     * the UI collects it, and dropping the unit silently evaluates sin(30°) as sin(30 rad).
     */
    private val angleUnit: StateFlow<AngleUnit> = settings.observeSettings()
        .map { it.angleUnit }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AngleUnit.DEG)

    val state: StateFlow<CalcUiState> = combine(
        editable,
        angleUnit,
        // Oldest first: the tape reads downwards like a till roll, so the newest sits nearest
        // the input. The repository orders pinned-first for the drawer, which is not what the
        // tape wants.
        history.observeHistory().map { entries ->
            // Sort by id as well as time: two calculations committed in the same millisecond
            // would otherwise keep the repository's newest-first order and read backwards.
            entries.sortedWith(compareBy({ it.createdAt }, { it.id }))
                .takeLast(TAPE_LENGTH)
                .map { entry ->
                    TapeItem(entry.id, entry.expression, NumberFormatter.format(entry.result))
                }
        },
    ) { edit, angleUnit, tape ->
        val outcome = engine.evaluate(
            expression = edit.expression,
            angleUnit = angleUnit,
            variables = mapOf("ans" to edit.lastResult),
        )
        CalcUiState(
            expression = edit.expression,
            preview = (outcome as? EvalOutcome.Success)
                ?.let { "= " + NumberFormatter.format(it.value) }
                .orEmpty(),
            error = outcome.displayableError(),
            tape = tape,
            memory = edit.memory,
            hasMemory = edit.hasMemory,
            memoryLabel = NumberFormatter.format(edit.memory),
            second = edit.second,
            angleUnit = angleUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalcUiState())

    fun onKey(key: CalcKey) {
        when (key) {
            is CalcKey.Insert -> editable.update { it.copy(expression = it.expression + key.text) }
            CalcKey.Clear -> editable.update { it.copy(expression = "") }
            CalcKey.Delete -> editable.update { it.copy(expression = it.expression.dropLast(1)) }
            CalcKey.ToggleSecond -> editable.update { it.copy(second = !it.second) }
            CalcKey.Negate -> editable.update {
                // Matches the design: flips a leading sign on the whole expression rather than
                // on the last operand.
                val e = it.expression
                it.copy(expression = if (e.startsWith("-")) e.drop(1) else "-$e")
            }
            CalcKey.Equals -> commit()
        }
    }

    fun setExpression(expression: String) {
        editable.update { it.copy(expression = expression) }
    }

    fun onMemoryKey(key: MemoryKey) {
        when (key) {
            MemoryKey.MC -> editable.update { it.copy(memory = 0.0, hasMemory = false) }
            // toLiteral, not formatPlain: the latter emits "1e+15" for a large memory, which
            // the parser reads back as 1 * e + 15 = 17.7.
            MemoryKey.MR -> editable.update {
                it.copy(expression = it.expression + NumberFormatter.toLiteral(it.memory))
            }
            MemoryKey.PLUS -> accumulateMemory { memory, value -> memory + value }
            MemoryKey.MINUS -> accumulateMemory { memory, value -> memory - value }
        }
    }

    /**
     * Adds the current value to memory. Falls back to the last result when the expression is
     * not evaluable, which is what makes M+ useful straight after pressing equals.
     */
    private fun accumulateMemory(combine: (Double, Double) -> Double) {
        editable.update { edit ->
            val current = engine
                .evaluate(
                    expression = edit.expression,
                    angleUnit = angleUnit.value,
                    variables = mapOf("ans" to edit.lastResult),
                )
                .let { (it as? EvalOutcome.Success)?.value }
                ?: edit.lastResult
            edit.copy(memory = combine(edit.memory, current), hasMemory = true)
        }
    }

    private fun commit() {
        val edit = editable.value
        val outcome = engine.evaluate(
            expression = edit.expression,
            angleUnit = angleUnit.value,
            variables = mapOf("ans" to edit.lastResult),
        )
        val value = (outcome as? EvalOutcome.Success)?.value ?: return

        viewModelScope.launch { history.add(edit.expression, value) }
        editable.update { it.copy(expression = "", lastResult = value) }
    }

    private companion object {
        const val TAPE_LENGTH = 30
    }
}

/**
 * An unfinished expression is not a mistake — the user is mid-keystroke. Only surface errors
 * that will not fix themselves with another key.
 */
private fun EvalOutcome.displayableError(): String? {
    val error = (this as? EvalOutcome.Failure)?.error ?: return null
    return when (error) {
        EvalError.EmptyExpression,
        EvalError.IncompleteExpression,
        EvalError.UnbalancedParentheses,
        -> null

        else -> error.message
    }
}
