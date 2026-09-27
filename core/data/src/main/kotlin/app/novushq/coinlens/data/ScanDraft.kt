package app.novushq.coinlens.data

import app.novushq.coinlens.identify.ImageInput
import app.novushq.coinlens.identify.ImageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Photos captured for the scan in progress. Capture writes it, Result reads it, both via Koin (single). */
data class ScanDraftState(
    val obverse: ByteArray? = null,
    val reverse: ByteArray? = null,
) {
    val isReady: Boolean get() = obverse != null

    fun toImages(): List<ImageInput> = buildList {
        obverse?.let { add(ImageInput(it, role = ImageRole.PRIMARY)) }
        reverse?.let { add(ImageInput(it, role = ImageRole.SECONDARY)) }
    }

    override fun equals(other: Any?): Boolean =
        other is ScanDraftState &&
            obverse.contentEqualsNullable(other.obverse) &&
            reverse.contentEqualsNullable(other.reverse)

    override fun hashCode(): Int = 31 * (obverse?.contentHashCode() ?: 0) + (reverse?.contentHashCode() ?: 0)

    private fun ByteArray?.contentEqualsNullable(other: ByteArray?) =
        if (this == null || other == null) this === other else contentEquals(other)
}

class ScanDraft {
    private val _state = MutableStateFlow(ScanDraftState())
    val state: StateFlow<ScanDraftState> = _state.asStateFlow()

    val current: ScanDraftState get() = _state.value

    fun setObverse(bytes: ByteArray) = _state.update { it.copy(obverse = bytes) }

    fun setReverse(bytes: ByteArray?) = _state.update { it.copy(reverse = bytes) }

    fun clear() {
        _state.value = ScanDraftState()
    }
}
