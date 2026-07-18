package com.vi5hnu.calculator.domain.usecase

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class DateSpan(
    val days: Long,
    val weeks: Double,
    val months: Double,
    val years: Int,
    val monthsPart: Int,
    val daysPart: Int,
) {
    /** The "Y·M·D" breakdown the design shows in its third tile. */
    val ymd: String get() = "$years·$monthsPart·$daysPart"
}

/**
 * Calendar-aware difference between two dates.
 *
 * Uses java.time rather than millisecond arithmetic, so leap years and month lengths are
 * handled by the library instead of by a 30.4375 fudge factor. minSdk 26 makes this available
 * without desugaring.
 */
@Singleton
class DateDifference @Inject constructor() {

    /** Order-independent: the span from A to B equals the span from B to A. */
    fun between(a: LocalDate, b: LocalDate): DateSpan {
        val start = if (a.isBefore(b)) a else b
        val end = if (a.isBefore(b)) b else a

        val days = ChronoUnit.DAYS.between(start, end)
        val period = Period.between(start, end)

        return DateSpan(
            days = days,
            weeks = days / 7.0,
            // Fractional months only make sense as an approximation; anchor it to the exact
            // whole-month count so it never contradicts the Y·M·D tile.
            months = ChronoUnit.MONTHS.between(start, end) +
                fractionOfMonth(start, end),
            years = period.years,
            monthsPart = period.months,
            daysPart = period.days,
        )
    }

    /** How far into the trailing partial month [end] sits, as a 0..1 fraction. */
    private fun fractionOfMonth(start: LocalDate, end: LocalDate): Double {
        val wholeMonths = ChronoUnit.MONTHS.between(start, end)
        val anchor = start.plusMonths(wholeMonths)
        val leftoverDays = ChronoUnit.DAYS.between(anchor, end)
        if (leftoverDays == 0L) return 0.0
        val monthLength = anchor.lengthOfMonth()
        return abs(leftoverDays.toDouble()) / monthLength
    }
}
