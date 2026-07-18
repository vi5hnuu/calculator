package com.vi5hnu.calculator.domain.model

/** The nine screens in the mode dock, in the order the design lists them. */
enum class CalcMode(val label: String) {
    STANDARD("Standard"),
    SCIENTIFIC("Scientific"),
    PROGRAMMER("Programmer"),
    PERCENT("Percent"),
    WORKSHEET("Worksheet"),
    CONVERT("Convert"),
    GRAPH("Graph"),
    FINANCE("Finance"),
    DATE("Date");

    /** Standard and Scientific share a screen; only the function tray differs. */
    val isCalc: Boolean get() = this == STANDARD || this == SCIENTIFIC

    companion object {
        val Default = SCIENTIFIC

        fun fromName(name: String?): CalcMode =
            entries.firstOrNull { it.name == name } ?: Default
    }
}

/** Follows the system, or is pinned by the user. Replaces the old Flutter ThemeProvider. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        val Default = SYSTEM

        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: Default
    }
}

/**
 * One committed calculation.
 *
 * The raw [result] is stored rather than a formatted string, so history re-renders correctly
 * if formatting ever changes and can be fed back into the engine losslessly.
 */
data class HistoryEntry(
    val id: Long = 0,
    val expression: String,
    val result: Double,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Programmer mode's four bases. */
enum class NumberBase(val label: String, val radix: Int) {
    HEX("HEX", 16),
    DEC("DEC", 10),
    OCT("OCT", 8),
    BIN("BIN", 2);

    /** The digits legal in this base, used to grey out impossible keys. */
    val digits: String get() = "0123456789ABCDEF".take(radix)
}
