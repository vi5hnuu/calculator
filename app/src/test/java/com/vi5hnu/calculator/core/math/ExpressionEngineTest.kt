package com.vi5hnu.calculator.core.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExpressionEngineTest {

    private val engine = ExpressionEngine()

    private fun eval(
        expression: String,
        angleUnit: AngleUnit = AngleUnit.RAD,
        variables: Map<String, Double> = emptyMap(),
        allowUnknownNames: Boolean = false,
    ): Double {
        val outcome = engine.evaluate(expression, angleUnit, variables, allowUnknownNames)
        assertThat(outcome).isInstanceOf(EvalOutcome.Success::class.java)
        return (outcome as EvalOutcome.Success).value
    }

    private fun errorOf(expression: String, allowUnknownNames: Boolean = false): EvalError {
        val outcome = engine.evaluate(
            expression,
            allowUnknownNames = allowUnknownNames,
        )
        assertThat(outcome).isInstanceOf(EvalOutcome.Failure::class.java)
        return (outcome as EvalOutcome.Failure).error
    }

    // ---------------------------------------------------------------- basics

    @Test
    fun `evaluates simple arithmetic`() {
        assertThat(eval("2+3")).isEqualTo(5.0)
        assertThat(eval("10-4")).isEqualTo(6.0)
        assertThat(eval("6*7")).isEqualTo(42.0)
        assertThat(eval("9/2")).isEqualTo(4.5)
    }

    @Test
    fun `honours operator precedence`() {
        assertThat(eval("2+3*4")).isEqualTo(14.0)
        assertThat(eval("(2+3)*4")).isEqualTo(20.0)
        assertThat(eval("2+10*2+3")).isEqualTo(25.0)
    }

    @Test
    fun `percent is remainder, matching the design and the previous engine`() {
        assertThat(eval("10%3")).isEqualTo(1.0)
        assertThat(eval("10%2")).isEqualTo(0.0)
    }

    @Test
    fun `power is right associative`() {
        assertThat(eval("2^3^2")).isEqualTo(512.0) // 2^(3^2), not (2^3)^2 = 64
    }

    // ------------------------------------------------- unary minus / signs
    //
    // The cases below are the ones commit 665f2d4 had to patch in the old Dart engine.

    @Test
    fun `leading plus is ignored`() {
        assertThat(eval("+2+3")).isEqualTo(5.0)
    }

    @Test
    fun `double minus is subtraction of a negative`() {
        assertThat(eval("3--3")).isEqualTo(6.0)
    }

    @Test
    fun `stacked signs resolve rather than failing`() {
        // The old Dart engine threw on this; a correct parser reads 2 - (-(-3)) = -1.
        assertThat(eval("2---3")).isEqualTo(-1.0)
    }

    @Test
    fun `leading minus negates`() {
        assertThat(eval("-5")).isEqualTo(-5.0)
        assertThat(eval("-5+2")).isEqualTo(-3.0)
    }

    @Test
    fun `unary minus binds looser than power`() {
        assertThat(eval("-2^2")).isEqualTo(-4.0) // -(2^2)
    }

    @Test
    fun `negative exponent parses`() {
        // The design's own engine returns NaN here; the eager-pop bug is fixed in ShuntingYard.
        assertThat(eval("2^-3")).isEqualTo(0.125)
    }

    // ------------------------------------------------ implicit multiplication

    @Test
    fun `inserts implied multiplication`() {
        assertThat(eval("2pi")).isWithin(1e-9).of(2 * Math.PI)
        assertThat(eval("3(4)")).isEqualTo(12.0)
        assertThat(eval("(1+2)(3)")).isEqualTo(9.0)
        assertThat(eval("2sqrt(9)")).isEqualTo(6.0)
    }

    // ------------------------------------------------------------ factorial

    @Test
    fun `computes factorial`() {
        assertThat(eval("5!")).isEqualTo(120.0)
        assertThat(eval("0!")).isEqualTo(1.0)
        assertThat(eval("3+5!")).isEqualTo(123.0)
        assertThat(eval("2^3!")).isEqualTo(64.0) // ! binds tighter than ^
    }

    @Test
    fun `factorial of a non-integer is undefined`() {
        assertThat(errorOf("2.5!")).isEqualTo(EvalError.UndefinedResult)
        assertThat(errorOf("(-1)!")).isEqualTo(EvalError.UndefinedResult)
    }

    @Test
    fun `factorial saturates past the Double range`() {
        assertThat(eval("171!")).isPositiveInfinity()
    }

    // ------------------------------------------------------------ functions

    @Test
    fun `trig respects degrees`() {
        assertThat(eval("sin(30)", AngleUnit.DEG)).isWithin(1e-9).of(0.5)
        assertThat(eval("cos(0)", AngleUnit.DEG)).isWithin(1e-9).of(1.0)
        assertThat(eval("asin(0.5)", AngleUnit.DEG)).isWithin(1e-9).of(30.0)
    }

    @Test
    fun `trig respects radians`() {
        assertThat(eval("sin(pi/2)", AngleUnit.RAD)).isWithin(1e-9).of(1.0)
        assertThat(eval("atan(1)", AngleUnit.RAD)).isWithin(1e-9).of(Math.PI / 4)
    }

    @Test
    fun `hyperbolic functions ignore the angle unit`() {
        assertThat(eval("sinh(0)", AngleUnit.DEG)).isEqualTo(0.0)
        assertThat(eval("cosh(0)", AngleUnit.DEG)).isEqualTo(1.0)
        assertThat(eval("tanh(0)", AngleUnit.RAD)).isEqualTo(0.0)
    }

    @Test
    fun `supports the remaining function set`() {
        assertThat(eval("log(100)")).isWithin(1e-9).of(2.0)
        assertThat(eval("ln(e)")).isWithin(1e-9).of(1.0)
        assertThat(eval("sqrt(16)")).isEqualTo(4.0)
        assertThat(eval("cbrt(27)")).isWithin(1e-9).of(3.0)
        assertThat(eval("abs(0-7)")).isEqualTo(7.0)
        assertThat(eval("exp(0)")).isEqualTo(1.0)
        assertThat(eval("floor(2.7)")).isEqualTo(2.0)
        assertThat(eval("ceil(2.1)")).isEqualTo(3.0)
        assertThat(eval("round(2.5)")).isEqualTo(3.0)
    }

    @Test
    fun `round breaks ties upwards, not towards even`() {
        // kotlin.math.round would give 2 and 4 here. Guards against a silent regression if
        // anyone "simplifies" MathFunctions back to the stdlib call.
        assertThat(eval("round(2.5)")).isEqualTo(3.0)
        assertThat(eval("round(3.5)")).isEqualTo(4.0)
        assertThat(eval("round(-2.5)")).isEqualTo(-2.0)
        assertThat(eval("round(2.4)")).isEqualTo(2.0)
    }

    @Test
    fun `nested function calls evaluate`() {
        assertThat(eval("sqrt(abs(0-16))")).isEqualTo(4.0)
    }

    @Test
    fun `domain violations report undefined`() {
        assertThat(errorOf("sqrt(0-1)")).isEqualTo(EvalError.UndefinedResult)
        assertThat(errorOf("ln(0-1)")).isEqualTo(EvalError.UndefinedResult)
    }

    // ------------------------------------------------------------ constants and variables

    @Test
    fun `resolves constants`() {
        assertThat(eval("pi")).isWithin(1e-12).of(Math.PI)
        assertThat(eval("e")).isWithin(1e-12).of(Math.E)
    }

    @Test
    fun `binds variables`() {
        assertThat(eval("x*2", variables = mapOf("x" to 21.0))).isEqualTo(42.0)
        assertThat(eval("ans+1", variables = mapOf("ans" to 9.0))).isEqualTo(10.0)
    }

    @Test
    fun `unbound variables are an error rather than a silent zero`() {
        // Defaulting to 0 (as the design mock does) would turn a mistyped name into a
        // confident wrong answer instead of a visible failure.
        assertThat(errorOf("x+5")).isEqualTo(EvalError.UnknownIdentifier("x"))
        assertThat(errorOf("ans+1")).isEqualTo(EvalError.UnknownIdentifier("ans"))
    }

    @Test
    fun `unknown names are rejected unless variables are allowed`() {
        assertThat(errorOf("foo+1")).isEqualTo(EvalError.UnknownIdentifier("foo"))
        assertThat(eval("foo+1", variables = mapOf("foo" to 1.0), allowUnknownNames = true))
            .isEqualTo(2.0)
    }

    // ------------------------------------------------------------ symbols

    @Test
    fun `accepts the pretty symbols the keypad inserts`() {
        assertThat(eval("6×7")).isEqualTo(42.0)
        assertThat(eval("8÷2")).isEqualTo(4.0)
        assertThat(eval("5−3")).isEqualTo(2.0)
        assertThat(eval("√(9)")).isEqualTo(3.0)
        assertThat(eval("π")).isWithin(1e-12).of(Math.PI)
    }

    // ------------------------------------------------------------ errors

    @Test
    fun `empty input is reported as empty, not as an error`() {
        assertThat(errorOf("")).isEqualTo(EvalError.EmptyExpression)
        assertThat(errorOf("   ")).isEqualTo(EvalError.EmptyExpression)
    }

    @Test
    fun `dangling operators are incomplete`() {
        assertThat(errorOf("2+")).isEqualTo(EvalError.IncompleteExpression)
        assertThat(errorOf("*5")).isEqualTo(EvalError.IncompleteExpression)
        // Another regression from the old Dart engine's comments.
        assertThat(errorOf("+0++2**3")).isEqualTo(EvalError.IncompleteExpression)
    }

    @Test
    fun `unbalanced brackets are reported`() {
        assertThat(errorOf("(2+3")).isEqualTo(EvalError.UnbalancedParentheses)
        assertThat(errorOf("2+3)")).isEqualTo(EvalError.UnbalancedParentheses)
    }

    @Test
    fun `malformed numbers are rejected`() {
        // parseFloat in the design would read this as 1.2 and carry on regardless.
        assertThat(errorOf("1.2.3")).isEqualTo(EvalError.MalformedNumber("1.2.3"))
    }

    @Test
    fun `unexpected characters are reported with the offender`() {
        val error = errorOf("2 # 3")
        assertThat(error).isInstanceOf(EvalError.UnexpectedCharacter::class.java)
        assertThat((error as EvalError.UnexpectedCharacter).character).isEqualTo('#')
    }

    @Test
    fun `division by zero is infinity rather than a failure`() {
        assertThat(eval("1/0")).isPositiveInfinity()
        assertThat(errorOf("0/0")).isEqualTo(EvalError.UndefinedResult)
    }
}
