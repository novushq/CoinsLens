package app.novushq.coinlens.ai

import app.novushq.coinlens.model.CoinIdentification
import app.novushq.coinlens.model.Confidence
import app.novushq.coinlens.model.ItemKind
import app.novushq.coinlens.model.Rarity
import app.novushq.coinlens.model.ValueEstimate
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.VarietyHint
import app.novushq.coinlens.model.usd
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Bundled dummy data for fake mode. [json] is exactly what Gemini would return
 * for `CoinIdentificationSpec.schema`, so fake runs exercise the real parser.
 */
object SampleCatalogue {

    val entries: List<CoinIdentification> = listOf(
        CoinIdentification(
            kind = ItemKind.COIN, name = "Lincoln steel cent", country = "United States",
            denomination = "1 cent", year = 1943, yearText = "1943", mint = "Philadelphia",
            composition = "Zinc-coated steel", weightGrams = 2.7, diameterMm = 19.0,
            rarity = Rarity.COMMON,
            description = "Wartime cent struck in steel to save copper for the war effort.",
            value = ValueEstimate(range(0.25, 2.0), range(3.0, 10.0), Confidence.HIGH, "Recent dealer listings"),
            alternatives = listOf("1943-D Lincoln steel cent", "1943-S Lincoln steel cent"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "Lincoln cent, doubled die obverse", country = "United States",
            denomination = "1 cent", year = 1955, yearText = "1955", mint = "Philadelphia",
            composition = "Bronze", weightGrams = 3.11, diameterMm = 19.0,
            rarity = Rarity.RARE,
            description = "Wheat-reverse cent. The 1955 doubled die is one of the best-known US errors.",
            value = ValueEstimate(range(1000.0, 2500.0), range(3500.0, 9000.0), Confidence.MEDIUM, "Auction records"),
            hints = listOf(
                VarietyHint(
                    "doubled die obverse",
                    "Strong, rounded doubling of the date and lettering.",
                    "Look at the date and at LIBERTY and IN GOD WE TRUST.",
                ),
            ),
            alternatives = listOf("1955 Lincoln cent (normal die)"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "Lincoln cent, S VDB", country = "United States",
            denomination = "1 cent", year = 1909, yearText = "1909", mintMark = "S", mint = "San Francisco",
            composition = "Bronze", weightGrams = 3.11, diameterMm = 19.0,
            rarity = Rarity.VERY_RARE,
            description = "Key date of the series, with the designer's initials VDB on the reverse.",
            value = ValueEstimate(range(700.0, 1500.0), range(2000.0, 5000.0), Confidence.MEDIUM, "Price guides"),
            alternatives = listOf("1909 VDB Lincoln cent", "1909-S Lincoln cent"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "Wisconsin state quarter", country = "United States",
            denomination = "25 cents", year = 2004, yearText = "2004", mintMark = "D", mint = "Denver",
            composition = "Copper-nickel clad", weightGrams = 5.67, diameterMm = 24.26,
            rarity = Rarity.SCARCE,
            description = "State quarter showing a cow, cheese wheel and ear of corn.",
            value = ValueEstimate(range(0.25, 1.0), range(50.0, 300.0), Confidence.LOW, "Normal coin vs. extra-leaf variety"),
            hints = listOf(
                VarietyHint(
                    "extra leaf",
                    "An extra leaf on the left of the corn stalk, curving high or low.",
                    "Check the left side of the corn stalk on the reverse.",
                ),
            ),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "Morgan dollar", country = "United States",
            denomination = "1 dollar", year = 1921, yearText = "1921", mint = "Philadelphia",
            composition = "90% silver", weightGrams = 26.73, diameterMm = 38.1,
            rarity = Rarity.COMMON,
            description = "Final year of the Morgan silver dollar.",
            value = ValueEstimate(range(30.0, 45.0), range(60.0, 150.0), Confidence.HIGH, "Silver value plus collector premium"),
            alternatives = listOf("1921-D Morgan dollar", "1921-S Morgan dollar"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "Kennedy half dollar", country = "United States",
            denomination = "50 cents", year = 1964, yearText = "1964", mint = "Philadelphia",
            composition = "90% silver", weightGrams = 12.5, diameterMm = 30.6,
            rarity = Rarity.COMMON,
            description = "First-year Kennedy half and the only year struck in 90% silver.",
            value = ValueEstimate(range(10.0, 15.0), range(18.0, 40.0), Confidence.HIGH, "Silver value plus collector premium"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "Two pence, \"New Pence\"", country = "United Kingdom",
            denomination = "2 pence", year = 1983, yearText = "1983", mint = "Royal Mint",
            composition = "Bronze", weightGrams = 7.12, diameterMm = 25.9,
            rarity = Rarity.RARE,
            description = "Some 1983 coins were struck with the old NEW PENCE wording instead of TWO PENCE.",
            value = ValueEstimate(range(0.05, 1.0), range(400.0, 1000.0), Confidence.LOW, "Normal coin vs. mule error"),
            hints = listOf(
                VarietyHint(
                    "NEW PENCE mule",
                    "Reverse reads NEW PENCE rather than TWO PENCE.",
                    "Read the wording above the feathers on the reverse.",
                ),
            ),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "Two rupees", country = "India",
            denomination = "2 rupees", year = 2011, yearText = "2011", mint = "Mumbai",
            composition = "Ferritic stainless steel", weightGrams = 4.85, diameterMm = 25.0,
            rarity = Rarity.COMMON,
            description = "Circulation coin with the rupee symbol, introduced in 2011.",
            value = ValueEstimate(range(0.05, 0.5), range(1.0, 3.0), Confidence.HIGH, "Catalogue value"),
        ),
        CoinIdentification(
            kind = ItemKind.BANKNOTE, name = "Two-dollar Federal Reserve note", country = "United States",
            denomination = "2 dollars", year = 2003, yearText = "2003", mint = "Bureau of Engraving and Printing",
            composition = "Cotton-linen paper",
            rarity = Rarity.COMMON,
            description = "Modern \$2 note with Jefferson on the front and the Declaration of Independence on the back.",
            value = ValueEstimate(range(2.0, 3.0), range(4.0, 12.0), Confidence.HIGH, "Face value plus premium for crisp notes"),
            alternatives = listOf("2003A \$2 note", "Star note (serial ends in *)"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "One cent", country = "Canada",
            denomination = "1 cent", year = 2012, yearText = "2012", mint = "Royal Canadian Mint",
            composition = "Copper-plated steel", weightGrams = 2.35, diameterMm = 19.05,
            rarity = Rarity.COMMON,
            description = "Final year of the Canadian penny before it was withdrawn.",
            value = ValueEstimate(range(0.02, 0.25), range(1.0, 5.0), Confidence.MEDIUM, "Collector listings"),
        ),
        CoinIdentification.unrecognized("This looks like a button, not a coin or banknote."),
    )

    /** Model-shaped JSON for each entry, in the same order as [entries]. */
    val json: List<String> = entries.map { toSpecJson(it).toString() }

    private fun range(low: Double, high: Double) = ValueRange(usd(low), usd(high))

    private fun toSpecJson(entry: CoinIdentification): JsonObject = buildJsonObject {
        put("recognized", entry.recognized)
        put("description", entry.description)
        if (!entry.recognized) return@buildJsonObject
        put("kind", entry.kind.name)
        put("name", entry.name)
        put("country", entry.country)
        put("denomination", entry.denomination)
        entry.year?.let { put("year", it) }
        put("yearText", entry.yearText)
        entry.mintMark?.let { put("mintMark", it) }
        entry.mint?.let { put("mint", it) }
        entry.composition?.let { put("composition", it) }
        entry.weightGrams?.let { put("weightGrams", it) }
        entry.diameterMm?.let { put("diameterMm", it) }
        put("rarity", entry.rarity.name)
        val value = entry.value
        value.circulated?.let {
            put("circulatedLowUsd", it.low.cents / 100.0)
            put("circulatedHighUsd", it.high.cents / 100.0)
        }
        value.uncirculated?.let {
            put("uncirculatedLowUsd", it.low.cents / 100.0)
            put("uncirculatedHighUsd", it.high.cents / 100.0)
        }
        put("confidence", value.confidence.name)
        put("basis", value.basis)
        putJsonArray("hints") {
            entry.hints.forEach { hint ->
                addJsonObject {
                    put("name", hint.name)
                    put("description", hint.description)
                    put("whereToLook", hint.whereToLook)
                }
            }
        }
        putJsonArray("alternatives") { entry.alternatives.forEach { add(it) } }
    }
}
