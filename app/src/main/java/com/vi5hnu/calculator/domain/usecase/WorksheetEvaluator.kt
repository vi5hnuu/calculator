package com.vi5hnu.calculator.domain.usecase

import com.vi5hnu.calculator.core.math.AngleUnit
import com.vi5hnu.calculator.core.math.EvalOutcome
import com.vi5hnu.calculator.core.math.ExpressionEngine
import com.vi5hnu.calculator.core.math.NumberFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class WorksheetLine(
    val index: Int,
    /** Set when the line was an assignment, e.g. `price` for `price = 250`. */
    val name: String?,
    val expression: String,
    val value: Double?,
    val formatted: String,
)

/**
 * Evaluates a multi-line worksheet where lines can name their results and refer back.
 *
 * `name = expr` binds a variable; a bare expression just evaluates. Either way the result
 * becomes `ans` for the following line. A line that fails renders as "—" and does not stop
 * the lines beneath it — a typo halfway down a sheet should not blank the whole thing.
 */
@Singleton
class WorksheetEvaluator @Inject constructor(
    private val engine: ExpressionEngine,
) {

    fun evaluate(text: String, angleUnit: AngleUnit = AngleUnit.RAD): List<WorksheetLine> {
        val variables = mutableMapOf<String, Double>()
        var ans = 0.0
        val output = mutableListOf<WorksheetLine>()

        text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { line ->
                val assignment = ASSIGNMENT.matchEntire(line)
                val name = assignment?.groupValues?.get(1)
                val body = assignment?.groupValues?.get(2) ?: line

                val outcome = engine.evaluate(
                    expression = body,
                    angleUnit = angleUnit,
                    variables = variables + ("ans" to ans),
                    allowUnknownNames = true,
                )

                val value = (outcome as? EvalOutcome.Success)?.value
                if (value != null) {
                    ans = value
                    if (name != null) variables[name] = value
                }

                output += WorksheetLine(
                    index = output.size,
                    name = name,
                    expression = line,
                    value = value,
                    formatted = value?.let(NumberFormatter::format) ?: "—",
                )
            }

        return output
    }

    private companion object {
        /** `name = expression`, where name is a normal identifier. */
        val ASSIGNMENT = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$")
    }
}
