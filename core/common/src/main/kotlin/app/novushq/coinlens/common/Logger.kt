package app.novushq.coinlens.common

/** Platform-free logging seam. The :app module binds an Android implementation. */
interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, error: Throwable? = null)
    fun e(tag: String, message: String, error: Throwable? = null)
}

object NoopLogger : Logger {
    override fun d(tag: String, message: String) = Unit
    override fun i(tag: String, message: String) = Unit
    override fun w(tag: String, message: String, error: Throwable?) = Unit
    override fun e(tag: String, message: String, error: Throwable?) = Unit
}
