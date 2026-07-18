package com.vi5hnu.calculator.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

data class LoanSummary(
    val monthlyPayment: Double,
    val totalInterest: Double,
    val totalPayable: Double,
)

/** Amortised loan (EMI) maths for Finance mode. */
@Singleton
class LoanCalculator @Inject constructor() {

    /**
     * @param principal borrowed amount
     * @param annualRatePercent nominal annual rate, e.g. 8.5
     * @param years tenure
     * @return null when the inputs cannot describe a loan (non-positive principal or tenure).
     */
    fun calculate(principal: Double, annualRatePercent: Double, years: Double): LoanSummary? {
        if (principal <= 0 || years <= 0 || annualRatePercent < 0) return null
        if (principal.isNaN() || annualRatePercent.isNaN() || years.isNaN()) return null

        val months = years * 12.0
        val monthlyRate = annualRatePercent / 12.0 / 100.0

        // An interest-free loan is just the principal spread evenly; the amortisation formula
        // divides by zero here.
        val payment = if (monthlyRate == 0.0) {
            principal / months
        } else {
            val growth = (1.0 + monthlyRate).pow(months)
            principal * monthlyRate * growth / (growth - 1.0)
        }

        val total = payment * months
        return LoanSummary(
            monthlyPayment = payment,
            totalInterest = total - principal,
            totalPayable = total,
        )
    }
}
