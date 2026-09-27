package app.novushq.coinlens.common

/** Every failure this app can express. Keep it exhaustive and UI-mappable. */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null,
) {
    data class Network(
        override val message: String = "No connection",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Timeout(
        override val message: String = "Request timed out",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Http(
        val code: Int,
        override val message: String = "Server error",
        val body: String? = null,
    ) : AppError(message)

    data class Storage(
        override val message: String = "Storage failure",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class NotFound(override val message: String = "Not found") : AppError(message)

    /** The user has no free, bonus or Pro scans left. UI routes to the paywall. */
    data class ScanLimitReached(
        override val message: String = "No scans left",
    ) : AppError(message)

    /** HTTP 429 / quota exhausted on the AI backend. Retry later; not the user's allowance. */
    data class QuotaExceeded(
        override val message: String = "Too many requests",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    /** Model refused the content (safety filters). */
    data class ContentBlocked(
        override val message: String = "Couldn't identify this image",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    /** Response arrived but could not be parsed into the schema. */
    data class Parse(
        override val message: String = "Couldn't read the result",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Validation(
        val field: String,
        override val message: String,
    ) : AppError(message)

    data class Unknown(
        override val message: String = "Something went wrong",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)
}
