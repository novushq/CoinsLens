package app.novushq.coinlens.ai

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.Logger
import app.novushq.coinlens.common.flatMap
import app.novushq.coinlens.common.safeCall
import app.novushq.coinlens.identify.FieldSchema
import app.novushq.coinlens.identify.IdentifyEngine
import app.novushq.coinlens.identify.IdentifySpec
import app.novushq.coinlens.identify.ImageInput
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withContext

/**
 * Gemini via Firebase AI Logic (Gemini Developer API backend). Generic over
 * [IdentifySpec]: the spec supplies prompt, schema and parser. Failures are
 * surfaced as [AppError]; this engine never substitutes fake data.
 */
class FirebaseGeminiEngine(
    private val modelConfig: ModelConfig,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : IdentifyEngine {

    override suspend fun <T> identify(spec: IdentifySpec<T>, images: List<ImageInput>): AppResult<T> {
        if (images.isEmpty()) {
            return AppResult.Failure(AppError.Validation("images", "At least one photo is required"))
        }
        val prepared = withContext(dispatchers.default) {
            images.map { ImageDownscaler.toJpeg(it.bytes) }
        }
        if (prepared.any { it == null }) {
            return AppResult.Failure(AppError.Validation("images", "A photo could not be read"))
        }
        val modelName = modelConfig.modelName.value
        return withContext(dispatchers.io) {
            safeCall(mapError = { AiErrorMapper.map(it) }) {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = spec.schema.toFirebaseSchema()
                    },
                    systemInstruction = content { text(spec.systemInstruction) },
                )
                val request = content {
                    prepared.filterNotNull().forEach { inlineData(it, ImageDownscaler.MIME_JPEG) }
                    text(spec.userPrompt(images))
                }
                model.generateContent(request).text
            }
        }.flatMap { text ->
            if (text.isNullOrBlank()) {
                logger.w(TAG, "Empty response from $modelName")
                AppResult.Failure(AppError.Parse("The model returned no result"))
            } else {
                spec.parse(text)
            }
        }.also { result ->
            if (result is AppResult.Failure) logger.w(TAG, "identify failed on $modelName: ${result.error}")
        }
    }

    private companion object {
        const val TAG = "FirebaseGeminiEngine"
    }
}

/** Maps the engine-neutral schema onto Firebase AI Logic's [Schema]. Every property is required unless listed in `optional`. */
internal fun FieldSchema.toFirebaseSchema(): Schema = when (this) {
    is FieldSchema.Obj -> Schema.obj(
        properties = properties.mapValues { (_, value) -> value.toFirebaseSchema() },
        optionalProperties = optional,
        description = description,
        nullable = nullable,
    )
    is FieldSchema.Arr -> Schema.array(
        items = items.toFirebaseSchema(),
        description = description,
        nullable = nullable,
        maxItems = maxItems,
    )
    is FieldSchema.Str -> Schema.string(description = description, nullable = nullable)
    is FieldSchema.Enum -> Schema.enumeration(values = values, description = description, nullable = nullable)
    is FieldSchema.Integer -> Schema.integer(description = description, nullable = nullable)
    is FieldSchema.Number -> Schema.double(description = description, nullable = nullable)
    is FieldSchema.Bool -> Schema.boolean(description = description, nullable = nullable)
}
