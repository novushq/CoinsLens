package app.novushq.coinlens.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun `usd rounds to the nearest cent`() {
        assertEquals(Money(1999), usd(19.99))
        assertEquals(Money(30), usd(0.296))
    }

    @Test
    fun `display string pads cents and keeps sign`() {
        assertEquals("$12.05", Money(1205).toDisplayString())
        assertEquals("-$0.50", Money(-50).toDisplayString())
        assertEquals("£1.00", Money(100, "GBP").toDisplayString())
    }

    @Test
    fun `arithmetic keeps currency`() {
        assertEquals(Money(350), Money(100) + Money(250))
        assertEquals(Money(300), Money(100) * 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `adding different currencies fails`() {
        Money(1) + Money(1, "GBP")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `inverted range is rejected`() {
        ValueRange(Money(5), Money(1))
    }

    @Test
    fun `allowance can scan when any source remains`() {
        assertEquals(false, ScanAllowance(false, 0, 0).canScan)
        assertEquals(true, ScanAllowance(false, 0, 1).canScan)
        assertEquals(true, ScanAllowance(true, 0, 0).canScan)
    }
}
