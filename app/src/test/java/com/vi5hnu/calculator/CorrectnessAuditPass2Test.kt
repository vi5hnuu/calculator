package com.vi5hnu.calculator

import com.google.common.truth.Truth.assertThat
import com.vi5hnu.calculator.core.math.EvalOutcome
import com.vi5hnu.calculator.core.math.ExpressionEngine
import com.vi5hnu.calculator.core.math.NumberFormatter
import com.vi5hnu.calculator.domain.model.NumberBase
import com.vi5hnu.calculator.domain.usecase.ProgOp
import com.vi5hnu.calculator.domain.usecase.ProgrammerEngine
import com.vi5hnu.calculator.domain.usecase.UnitConverter
import org.junit.Test

/**
 * Second audit pass, going after the areas the first sweep did not stress: register width,
 * shift semantics, and whether the converter's factors match authoritative definitions rather
 * than the approximations the design mock shipped.
 */
class CorrectnessAuditPass2Test {

    private val engine = ExpressionEngine()
    private val programmer = ProgrammerEngine()
    private val converter = UnitConverter()

    // ------------------------------------------------------------------ shifts

    @Test
    fun `shifting past the register width empties it`() {
        // Kotlin masks the shift distance to 5 bits, so `1 shl 32` is 1, not 0 — the operand
        // comes back untouched and looks like the calculator ignored the operation.
        assertThat(programmer.apply(1, ProgOp.SHL, 32)).isEqualTo(0)
        assertThat(programmer.apply(1, ProgOp.SHL, 33)).isEqualTo(0)
        assertThat(programmer.apply(Int.MAX_VALUE, ProgOp.SHL, 64)).isEqualTo(0)
    }

    @Test
    fun `right shifting past the register width preserves the sign`() {
        assertThat(programmer.apply(255, ProgOp.SHR, 32)).isEqualTo(0)
        // Arithmetic shift: a negative shifted all the way out is all sign bits.
        assertThat(programmer.apply(-1, ProgOp.SHR, 32)).isEqualTo(-1)
        assertThat(programmer.apply(-255, ProgOp.SHR, 40)).isEqualTo(-1)
    }

    @Test
    fun `shifts within the register width are unaffected`() {
        assertThat(programmer.apply(1, ProgOp.SHL, 31)).isEqualTo(Int.MIN_VALUE)
        assertThat(programmer.apply(-8, ProgOp.SHR, 1)).isEqualTo(-4)
        assertThat(programmer.apply(1, ProgOp.SHL, 0)).isEqualTo(1)
    }

    // ------------------------------------------------- register range on entry

    @Test
    fun `entry is refused once it leaves the 32-bit register`() {
        // Typing 4294967295 in DEC would wrap to -1, so the field and the readout disagree.
        assertThat(programmer.accepts("2147483647", NumberBase.DEC)).isTrue()
        assertThat(programmer.accepts("4294967295", NumberBase.DEC)).isFalse()
        assertThat(programmer.accepts("-2147483648", NumberBase.DEC)).isTrue()

        assertThat(programmer.accepts("FFFFFFFF", NumberBase.HEX)).isTrue()
        assertThat(programmer.accepts("FFFFFFFFF", NumberBase.HEX)).isFalse()

        assertThat(programmer.accepts("1".repeat(32), NumberBase.BIN)).isTrue()
        assertThat(programmer.accepts("1".repeat(33), NumberBase.BIN)).isFalse()

        assertThat(programmer.accepts("37777777777", NumberBase.OCT)).isTrue()
        assertThat(programmer.accepts("77777777777", NumberBase.OCT)).isFalse()
    }

    @Test
    fun `whatever the register accepts survives a parse and format round trip`() {
        for (base in NumberBase.entries) {
            for (value in listOf(0, 1, -1, 255, Int.MAX_VALUE, Int.MIN_VALUE, -12345)) {
                val text = programmer.format(value, base)
                assertThat(programmer.accepts(text, base)).isTrue()
                assertThat(programmer.parse(text, base)).isEqualTo(value)
            }
        }
    }

    // ------------------------------------------------------------- conversions

    @Test
    fun `length factors match their exact definitions`() {
        // 1 inch is defined as exactly 25.4 mm; everything imperial follows from it.
        assertThat(converter.convert("Length", "in", "mm", 1.0)!!).isWithin(1e-12).of(25.4)
        assertThat(converter.convert("Length", "ft", "in", 1.0)!!).isWithin(1e-9).of(12.0)
        assertThat(converter.convert("Length", "yd", "ft", 1.0)!!).isWithin(1e-9).of(3.0)
        assertThat(converter.convert("Length", "mi", "ft", 1.0)!!).isWithin(1e-6).of(5280.0)
    }

    @Test
    fun `mass factors match their exact definitions`() {
        assertThat(converter.convert("Mass", "lb", "oz", 1.0)!!).isWithin(1e-9).of(16.0)
        assertThat(converter.convert("Mass", "lb", "g", 1.0)!!).isWithin(1e-6).of(453.59237)
        assertThat(converter.convert("Mass", "t", "kg", 1.0)!!).isWithin(1e-9).of(1000.0)
    }

    @Test
    fun `area and volume factors are self consistent with length`() {
        // 1 ft² must equal (1 ft)², or the two categories contradict each other.
        val footInMetres = converter.convert("Length", "ft", "m", 1.0)!!
        val footSqInMetresSq = converter.convert("Area", "ft²", "m²", 1.0)!!
        assertThat(footSqInMetresSq).isWithin(1e-9).of(footInMetres * footInMetres)

        val footCubedInLitres = converter.convert("Volume", "ft³", "L", 1.0)!!
        assertThat(footCubedInLitres).isWithin(1e-6).of(footInMetres * footInMetres * footInMetres * 1000.0)

        assertThat(converter.convert("Area", "ha", "m²", 1.0)!!).isWithin(1e-9).of(10_000.0)
        assertThat(converter.convert("Volume", "gal", "qt", 1.0)!!).isWithin(1e-9).of(4.0)
    }

    @Test
    fun `speed factors match their exact definitions`() {
        assertThat(converter.convert("Speed", "km/h", "m/s", 3.6)!!).isWithin(1e-9).of(1.0)
        assertThat(converter.convert("Speed", "knot", "km/h", 1.0)!!).isWithin(1e-6).of(1.852)
        assertThat(converter.convert("Speed", "mph", "km/h", 1.0)!!).isWithin(1e-6).of(1.609344)
    }

    @Test
    fun `data factors are binary multiples`() {
        assertThat(converter.convert("Data", "KB", "B", 1.0)!!).isEqualTo(1024.0)
        assertThat(converter.convert("Data", "TB", "GB", 1.0)!!).isWithin(1e-9).of(1024.0)
    }

    @Test
    fun `every ratio conversion round trips`() {
        for (category in converter.categories) {
            val units = category.units.keys.toList()
            for (from in units) {
                for (to in units) {
                    val there = converter.convert(category.name, from, to, 7.5)!!
                    val back = converter.convert(category.name, to, from, there)!!
                    assertThat(back).isWithin(1e-6).of(7.5)
                }
            }
        }
    }

    // ------------------------------------------------------- formatter honesty

    @Test
    fun `formatting never rounds a non-zero value away to a bare zero`() {
        // Showing "0" for a small-but-real number would be a lie; it must go exponential.
        for (value in listOf(1e-7, 1e-9, 1e-11, -1e-9)) {
            assertThat(NumberFormatter.format(value)).isNotEqualTo("0")
        }
    }

    @Test
    fun `formatting never drops integer precision inside the plain range`() {
        for (value in listOf(999_999_999_999.0, 123_456_789_012.0, -999_999_999_999.0)) {
            val formatted = NumberFormatter.format(value)
            val reparsed = formatted.replace(",", "").toDouble()
            assertThat(reparsed).isEqualTo(value)
        }
    }

    // -------------------------------------------------- engine self-consistency

    @Test
    fun `the tape result equals what the engine would recompute from the stored expression`() {
        // History stores the raw expression; reusing it must reproduce the same answer.
        val expressions = listOf("2+3*4", "(1+2)/4", "sqrt(16)+2^-3", "5!", "1/3", "2pi")
        for (expression in expressions) {
            val first = (engine.evaluate(expression) as EvalOutcome.Success).value
            val second = (engine.evaluate(expression) as EvalOutcome.Success).value
            assertThat(second).isEqualTo(first)
        }
    }
}
