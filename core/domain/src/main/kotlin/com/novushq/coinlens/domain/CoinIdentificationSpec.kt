package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppError
import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.identify.FieldSchema
import com.novushq.coinlens.identify.IdentifySpec
import com.novushq.coinlens.identify.ImageInput
import com.novushq.coinlens.model.CoinIdentification
import com.novushq.coinlens.model.Confidence
import com.novushq.coinlens.model.ItemKind
import com.novushq.coinlens.model.usd
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
            "Be conservative: report values as low-high ranges in US dollars, never single points, " +
            "and give both a circulated range (worn, typical finds) and an uncirculated range when " +
            "the piece was issued for circulation. Use LOW confidence when the date, mint mark or " +
            "type is not clearly readable. List error or variety hints only when visible evidence " +
            "in the photo supports them; they are possibilities, not findings. If the image is not " +
            "a coin or banknote, return recognized=false with a one-line description of what it is. " +
            "Give at most 3 alternative identifications."

    override fun userPrompt(images: List<ImageInput>): String = buildString {
        append(
            if (images.size > 1) {
                "The first photo is the front (obverse), the second the back (reverse) of one item. "
            } else {
                "One photo of the front (obverse) of one item. "
            },
        )
        append("Identify it. ")
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
            "circulatedLowUsd" to FieldSchema.Number("circulated low, US dollars", nullable = true),
            "circulatedHighUsd" to FieldSchema.Number("circulated high, US dollars", nullable = true),
            "uncirculatedLowUsd" to FieldSchema.Number("uncirculated low, US dollars", nullable = true),
            "uncirculatedHighUsd" to FieldSchema.Number("uncirculated high, US dollars", nullable = true),
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
            "circulatedLowUsd", "circulatedHighUsd",
            "uncirculatedLowUsd", "uncirculatedHighUsd", "basis",
        ),
    )

    @Serializable
    internal data class HintJson(
        val name: String = "",
        val description: String? = null,
        val whereToLook: String? = null,
    )

    @Serializable
    internal data class CoinJson(
        val kind: String = "COIN",
        val name: String = "",
        val country: String = "",
        val denomination: String = "",
        val year: Int? = null,
        val yearText: String? = null,
        val mintMark: String? = null,
        val mint: String? = null,
        val composition: String? = null,
        val weightGrams: Double? = null,
        val diameterMm: Double? = null,
        val rarity: String = "UNKNOWN",
        val description: String = "",
        val circulatedLowUsd: Double? = null,
        val circulatedHighUsd: Double? = null,
        val uncirculatedLowUsd: Double? = null,
        val uncirculatedHighUsd: Double? = null,
        val confidence: String = "LOW",
        val basis: String? = null,
        val hints: List<HintJson> = emptyList(),
        val alternatives: List<String> = emptyList(),
        val recognized: Boolean = true,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    override fun parse(json: String): AppResult<CoinIdentification> {
        val body = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val dto = try {
            this.json.decodeFromString<CoinJson>(body)
        } catch (e: IllegalArgumentException) {
            // SerializationException extends IllegalArgumentException.
            return AppResult.Failure(AppError.Parse(cause = e))
        }
        if (!dto.recognized) return AppResult.Success(CoinIdentification.unrecognized(dto.description))
        if (dto.name.isBlank()) return AppResult.Failure(AppError.Parse("Missing coin name"))
        val circulated = rangeOrNull(dto.circulatedLowUsd, dto.circulatedHighUsd)
        val uncirculated = rangeOrNull(dto.uncirculatedLowUsd, dto.uncirculatedHighUsd)
        if (circulated == null && uncirculated == null) {
            return AppResult.Failure(AppError.Parse("Missing value range"))
        }
        return AppResult.Success(
            CoinIdentification(
                kind = if (dto.kind.equals("BANKNOTE", true)) ItemKind.BANKNOTE else ItemKind.COIN,
                name = dto.name,
                country = dto.country,
                denomination = dto.denomination,
                year = dto.year?.takeIf { it in 1..2100 },
                yearText = dto.yearText?.takeIf { it.isNotBlank() } ?: dto.year?.toString().orEmpty(),
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
                    basis = dto.basis.orEmpty(),
                ),
                hints = dto.hints.filter { it.name.isNotBlank() }.take(5).map {
                    VarietyHint(it.name.trim(), it.description.orEmpty(), it.whereToLook.orEmpty())
                },
                alternatives = dto.alternatives.filter { it.isNotBlank() }.take(3),
                recognized = true,
            ),
        )
    }

    /** Inverted or negative model output is coerced (min/max, floor 0), never fatal. */
    private fun rangeOrNull(low: Double?, high: Double?): ValueRange? {
        val bounds = listOfNotNull(low, high).filter { it.isFinite() }.map { usd(it.coerceAtLeast(0.0)) }
        if (bounds.isEmpty()) return null
        return ValueRange(bounds.minBy { it.cents }, bounds.maxBy { it.cents })
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
