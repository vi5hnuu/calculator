package com.vi5hnu.calculator.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryRow(
    val id: Long,
    val expression: String,
    val result: String,
    val pinned: Boolean,
    val createdAt: Long,
)

data class HistoryUiState(
    val rows: List<HistoryRow> = emptyList(),
    val query: String = "",
    /** True only when the store itself is empty, not when a search merely matches nothing. */
    val isEmpty: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val history: HistoryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    val queryState: StateFlow<String> = query.asStateFlow()

    val state: StateFlow<HistoryUiState> =
        combine(history.observeHistory(), query) { entries, search ->
            val rows = entries
                .filter { entry ->
                    search.isBlank() ||
                        entry.expression.contains(search, ignoreCase = true) ||
                        NumberFormatter.format(entry.result).contains(search, ignoreCase = true)
                }
                .map { entry ->
                    HistoryRow(
                        id = entry.id,
                        expression = entry.expression,
                        result = NumberFormatter.format(entry.result),
                        pinned = entry.pinned,
                        createdAt = entry.createdAt,
                    )
                }
            HistoryUiState(rows = rows, query = search, isEmpty = entries.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun togglePin(id: Long, pinned: Boolean) = viewModelScope.launch {
        history.setPinned(id, !pinned)
    }

    fun delete(id: Long) = viewModelScope.launch { history.delete(id) }

    fun clear() = viewModelScope.launch { history.clear() }
}
