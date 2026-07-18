package com.vi5hnu.calculator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vi5hnu.calculator.core.designsystem.Accent
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.domain.model.CalcMode
import com.vi5hnu.calculator.domain.model.ThemeMode
import com.vi5hnu.calculator.domain.repository.SettingsRepository
import com.vi5hnu.calculator.domain.repository.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = ThemeMode.fromName(prefs[Keys.THEME]),
            accent = Accent.fromName(prefs[Keys.ACCENT]),
            angleUnit = prefs[Keys.ANGLE]?.let { runCatching { AngleUnit.valueOf(it) }.getOrNull() }
                ?: AngleUnit.DEG,
            mode = CalcMode.fromName(prefs[Keys.MODE]),
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME, mode.name)

    override suspend fun setAccent(accent: Accent) = put(Keys.ACCENT, accent.name)

    override suspend fun setAngleUnit(unit: AngleUnit) = put(Keys.ANGLE, unit.name)

    override suspend fun setMode(mode: CalcMode) = put(Keys.MODE, mode.name)

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val ANGLE = stringPreferencesKey("angle_unit")
        val MODE = stringPreferencesKey("mode")
    }
}
