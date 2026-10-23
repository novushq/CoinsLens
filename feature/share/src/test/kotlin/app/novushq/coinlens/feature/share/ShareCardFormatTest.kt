package app.novushq.coinlens.feature.share

import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.usd
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareCardFormatTest {
    @Test
    fun `value range formats low to high`() {
        assertEquals(
            "$0.10 – $25.00",
            ShareCardRenderer.formatValueRange(ValueRange(usd(0.10), usd(25.0))),
        )
    }

    @Test
    fun `metadata joins country year and mint`() {
        assertEquals(
            "UNITED STATES · 1969 · S",
            ShareCardRenderer.formatMetadata("United States", "1969", "S"),
        )
    }

    @Test
    fun `metadata skips blank parts`() {
        assertEquals("USA", ShareCardRenderer.formatMetadata("USA", "", null))
        assertEquals("", ShareCardRenderer.formatMetadata(null, "  ", ""))
    }
}
