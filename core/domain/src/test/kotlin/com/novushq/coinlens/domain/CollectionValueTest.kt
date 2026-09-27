package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppError
import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.model.CollectionItem
import com.novushq.coinlens.model.CoinIdentification
import com.novushq.coinlens.model.Confidence
import com.novushq.coinlens.model.Grade
import com.novushq.coinlens.model.ItemKind
import com.novushq.coinlens.model.Money
import com.novushq.coinlens.model.Persona
import com.novushq.coinlens.model.Rarity
import com.novushq.coinlens.model.ValuedItem
import com.novushq.coinlens.model.ValueEstimate
import com.novushq.coinlens.model.ValueRange
import com.novushq.coinlens.model.usd
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionValueTest {

    @get:Rule val main = MainDispatcherRule()

    private fun id(circ: ValueRange?, unc: ValueRange?) = CoinIdentification(
        kind = ItemKind.COIN, name = "t", country = "c", denomination = "d",
        value = ValueEstimate(circ, unc, Confidence.MEDIUM),
    )

    private fun item(
        id: String = "i",
        folder: String? = null,
        grade: Grade? = null,
        qty: Int = 1,
        price: Money? = null,
    ) = CollectionItem(id, "s", folder, grade, "", price, qty, 0)

    @Test fun `circulated grade uses circulated range`() {
        val summary = ComputeCollectionValueUseCase().invoke(
            listOf(ValuedItem(item(grade = Grade.FINE), id(ValueRange(usd(1.0), usd(3.0)), ValueRange(usd(10.0), usd(20.0))))),
        )
        assertEquals(Money(100), summary.totalLow)
        assertEquals(Money(300), summary.totalHigh)
        assertEquals(Money(200), summary.totalMid)
    }

    @Test fun `uncirculated grade uses uncirculated range`() {
        val summary = ComputeCollectionValueUseCase().invoke(
            listOf(ValuedItem(item(grade = Grade.MINT_STATE), id(ValueRange(usd(1.0), usd(3.0)), ValueRange(usd(10.0), usd(20.0))))),
        )
        assertEquals(Money(1000), summary.totalLow)
        assertEquals(Money(1500), summary.totalMid)
    }

    @Test fun `quantity multiplies and cost accumulates`() {
        val summary = ComputeCollectionValueUseCase().invoke(
            listOf(ValuedItem(item(qty = 3, price = Money(50)), id(ValueRange(usd(1.0), usd(1.0)), null))),
        )
        assertEquals(1, summary.itemCount)
        assertEquals(Money(300), summary.totalMid)
        assertEquals(Money(150), summary.totalCost)
    }

    @Test fun `missing identification counts item but adds no value`() {
        val summary = ComputeCollectionValueUseCase().invoke(listOf(ValuedItem(item(), null)))
        assertEquals(1, summary.itemCount)
        assertEquals(Money(0), summary.totalMid)
    }

    @Test fun `null folder buckets to unsorted`() {
        val summary = ComputeCollectionValueUseCase().invoke(
            listOf(ValuedItem(item(folder = "f1"), id(ValueRange(usd(1.0), usd(1.0)), null))),
        )
        assertEquals(Money(100), summary.byFolder[ComputeCollectionValueUseCase.UNSORTED])
    }

    @Test fun `add to collection validates quantity and price`() = runTest(main.dispatcher) {
        val useCase = AddToCollectionUseCase(FakeCollectionRepository())
        val badQty = useCase(AddToCollectionUseCase.Params("s", quantity = 0))
        assertTrue(badQty is AppResult.Failure && badQty.error is AppError.Validation)
        val badPrice = useCase(AddToCollectionUseCase.Params("s", purchasePrice = Money(-5)))
        assertTrue(badPrice is AppResult.Failure && badPrice.error is AppError.Validation)
        val ok = useCase(AddToCollectionUseCase.Params("s", grade = Grade.GOOD, quantity = 2))
        assertTrue(ok is AppResult.Success)
    }

    @Test fun `bonus scan grants exactly one`() = runTest(main.dispatcher) {
        val prefs = FakePreferences()
        GrantBonusScanUseCase(prefs)()
        GrantBonusScanUseCase(prefs)()
        assertEquals(2, prefs.bonusScans.first())
    }

    @Test fun `onboarding persists persona and done`() = runTest(main.dispatcher) {
        val prefs = FakePreferences()
        CompleteOnboardingUseCase(prefs)(Persona.DETECTORIST)
        assertEquals(Persona.DETECTORIST, prefs.persona.first())
        assertEquals(true, prefs.onboardingDone.first())
    }

    @Test fun `rarity defaults unknown and money formats`() {
        assertEquals(Rarity.UNKNOWN, id(null, ValueRange(usd(1.0), usd(2.0))).rarity)
        assertEquals("\$12.50", Money(1250).toDisplayString())
    }
}
