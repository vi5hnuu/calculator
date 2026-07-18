package com.vi5hnu.calculator.core.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/** Whether trigonometric arguments and results are read as degrees or radians. */
enum class AngleUnit {
    DEG,
    RAD;

    val label: String get() = name
}

/**
 * The single-argument functions the calculator understands.
 *
 * Mirrors the design's `FNAMES` list exactly, so any expression written against the mock
 * evaluates identically here.
 */
internal object MathFunctions {

    val NAMES: Set<String> = setOf(
        "sin", "cos", "tan",
        "asin", "acos", "atan",
        "sinh", "cosh", "tanh",
        "log", "ln",
        "sqrt", "cbrt", "abs", "exp",
        "floor", "ceil", "round",
    )

    /**
     * Applies [name] to [argument].
     *
     * Only the circular trig functions respect [angleUnit] — hyperbolic functions take a real
     * argument, and the rest are unit-agnostic. Domain errors surface as NaN and are caught by
     * the evaluator rather than throwing here.
     */
    fun call(name: String, argument: Double, angleUnit: AngleUnit): Double {
        val degrees = angleUnit == AngleUnit.DEG
        fun toRadians(v: Double) = if (degrees) v * PI / 180.0 else v
        fun fromRadians(v: Double) = if (degrees) v * 180.0 / PI else v

        return when (name) {
            "sin" -> sin(toRadians(argument))
            "cos" -> cos(toRadians(argument))
            "tan" -> tan(toRadians(argument))
            "asin" -> fromRadians(asin(argument))
            "acos" -> fromRadians(acos(argument))
            "atan" -> fromRadians(atan(argument))
            "sinh" -> sinh(argument)
            "cosh" -> cosh(argument)
            "tanh" -> tanh(argument)
            "log" -> log10(argument)
            "ln" -> ln(argument)
            "sqrt" -> sqrt(argument)
            "cbrt" -> cbrt(argument)
            "abs" -> abs(argument)
            "exp" -> exp(argument)
            "floor" -> floor(argument)
            "ceil" -> ceil(argument)
            // Deliberately not kotlin.math.round, which breaks ties towards the even integer
            // and so makes round(2.5) = 2. Calculator users expect ties to go up.
            "round" -> floor(argument + 0.5)
            else -> Double.NaN
        }
    }

    /**
     * Factorial via the gamma-free integer path only.
     *
     * Non-integers and negatives are undefined (NaN); 171! overflows a Double, so anything
     * above 170 saturates to infinity exactly as the design does.
     */
    fun factorial(n: Double): Double {
        if (n < 0 || n != floor(n) || n.isNaN()) return Double.NaN
        if (n > 170) return Double.POSITIVE_INFINITY
        var result = 1.0
        for (i in 2..n.toInt()) result *= i
        return result
    }
}
