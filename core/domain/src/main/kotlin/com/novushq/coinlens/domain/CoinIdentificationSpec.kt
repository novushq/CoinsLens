package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppError
import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.identify.FieldSchema
import com.novushq.coinlens.identify.IdentifySpec
import com.novushq.coinlens.identify.ImageInput
import com.novushq.coinlens.model.CoinIdentification
import com.novushq.coinlens.model.Confidence
import com.novushq.coinlens.model.ItemKind
import com.novushq.coinlens.model.Money
import com.novushq.coinlens.model.Rarity
import com.novushq.coinlens.model.ValueEstimate
import com.novushq.coinlens.model.ValueRange
import com.novushq.coinlens.model.VarietyHint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * CoinLens prompt spec for the generic Identify Engine. Conservative by design:
 * ranges not points, LOW confidence when unsure, hints only on visible evidence,
 * recognized=false for non-coins.
 */
class CoinIdentificationSpec : IdentifySpec<CoinIdentification> {

    override val systemInstruction: String =
        "You are a numismatist identifying coins and banknotes from photos. " +
            "Be conservative: report value as low-high ranges in USD cents, never single points. " +
            "Use LOW confidence when unsure. List variety or error hints only when visible " +
            "evidence exists, phrased as possibilities. If the image is not a coin or banknote, " +
            "return recognized=false. Up to 3 alternatives in 'alternatives'."

    override fun userPrompt(images: List<ImageInput>): String = buildString {
        append("Identify this ${if (images.size > 1) "coin shown obverse then reverse" else "coin or banknote"}. ")
        append("Return kind, name, country, denomination, year, mint mark and mint, composition, ")
        append("weight, diameter, rarity, description, circulated and uncirculated USD value ranges, ")
        append("confidence, possible variety hints and alternatives.")
    }

    override val schema: FieldSchema.Obj = FieldSchema.Obj(
        properties = mapOf(
            "kind" to FieldSchema.Enum(listOf("COIN", "BANKNOTE"), "coin or banknote"),
            "name" to FieldSchema.Str("catalogue name, e.g. Lincoln cent"),
            "country" to FieldSchema.Str("issuing country"),
            "denomination" to FieldSchema.Str("face value, e.g. 1 cent"),
            "year" to FieldSchema.Integer("year struck, if readable", nullable = true),
            "yearText" to FieldSchema.Str("year as printed, e.g. 'no date'", nullable = true),
            "mintMark" to FieldSchema.Str("mint mark letter, if visible", nullable = true),
            "mint" to FieldSchema.Str("mint facility, if known", nullable = true),
            "composition" to FieldSchema.Str("metal/paper composition", nullable = true),
            "weightGrams" to FieldSchema.Number("weight in grams", nullable = true),
            "diameterMm" to FieldSchema.Number("diameter in mm", nullable = true),
            "rarity" to FieldSchema.Enum(listOf("COMMON", "SCARCE", "RARE", "VERY_RARE", "UNKNOWN")),
            "description" to FieldSchema.Str("one short paragraph"),
            "circulatedLowCents" to FieldSchema.Integer("circulated low USD cents", nullable = true),
            "circulatedHighCents" to FieldSchema.Integer("circulated high USD cents", nullable = true),
            "uncirculatedLowCents" to FieldSchema.Integer("uncirculated low USD cents", nullable = true),
            "uncirculatedHighCents" to FieldSchema.Integer("uncirculated high USD cents", nullable = true),
            "confidence" to FieldSchema.Enum(listOf("HIGH", "MEDIUM", "LOW")),
            "basis" to FieldSchema.Str("one line on what the range is based on", nullable = true),
            "hints" to FieldSchema.Arr(
                FieldSchema.Obj(
                    mapOf(
                        "name" to FieldSchema.Str("variety name without 'Possible'"),
                        "description" to FieldSchema.Str("short description", nullable = true),
                        "whereToLook" to FieldSchema.Str("where to look on the coin", nullable = true),
                    ),
                    optional = listOf("description", "whereToLook"),
                ),
                maxItems = 5,
            ),
            "alternatives" to FieldSchema.Arr(FieldSchema.Str(), maxItems = 3),
            "recognized" to FieldSchema.Bool("false when not a coin or banknote"),
        ),
        optional = listOf(
            "year", "yearText", "mintMark", "mint", "composition", "weightGrams", "diameterMm",
            "circulatedLowCents", "circulatedHighCents",
            "uncirculatedLowCents", "uncirculatedHighCents", "basis",
        ),
    )

    @Serializable
    internal data class HintJson(
        val name: String = "",
        val description: String = "",
        val whereToLook: String = "",
    )

    @Serializable
    internal data class CoinJson(
        val kind: String = "COIN",
        val name: String = "",
        val country: String = "",
        val denomination: String = "",
        val year: Int? = null,
        val yearText: String = "",
        val mintMark: String? = null,
        val mint: String? = null,
        val composition: String? = null,
        val weightGrams: Double? = null,
        val diameterMm: Double? = null,
        val rarity: String = "UNKNOWN",
        val description: String = "",
        val circulatedLowCents: Long? = null,
        val circulatedHighCents: Long? = null,
        val uncirculatedLowCents: Long? = null,
        val uncirculatedHighCents: Long? = null,
        val confidence: String = "LOW",
        val basis: String = "",
        val hints: List<HintJson> = emptyList(),
        val alternatives: List<String> = emptyList(),
        val recognized: Boolean = true,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    override fun parse(json: String): AppResult<CoinIdentification> {
        val dto = try {
            this.json.decodeFromString<CoinJson>(json)
        } catch (e: Exception) {
            return AppResult.Failure(AppError.Parse(cause = e))
        }
        if (!dto.recognized) return AppResult.Success(CoinIdentification.unrecognized(dto.description))
        if (dto.name.isBlank()) return AppResult.Failure(AppError.Parse("Missing coin name"))
        val circulated = rangeOrNull(dto.circulatedLowCents, dto.circulatedHighCents)
        val uncirculated = rangeOrNull(dto.uncirculatedLowCents, dto.uncirculatedHighCents)
        if (circulated == null && uncirculated == null) {
            return AppResult.Failure(AppError.Parse("Missing value range"))
        }
        return AppResult.Success(
            CoinIdentification(
                kind = if (dto.kind.equals("BANKNOTE", true)) ItemKind.BANKNOTE else ItemKind.COIN,
                name = dto.name,
                country = dto.country,
                denomination = dto.denomination,
                year = dto.year,
                yearText = dto.yearText,
                mintMark = dto.mintMark?.ifBlank { null },
                mint = dto.mint?.ifBlank { null },
                composition = dto.composition?.ifBlank { null },
                weightGrams = dto.weightGrams?.takeIf { it > 0 },
                diameterMm = dto.diameterMm?.takeIf { it > 0 },
                rarity = parseRarity(dto.rarity),
                description = dto.description,
                value = ValueEstimate(
                    circulated = circulated,
                    uncirculated = uncirculated,
                    confidence = parseConfidence(dto.confidence),
                    basis = dto.basis,
                ),
                hints = dto.hints.filter { it.name.isNotBlank() }.take(5).map {
                    VarietyHint(it.name, it.description, it.whereToLook)
                },
                alternatives = dto.alternatives.filter { it.isNotBlank() }.take(3),
                recognized = true,
            ),
        )
    }

    /** Inverted or negative model output is coerced (min/max, floor 0), never fatal. */
    private fun rangeOrNull(low: Long?, high: Long?): ValueRange? {
        if (low == null && high == null) return null
        val lo = maxOf(0, minOf(low ?: high!!, high ?: low!!))
        val hi = maxOf(0, maxOf(low ?: high!!, high ?: low!!))
        return ValueRange(Money(lo), Money(hi))
    }

    private fun parseConfidence(raw: String): Confidence = when (raw.uppercase()) {
        "HIGH" -> Confidence.HIGH
        "MEDIUM" -> Confidence.MEDIUM
        else -> Confidence.LOW
    }

    private fun parseRarity(raw: String): Rarity = when (raw.uppercase()) {
        "COMMON" -> Rarity.COMMON
        "SCARCE" -> Rarity.SCARCE
        "RARE" -> Rarity.RARE
        "VERY_RARE" -> Rarity.VERY_RARE
        else -> Rarity.UNKNOWN
    }
}
