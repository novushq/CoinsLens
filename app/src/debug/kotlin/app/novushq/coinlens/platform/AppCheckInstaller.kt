package app.novushq.coinlens.platform

import app.novushq.coinlens.common.Logger
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug builds: App Check debug provider. The SDK prints the token to logcat on first use. */
internal object AppCheckInstaller {
    fun install(logger: Logger) {
        Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        logger.i(
            TAG,
            "App Check debug provider installed. Register the debug token printed by DebugAppCheckProvider " +
                "in Firebase console > App Check > Apps > Manage debug tokens.",
        )
    }

    private const val TAG = "AppCheck"
}
