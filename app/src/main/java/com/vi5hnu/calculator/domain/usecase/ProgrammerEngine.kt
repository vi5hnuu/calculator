package com.vi5hnu.calculator.domain.usecase

import com.vi5hnu.calculator.domain.model.NumberBase
import javax.inject.Inject
import javax.inject.Singleton

/** The bitwise and arithmetic operations on the programmer keypad. */
enum class ProgOp(val label: String) {
    AND("AND"), OR("OR"), XOR("XOR"),
    SHL("<<"), SHR(">>"),
    ADD("+"), SUB("−"), MUL("×"), DIV("÷"),
}

/**
 * Programmer mode's calculator.
 *
 * Deliberately does not use [com.vi5hnu.calculator.core.math.ExpressionEngine]: this is an
 * immediate-execution calculator over 32-bit signed integers, not an expression parser over
 * Doubles. The design's JavaScript coerces with `| 0` at every step to emulate that; Kotlin's
 * Int *is* a 32-bit signed integer, so wrapping and overflow come out right for free.
 */
@Singleton
class ProgrammerEngine @Inject constructor() {

    /** Parses an entry string written in [base]. Unparseable text reads as 0. */
    fun parse(entry: String, base: NumberBase): Int =
        entry.toIntOrNull(base.radix) ?: entry.toLongOrNull(base.radix)?.toInt() ?: 0

    /**
     * Whether [entry] still fits the 32-bit register.
     *
     * Guards keystrokes: without it, typing `4294967295` in DEC wraps to -1 and the field
     * contradicts the readout. Non-decimal bases are written unsigned, so their ceiling is
     * 0xFFFFFFFF rather than Int.MAX_VALUE.
     */
    fun accepts(entry: String, base: NumberBase): Boolean {
        val value = entry.toLongOrNull(base.radix) ?: return false
        return when (base) {
            NumberBase.DEC -> value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
            else -> value in 0L..0xFFFFFFFFL
        }
    }

    /**
     * Renders [value] in [base].
     *
     * Non-decimal bases show the unsigned bit pattern — the conventional way to read a
     * negative number in hex or binary, and what the design's `>>> 0` produces.
     */
    fun format(value: Int, base: NumberBase): String = when (base) {
        NumberBase.DEC -> value.toString()
        NumberBase.HEX -> value.toUInt().toString(16).uppercase()
        NumberBase.OCT -> value.toUInt().toString(8)
        NumberBase.BIN -> value.toUInt().toString(2)
    }

    fun apply(left: Int, op: ProgOp, right: Int): Int = when (op) {
        ProgOp.AND -> left and right
        ProgOp.OR -> left or right
        ProgOp.XOR -> left xor right
        // The JVM masks the shift distance to 5 bits, so `1 shl 32` would come back as 1 —
        // looking as though the calculator ignored the keypress. Shifting by the register
        // width or more empties it; an arithmetic right shift leaves the sign behind.
        ProgOp.SHL -> if (right in 0..31) left shl right else 0
        ProgOp.SHR -> if (right in 0..31) left shr right else if (left < 0) -1 else 0
        ProgOp.ADD -> left + right
        ProgOp.SUB -> left - right
        ProgOp.MUL -> left * right
        // Integer division by zero throws on the JVM but yields 0 in the design; keep the
        // calculator alive rather than crash on a stray "5 / 0 =".
        ProgOp.DIV -> if (right == 0) 0 else left / right
    }

    /** Groups digits for readability: binary in nibbles, hex in fours. */
    fun grouped(value: Int, base: NumberBase): String {
        val text = format(value, base)
        val size = when (base) {
            NumberBase.BIN -> 4
            NumberBase.HEX -> 4
            NumberBase.DEC -> 3
            NumberBase.OCT -> return text
        }
        val separator = if (base == NumberBase.DEC) "," else " "
        val negative = text.startsWith("-")
        val body = text.removePrefix("-")
        if (body.length <= size) return text

        val chunks = body.reversed().chunked(size).joinToString(separator).reversed()
        return if (negative) "-$chunks" else chunks
    }
}
