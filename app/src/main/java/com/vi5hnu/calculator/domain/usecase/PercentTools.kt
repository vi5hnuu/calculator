package com.vi5hnu.calculator.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

data class TipSummary(
    val tipAmount: Double,
    val total: Double,
    val perPerson: Double,
)

/** The percent operations offered beneath the tip calculator. */
enum class PercentOp(val key: String, val label: String) {
    OF("of", "% of"),
    ADD("add", "+ %"),
    SUB("sub", "− %");

    companion object {
        val Default = SUB
    }
}

/** Tip/split and the standalone percent tools in Percent mode. */
@Singleton
class PercentTools @Inject constructor() {

    fun tip(bill: Double, tipPercent: Double, split: Int): TipSummary? {
        if (bill.isNaN() || tipPercent.isNaN()) return null
        val people = split.coerceAtLeast(1)
        val tipAmount = bill * tipPercent / 100.0
        val total = bill + tipAmount
        return TipSummary(
            tipAmount = tipAmount,
            total = total,
            perPerson = total / people,
        )
    }

    /**
     * @return `value` transformed by `percent` per [op]: a percentage of it, or it grown or
     *   shrunk by that percentage — i.e. discount and markup.
     */
    fun apply(op: PercentOp, value: Double, percent: Double): Double? {
        if (value.isNaN() || percent.isNaN()) return null
        return when (op) {
            PercentOp.OF -> value * percent / 100.0
            PercentOp.ADD -> value * (1.0 + percent / 100.0)
            PercentOp.SUB -> value * (1.0 - percent / 100.0)
        }
    }
}
