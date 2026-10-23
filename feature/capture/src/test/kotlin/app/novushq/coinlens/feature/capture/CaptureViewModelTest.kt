package app.novushq.coinlens.feature.capture

import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.data.ScanDraft
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.testing.FakeEntitlements
import app.novushq.coinlens.testing.FakePreferences
import app.novushq.coinlens.testing.MainDispatcherRule
import app.cash.turbine.test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaptureViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private fun viewModel(
        used: Int = 0,
        bonus: Int = 0,
        pro: Boolean = false,
        draft: ScanDraft = ScanDraft(),
    ) = CaptureViewModel(
        ObserveScanAllowanceUseCase(FakeEntitlements(pro), FakePreferences(used, bonus)),
        draft,
    )

    private fun content(vm: CaptureViewModel): CaptureContent =
        (vm.state.value as UiState.Success).data

    @Test
    fun `starts ready to capture when allowance remains`() {
        val vm = viewModel()
        val current = content(vm)
        assertFalse(current.quotaExhausted)
        assertEquals(CaptureStep.FRONT, current.step)
        assertFalse(current.canFinish)
    }

    @Test
    fun `exhausted allowance flags quota and redirects once`() {
        val vm = viewModel(used = 1)
        assertTrue(content(vm).quotaExhausted)
        vm.events.test {
            assertEquals(CaptureEvent.NavigatePaywall, awaitItem())
        }
    }

    @Test
    fun `front photo advances to back then done writes the draft`() {
        val draft = ScanDraft()
        val vm = viewModel(draft = draft)
        val front = byteArrayOf(1, 2, 3)
        val back = byteArrayOf(4, 5, 6)

        vm.onPhotoSaved(front)
        assertEquals(CaptureStep.BACK, content(vm).step)
        assertArrayEquals(front, content(vm).frontBytes)
        assertTrue(content(vm).canFinish)

        vm.onPhotoSaved(back)
        assertArrayEquals(back, content(vm).backBytes)

        vm.events.test {
            vm.onDone()
            assertEquals(CaptureEvent.NavigateResult, awaitItem())
        }
        assertArrayEquals(front, draft.current.obverse)
        assertArrayEquals(back, draft.current.reverse)
    }

    @Test
    fun `skipping back finishes with the front photo alone`() {
        val draft = ScanDraft()
        val vm = viewModel(draft = draft)
        val front = byteArrayOf(7, 8)
        vm.onPhotoSaved(front)

        vm.events.test {
            vm.onSkipBack()
            assertEquals(CaptureEvent.NavigateResult, awaitItem())
        }
        assertArrayEquals(front, draft.current.obverse)
        assertEquals(null, draft.current.reverse)
    }

    @Test
    fun `done without a front photo does nothing`() {
        val draft = ScanDraft()
        val vm = viewModel(draft = draft)
        vm.onDone()
        assertEquals(null, draft.current.obverse)
    }

    @Test
    fun `retake clears the current side`() {
        val vm = viewModel()
        vm.onPhotoSaved(byteArrayOf(1))
        assertEquals(CaptureStep.BACK, content(vm).step)
        vm.onRetake()
        assertEquals(null, content(vm).backBytes)
        assertEquals(CaptureStep.BACK, content(vm).step)
    }

    @Test
    fun `photo error surfaces then retry returns to capture`() {
        val vm = viewModel()
        vm.onPhotoSaved(byteArrayOf(1))
        vm.onPhotoError()
        assertTrue(vm.state.value is UiState.Error)
        vm.retry()
        val current = content(vm)
        assertEquals(CaptureStep.BACK, current.step)
        assertArrayEquals(byteArrayOf(1), current.frontBytes)
    }

    @Test
    fun `quality flags update without blocking`() {
        val vm = viewModel()
        vm.onQuality(glare = true, blurry = false)
        assertTrue(content(vm).glareWarning)
        assertFalse(content(vm).blurWarning)
        vm.onQuality(glare = false, blurry = true)
        assertFalse(content(vm).glareWarning)
        assertTrue(content(vm).blurWarning)
    }
}
