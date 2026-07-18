package com.vi5hnu.calculator.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/**
 * Turns a Double into the string the user sees.
 *
 * This is where binary floating point is made presentable. Rounding to 10 decimal places is
 * what keeps `0.1 + 0.2` showing as `0.3` rather than `0.30000000000000004`, and it is the
 * reason the engine can stay on Double instead of paying for BigDecimal arithmetic
 * throughout. BigDecimal appears here, in formatting only, because Kotlin's `Double.toString`
 * would render whole numbers as "5.0" and large ones as "1.0E21".
 */
object NumberFormatter {

    private const val PLACEHOLDER = "—"

    private const val SIGNIFICANT_DIGITS = 12

    /** Formats with thousands separators, e.g. `1,234.5`. */
    fun format(value: Double): String {
        if (value.isNaN()) return PLACEHOLDER
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

        // Anything this close to zero is rounding debris from a cancelling subtraction.
        val v = if (abs(value) < 1e-12) 0.0 else value
        val text = if (v != 0.0 && (abs(v) >= 1e12 || abs(v) < 1e-6)) {
            exponential(v)
        } else {
            plain(v)
        }
        return group(text)
    }

    /**
     * Renders a value as text that can be pasted straight back into an expression and parsed
     * to the same number. Used by memory recall.
     *
     * Deliberately not a display format. Neither display form survives a round trip: grouping
     * ("1,234") truncates at the comma, and exponential form ("1e+15") re-lexes as
     * `1 * e + 15` = 17.7, because `e` is Euler's number to the parser. Always plain decimal.
     */
    fun toLiteral(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        if (value == 0.0) return "0"
        // Significant digits, not decimal places: setScale(10) would flatten 1e-12 to "0".
        // Twelve is comfortably more than the ten the display rounds to, so the literal always
        // reads back as the number the user was shown.
        return BigDecimal(value)
            .round(MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP))
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun plain(v: Double): String =
        BigDecimal(v).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun exponential(v: Double): String {
        val raw = String.format(Locale.US, "%.6e", v) // "1.234560e+15"
        val splitAt = raw.indexOf('e')
        var mantissa = raw.substring(0, splitAt)
        if (mantissa.contains('.')) mantissa = mantissa.trimEnd('0').trimEnd('.')

        val exponent = raw.substring(splitAt + 1) // "+15" / "-07"
        val sign = exponent.first()
        val digits = exponent.drop(1).trimStart('0').ifEmpty { "0" }
        return "${mantissa}e$sign$digits"
    }

    private fun group(text: String): String {
        val negative = text.startsWith("-")
        val body = if (negative) text.substring(1) else text

        val dot = body.indexOf('.')
        val integerPart = if (dot >= 0) body.substring(0, dot) else body
        val fraction = if (dot >= 0) body.substring(dot) else ""

        // Exponential form is left alone; grouping its digits would be nonsense.
        if (!integerPart.all { it.isDigit() }) return text

        return buildString {
            if (negative) append('-')
            append(groupDigits(integerPart))
            append(fraction)
        }
    }

    private fun groupDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        val lead = digits.length % 3
        if (lead > 0) sb.append(digits, 0, lead)
        var i = lead
        while (i < digits.length) {
            if (sb.isNotEmpty()) sb.append(',')
            sb.append(digits, i, i + 3)
            i += 3
        }
        return sb.toString()
    }
}
