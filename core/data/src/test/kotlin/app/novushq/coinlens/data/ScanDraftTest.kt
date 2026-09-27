package app.novushq.coinlens.data

import app.novushq.coinlens.identify.ImageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDraftTest {

    @Test
    fun `draft maps sides to image roles and clears`() {
        val draft = ScanDraft()
        assertFalse(draft.current.isReady)

        draft.setObverse(byteArrayOf(1))
        draft.setReverse(byteArrayOf(2))

        assertTrue(draft.current.isReady)
        assertEquals(listOf(ImageRole.PRIMARY, ImageRole.SECONDARY), draft.current.toImages().map { it.role })
        assertEquals(ScanDraftState(byteArrayOf(1), byteArrayOf(2)), draft.state.value)

        draft.clear()
        assertTrue(draft.current.toImages().isEmpty())
    }
}
