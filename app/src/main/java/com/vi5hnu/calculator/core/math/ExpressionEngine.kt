package com.vi5hnu.calculator.core.math

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one entry point to the maths engine.
 *
 * Stateless and free of Android dependencies, so it is exercised entirely by plain JVM unit
 * tests. Everything except Programmer mode routes through here.
 */
@Singleton
class ExpressionEngine @Inject constructor() {

    /**
     * @param angleUnit how trig arguments are read. Graphing forces [AngleUnit.RAD] regardless
     *   of the user's setting, because a curve plotted in degrees is meaningless.
     * @param variables bindings for `x`, `ans`, and any worksheet names.
     * @param allowUnknownNames when true an unrecognised identifier becomes a variable rather
     *   than an error. Worksheet mode needs this; the keypad does not.
     */
    fun evaluate(
        expression: String,
        angleUnit: AngleUnit = AngleUnit.RAD,
        variables: Map<String, Double> = emptyMap(),
        allowUnknownNames: Boolean = false,
    ): EvalOutcome {
        val source = normalise(expression)
        if (source.isBlank()) return EvalOutcome.Failure(EvalError.EmptyExpression)

        return try {
            val tokens = Lexer(allowUnknownNames).tokenize(source)
            val rpn = ShuntingYard.toRpn(Preprocessor.process(tokens))
            val value = RpnEvaluator(angleUnit, variables).evaluate(rpn)

            // Infinity is a legitimate answer (1/0) and formats as ∞; NaN never is.
            if (value.isNaN()) EvalOutcome.Failure(EvalError.UndefinedResult)
            else EvalOutcome.Success(value)
        } catch (e: LexException) {
            EvalOutcome.Failure(e.error)
        } catch (e: ParseException) {
            EvalOutcome.Failure(e.error)
        }
    }

    /** Accepts the prettified symbols the keypad inserts. */
    private fun normalise(raw: String): String = raw
        .replace('×', '*')
        .replace('÷', '/')
        .replace('−', '-')
        .replace("π", "pi")
        .replace("√", "sqrt")
}
