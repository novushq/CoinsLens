package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppError
import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.model.Confidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinIdentificationSpecTest {

    private val spec = CoinIdentificationSpec()

    private fun json(
        name: String = "1955 doubled-die Lincoln cent",
        circLow: Long? = 1000,
        circHigh: Long? = 2500,
        extra: String = "",
    ) = """{"kind":"COIN","name":"$name","country":"USA","denomination":"1 cent","year":1955,
      "mintMark":"none","rarity":"RARE","description":"Doubled die obverse.",
      "circulatedLowCents":${circLow ?: "null"},"circulatedHighCents":${circHigh ?: "null"},
      "uncirculatedLowCents":null,"uncirculatedHighCents":null,
      "confidence":"HIGH","basis":"eBay sold listings",
      "hints":[{"name":"doubled die","description":"doubling","whereToLook":"date"}],
      "alternatives":["1955 Lincoln cent"],"recognized":true$extra}"""

    @Test fun `valid json parses with all fields`() {
        val result = spec.parse(json())
        assertTrue(result is AppResult.Success)
        val id = (result as AppResult.Success).data
        assertEquals("1955 doubled-die Lincoln cent", id.name)
        assertEquals(1955, id.year)
        assertEquals(Confidence.HIGH, id.value.confidence)
        assertEquals(1000L, id.value.circulated?.low?.cents)
        assertEquals("Possible doubled die", id.hints.first().displayName())
        assertEquals(1, id.alternatives.size)
    }

    @Test fun `unknown keys are ignored`() {
        assertTrue(spec.parse(json(extra = ""","futureField":123""")).isSuccess)
    }

    @Test fun `inverted range is coerced not fatal`() {
        val result = spec.parse(json(circLow = 2500, circHigh = 1000))
        assertTrue(result is AppResult.Success)
        val range = (result as AppResult.Success).data.value.circulated!!
        assertEquals(1000L, range.low.cents)
        assertEquals(2500L, range.high.cents)
    }

    @Test fun `blank name fails parse`() {
        val result = spec.parse(json(name = ""))
        assertTrue(result is AppResult.Failure && result.error is AppError.Parse)
    }

    @Test fun `no ranges fails parse`() {
        val result = spec.parse(json(circLow = null, circHigh = null))
        assertTrue(result is AppResult.Failure && result.error is AppError.Parse)
    }

    @Test fun `garbage fails parse without throwing`() {
        val result = spec.parse("not json {{{")
        assertTrue(result is AppResult.Failure && result.error is AppError.Parse)
    }

    @Test fun `unrecognized flag maps to unrecognized identification`() {
        val result = spec.parse("""{"recognized":false,"description":"a cat"}""")
        assertTrue(result is AppResult.Success)
        assertEquals(false, (result as AppResult.Success).data.recognized)
    }

    @Test fun `unknown confidence defaults low`() {
        val result = spec.parse(json().replace("\"HIGH\"", "\"COSMIC\""))
        assertEquals(Confidence.LOW, (result as AppResult.Success).data.value.confidence)
    }
}
