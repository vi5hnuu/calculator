package com.vi5hnu.calculator.feature.programmer

import androidx.lifecycle.ViewModel
import com.vi5hnu.calculator.domain.model.NumberBase
import com.vi5hnu.calculator.domain.usecase.ProgOp
import com.vi5hnu.calculator.domain.usecase.ProgrammerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BaseRow(
    val base: NumberBase,
    val value: String,
    val selected: Boolean,
)

data class ProgrammerUiState(
    val base: NumberBase = NumberBase.DEC,
    val entry: String = "0",
    val rows: List<BaseRow> = emptyList(),
    /** Digits illegal in the current base are disabled rather than hidden. */
    val enabledDigits: Set<Char> = emptySet(),
)

@HiltViewModel
class ProgrammerViewModel @Inject constructor(
    private val engine: ProgrammerEngine,
) : ViewModel() {

    private data class Session(
        val base: NumberBase = NumberBase.DEC,
        val entry: String = "0",
        val accumulator: Int? = null,
        val pendingOp: ProgOp? = null,
        /** True when the next digit should replace the entry rather than extend it. */
        val overwrite: Boolean = true,
    )

    private val session = MutableStateFlow(Session())

    private val _state = MutableStateFlow(ProgrammerUiState())
    val state: StateFlow<ProgrammerUiState> = _state.asStateFlow()

    init {
        session.value.let { render(it) }
    }

    fun onBaseSelected(base: NumberBase) = session.update { current ->
        // Re-render the same number in the new base rather than reinterpreting the digits.
        val value = engine.parse(current.entry, current.base)
        current.copy(
            base = base,
            entry = engine.format(value, base),
            overwrite = true,
        ).also(::render)
    }

    fun onDigit(digit: Char) = session.update { current ->
        if (digit !in current.base.digits) return@update current
        val entry = when {
            current.overwrite || current.entry == "0" -> digit.toString()
            else -> current.entry + digit
        }
        // Refuse anything the register cannot hold, rather than let the entry wrap and
        // contradict the readouts beneath it.
        if (!engine.accepts(entry, current.base)) return@update current
        current.copy(entry = entry, overwrite = false).also(::render)
    }

    fun onClearEntry() = session.update {
        Session(base = it.base).also(::render)
    }

    fun onDelete() = session.update { current ->
        current.copy(entry = current.entry.dropLast(1).ifEmpty { "0" }).also(::render)
    }

    fun onNot() = session.update { current ->
        val value = engine.parse(current.entry, current.base).inv()
        current.copy(entry = engine.format(value, current.base), overwrite = true).also(::render)
    }

    fun onNegate() = session.update { current ->
        val value = -engine.parse(current.entry, current.base)
        current.copy(entry = engine.format(value, current.base), overwrite = true).also(::render)
    }

    fun onOperator(op: ProgOp) = session.update { current ->
        val entered = engine.parse(current.entry, current.base)

        // Chaining "2 + 3 + 4" folds the pending operation before recording the new one, so
        // the display keeps up without waiting for equals.
        val accumulator = if (current.pendingOp != null && !current.overwrite) {
            engine.apply(current.accumulator ?: 0, current.pendingOp, entered)
        } else {
            entered
        }

        current.copy(
            entry = engine.format(accumulator, current.base),
            accumulator = accumulator,
            pendingOp = op,
            overwrite = true,
        ).also(::render)
    }

    fun onEquals() = session.update { current ->
        val op = current.pendingOp ?: return@update current
        val result = engine.apply(
            current.accumulator ?: 0,
            op,
            engine.parse(current.entry, current.base),
        )
        current.copy(
            entry = engine.format(result, current.base),
            accumulator = null,
            pendingOp = null,
            overwrite = true,
        ).also(::render)
    }

    private fun render(session: Session) {
        val value = engine.parse(session.entry, session.base)
        _state.value = ProgrammerUiState(
            base = session.base,
            entry = session.entry,
            rows = NumberBase.entries.map { base ->
                BaseRow(
                    base = base,
                    value = engine.grouped(value, base),
                    selected = base == session.base,
                )
            },
            enabledDigits = session.base.digits.toSet(),
        )
    }
}
