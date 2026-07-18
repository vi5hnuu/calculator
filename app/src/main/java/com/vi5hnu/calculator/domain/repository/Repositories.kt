package com.vi5hnu.calculator.domain.repository

import com.vi5hnu.calculator.core.designsystem.Accent
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.domain.model.CalcMode
import com.vi5hnu.calculator.domain.model.HistoryEntry
import com.vi5hnu.calculator.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts live in `domain` and are implemented in `data`, so nothing above this
 * layer knows that history is Room or that settings are DataStore.
 */
interface HistoryRepository {

    /** All entries, pinned first, then newest first. */
    fun observeHistory(): Flow<List<HistoryEntry>>

    suspend fun add(expression: String, result: Double): Long

    suspend fun setPinned(id: Long, pinned: Boolean)

    suspend fun delete(id: Long)

    suspend fun clear()
}

interface SettingsRepository {

    fun observeSettings(): Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setAccent(accent: Accent)

    suspend fun setAngleUnit(unit: AngleUnit)

    suspend fun setMode(mode: CalcMode)
}

/** Everything persisted about how the app should look and behave on next launch. */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.Default,
    val accent: Accent = Accent.Default,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val mode: CalcMode = CalcMode.Default,
)
