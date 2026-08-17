package dev.nichidori.saku.data.dao

import androidx.room.Room
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.CreditEntity
import dev.nichidori.saku.data.entity.InstallmentEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Clock

class InstallmentDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: InstallmentDao

    private val categoryId = "cat-1"
    private val creditId = "credit-1"

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        dao = db.installmentDao()
        val now = Clock.System.now().toEpochMilliseconds()
        runTest {
            db.categoryDao().insert(
                CategoryEntity(
                    id = categoryId,
                    name = "Gadget",
                    type = TrxTypeEntity.Expense,
                    createdAt = now,
                    updatedAt = null,
                    parentId = null,
                    icon = null,
                )
            )
            db.creditDao().insert(
                CreditEntity(
                    id = creditId,
                    name = "My Credit",
                    limit = 10_000_000L,
                    currentAmount = 0L,
                    createdAt = now,
                    updatedAt = null,
                )
            )
        }
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private fun sampleInstallment(id: String, nextIndex: Int = 0) = InstallmentEntity(
        id = id,
        description = "iPhone 15",
        categoryId = categoryId,
        creditId = creditId,
        principal = 12_000_000L,
        months = 12,
        monthlyRatePercent = 0.0,
        totalAmount = 12_000_000L,
        monthlyPayment = 1_000_000L,
        lastPayment = 1_000_000L,
        startAt = Clock.System.now().toEpochMilliseconds(),
        dueDay = 15,
        nextIndex = nextIndex,
        createdAt = Clock.System.now().toEpochMilliseconds(),
        updatedAt = null,
    )

    @Test
    fun insertAndGetById_shouldReturnMatchingInstallment() = runTest {
        dao.insert(sampleInstallment("inst-1"))

        val result = dao.getById("inst-1")

        assertNotNull(result)
        assertEquals("iPhone 15", result.description)
        assertEquals(creditId, result.creditId)
        assertEquals(12, result.months)
        assertEquals(1_000_000L, result.monthlyPayment)
    }

    @Test
    fun getById_shouldReturnNullForNonExistentId() = runTest {
        assertNull(dao.getById("non-existent"))
    }

    @Test
    fun getAll_shouldReturnAllInstallments() = runTest {
        dao.insert(sampleInstallment("inst-1"))
        dao.insert(sampleInstallment("inst-2"))

        val result = dao.getAll()

        assertEquals(2, result.size)
    }

    @Test
    fun update_shouldPersistChanges() = runTest {
        dao.insert(sampleInstallment("inst-1"))

        dao.update(sampleInstallment("inst-1", nextIndex = 5))

        val result = dao.getById("inst-1")
        assertEquals(5, result?.nextIndex)
    }

    @Test
    fun deleteById_shouldRemoveInstallment() = runTest {
        dao.insert(sampleInstallment("inst-1"))

        dao.deleteById("inst-1")

        assertNull(dao.getById("inst-1"))
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun getPendingCountByCreditId_shouldCountOnlyPendingInstallments() = runTest {
        dao.insert(sampleInstallment("inst-pending", nextIndex = 3))
        dao.insert(sampleInstallment("inst-almost-done", nextIndex = 11))
        dao.insert(sampleInstallment("inst-completed", nextIndex = 12))

        val result = dao.getPendingCountByCreditId(creditId)

        assertEquals(2, result)
    }

    @Test
    fun getPendingCountByCreditId_shouldReturnZeroForUnknownCredit() = runTest {
        assertEquals(0, dao.getPendingCountByCreditId("unknown-credit"))
    }
}