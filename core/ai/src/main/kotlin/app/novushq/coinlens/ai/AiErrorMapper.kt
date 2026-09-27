package app.novushq.coinlens.ai

import app.novushq.coinlens.common.AppError
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.ServerException
import java.io.IOException
import java.net.SocketTimeoutException

/** Firebase AI Logic failures → the app's [AppError] vocabulary (network, quota/429, blocked/safety, parse). */
internal object AiErrorMapper {

    fun map(error: Throwable): AppError = when (error) {
        is QuotaExceededException -> AppError.QuotaExceeded(cause = error)
        is PromptBlockedException,
        is ContentBlockedException,
        is ResponseStoppedException,
        -> AppError.ContentBlocked(cause = error)
        is SerializationException -> AppError.Parse(cause = error)
        is RequestTimeoutException, is SocketTimeoutException -> AppError.Timeout(cause = error)
        is ServerException -> if (error.mentions429()) {
            AppError.QuotaExceeded(cause = error)
        } else {
            AppError.Network("The identification service is unavailable", error)
        }
        is IOException -> AppError.Network(cause = error)
        else -> error.cause?.takeIf { it is IOException }?.let { AppError.Network(cause = it) }
            ?: AppError.Unknown(error.message ?: "Identification failed", error)
    }

    private fun Throwable.mentions429(): Boolean {
        val text = message.orEmpty()
        return "429" in text || "RESOURCE_EXHAUSTED" in text
    }
}
