package com.vi5hnu.calculator.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A convertible quantity. Every unit is expressed as a multiple of the category's base unit,
 * so conversion is `value * from / to`.
 */
data class UnitCategory(
    val name: String,
    val units: Map<String, Double>,
    /** Shown as an amber advisory above the fields, where a unit needs a caveat. */
    val note: String? = null,
)

/**
 * Unit conversion for the nine categories in the design.
 *
 * Temperature is the one category that cannot be a simple ratio — C/F/K differ by offset as
 * well as scale — so it takes a dedicated path.
 */
@Singleton
class UnitConverter @Inject constructor() {

    val categories: List<UnitCategory> = listOf(
        UnitCategory(
            "Length",
            linkedMapOf(
                "m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001,
                "mi" to 1609.344, "yd" to 0.9144, "ft" to 0.3048, "in" to 0.0254,
            ),
        ),
        UnitCategory(
            "Mass",
            linkedMapOf(
                "kg" to 1.0, "g" to 0.001, "mg" to 0.000001,
                "lb" to 0.45359237, "oz" to 0.028349523125, "t" to 1000.0,
            ),
        ),
        UnitCategory("Temp", linkedMapOf("C" to 1.0, "F" to 1.0, "K" to 1.0)),
        UnitCategory(
            "Area",
            linkedMapOf(
                "m²" to 1.0, "km²" to 1_000_000.0, "cm²" to 0.0001,
                "ft²" to 0.09290304, "ac" to 4046.8564224, "ha" to 10_000.0,
            ),
        ),
        UnitCategory(
            "Volume",
            linkedMapOf(
                "L" to 1.0, "mL" to 0.001, "m³" to 1000.0,
                "gal" to 3.785411784, "qt" to 0.946352946, "ft³" to 28.316846592,
            ),
        ),
        UnitCategory(
            "Speed",
            linkedMapOf(
                "m/s" to 1.0, "km/h" to 0.2777777777777778,
                "mph" to 0.44704, "knot" to 0.5144444444444445,
            ),
        ),
        UnitCategory(
            "Data",
            linkedMapOf(
                "B" to 1.0, "KB" to 1024.0, "MB" to 1048576.0,
                "GB" to 1073741824.0, "TB" to 1099511627776.0,
            ),
            // KB is genuinely ambiguous: 1024 bytes by operating-system convention, but 1000
            // under SI (where 1024 is a KiB). Saying which one we mean costs a line of text
            // and stops the answer looking wrong to whichever camp the user is in.
            note = "Binary multiples: 1 KB = 1024 B, as reported by operating systems. " +
                "SI decimal units (1 kB = 1000 B) differ.",
        ),
        UnitCategory(
            "Time",
            linkedMapOf(
                "s" to 1.0, "min" to 60.0, "h" to 3600.0,
                "day" to 86400.0, "week" to 604800.0,
            ),
        ),
        // Currency is deliberately absent. Every other category here is a fixed physical
        // definition that cannot go stale; exchange rates would ship already wrong and drift
        // further every day, and a calculator that quietly reports a wrong number is worse
        // than one that declines to answer. Fetching live rates would need network access,
        // an API key and a cache — and would still fall back to stale figures offline.
    )

    fun category(name: String): UnitCategory =
        categories.firstOrNull { it.name == name } ?: categories.first()

    /** Converts [value] between two units of [categoryName]. Returns null if units are unknown. */
    fun convert(categoryName: String, from: String, to: String, value: Double): Double? {
        if (categoryName == TEMPERATURE) return convertTemperature(from, to, value)

        val units = category(categoryName).units
        val fromFactor = units[from] ?: return null
        val toFactor = units[to] ?: return null
        return value * fromFactor / toFactor
    }

    private fun convertTemperature(from: String, to: String, value: Double): Double? {
        val celsius = when (from) {
            "C" -> value
            "F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> return null
        }
        return when (to) {
            "C" -> celsius
            "F" -> celsius * 9.0 / 5.0 + 32.0
            "K" -> celsius + 273.15
            else -> null
        }
    }

    companion object {
        const val TEMPERATURE = "Temp"
    }
}
