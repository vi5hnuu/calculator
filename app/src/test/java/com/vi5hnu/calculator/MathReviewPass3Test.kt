package com.vi5hnu.calculator

import com.google.common.truth.Truth.assertThat
import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.core.math.EvalError
import com.vi5hnu.calculator.core.math.EvalOutcome
import com.vi5hnu.calculator.core.math.ExpressionEngine
import com.vi5hnu.calculator.core.math.NumberFormatter
import org.junit.Test

/**
 * Third math pass, going after the corners the earlier suites did not touch: modulo
 * precedence and sign, power/unary interaction, factorial binding, degenerate parentheses,
 * function application without brackets, and formatter round-off at the display boundary.
 *
 * Each case is the actual keystroke sequence a user could type, checked against the
 * mathematically correct answer — not against the design mock, which is wrong in several of
 * these.
 */
class MathReviewPass3Test {

    private val engine = ExpressionEngine()

    private fun value(expr: String, angle: AngleUnit = AngleUnit.RAD): Double {
        val outcome = engine.evaluate(expr, angle)
        assertThat(outcome).isInstanceOf(EvalOutcome.Success::class.java)
        return (outcome as EvalOutcome.Success).value
    }

    private fun error(expr: String): EvalError =
        (engine.evaluate(expr) as EvalOutcome.Failure).error

    private fun shown(expr: String, angle: AngleUnit = AngleUnit.RAD): String =
        NumberFormatter.format(value(expr, angle))

    // ---------------------------------------------------------------- modulo

    @Test
    fun `modulo shares multiplication precedence and reads left to right`() {
        assertThat(value("10%3+1")).isEqualTo(2.0)   // (10%3)+1
        assertThat(value("1+10%3")).isEqualTo(2.0)   // 1+(10%3)
        assertThat(value("2*10%3")).isEqualTo(2.0)   // (2*10)%3 = 20%3
        assertThat(value("10%3*2")).isEqualTo(2.0)   // (10%3)*2
    }

    @Test
    fun `modulo follows Kotlin sign and works on reals`() {
        assertThat(value("-7%3")).isEqualTo(-1.0)   // truncated remainder keeps dividend sign
        assertThat(value("7%-3")).isEqualTo(1.0)
        assertThat(value("5.5%2")).isEqualTo(1.5)
    }

    @Test
    fun `modulo by zero is undefined, not a crash`() {
        assertThat(error("10%0")).isEqualTo(EvalError.UndefinedResult)
    }

    // ------------------------------------------------ power and unary minus

    @Test
    fun `unary minus and power compose the standard way`() {
        assertThat(value("-2^-2")).isEqualTo(-0.25)  // -(2^(-2))
        assertThat(value("-2^2")).isEqualTo(-4.0)     // -(2^2)
        assertThat(value("2^-2^2")).isEqualTo(0.0625) // 2^(-(2^2)) = 2^-4
        assertThat(value("(-2)^2")).isEqualTo(4.0)
    }

    @Test
    fun `zero to the zero follows the platform convention`() {
        // Math.pow(0,0) is 1; documenting the choice so a change is deliberate.
        assertThat(value("0^0")).isEqualTo(1.0)
    }

    // ------------------------------------------------------------ factorial

    @Test
    fun `factorial binds tighter than power and unary minus`() {
        assertThat(value("2^3!")).isEqualTo(64.0)   // 2^(3!) = 2^6
        assertThat(value("-3!")).isEqualTo(-6.0)     // -(3!)
    }

    @Test
    fun `factorial applies to a parenthesised group`() {
        assertThat(value("(2+3)!")).isEqualTo(120.0)
        assertThat(value("3!!")).isEqualTo(720.0)    // (3!)! = 6! = 720
    }

    // --------------------------------------------- function application forms

    @Test
    fun `an identifier may contain digits, so sin2 is a name and not sin of 2`() {
        // Identifiers allow trailing digits (worksheet variables like x2, item1), so `sin2` is
        // one unknown name rather than sin applied to 2. The keypad always inserts `sin(`, so a
        // bare `sin2` only arises from free text, where an error is the right answer.
        assertThat(error("sin2")).isEqualTo(EvalError.UnknownIdentifier("sin2"))
        assertThat(value("sin(2)", AngleUnit.RAD)).isWithin(1e-9).of(Math.sin(2.0))
    }

    @Test
    fun `a value juxtaposed before a function needs the function to have an argument`() {
        // 2sin has an implicit multiply inserted, but sin then has no operand -> incomplete.
        assertThat(error("2sin")).isEqualTo(EvalError.IncompleteExpression)
    }

    @Test
    fun `a bare function name with no argument is incomplete`() {
        assertThat(error("sin")).isEqualTo(EvalError.IncompleteExpression)
        assertThat(error("sin+3")).isEqualTo(EvalError.IncompleteExpression)
    }

    // ---------------------------------------------------- degenerate brackets

    @Test
    fun `empty parentheses are not a value`() {
        assertThat(error("()")).isEqualTo(EvalError.IncompleteExpression)
        assertThat(error("sin()")).isEqualTo(EvalError.IncompleteExpression)
        assertThat(error("2*()")).isEqualTo(EvalError.IncompleteExpression)
    }

    @Test
    fun `nested and redundant brackets collapse correctly`() {
        assertThat(value("((((5))))")).isEqualTo(5.0)
        assertThat(value("((2+3))*((4))")).isEqualTo(20.0)
    }

    // --------------------------------------------------- leading decimal forms

    @Test
    fun `leading and trailing decimal points parse`() {
        assertThat(value(".5")).isEqualTo(0.5)
        assertThat(value("5.")).isEqualTo(5.0)
        assertThat(value(".5+.5")).isEqualTo(1.0)
    }

    @Test
    fun `a double decimal point is rejected`() {
        assertThat(error("5..3")).isInstanceOf(EvalError.MalformedNumber::class.java)
    }

    // ------------------------------------------------- floating-point display

    @Test
    fun `results that should be whole are shown whole`() {
        // Classic cancellation traps — each is off by ULPs before rounding.
        assertThat(shown("1/3*3")).isEqualTo("1")
        assertThat(shown("0.1+0.1+0.1")).isEqualTo("0.3")
        assertThat(shown("sqrt(2)^2")).isEqualTo("2")
        assertThat(shown("cbrt(27)")).isEqualTo("3")
    }

    @Test
    fun `trig identities land on clean values in degrees`() {
        assertThat(shown("sin(30)", AngleUnit.DEG)).isEqualTo("0.5")
        assertThat(shown("cos(60)", AngleUnit.DEG)).isEqualTo("0.5")
        assertThat(shown("sin(90)", AngleUnit.DEG)).isEqualTo("1")
        assertThat(shown("tan(45)", AngleUnit.DEG)).isEqualTo("1")
        assertThat(shown("cos(90)", AngleUnit.DEG)).isEqualTo("0") // ~6.1e-17, clamped to 0
    }

    // ------------------------------------------------------ infinities and ans

    @Test
    fun `division by zero and log of zero surface as signed infinity`() {
        assertThat(shown("1/0")).isEqualTo("∞")
        assertThat(shown("-1/0")).isEqualTo("-∞")
        assertThat(shown("ln(0)")).isEqualTo("-∞")
    }

    @Test
    fun `ans resolves to its binding, which the calculator seeds to zero`() {
        // The engine errors on an unbound name (typo protection); callers bind ans. The
        // calculator binds it to the last result, defaulting to 0 before any calculation.
        val outcome = engine.evaluate("ans+5", variables = mapOf("ans" to 0.0))
        assertThat((outcome as EvalOutcome.Success).value).isEqualTo(5.0)
        // Unbound, it is a typo, not a silent zero.
        assertThat(error("ans+5")).isEqualTo(EvalError.UnknownIdentifier("ans"))
    }

    @Test
    fun `constants combine with implicit multiplication when the digit leads`() {
        assertThat(value("2pi", AngleUnit.RAD)).isWithin(1e-9).of(2 * Math.PI)
        assertThat(value("2e", AngleUnit.RAD)).isWithin(1e-9).of(2 * Math.E)
        // pi2 is a single identifier (digits are legal in names), hence an unknown name — the
        // deliberate cost of supporting variables like x2. `2pi` and `pi*2` both work.
        assertThat(error("pi2")).isEqualTo(EvalError.UnknownIdentifier("pi2"))
        assertThat(value("pi*2", AngleUnit.RAD)).isWithin(1e-9).of(Math.PI * 2)
    }
}
