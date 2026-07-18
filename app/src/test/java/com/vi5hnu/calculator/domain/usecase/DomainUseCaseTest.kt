package com.vi5hnu.calculator.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.vi5hnu.calculator.core.math.ExpressionEngine
import com.vi5hnu.calculator.domain.model.NumberBase
import org.junit.Test
import java.time.LocalDate

class UnitConverterTest {

    private val converter = UnitConverter()

    @Test
    fun `converts within a ratio category`() {
        assertThat(converter.convert("Length", "m", "cm", 1.0)).isWithin(1e-9).of(100.0)
        assertThat(converter.convert("Length", "km", "mi", 1.0)).isWithin(1e-4).of(0.621371)
        assertThat(converter.convert("Mass", "kg", "lb", 1.0)).isWithin(1e-6).of(2.20462262)
        assertThat(converter.convert("Data", "GB", "MB", 1.0)).isWithin(1e-9).of(1024.0)
        assertThat(converter.convert("Time", "h", "min", 2.0)).isWithin(1e-9).of(120.0)
    }

    @Test
    fun `converts temperature across its offsets`() {
        assertThat(converter.convert("Temp", "C", "F", 100.0)).isWithin(1e-9).of(212.0)
        assertThat(converter.convert("Temp", "C", "F", 0.0)).isWithin(1e-9).of(32.0)
        assertThat(converter.convert("Temp", "F", "C", 32.0)).isWithin(1e-9).of(0.0)
        assertThat(converter.convert("Temp", "C", "K", 0.0)).isWithin(1e-9).of(273.15)
        assertThat(converter.convert("Temp", "K", "C", 273.15)).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `temperature round trips`() {
        val f = converter.convert("Temp", "C", "F", 37.0)!!
        assertThat(converter.convert("Temp", "F", "C", f)).isWithin(1e-9).of(37.0)
    }

    @Test
    fun `identity conversion is lossless`() {
        assertThat(converter.convert("Length", "m", "m", 5.0)).isEqualTo(5.0)
        assertThat(converter.convert("Temp", "K", "K", 5.0)).isWithin(1e-9).of(5.0)
    }

    @Test
    fun `unknown units yield null rather than a wrong number`() {
        assertThat(converter.convert("Length", "m", "parsec", 1.0)).isNull()
        assertThat(converter.convert("Temp", "C", "R", 1.0)).isNull()
    }

    @Test
    fun `no category can go stale`() {
        // Currency was dropped deliberately: every remaining category is a fixed physical
        // definition, so nothing here can silently drift out of date.
        assertThat(converter.categories.map { it.name }).doesNotContain("Currency")
        assertThat(converter.categories.map { it.name }).containsExactly(
            "Length", "Mass", "Temp", "Area", "Volume", "Speed", "Data", "Time",
        ).inOrder()
    }

    @Test
    fun `data spells out which kilobyte it means`() {
        // 1 KB = 1024 B here; without saying so the answer looks wrong to an SI reader.
        assertThat(converter.category("Data").note).isNotNull()
        assertThat(converter.category("Length").note).isNull()
    }

    @Test
    fun `every category has at least two units to convert between`() {
        assertThat(converter.categories).hasSize(8)
        converter.categories.forEach { assertThat(it.units.size).isAtLeast(2) }
    }
}

class LoanCalculatorTest {

    private val calculator = LoanCalculator()

    @Test
    fun `computes a standard amortised payment`() {
        // 500,000 at 8.5% over 20 years — the design's default inputs. Expected figures
        // cross-checked against the standard amortisation formula independently.
        val summary = calculator.calculate(500_000.0, 8.5, 20.0)!!
        assertThat(summary.monthlyPayment).isWithin(0.01).of(4339.1162)
        assertThat(summary.totalPayable).isWithin(0.5).of(1_041_387.88)
        assertThat(summary.totalInterest).isWithin(0.5).of(541_387.88)
    }

    @Test
    fun `an interest free loan divides evenly`() {
        val summary = calculator.calculate(12_000.0, 0.0, 1.0)!!
        assertThat(summary.monthlyPayment).isWithin(1e-9).of(1000.0)
        assertThat(summary.totalInterest).isWithin(1e-9).of(0.0)
        assertThat(summary.totalPayable).isWithin(1e-9).of(12_000.0)
    }

    @Test
    fun `rejects inputs that cannot describe a loan`() {
        assertThat(calculator.calculate(0.0, 5.0, 10.0)).isNull()
        assertThat(calculator.calculate(-1.0, 5.0, 10.0)).isNull()
        assertThat(calculator.calculate(1000.0, 5.0, 0.0)).isNull()
        assertThat(calculator.calculate(1000.0, -1.0, 5.0)).isNull()
    }

    @Test
    fun `total interest is always positive when a rate is charged`() {
        val summary = calculator.calculate(100_000.0, 6.0, 15.0)!!
        assertThat(summary.totalInterest).isGreaterThan(0.0)
    }
}

class DateDifferenceTest {

    private val dates = DateDifference()

    @Test
    fun `counts plain days`() {
        val span = dates.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        assertThat(span.days).isEqualTo(30)
        assertThat(span.weeks).isWithin(0.05).of(4.29)
    }

    @Test
    fun `is order independent`() {
        val forward = dates.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))
        val backward = dates.between(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1))
        assertThat(backward.days).isEqualTo(forward.days)
        assertThat(backward.ymd).isEqualTo(forward.ymd)
    }

    @Test
    fun `accounts for a leap day`() {
        // 2024 is a leap year, so February contributes 29 days.
        val span = dates.between(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1))
        assertThat(span.days).isEqualTo(29)
    }

    @Test
    fun `a non leap year February is shorter`() {
        val span = dates.between(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1))
        assertThat(span.days).isEqualTo(28)
    }

    @Test
    fun `breaks a span into years, months and days`() {
        val span = dates.between(LocalDate.of(2020, 1, 15), LocalDate.of(2023, 4, 20))
        assertThat(span.years).isEqualTo(3)
        assertThat(span.monthsPart).isEqualTo(3)
        assertThat(span.daysPart).isEqualTo(5)
        assertThat(span.ymd).isEqualTo("3·3·5")
    }

    @Test
    fun `an identical date is a zero span`() {
        val span = dates.between(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 15))
        assertThat(span.days).isEqualTo(0)
        assertThat(span.ymd).isEqualTo("0·0·0")
    }

    @Test
    fun `whole month spans report no fractional part`() {
        val span = dates.between(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 15))
        assertThat(span.months).isWithin(1e-9).of(3.0)
    }
}

class ProgrammerEngineTest {

    private val engine = ProgrammerEngine()

    @Test
    fun `formats across bases`() {
        assertThat(engine.format(255, NumberBase.HEX)).isEqualTo("FF")
        assertThat(engine.format(255, NumberBase.DEC)).isEqualTo("255")
        assertThat(engine.format(255, NumberBase.OCT)).isEqualTo("377")
        assertThat(engine.format(255, NumberBase.BIN)).isEqualTo("11111111")
    }

    @Test
    fun `negatives show their unsigned bit pattern outside decimal`() {
        assertThat(engine.format(-1, NumberBase.DEC)).isEqualTo("-1")
        assertThat(engine.format(-1, NumberBase.HEX)).isEqualTo("FFFFFFFF")
        assertThat(engine.format(-1, NumberBase.BIN)).isEqualTo("1".repeat(32))
    }

    @Test
    fun `parses across bases`() {
        assertThat(engine.parse("FF", NumberBase.HEX)).isEqualTo(255)
        assertThat(engine.parse("11111111", NumberBase.BIN)).isEqualTo(255)
        assertThat(engine.parse("377", NumberBase.OCT)).isEqualTo(255)
        assertThat(engine.parse("", NumberBase.DEC)).isEqualTo(0)
        assertThat(engine.parse("zzz", NumberBase.DEC)).isEqualTo(0)
    }

    @Test
    fun `parse and format round trip through every base`() {
        NumberBase.entries.forEach { base ->
            val text = engine.format(-12345, base)
            assertThat(engine.parse(text, base)).isEqualTo(-12345)
        }
    }

    @Test
    fun `applies bitwise operations`() {
        assertThat(engine.apply(0b1100, ProgOp.AND, 0b1010)).isEqualTo(0b1000)
        assertThat(engine.apply(0b1100, ProgOp.OR, 0b1010)).isEqualTo(0b1110)
        assertThat(engine.apply(0b1100, ProgOp.XOR, 0b1010)).isEqualTo(0b0110)
        assertThat(engine.apply(1, ProgOp.SHL, 4)).isEqualTo(16)
        assertThat(engine.apply(16, ProgOp.SHR, 4)).isEqualTo(1)
    }

    @Test
    fun `arithmetic wraps at 32 bits`() {
        assertThat(engine.apply(Int.MAX_VALUE, ProgOp.ADD, 1)).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun `division by zero yields zero instead of throwing`() {
        assertThat(engine.apply(5, ProgOp.DIV, 0)).isEqualTo(0)
    }

    @Test
    fun `integer division truncates`() {
        assertThat(engine.apply(7, ProgOp.DIV, 2)).isEqualTo(3)
        assertThat(engine.apply(-7, ProgOp.DIV, 2)).isEqualTo(-3)
    }

    @Test
    fun `groups digits for readability`() {
        assertThat(engine.grouped(255, NumberBase.BIN)).isEqualTo("1111 1111")
        assertThat(engine.grouped(1_234_567, NumberBase.DEC)).isEqualTo("1,234,567")
    }
}

class PercentToolsTest {

    private val tools = PercentTools()

    @Test
    fun `splits a tipped bill`() {
        val summary = tools.tip(1200.0, 15.0, 2)!!
        assertThat(summary.tipAmount).isWithin(1e-9).of(180.0)
        assertThat(summary.total).isWithin(1e-9).of(1380.0)
        assertThat(summary.perPerson).isWithin(1e-9).of(690.0)
    }

    @Test
    fun `a zero or negative split still charges one person`() {
        assertThat(tools.tip(100.0, 0.0, 0)!!.perPerson).isWithin(1e-9).of(100.0)
        assertThat(tools.tip(100.0, 0.0, -3)!!.perPerson).isWithin(1e-9).of(100.0)
    }

    @Test
    fun `applies the percent tools`() {
        assertThat(tools.apply(PercentOp.OF, 200.0, 10.0)).isWithin(1e-9).of(20.0)
        assertThat(tools.apply(PercentOp.ADD, 200.0, 10.0)).isWithin(1e-9).of(220.0)
        assertThat(tools.apply(PercentOp.SUB, 200.0, 10.0)).isWithin(1e-9).of(180.0)
    }
}

class WorksheetEvaluatorTest {

    private val evaluator = WorksheetEvaluator(ExpressionEngine())

    @Test
    fun `binds variables and reuses them`() {
        val lines = evaluator.evaluate(
            """
            price = 250
            qty = 4
            subtotal = price*qty
            subtotal*1.08
            """.trimIndent(),
        )
        assertThat(lines).hasSize(4)
        assertThat(lines[0].value).isEqualTo(250.0)
        assertThat(lines[2].value).isEqualTo(1000.0)
        assertThat(lines[2].name).isEqualTo("subtotal")
        assertThat(lines[3].value).isWithin(1e-9).of(1080.0)
    }

    @Test
    fun `ans refers to the previous line`() {
        val lines = evaluator.evaluate("2+3\nans*10")
        assertThat(lines[1].value).isEqualTo(50.0)
    }

    @Test
    fun `a broken line does not derail the rest`() {
        val lines = evaluator.evaluate("a = 5\nthis is not maths(\na*2")
        assertThat(lines[1].value).isNull()
        assertThat(lines[1].formatted).isEqualTo("—")
        // `a` survives, so the final line still resolves.
        assertThat(lines[2].value).isEqualTo(10.0)
    }

    @Test
    fun `blank lines are skipped`() {
        val lines = evaluator.evaluate("1+1\n\n\n2+2")
        assertThat(lines).hasSize(2)
    }

    @Test
    fun `reassignment takes effect for later lines only`() {
        val lines = evaluator.evaluate("x = 1\nx = 2\nx*10")
        assertThat(lines[0].value).isEqualTo(1.0)
        assertThat(lines[2].value).isEqualTo(20.0)
    }

    @Test
    fun `empty input produces no lines`() {
        assertThat(evaluator.evaluate("")).isEmpty()
    }
}
