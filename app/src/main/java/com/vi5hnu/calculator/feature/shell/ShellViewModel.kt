package com.vi5hnu.calculator.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vi5hnu.calculator.core.designsystem.Accent
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.domain.model.CalcMode
import com.vi5hnu.calculator.domain.model.ThemeMode
import com.vi5hnu.calculator.domain.repository.SettingsRepository
import com.vi5hnu.calculator.domain.repository.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns everything that is global to the app: which mode is showing, how it is themed, and
 * whether the history drawer is open.
 *
 * Per-mode state lives in each feature's own ViewModel.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    /**
     * Null until DataStore has produced its first value. The splash screen holds until this is
     * non-null, so the app never flashes the wrong theme or mode on launch — the same job the
     * Flutter version used flutter_native_splash's preserve/remove for.
     */
    val settingsState: StateFlow<UserSettings?> = settings.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _historyOpen = MutableStateFlow(false)
    val historyOpen: StateFlow<Boolean> = _historyOpen.asStateFlow()

    fun setMode(mode: CalcMode) = viewModelScope.launch { settings.setMode(mode) }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setAccent(accent: Accent) = viewModelScope.launch { settings.setAccent(accent) }

    fun toggleAngleUnit() = viewModelScope.launch {
        val current = settingsState.value?.angleUnit ?: AngleUnit.DEG
        settings.setAngleUnit(if (current == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG)
    }

    fun setHistoryOpen(open: Boolean) {
        _historyOpen.value = open
    }
}
