package app.novushq.coinlens.ai

import app.novushq.coinlens.common.Logger
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** Remote Config key and bundled default (also in res/xml/remote_config_defaults.xml). */
const val MODEL_NAME_KEY = "model_name"
const val DEFAULT_MODEL_NAME = "gemini-3.5-flash-lite"

enum class AiMode { FIREBASE, FAKE }

/** What Settings shows under "AI mode". */
data class AiInfo(val mode: AiMode, val modelName: String)

/** Source of the Gemini model name. Reading it never blocks; [refresh] fetches in the background. */
interface ModelConfig {
    val modelName: StateFlow<String>
    fun refresh()
}

/** Fake mode: no Firebase, the default name is only informational. */
class StaticModelConfig(name: String = DEFAULT_MODEL_NAME) : ModelConfig {
    override val modelName: StateFlow<String> = MutableStateFlow(name).asStateFlow()
    override fun refresh() = Unit
}

/**
 * Firebase Remote Config `model_name`. Starts at the bundled default and switches
 * once fetch-and-activate completes, so a scan never waits on the network.
 */
class RemoteModelConfig(
    private val logger: Logger,
    private val isDebug: Boolean,
) : ModelConfig {
    private val name = MutableStateFlow(DEFAULT_MODEL_NAME)
    override val modelName: StateFlow<String> = name.asStateFlow()

    override fun refresh() {
        val config = Firebase.remoteConfig
        config.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (isDebug) DEBUG_FETCH_INTERVAL_S else RELEASE_FETCH_INTERVAL_S
            },
        )
        config.setDefaultsAsync(R.xml.remote_config_defaults)
            .continueWithTask {
                publish(config.getString(MODEL_NAME_KEY))
                config.fetchAndActivate()
            }
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) logger.w(TAG, "Remote Config fetch failed; keeping ${name.value}", task.exception)
                publish(config.getString(MODEL_NAME_KEY))
            }
    }

    private fun publish(value: String) {
        if (value.isNotBlank()) name.value = value.trim()
    }

    private companion object {
        const val TAG = "RemoteModelConfig"
        const val DEBUG_FETCH_INTERVAL_S = 0L
        const val RELEASE_FETCH_INTERVAL_S = 3_600L
    }
}

class AiInfoProvider(private val mode: AiMode, private val config: ModelConfig) {
    val info: Flow<AiInfo> = config.modelName.map { AiInfo(mode, it) }
    val current: AiInfo get() = AiInfo(mode, config.modelName.value)
}
