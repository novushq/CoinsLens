package app.novushq.coinlens.domain

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.ValuedItem
import app.novushq.coinlens.model.usd
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CollectionValueTest {
    private val compute = ComputeCollectionValueUseCase()
    private val t0 = Instant.EPOCH

    private fun valued(
        grade: Grade? = null,
        quantity: Int = 1,
        folderId: String? = null,
        price: Money? = null,
        identification: app.novushq.coinlens.model.CoinIdentification = coin(),
    ) = ValuedItem(
        item = CollectionItem("i-$grade-$quantity-$folderId", "s", folderId, grade, "", price, quantity, t0),
        scan = ScanRecord("s", t0, "/o.jpg", null, identification),
    )

    @Test
    fun `empty collection is zero`() {
        assertEquals(CollectionSummary(), compute(emptyList()))
    }

    @Test
    fun `circulated grade uses circulated range`() {
        val s = compute(listOf(valued(Grade.FINE)))
        assertEquals(usd(1.0), s.totalLow)
        assertEquals(usd(3.0), s.totalHigh)
        assertEquals(usd(2.0), s.totalMid)
    }

    @Test
    fun `AU, MS and proof use uncirculated range`() {
        listOf(Grade.ABOUT_UNCIRCULATED, Grade.MINT_STATE, Grade.PROOF).forEach { grade ->
            assertEquals(usd(15.0), compute(listOf(valued(grade))).totalMid)
        }
    }

    @Test
    fun `ungraded items use circulated range`() {
        assertEquals(usd(2.0), compute(listOf(valued(grade = null))).totalMid)
    }

    @Test
    fun `missing band falls back to the other band`() {
        val onlyUnc = coin(circulated = null)
        assertEquals(usd(15.0), compute(listOf(valued(Grade.GOOD, identification = onlyUnc))).totalMid)
        val onlyCirc = coin(uncirculated = null)
        assertEquals(usd(2.0), compute(listOf(valued(Grade.MINT_STATE, identification = onlyCirc))).totalMid)
    }

    @Test
    fun `quantity multiplies value and cost`() {
        val s = compute(listOf(valued(Grade.FINE, quantity = 3, price = usd(0.5))))
        assertEquals(usd(6.0), s.totalMid)
        assertEquals(usd(1.5), s.totalCost)
        assertEquals(3, s.itemCount)
    }

    @Test
    fun `midpoint of odd cents rounds down`() {
        val odd = coin(circulated = ValueRange(Money(1), Money(2)), uncirculated = null)
        assertEquals(Money(1), compute(listOf(valued(identification = odd))).totalMid)
    }

    @Test
    fun `totals are grouped by folder with unsorted bucket`() {
        val s = compute(listOf(valued(folderId = "a"), valued(folderId = "a", quantity = 2), valued(folderId = null)))
        assertEquals(usd(6.0), s.byFolder["a"])
        assertEquals(usd(2.0), s.byFolder[CollectionSummary.UNSORTED])
        assertEquals(usd(8.0), s.totalMid)
    }

    @Test
    fun `unrecognized scans add no value but keep cost`() {
        val s = compute(listOf(valued(identification = coin(recognized = false), price = usd(1.0))))
        assertEquals(Money.ZERO, s.totalMid)
        assertEquals(usd(1.0), s.totalCost)
    }

    @Test
    fun `add to collection validates input`() = runTest {
        val repo = FakeCollectionRepository()
        val add = AddToCollectionUseCase(repo, clock = { t0 }, newId = { "item-1" })

        assertTrue(add(AddToCollectionUseCase.Params(scanId = "")).errorOrNull() is AppError.Validation)
        assertTrue(add(AddToCollectionUseCase.Params("s", quantity = 0)).errorOrNull() is AppError.Validation)
        assertTrue(add(AddToCollectionUseCase.Params("s", purchasePrice = Money(-1))).errorOrNull() is AppError.Validation)
        assertTrue(repo.items.isEmpty())
    }

    @Test
    fun `add to collection stores a trimmed item`() = runTest {
        val repo = FakeCollectionRepository()
        val add = AddToCollectionUseCase(repo, clock = { t0 }, newId = { "item-1" })

        val result = add(AddToCollectionUseCase.Params("s", "f", Grade.MINT_STATE, "  nice luster ", usd(4.0), 2))

        assertEquals("item-1", result.getOrNull())
        val item = repo.items.getValue("item-1")
        assertEquals("nice luster", item.gradeNotes)
        assertEquals(2, item.quantity)
        assertEquals(t0, item.addedAt)
    }
}
