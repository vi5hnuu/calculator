package com.vi5hnu.calculator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vi5hnu.calculator.core.designsystem.MathProTheme
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.core.math.ExpressionEngine
import com.vi5hnu.calculator.domain.repository.UserSettings
import com.vi5hnu.calculator.feature.calc.CalcKeypad
import com.vi5hnu.calculator.feature.calc.CalcViewModel
import com.vi5hnu.calculator.feature.calc.CalcWorkArea
import com.vi5hnu.calculator.feature.calc.FunctionTray
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives the real keypad against the real ViewModel.
 *
 * The unit tests prove the engine is right; these prove the buttons are wired to it. A key
 * mapped to the wrong action would pass every test in the other suites and still hand the
 * user a wrong number.
 *
 * Runs on the JVM under Robolectric, so it stays part of `testDebugUnitTest` and needs no
 * emulator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class KeypadUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun launch(angleUnit: AngleUnit = AngleUnit.DEG): CalcViewModel {
        val viewModel = CalcViewModel(
            engine = ExpressionEngine(),
            history = FakeHistory(),
            settings = FakeSettings(UserSettings(angleUnit = angleUnit)),
        )
        compose.setContent {
            MathProTheme(darkTheme = true) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize()) {
                    CalcWorkArea(state = state, onReuse = {}, modifier = Modifier.weight(1f))
                    FunctionTray(
                        second = state.second,
                        angleLabel = state.angleUnit.label,
                        onKey = viewModel::onKey,
                        onToggleAngle = {},
                    )
                    CalcKeypad(onKey = viewModel::onKey, onMemory = viewModel::onMemoryKey)
                }
            }
        }
        return viewModel
    }

    /** Taps a digit or operator by its printed label. */
    private fun tap(label: String) = compose.onNodeWithText(label).performClick()

    /** Taps a key whose label is a glyph, by the name a screen reader would read. */
    private fun tapSpoken(description: String) =
        compose.onNodeWithContentDescription(description).performClick()

    @Test
    fun `digits and plus produce the right sum`() {
        launch()
        tap("7"); tapSpoken("Plus"); tap("8")
        compose.onNodeWithText("= 15").assertIsDisplayed()
    }

    @Test
    fun `each arithmetic key is wired to its own operation`() {
        // Guards against a copy-paste slip that maps × to + or similar: every one of these
        // would still pass an engine-level test.
        launch()
        tap("8"); tapSpoken("Divide"); tap("2")
        compose.onNodeWithText("= 4").assertIsDisplayed()

        tapSpoken("All clear")
        tap("8"); tapSpoken("Multiply"); tap("2")
        compose.onNodeWithText("= 16").assertIsDisplayed()

        tapSpoken("All clear")
        tap("8"); tapSpoken("Minus"); tap("2")
        compose.onNodeWithText("= 6").assertIsDisplayed()

        tapSpoken("All clear")
        tap("8"); tapSpoken("Modulo"); tap("3")
        compose.onNodeWithText("= 2").assertIsDisplayed()
    }

    @Test
    fun `equals commits the result to the tape`() {
        launch()
        tap("6"); tapSpoken("Multiply"); tap("7"); tapSpoken("Equals")
        compose.onNodeWithText("= 42").assertIsDisplayed()
        // The input clears after commit, so the tape is the only thing showing 42.
        compose.onNodeWithText("6×7").assertIsDisplayed()
    }

    @Test
    fun `delete removes only the last character`() {
        launch()
        tap("1"); tap("2"); tap("3")
        tapSpoken("Delete")
        compose.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun `all clear empties the input`() {
        launch()
        tap("1"); tap("2")
        compose.onNodeWithText("= 12").assertIsDisplayed()
        tapSpoken("All clear")
        // Asserting on the placeholder "0" would be ambiguous — the keypad has a 0 key too.
        compose.onNodeWithText("12").assertDoesNotExist()
        compose.onNodeWithText("= 12").assertDoesNotExist()
    }

    @Test
    fun `the decimal point key is wired`() {
        launch()
        tap("1"); tapSpoken("Decimal point"); tap("5")
        compose.onNodeWithText("= 1.5").assertIsDisplayed()
    }

    @Test
    fun `the sign key negates`() {
        launch()
        tap("5"); tapSpoken("Toggle sign")
        compose.onNodeWithText("= -5").assertIsDisplayed()
    }

    @Test
    fun `the keypad respects the degree setting end to end`() {
        // The whole angle-unit chain: setting -> ViewModel -> engine -> displayed result.
        launch(AngleUnit.DEG)
        tapSpoken("Sine"); tap("3"); tap("0"); tapSpoken("Close bracket")
        compose.onNodeWithText("= 0.5").assertIsDisplayed()
    }

    @Test
    fun `the same expression in radians gives the radian answer`() {
        launch(AngleUnit.RAD)
        tapSpoken("Sine"); tap("3"); tap("0"); tapSpoken("Close bracket")
        compose.onNodeWithText("= -0.9880316241").assertIsDisplayed()
    }

    @Test
    fun `the second toggle swaps the trig keys for their inverses`() {
        launch(AngleUnit.DEG)
        compose.onNodeWithText("sin").assertIsDisplayed()
        tapSpoken("Second functions, off")
        // asin(0.5) = 30° — proves the shifted key inserts asin( and not sin(.
        tapSpoken("Arc sine"); tapSpoken("Decimal point"); tap("5"); tapSpoken("Close bracket")
        compose.onNodeWithText("= 30").assertIsDisplayed()
    }

    @Test
    fun `memory keys round trip a value through the display`() {
        launch()
        tap("4"); tap("2")
        tapSpoken("Memory add")
        tapSpoken("All clear")
        tapSpoken("Memory recall")
        compose.onNodeWithText("= 42").assertIsDisplayed()
    }

    @Test
    fun `memory clear empties the memory indicator`() {
        launch()
        tap("9")
        tapSpoken("Memory add")
        compose.onNodeWithText("M = 9").assertIsDisplayed()
        tapSpoken("Memory clear")
        compose.onNodeWithText("M = 9").assertDoesNotExist()
    }
}
