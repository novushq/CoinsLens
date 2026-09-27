package app.novushq.coinlens.platform

import app.novushq.coinlens.common.Logger
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Release builds: Play Integrity attestation. */
internal object AppCheckInstaller {
    fun install(logger: Logger) {
        Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        logger.i(TAG, "App Check Play Integrity provider installed")
    }

    private const val TAG = "AppCheck"
}
