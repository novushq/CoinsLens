package app.novushq.coinlens.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.data.ScanDraft
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.model.ScanAllowance
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class CaptureStep { FRONT, BACK }

/**
 * Content for the capture flow. Photos are held here until Done, when both
 * are written to [ScanDraft] together; [quotaExhausted] renders the Empty
 * state instead of the camera.
 */
data class CaptureContent(
    val step: CaptureStep = CaptureStep.FRONT,
    val frontBytes: ByteArray? = null,
    val backBytes: ByteArray? = null,
    val glareWarning: Boolean = false,
    val blurWarning: Boolean = false,
    val quotaExhausted: Boolean = false,
) {
    val currentBytes: ByteArray? get() = if (step == CaptureStep.FRONT) frontBytes else backBytes
    val canFinish: Boolean get() = frontBytes != null

    override fun equals(other: Any?): Boolean =
        other is CaptureContent &&
            step == other.step &&
            frontBytes.contentEqualsNullable(other.frontBytes) &&
            backBytes.contentEqualsNullable(other.backBytes) &&
            glareWarning == other.glareWarning &&
            blurWarning == other.blurWarning &&
            quotaExhausted == other.quotaExhausted

    override fun hashCode(): Int {
        var result = step.hashCode()
        result = 31 * result + (frontBytes?.contentHashCode() ?: 0)
        result = 31 * result + (backBytes?.contentHashCode() ?: 0)
        result = 31 * result + glareWarning.hashCode()
        result = 31 * result + blurWarning.hashCode()
        result = 31 * result + quotaExhausted.hashCode()
        return result
    }

    private fun ByteArray?.contentEqualsNullable(other: ByteArray?) =
        if (this == null || other == null) this === other else contentEquals(other)
}

sealed interface CaptureEvent {
    data object NavigatePaywall : CaptureEvent
    data object NavigateResult : CaptureEvent
}

class CaptureViewModel(
    private val observeAllowance: ObserveScanAllowanceUseCase,
    private val draft: ScanDraft,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<CaptureContent>>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _events = Channel<CaptureEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastAllowance: ScanAllowance? = null
    private var lastContent: CaptureContent = CaptureContent()
    private var paywallRedirectSent = false

    init {
        viewModelScope.launch {
            observeAllowance().collect { allowance ->
                lastAllowance = allowance
                onAllowance(allowance)
            }
        }
    }

    /** Re-render from the last allowance emission (retry entry point). */
    fun retry() {
        val allowance = lastAllowance
        if (allowance == null) {
            _state.value = UiState.Loading
        } else {
            render(lastContent.copy(quotaExhausted = !allowance.canScan))
        }
    }

    /** Live preview quality flags from the ImageAnalysis analyzer; advisory only. */
    fun onQuality(glare: Boolean, blurry: Boolean) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.glareWarning == glare && current.blurWarning == blurry) return
        render(current.copy(glareWarning = glare, blurWarning = blurry))
    }

    /** A cropped/compressed photo for the current step, from camera or gallery. */
    fun onPhotoSaved(bytes: ByteArray) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        render(
            if (current.step == CaptureStep.FRONT) {
                current.copy(frontBytes = bytes, step = CaptureStep.BACK)
            } else {
                current.copy(backBytes = bytes)
            },
        )
    }

    /** Discard the current side's photo and stay on the same step. */
    fun onRetake() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        render(
            if (current.step == CaptureStep.FRONT) current.copy(frontBytes = null)
            else current.copy(backBytes = null),
        )
    }

    fun onSwitchSide() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        render(
            current.copy(step = if (current.step == CaptureStep.FRONT) CaptureStep.BACK else CaptureStep.FRONT),
        )
    }

    /** Back is optional: finish with the front photo alone. */
    fun onSkipBack() = onDone()

    /** Photo pipeline failure surfaces the Error state; retry returns to capture. */
    fun onPhotoError() {
        _state.value = UiState.Error(
            AppError.Unknown("Could not read that photo."),
            retryable = true,
        )
    }

    /** Write both sides into ScanDraft and leave for Result(scanId = null). */
    fun onDone() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        val front = current.frontBytes ?: return
        draft.setObverse(front)
        draft.setReverse(current.backBytes)
        _events.trySend(CaptureEvent.NavigateResult)
    }

    private fun onAllowance(allowance: ScanAllowance) {
        if (!allowance.canScan) {
            render(lastContent.copy(quotaExhausted = true))
            if (!paywallRedirectSent) {
                paywallRedirectSent = true
                _events.trySend(CaptureEvent.NavigatePaywall)
            }
        } else {
            paywallRedirectSent = false
            render(lastContent.copy(quotaExhausted = false))
        }
    }

    private fun render(content: CaptureContent) {
        lastContent = content
        _state.value = UiState.Success(content)
    }
}
