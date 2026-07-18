package com.vi5hnu.calculator

import com.vi5hnu.calculator.core.designsystem.Accent
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.domain.model.CalcMode
import com.vi5hnu.calculator.domain.model.HistoryEntry
import com.vi5hnu.calculator.domain.model.ThemeMode
import com.vi5hnu.calculator.domain.repository.HistoryRepository
import com.vi5hnu.calculator.domain.repository.SettingsRepository
import com.vi5hnu.calculator.domain.repository.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory stand-ins, shared across the test suites. */

internal class FakeHistory : HistoryRepository {

    /** Every (expression, result) that reached the repository, in order. */
    val added = mutableListOf<Pair<String, Double>>()

    private val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun observeHistory(): Flow<List<HistoryEntry>> = entries.asStateFlow()

    override suspend fun add(expression: String, result: Double): Long {
        added += expression to result
        entries.update {
            it + HistoryEntry(id = it.size + 1L, expression = expression, result = result)
        }
        return entries.value.size.toLong()
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        entries.update { rows -> rows.map { if (it.id == id) it.copy(pinned = pinned) else it } }
    }

    override suspend fun delete(id: Long) {
        entries.update { rows -> rows.filterNot { it.id == id } }
    }

    override suspend fun clear() {
        entries.value = emptyList()
    }
}

internal class FakeSettings(initial: UserSettings = UserSettings()) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    override fun observeSettings(): Flow<UserSettings> = state.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setAccent(accent: Accent) = state.update { it.copy(accent = accent) }

    override suspend fun setAngleUnit(unit: AngleUnit) = state.update { it.copy(angleUnit = unit) }

    override suspend fun setMode(mode: CalcMode) = state.update { it.copy(mode = mode) }
}
