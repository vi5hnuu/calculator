package com.vi5hnu.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.domain.model.ThemeMode
import com.vi5hnu.calculator.feature.ads.AdsConsentManager
import com.vi5hnu.calculator.feature.shell.MathProApp
import com.vi5hnu.calculator.feature.shell.ShellViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val shellViewModel: ShellViewModel by viewModels()
    private lateinit var consentManager: AdsConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        // Hold the splash until the persisted theme and mode are known, so the app never
        // renders once with defaults and then snaps to the user's settings.
        splash.setKeepOnScreenCondition { shellViewModel.settingsState.value == null }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Gather consent (and, only if allowed, start the ads SDK) before the banner can show.
        consentManager = AdsConsentManager(this)
        consentManager.gatherConsentThenInitialize(this)

        setContent {
            val settings by shellViewModel.settingsState.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent

            val dark = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MathProTheme(darkTheme = dark, accent = current.accent) {
                MathProApp(
                    mode = current.mode,
                    themeMode = current.themeMode,
                    accent = current.accent,
                    shellViewModel = shellViewModel,
                    // The banner composes only once consent permits ad requests.
                    adsAllowed = consentManager.canRequestAds,
                )
            }
        }
    }
}
