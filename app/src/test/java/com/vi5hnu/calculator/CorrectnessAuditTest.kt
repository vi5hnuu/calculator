package com.vi5hnu.calculator

import com.google.common.truth.Truth.assertThat
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.core.math.EvalError
import com.vi5hnu.calculator.core.math.EvalOutcome
import com.vi5hnu.calculator.core.math.ExpressionEngine
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.repository.UserSettings
import com.vi5hnu.calculator.domain.usecase.WorksheetEvaluator
import com.vi5hnu.calculator.feature.calc.CalcKey
import com.vi5hnu.calculator.feature.calc.CalcViewModel
import com.vi5hnu.calculator.feature.calc.MemoryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * A correctness audit: a calculator that shows a wrong number is worse than one that shows
 * nothing. Every test here pins a case where the app could quietly produce a plausible but
 * incorrect answer, as opposed to an obvious error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CorrectnessAuditTest {

    private val engine = ExpressionEngine()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ------------------------------------------------------------------
    // Anything the app writes back into an expression must parse back to
    // the same number. This is where "1e+15" would silently become 1*e+15.
    // ------------------------------------------------------------------

    @Test
    fun `values re-inserted into an expression round trip exactly`() {
        val values = listOf(
            0.0, 1.0, -1.0, 0.5, -0.5,
            1234.5, -1234.5,
            1_000_000.0, 1234567.0,
            1e12, 1e15, -1e15, 1e21,
            1e-6, 1e-7, 1e-12,
            0.1 + 0.2,
            Math.PI,
        )
        for (value in values) {
            val literal = NumberFormatter.toLiteral(value)
            val outcome = engine.evaluate(literal)
            assertThat(outcome).isInstanceOf(EvalOutcome.Success::class.java)
            val parsed = (outcome as EvalOutcome.Success).value
            assertThat(NumberFormatter.format(parsed)).isEqualTo(NumberFormatter.format(value))
        }
    }

    @Test
    fun `a literal never contains an e that would lex as Euler's number`() {
        // "1e+15" parses as 1 * e + 15 = 17.7, not 1000000000000000.
        assertThat(NumberFormatter.toLiteral(1e15)).doesNotContain("e")
        assertThat(NumberFormatter.toLiteral(1e-7)).doesNotContain("e")
    }

    @Test
    fun `a literal never contains grouping that would truncate at the comma`() {
        assertThat(NumberFormatter.toLiteral(1234567.0)).doesNotContain(",")
    }

    // ------------------------------------------------------------------
    // Unbound names must fail loudly. Reading them as 0 turns a typo into
    // a confident wrong answer.
    // ------------------------------------------------------------------

    @Test
    fun `an unbound variable is an error, not zero`() {
        val outcome = engine.evaluate("x+5", allowUnknownNames = true)
        assertThat(outcome).isInstanceOf(EvalOutcome.Failure::class.java)
        assertThat((outcome as EvalOutcome.Failure).error)
            .isEqualTo(EvalError.UnknownIdentifier("x"))
    }

    @Test
    fun `a mistyped worksheet name reports a dash rather than a number`() {
        val evaluator = WorksheetEvaluator(engine)
        val lines = evaluator.evaluate("qty = 4\nqtyy*3")
        // The typo must not read as 0*3 = 0.
        assertThat(lines[1].value).isNull()
        assertThat(lines[1].formatted).isEqualTo("—")
    }

    // ------------------------------------------------------------------
    // The angle unit must reach every path that evaluates trig, not just
    // the main display.
    // ------------------------------------------------------------------

    @Test
    fun `worksheet honours the angle unit`() {
        val evaluator = WorksheetEvaluator(engine)
        val deg = evaluator.evaluate("sin(30)", AngleUnit.DEG)
        assertThat(deg[0].value!!).isWithin(1e-9).of(0.5)

        val rad = evaluator.evaluate("sin(30)", AngleUnit.RAD)
        assertThat(rad[0].value!!).isWithin(1e-9).of(Math.sin(30.0))
    }

    @Test
    fun `memory plus honours the angle unit`() = runTest {
        val settings = FakeSettings(UserSettings(angleUnit = AngleUnit.DEG))
        val viewModel = CalcViewModel(engine, FakeHistory(), settings)
        launchCollector(viewModel)

        viewModel.onKey(CalcKey.Insert("sin(30)"))
        viewModel.onMemoryKey(MemoryKey.PLUS)

        // sin(30°) = 0.5. If the angle unit is dropped, this is sin(30 rad) = -0.988.
        assertThat(viewModel.state.value.memory).isWithin(1e-9).of(0.5)
    }

    @Test
    fun `equals honours the angle unit`() = runTest {
        val history = FakeHistory()
        val settings = FakeSettings(UserSettings(angleUnit = AngleUnit.DEG))
        val viewModel = CalcViewModel(engine, history, settings)
        launchCollector(viewModel)

        viewModel.onKey(CalcKey.Insert("sin(30)"))
        viewModel.onKey(CalcKey.Equals)

        assertThat(history.added.single().second).isWithin(1e-9).of(0.5)
    }

    @Test
    fun `recalling memory yields the exact stored value back`() = runTest {
        val settings = FakeSettings(UserSettings(angleUnit = AngleUnit.DEG))
        val history = FakeHistory()
        val viewModel = CalcViewModel(engine, history, settings)
        launchCollector(viewModel)

        // Bank a large number, clear, then recall it and evaluate.
        viewModel.onKey(CalcKey.Insert("1000000000000000"))
        viewModel.onMemoryKey(MemoryKey.PLUS)
        viewModel.onKey(CalcKey.Clear)
        viewModel.onMemoryKey(MemoryKey.MR)
        viewModel.onKey(CalcKey.Equals)

        assertThat(history.added.single().second).isEqualTo(1e15)
    }

    /**
     * The ViewModel's state is a `WhileSubscribed` flow, so it only recomputes while something
     * collects it. In the app that is the UI; here it has to be us. The collector must run on
     * an unconfined dispatcher, or it would sit queued behind the assertions.
     */
    private fun TestScope.launchCollector(viewModel: CalcViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }
    }
}

