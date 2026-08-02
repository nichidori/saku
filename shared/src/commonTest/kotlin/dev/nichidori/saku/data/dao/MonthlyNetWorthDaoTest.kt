package dev.nichidori.saku.data.dao

import androidx.room.Room
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.MonthlyNetWorthEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonthlyNetWorthDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MonthlyNetWorthDao

    private val jan2025 = MonthlyNetWorthEntity(
        year = 2025,
        month = 1,
        netWorth = 100_000L,
        createdAt = 1_000L,
        updatedAt = null
    )

    private val feb2025 = MonthlyNetWorthEntity(
        year = 2025,
        month = 2,
        netWorth = 150_000L,
        createdAt = 2_000L,
        updatedAt = 3_000L
    )

    private val mar2025 = MonthlyNetWorthEntity(
        year = 2025,
        month = 3,
        netWorth = 120_000L,
        createdAt = 4_000L,
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        dao = db.monthlyNetWorthDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGetByYearMonth_shouldReturnMatchingRecord() = runTest {
        dao.upsert(feb2025)

        val result = dao.getByYearMonth(year = 2025, month = 2)

        assertEquals(feb2025, result)
    }

    @Test
    fun getByYearMonth_shouldReturnNullIfNotFound() = runTest {
        val result = dao.getByYearMonth(year = 2025, month = 12)

        assertNull(result)
    }

    @Test
    fun upsert_withExistingPrimaryKey_shouldReplaceRecord() = runTest {
        dao.upsert(jan2025)
        val replacement = jan2025.copy(netWorth = 200_000L, updatedAt = 9_000L)

        dao.upsert(replacement)

        val result = dao.getByYearMonth(year = 2025, month = 1)
        assertEquals(replacement, result)
    }

    @Test
    fun getAll_shouldReturnRecordsOrderedByYearThenMonth() = runTest {
        dao.upsert(mar2025)
        dao.upsert(jan2025)
        dao.upsert(feb2025)

        val result = dao.getAll()

        assertEquals(listOf(jan2025, feb2025, mar2025), result)
    }

    @Test
    fun getFromYearMonth_shouldReturnRecordsFromBoundaryInclusive() = runTest {
        dao.upsert(jan2025)
        dao.upsert(feb2025)
        dao.upsert(mar2025)
        dao.upsert(jan2025.copy(year = 2026, month = 1, createdAt = 5_000L))

        val result = dao.getFromYearMonth(year = 2025, month = 2)

        assertEquals(listOf(feb2025, mar2025, jan2025.copy(year = 2026, month = 1, createdAt = 5_000L)), result)
    }

    @Test
    fun deleteAll_shouldClearAllRecords() = runTest {
        dao.upsert(jan2025)
        dao.upsert(feb2025)

        dao.deleteAll()

        assertEquals(emptyList(), dao.getAll())
    }
}
