package dev.nichidori.saku.data.dao

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.AccountEntity
import dev.nichidori.saku.data.entity.AccountTypeEntity
import dev.nichidori.saku.data.entity.MonthlyAccountBalanceEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val cashAccount = AccountEntity(
    id = "acc-cash",
    name = "Cash",
    initialAmount = 0,
    currentAmount = 0,
    type = AccountTypeEntity.Cash,
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

private val bankAccount = AccountEntity(
    id = "acc-bank",
    name = "Bank",
    initialAmount = 0,
    currentAmount = 0,
    type = AccountTypeEntity.Bank,
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

private val cashBalance2025_01 = MonthlyAccountBalanceEntity(
    year = 2025,
    month = 1,
    accountId = "acc-cash",
    balance = 1_000_000L
)

private val bankBalance2025_01 = MonthlyAccountBalanceEntity(
    year = 2025,
    month = 1,
    accountId = "acc-bank",
    balance = 5_000_000L
)

private val cashBalance2025_02 = MonthlyAccountBalanceEntity(
    year = 2025,
    month = 2,
    accountId = "acc-cash",
    balance = 1_200_000L
)

private val cashBalance2025_03 = MonthlyAccountBalanceEntity(
    year = 2025,
    month = 3,
    accountId = "acc-cash",
    balance = 800_000L
)

private val cashBalance2026_01 = MonthlyAccountBalanceEntity(
    year = 2026,
    month = 1,
    accountId = "acc-cash",
    balance = 2_000_000L
)

class MonthlyAccountBalanceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MonthlyAccountBalanceDao

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        runBlocking {
            db.accountDao().insert(cashAccount)
            db.accountDao().insert(bankAccount)
        }
        dao = db.monthlyAccountBalanceDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetByYearMonth_shouldReturnMatchingRecords() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(bankBalance2025_01)

        val results = dao.getByYearMonth(2025, 1)

        assertEquals(2, results.size)
        assertTrue(results.any { it.accountId == "acc-cash" })
        assertTrue(results.any { it.accountId == "acc-bank" })
    }

    @Test
    fun getByYearMonth_withNonExistentMonth_shouldReturnEmptyList() = runTest {
        dao.insert(cashBalance2025_01)

        val results = dao.getByYearMonth(2025, 2)

        assertTrue(results.isEmpty())
    }

    @Test
    fun insert_withOnConflictReplace_shouldReplaceExistingRecord() = runTest {
        dao.insert(cashBalance2025_01)

        val updatedRecord = cashBalance2025_01.copy(balance = 2_000_000L)
        dao.insert(updatedRecord)

        val results = dao.getByYearMonth(2025, 1)
        assertEquals(1, results.size)
        assertEquals(2_000_000L, results.first().balance)
    }

    @Test
    fun insertAllAndGetByYearMonth_shouldInsertMultipleRecords() = runTest {
        dao.insertAll(listOf(cashBalance2025_01, bankBalance2025_01))

        val results = dao.getByYearMonth(2025, 1)

        assertEquals(2, results.size)
    }

    @Test
    fun update_shouldModifyExistingRecord() = runTest {
        dao.insert(cashBalance2025_01)

        val updatedRecord = cashBalance2025_01.copy(balance = 3_000_000L)
        dao.update(updatedRecord)

        val results = dao.getByYearMonth(2025, 1)
        assertEquals(3_000_000L, results.first().balance)
    }

    @Test
    fun updateAll_shouldModifyMultipleRecords() = runTest {
        dao.insertAll(listOf(cashBalance2025_01, cashBalance2025_02))

        val updatedRecords = listOf(
            cashBalance2025_01.copy(balance = 4_000_000L),
            cashBalance2025_02.copy(balance = 5_000_000L)
        )
        dao.updateAll(updatedRecords)

        val results = dao.getByYearMonth(2025, 1)
        assertEquals(4_000_000L, results.first().balance)
    }

    @Test
    fun getByYearMonthRange_shouldReturnRecordsInRange() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(cashBalance2025_02)
        dao.insert(cashBalance2025_03)

        val results = dao.getByYearMonthRange(2025, 1, 2025, 2)

        assertEquals(2, results.size)
        assertTrue(results.all { it.year == 2025 && it.month <= 2 })
    }

    @Test
    fun getByYearMonthRange_withEmptyDatabase_shouldReturnEmptyList() = runTest {
        val results = dao.getByYearMonthRange(2025, 1, 2025, 12)

        assertTrue(results.isEmpty())
    }

    @Test
    fun getByYearMonthRange_spanningMultipleYears_shouldReturnAllRecords() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(cashBalance2026_01)

        val results = dao.getByYearMonthRange(2025, 1, 2026, 12)

        assertEquals(2, results.size)
        assertTrue(results.any { it.year == 2025 && it.month == 1 })
        assertTrue(results.any { it.year == 2026 && it.month == 1 })
    }

    @Test
    fun getByAccountIdAndYearMonthRange_shouldReturnFilteredRecords() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(cashBalance2025_02)
        dao.insert(cashBalance2025_03)
        dao.insert(bankBalance2025_01)

        val results = dao.getByAccountIdAndYearMonthRange(
            accountId = "acc-cash",
            startYear = 2025,
            startMonth = 1,
            endYear = 2025,
            endMonth = 3
        )

        assertEquals(3, results.size)
        assertTrue(results.all { it.accountId == "acc-cash" })
    }

    @Test
    fun getByAccountIdAndYearMonthRange_withNonExistentAccount_shouldReturnEmptyList() = runTest {
        dao.insert(cashBalance2025_01)

        val results = dao.getByAccountIdAndYearMonthRange(
            accountId = "non-existent",
            startYear = 2025,
            startMonth = 1,
            endYear = 2025,
            endMonth = 12
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun getNetWorthByYearMonth_shouldReturnSumOfBalances() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(bankBalance2025_01)

        val netWorth = dao.getNetWorthByYearMonth(2025, 1)

        assertEquals(6_000_000L, netWorth)
    }

    @Test
    fun getNetWorthByYearMonth_withEmptyDatabase_shouldReturnNull() = runTest {
        val netWorth = dao.getNetWorthByYearMonth(2025, 1)

        assertNull(netWorth)
    }

    @Test
    fun getNetWorthByYearMonth_withSingleAccount_shouldReturnBalance() = runTest {
        dao.insert(cashBalance2025_01)

        val netWorth = dao.getNetWorthByYearMonth(2025, 1)

        assertEquals(1_000_000L, netWorth)
    }

    @Test
    fun getNetWorthByYearMonth_withNegativeBalance_shouldCalculateCorrectly() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(bankBalance2025_01.copy(balance = -500_000L))

        val netWorth = dao.getNetWorthByYearMonth(2025, 1)

        assertEquals(500_000L, netWorth)
    }

    @Test
    fun getNetWorthsByYearMonthRange_shouldReturnGroupedNetWorths() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(bankBalance2025_01)
        dao.insert(cashBalance2025_02)
        dao.insert(cashBalance2025_02.copy(accountId = "acc-bank", balance = 6_000_000L))

        val results = dao.getNetWorthsByYearMonthRange(2025, 1, 2025, 2)

        assertEquals(2, results.size)
        val jan = results.first { it.month == 1 }
        val feb = results.first { it.month == 2 }
        assertEquals(6_000_000L, jan.netWorth)
        assertEquals(7_200_000L, feb.netWorth)
    }

    @Test
    fun getNetWorthsByYearMonthRange_withEmptyDatabase_shouldReturnEmptyList() = runTest {
        val results = dao.getNetWorthsByYearMonthRange(2025, 1, 2025, 12)

        assertTrue(results.isEmpty())
    }

    @Test
    fun getNetWorthsByYearMonthRange_spanningMultipleYears_shouldReturnAllMonths() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(cashBalance2026_01)

        val results = dao.getNetWorthsByYearMonthRange(2025, 1, 2026, 12)

        assertEquals(2, results.size)
    }

    @Test
    fun getByYearMonthRange_withSameStartAndEndMonth_shouldReturnSingleMonth() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(cashBalance2025_02)

        val results = dao.getByYearMonthRange(2025, 1, 2025, 1)

        assertEquals(1, results.size)
        assertEquals(2025, results.first().year)
        assertEquals(1, results.first().month)
    }

    @Test
    fun insert_withSameYearMonthDifferentAccounts_shouldStoreAll() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(bankBalance2025_01)

        val results = dao.getByYearMonth(2025, 1)

        assertEquals(2, results.size)
        assertTrue(results.map { it.accountId }.containsAll(listOf("acc-cash", "acc-bank")))
    }

    @Test
    fun getByYearMonthRange_orderingShouldBeCorrect() = runTest {
        dao.insert(cashBalance2025_03)
        dao.insert(cashBalance2025_01)
        dao.insert(cashBalance2025_02)

        val results = dao.getByYearMonthRange(2025, 1, 2025, 3)

        assertEquals(3, results.size)
        assertEquals(1, results[0].month)
        assertEquals(2, results[1].month)
        assertEquals(3, results[2].month)
    }

    @Test
    fun getNetWorthByYearMonth_withZeroBalances_shouldReturnZero() = runTest {
        dao.insert(cashBalance2025_01.copy(balance = 0L))
        dao.insert(bankBalance2025_01.copy(balance = 0L))

        val netWorth = dao.getNetWorthByYearMonth(2025, 1)

        assertEquals(0L, netWorth)
    }

    @Test
    fun getByAccountIdAndYearMonthRange_withSameStartAndEndMonth_shouldReturnSingleMonth() = runTest {
        dao.insert(cashBalance2025_01)
        dao.insert(cashBalance2025_02)

        val results = dao.getByAccountIdAndYearMonthRange(
            accountId = "acc-cash",
            startYear = 2025,
            startMonth = 1,
            endYear = 2025,
            endMonth = 1
        )

        assertEquals(1, results.size)
        assertEquals(1, results.first().month)
    }
}
