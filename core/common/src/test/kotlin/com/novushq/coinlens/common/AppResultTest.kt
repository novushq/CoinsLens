package com.novushq.coinlens.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun `map transforms success payload`() {
        val result = AppResult.Success(2).map { it * 3 }
        assertEquals(AppResult.Success(6), result)
    }

    @Test
    fun `map leaves failure untouched`() {
        val error = AppError.NotFound()
        val result: AppResult<Int> = AppResult.Failure(error)
        assertEquals(error, result.map { it * 3 }.errorOrNull())
    }

    @Test
    fun `flatMap chains results`() {
        val result = AppResult.Success(4).flatMap { (it + 1).asSuccess() }
        assertEquals(5, result.getOrNull())
    }

    @Test
    fun `toUiState maps empty payloads to Empty`() {
        val state = AppResult.Success(emptyList<String>()).toUiState { it.isEmpty() }
        assertTrue(state is UiState.Empty)
    }
}
