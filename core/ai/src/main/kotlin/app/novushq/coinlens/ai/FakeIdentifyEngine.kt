package app.novushq.coinlens.ai

import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.identify.IdentifyEngine
import app.novushq.coinlens.identify.IdentifySpec
import app.novushq.coinlens.identify.ImageInput
import app.novushq.coinlens.model.CoinIdentification
import app.novushq.coinlens.model.Confidence
import app.novushq.coinlens.model.ItemKind
import app.novushq.coinlens.model.Rarity
import app.novushq.coinlens.model.ValueEstimate
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.VarietyHint
import app.novushq.coinlens.model.usd
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Bundled sample catalogue for fake mode (no google-services.json) and tests. */
object SampleCatalogue {

    val entries: List<CoinIdentification> = listOf(
        CoinIdentification(
            kind = ItemKind.COIN, name = "Lincoln steel cent", country = "USA",
            denomination = "1 cent", year = 1943, yearText = "1943", mint = "Philadelphia",
            composition = "Zinc-coated steel", weightGrams = 2.7, diameterMm = 19.0,
            rarity = Rarity.COMMON, description = "Wartime steel cent.",
            value = ValueEstimate(ValueRange(usd(0.25), usd(2.0)), ValueRange(usd(3.0), usd(10.0)), Confidence.HIGH, "Dealer listings"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "1955 doubled-die Lincoln cent", country = "USA",
            denomination = "1 cent", year = 1955, yearText = "1955",
            composition = "Bronze", weightGrams = 3.11, diameterMm = 19.0,
            rarity = Rarity.RARE, description = "Famous doubled-die obverse.",
            value = ValueEstimate(ValueRange(usd(1000.0), usd(2500.0)), ValueRange(usd(3500.0), usd(9000.0)), Confidence.HIGH, "Auction records"),
            hints = listOf(VarietyHint("doubled die", "Strong doubling on date and legends", "Look at the date and LIBERTY")),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "1909-S VDB Lincoln cent", country = "USA",
            denomination = "1 cent", year = 1909, yearText = "1909", mintMark = "S", mint = "San Francisco",
            composition = "Bronze", weightGrams = 3.11, diameterMm = 19.0,
            rarity = Rarity.VERY_RARE, description = "Key date with designer initials.",
            value = ValueEstimate(ValueRange(usd(700.0), usd(1500.0)), ValueRange(usd(2000.0), usd(5000.0)), Confidence.MEDIUM, "Price guides"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "2004-D Wisconsin quarter, extra leaf", country = "USA",
            denomination = "25 cents", year = 2004, yearText = "2004", mintMark = "D", mint = "Denver",
            composition = "Copper-nickel clad", weightGrams = 5.67, diameterMm = 24.26,
            rarity = Rarity.SCARCE, description = "Extra leaf variety on the corn stalk.",
            value = ValueEstimate(ValueRange(usd(50.0), usd(150.0)), ValueRange(usd(200.0), usd(500.0)), Confidence.MEDIUM, "Dealer listings"),
            hints = listOf(VarietyHint("extra leaf", "Extra leaf on the corn", "Check the corn stalk on the reverse")),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "1921 Morgan dollar", country = "USA",
            denomination = "1 dollar", year = 1921, yearText = "1921",
            composition = "90% silver", weightGrams = 26.73, diameterMm = 38.1,
            rarity = Rarity.COMMON, description = "Late-date Morgan dollar.",
            value = ValueEstimate(ValueRange(usd(25.0), usd(45.0)), ValueRange(usd(60.0), usd(150.0)), Confidence.HIGH, "Melt plus premium"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "1964 Kennedy half dollar", country = "USA",
            denomination = "50 cents", year = 1964, yearText = "1964",
            composition = "90% silver", weightGrams = 12.5, diameterMm = 30.6,
            rarity = Rarity.COMMON, description = "Last 90% silver half dollar.",
            value = ValueEstimate(ValueRange(usd(10.0), usd(15.0)), ValueRange(usd(20.0), usd(40.0)), Confidence.HIGH, "Melt plus premium"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "1983 2 pence 'New Pence'", country = "United Kingdom",
            denomination = "2 pence", year = 1983, yearText = "1983",
            composition = "Bronze", weightGrams = 7.12, diameterMm = 25.9,
            rarity = Rarity.SCARCE, description = "Transitional 'New Pence' reverse.",
            value = ValueEstimate(ValueRange(usd(0.5), usd(3.0)), ValueRange(usd(5.0), usd(15.0)), Confidence.MEDIUM, "Collector listings"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "2 rupees 2011", country = "India",
            denomination = "2 rupees", year = 2011, yearText = "2011",
            composition = "Stainless steel", weightGrams = 5.62, diameterMm = 27.0,
            rarity = Rarity.COMMON, description = "Stainless steel 2 rupee coin.",
            value = ValueEstimate(ValueRange(usd(0.1), usd(0.5)), ValueRange(usd(1.0), usd(3.0)), Confidence.HIGH, "Catalogue value"),
        ),
        CoinIdentification(
            kind = ItemKind.BANKNOTE, name = "$2 note 2003", country = "USA",
            denomination = "2 dollars", year = 2003, yearText = "2003",
            rarity = Rarity.COMMON, description = "Modern $2 Federal Reserve note.",
            value = ValueEstimate(ValueRange(usd(2.0), usd(3.0)), ValueRange(usd(5.0), usd(12.0)), Confidence.HIGH, "Face plus premium"),
        ),
        CoinIdentification(
            kind = ItemKind.COIN, name = "2012 penny", country = "Canada",
            denomination = "1 cent", year = 2012, yearText = "2012",
            composition = "Copper-plated steel", weightGrams = 2.35, diameterMm = 19.05,
            rarity = Rarity.COMMON, description = "Final year of the Canadian penny.",
            value = ValueEstimate(ValueRange(usd(0.1), usd(1.0)), ValueRange(usd(2.0), usd(6.0)), Confidence.MEDIUM, "Collector listings"),
        ),
        CoinIdentification.unrecognized(),
    )
}

/**
 * Deterministic fake engine: picks a catalogue entry from the image bytes hash
 * so results are stable across runs. Used in fake mode and all tests.
 */
class FakeIdentifyEngine(
    private val dispatchers: DispatcherProvider,
    private val delayMillis: Long = 400,
) : IdentifyEngine {
    override suspend fun <T> identify(spec: IdentifySpec<T>, images: List<ImageInput>): AppResult<T> {
        if (delayMillis > 0) delay(delayMillis)
        val bytes = images.firstOrNull()?.bytes ?: ByteArray(0)
        var hash = 0
        for (b in bytes) hash = 31 * hash + (b.toInt() and 0xff)
        val entry = SampleCatalogue.entries[Math.floorMod(hash, SampleCatalogue.entries.size)]
        return withContext(dispatchers.default) { spec.parse(sampleJson(entry)) }
    }

    /** Serializes a catalogue entry back through the spec so fakes test the real parse path. */
    private fun sampleJson(entry: CoinIdentification): String {
        if (!entry.recognized) return """{"recognized":false,"description":"${entry.description}"}"""
        val v = entry.value
        return """{"kind":"${entry.kind}","name":"${entry.name}","country":"${entry.country}",
          "denomination":"${entry.denomination}","year":${entry.year ?: "null"},
          "yearText":"${entry.yearText}","mintMark":${entry.mintMark?.let { "\"$it\"" } ?: "null"},
          "mint":${entry.mint?.let { "\"$it\"" } ?: "null"},
          "composition":${entry.composition?.let { "\"$it\"" } ?: "null"},
          "weightGrams":${entry.weightGrams ?: "null"},"diameterMm":${entry.diameterMm ?: "null"},
          "rarity":"${entry.rarity}","description":"${entry.description}",
          "circulatedLowCents":${v.circulated?.low?.cents ?: "null"},
          "circulatedHighCents":${v.circulated?.high?.cents ?: "null"},
          "uncirculatedLowCents":${v.uncirculated?.low?.cents ?: "null"},
          "uncirculatedHighCents":${v.uncirculated?.high?.cents ?: "null"},
          "confidence":"${v.confidence}","basis":"${v.basis}",
          "hints":[${entry.hints.joinToString(",") { """{"name":"${it.name}","description":"${it.description}","whereToLook":"${it.whereToLook}"}""" }}],
          "alternatives":[${entry.alternatives.joinToString(",") { "\"$it\"" }}],"recognized":true}"""
    }
}
