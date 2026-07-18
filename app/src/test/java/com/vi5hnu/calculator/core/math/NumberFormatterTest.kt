package com.vi5hnu.calculator.core.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun `whole numbers lose the decimal tail`() {
        assertThat(NumberFormatter.format(5.0)).isEqualTo("5")
        assertThat(NumberFormatter.format(0.0)).isEqualTo("0")
        assertThat(NumberFormatter.format(-3.0)).isEqualTo("-3")
    }

    @Test
    fun `floating point noise is rounded away`() {
        // The whole reason the engine can stay on Double.
        assertThat(NumberFormatter.format(0.1 + 0.2)).isEqualTo("0.3")
        assertThat(NumberFormatter.format(1.0 - 0.9)).isEqualTo("0.1")
    }

    @Test
    fun `thousands are grouped`() {
        assertThat(NumberFormatter.format(1234.0)).isEqualTo("1,234")
        assertThat(NumberFormatter.format(1234567.0)).isEqualTo("1,234,567")
        assertThat(NumberFormatter.format(-1234567.0)).isEqualTo("-1,234,567")
        assertThat(NumberFormatter.format(1234.5)).isEqualTo("1,234.5")
        assertThat(NumberFormatter.format(999.0)).isEqualTo("999")
    }

    @Test
    fun `very small and very large values go exponential`() {
        assertThat(NumberFormatter.format(1e15)).isEqualTo("1e+15")
        assertThat(NumberFormatter.format(1.5e20)).isEqualTo("1.5e+20")
        assertThat(NumberFormatter.format(1e-7)).isEqualTo("1e-7")
    }

    @Test
    fun `values just inside the plain range stay plain`() {
        assertThat(NumberFormatter.format(1e-6)).isEqualTo("0.000001")
    }

    @Test
    fun `cancellation debris collapses to zero`() {
        assertThat(NumberFormatter.format(1e-13)).isEqualTo("0")
    }

    @Test
    fun `non-finite values get symbols`() {
        assertThat(NumberFormatter.format(Double.POSITIVE_INFINITY)).isEqualTo("∞")
        assertThat(NumberFormatter.format(Double.NEGATIVE_INFINITY)).isEqualTo("-∞")
        assertThat(NumberFormatter.format(Double.NaN)).isEqualTo("—")
    }

    @Test
    fun `toLiteral omits grouping and exponents so its output can be re-parsed`() {
        assertThat(NumberFormatter.toLiteral(1234567.0)).isEqualTo("1234567")
        assertThat(NumberFormatter.toLiteral(1234.5)).isEqualTo("1234.5")
        assertThat(NumberFormatter.toLiteral(1e15)).isEqualTo("1000000000000000")
        assertThat(NumberFormatter.toLiteral(0.0)).isEqualTo("0")
    }
}
