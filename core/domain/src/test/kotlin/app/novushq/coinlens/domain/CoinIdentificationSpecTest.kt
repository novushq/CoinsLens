package app.novushq.coinlens.domain

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.identify.FieldSchema
import app.novushq.coinlens.model.Confidence
import app.novushq.coinlens.model.ItemKind
import app.novushq.coinlens.model.Rarity
import app.novushq.coinlens.model.usd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinIdentificationSpecTest {
    private val spec = CoinIdentificationSpec()

    private val full = """
        {"kind":"COIN","name":"Morgan dollar","country":"USA","denomination":"1 dollar","year":1921,
         "yearText":"1921","mintMark":"D","mint":"Denver","composition":"90% silver","weightGrams":26.73,
         "diameterMm":38.1,"rarity":"COMMON","description":"Last Morgan year.",
         "circulatedLowUsd":35,"circulatedHighUsd":45.5,"uncirculatedLowUsd":60,"uncirculatedHighUsd":120,
         "confidence":"HIGH","basis":"Dealer prices","hints":[{"name":"doubled die","description":"d","whereToLook":"date"}],
         "alternatives":["Peace dollar"],"recognized":true,"extra":"ignored"}
    """.trimIndent()

    @Test
    fun `valid json parses with all fields`() {
        val c = requireNotNull(spec.parse(full).getOrNull())
        assertEquals(ItemKind.COIN, c.kind)
        assertEquals("Morgan dollar", c.name)
        assertEquals(1921, c.year)
        assertEquals("D", c.mintMark)
        assertEquals(Rarity.COMMON, c.rarity)
        assertEquals(usd(35.0), c.value.circulated?.low)
        assertEquals(usd(45.5), c.value.circulated?.high)
        assertEquals(usd(120.0), c.value.uncirculated?.high)
        assertEquals(Confidence.HIGH, c.value.confidence)
        assertEquals("doubled die", c.hints.single().name)
        assertEquals(listOf("Peace dollar"), c.alternatives)
        assertTrue(c.recognized)
    }

    @Test
    fun `markdown fenced json is accepted`() {
        assertTrue(spec.parse("```json\n$full\n```").isSuccess)
    }

    @Test
    fun `malformed json is a parse error`() {
        assertTrue(spec.parse("{not json").errorOrNull() is AppError.Parse)
        assertTrue(spec.parse("").errorOrNull() is AppError.Parse)
    }

    @Test
    fun `inverted and negative ranges are coerced`() {
        val c = requireNotNull(
            spec.parse("""{"name":"X","circulatedLowUsd":9,"circulatedHighUsd":-3,"confidence":"LOW"}""").getOrNull(),
        )
        assertEquals(usd(0.0), c.value.circulated?.low)
        assertEquals(usd(9.0), c.value.circulated?.high)
        assertNull(c.value.uncirculated)
    }

    @Test
    fun `single bound becomes a point range`() {
        val c = requireNotNull(spec.parse("""{"name":"X","uncirculatedHighUsd":5}""").getOrNull())
        assertEquals(usd(5.0), c.value.uncirculated?.low)
        assertEquals(usd(5.0), c.value.uncirculated?.high)
    }

    @Test
    fun `missing value range or name is a parse error`() {
        assertTrue(spec.parse("""{"name":"X","confidence":"HIGH"}""").errorOrNull() is AppError.Parse)
        assertTrue(spec.parse("""{"name":" ","circulatedLowUsd":1}""").errorOrNull() is AppError.Parse)
    }

    @Test
    fun `unknown enums fall back conservatively`() {
        val c = requireNotNull(
            spec.parse("""{"name":"X","kind":"MEDAL","rarity":"LEGENDARY","confidence":"SURE","circulatedLowUsd":1}""").getOrNull(),
        )
        assertEquals(ItemKind.COIN, c.kind)
        assertEquals(Rarity.UNKNOWN, c.rarity)
        assertEquals(Confidence.LOW, c.value.confidence)
    }

    @Test
    fun `nulls for optional fields are tolerated`() {
        val c = requireNotNull(
            spec.parse(
                """{"name":"X","year":null,"yearText":null,"basis":null,"mintMark":"","circulatedLowUsd":1,"hints":[{"name":"clip","description":null}]}""",
            ).getOrNull(),
        )
        assertNull(c.year)
        assertNull(c.mintMark)
        assertEquals("", c.hints.single().description)
    }

    @Test
    fun `hints and alternatives are capped and blanks dropped`() {
        val hints = (1..8).joinToString(",") { """{"name":"h$it"}""" }
        val c = requireNotNull(
            spec.parse("""{"name":"X","circulatedLowUsd":1,"hints":[{"name":""},$hints],"alternatives":["a","","b","c","d"]}""").getOrNull(),
        )
        assertEquals(5, c.hints.size)
        assertEquals(listOf("a", "b", "c"), c.alternatives)
    }

    @Test
    fun `recognized false yields unrecognized identification`() {
        val c = requireNotNull(spec.parse("""{"recognized":false,"description":"A bottle cap"}""").getOrNull())
        assertFalse(c.recognized)
        assertEquals("A bottle cap", c.description)
    }

    @Test
    fun `schema optional fields all exist and core fields are required`() {
        val schema = spec.schema
        assertTrue(schema.properties.keys.containsAll(schema.optional))
        listOf("name", "kind", "confidence", "recognized", "hints").forEach { assertFalse(it in schema.optional) }
        assertTrue(schema.properties["kind"] is FieldSchema.Enum)
    }

    @Test
    fun `prompt mentions reverse only when two photos given`() {
        assertTrue(spec.userPrompt(listOf(front, back)).contains("reverse"))
        assertFalse(spec.userPrompt(listOf(front)).contains("reverse"))
    }
}
