package com.novushq.coinlens.domain

import com.novushq.coinlens.model.Persona
import com.novushq.coinlens.model.ScanAllowance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowanceTest {
    @Test
    fun `fresh install has exactly one free scan`() = runTest {
        val allowance = ObserveScanAllowanceUseCase(FakeEntitlements(), FakePreferences())().first()
        assertEquals(ScanAllowance(isPro = false, freeRemaining = 1, bonusRemaining = 0), allowance)
        assertTrue(allowance.canScan)
    }

    @Test
    fun `used free scan blocks scanning`() = runTest {
        val allowance = ObserveScanAllowanceUseCase(FakeEntitlements(), FakePreferences(used = 1))().first()
        assertEquals(0, allowance.freeRemaining)
        assertFalse(allowance.canScan)
    }

    @Test
    fun `over-consumed counters never go negative`() = runTest {
        val allowance = ObserveScanAllowanceUseCase(FakeEntitlements(), FakePreferences(used = 7, bonus = -2))().first()
        assertEquals(0, allowance.freeRemaining)
        assertEquals(0, allowance.bonusRemaining)
    }

    @Test
    fun `pro can always scan`() = runTest {
        val allowance = ObserveScanAllowanceUseCase(FakeEntitlements(pro = true), FakePreferences(used = 1))().first()
        assertTrue(allowance.canScan)
    }

    @Test
    fun `granting a bonus adds exactly one scan`() = runTest {
        val prefs = FakePreferences(used = 1)
        val observe = ObserveScanAllowanceUseCase(FakeEntitlements(), prefs)
        assertFalse(observe().first().canScan)

        GrantBonusScanUseCase(prefs)()

        assertEquals(1, observe().first().bonusRemaining)
        assertTrue(observe().first().canScan)
    }

    @Test
    fun `completing onboarding persists persona and done flag`() = runTest {
        val prefs = FakePreferences()
        assertTrue(CompleteOnboardingUseCase(prefs)(Persona.DETECTORIST).isSuccess)
        assertEquals(Persona.DETECTORIST, prefs.personaState.value)
        assertTrue(prefs.doneState.value)
    }

    @Test
    fun `skipped quiz still completes onboarding`() = runTest {
        val prefs = FakePreferences()
        CompleteOnboardingUseCase(prefs)(null)
        assertEquals(null, prefs.personaState.value)
        assertTrue(prefs.doneState.value)
    }
}
