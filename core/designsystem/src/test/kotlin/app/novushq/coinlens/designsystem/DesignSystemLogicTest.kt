package app.novushq.coinlens.designsystem

import app.novushq.coinlens.designsystem.component.RangeScale
import app.novushq.coinlens.designsystem.component.formatMoney
import app.novushq.coinlens.designsystem.component.hintPhrase
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.usd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class DesignSystemLogicTest {

    @Test
    fun `range scale spans both bands`() {
        val scale = RangeScale.of(ValueRange(usd(1.0), usd(3.0)), ValueRange(usd(10.0), usd(21.0)))!!
        assertEquals(RangeScale(100, 2100), scale)
        assertEquals(0f, scale.fraction(100))
        assertEquals(0.5f, scale.fraction(1100))
        assertEquals(1f, scale.fraction(99_999))
    }

    @Test
    fun `range scale handles single and degenerate ranges`() {
        assertNull(RangeScale.of(null, null))
        val flat = RangeScale.of(ValueRange(usd(5.0), usd(5.0)), null)!!
        assertEquals(0f, flat.fraction(500))
    }

    @Test
    fun `money formats per locale with optional whole units`() {
        assertEquals("$1,234.50", formatMoney(usd(1234.5), Locale.US))
        assertEquals("$1,235", formatMoney(usd(1234.5), Locale.US, wholeUnits = true))
        assertEquals("£0.02", formatMoney(Money(2, "GBP"), Locale.UK))
    }

    @Test
    fun `hint phrase lowercases words but keeps acronyms`() {
        assertEquals("doubled die obverse", hintPhrase("Doubled die obverse"))
        assertEquals("NEW PENCE mule", hintPhrase("NEW PENCE mule"))
        assertEquals("VDB", hintPhrase(" VDB "))
    }
}
