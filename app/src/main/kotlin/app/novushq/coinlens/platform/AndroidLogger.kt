package app.novushq.coinlens.platform

import android.util.Log
import app.novushq.coinlens.BuildConfig
import app.novushq.coinlens.common.Logger

class AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    override fun i(tag: String, message: String) = Unit.also { Log.i(tag, message) }

    override fun w(tag: String, message: String, error: Throwable?) {
        Log.w(tag, message, error)
    }

    override fun e(tag: String, message: String, error: Throwable?) {
        Log.e(tag, message, error)
    }
}
