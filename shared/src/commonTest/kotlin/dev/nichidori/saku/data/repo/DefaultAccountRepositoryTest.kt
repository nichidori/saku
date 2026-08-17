package dev.nichidori.saku.data.repo

import androidx.room.Room
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.*
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultAccountRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultAccountRepository

    private val account = Account(
        id = "acc-1",
        name = "Cash",
        currentAmount = 10_000L,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val currentMonth: YearMonth
        get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        repository = DefaultAccountRepository(db)
    }

    private suspend fun currentMonthRecord(): MonthlyNetWorthEntity? {
        return db.monthlyNetWorthDao().getByYearMonth(
            year = currentMonth.year,
            month = currentMonth.month.number
        )
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addAccount_shouldInsertAccountWithGeneratedIdAndCreatedAt() = runTest {
        repository.addAccount(account.name, account.currentAmount, account.type)
        val accounts = db.accountDao().getAll().map { it.toDomain() }
        assertEquals(1, accounts.size)
        assertNotEquals("acc-1", accounts.first().id)
        assertTrue(accounts.first().createdAt > Instant.fromEpochMilliseconds(0))
    }

    @Test
    fun getAccountById_shouldReturnCorrectAccount() = runTest {
        db.accountDao().insert(account.toEntity())
        val result = repository.getAccountById(account.id)
        assertEquals(account.name, result?.name)
    }

    @Test
    fun getAccountById_shouldReturnNullIfNotFound() = runTest {
        val result = repository.getAccountById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getAllAccounts_shouldReturnAllAccounts() = runTest {
        db.accountDao().insert(account.toEntity())
        val result = repository.getAllAccounts()
        assertEquals(1, result.size)
        assertEquals(account.name, result.first().name)
    }

    @Test
    fun updateAccount_shouldUpdateExistingAccount() = runTest {
        db.accountDao().insert(account.toEntity())
        repository.updateAccount(account.id, "Updated Cash", account.type)
        val result = db.accountDao().getById(account.id)!!.toDomain()
        assertEquals("Updated Cash", result.name)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun updateAccount_shouldNotChangeCurrentAmount() = runTest {
        val existing = account.copy(currentAmount = 25_000L)
        db.accountDao().insert(existing.toEntity())
        repository.updateAccount(account.id, "Updated Cash", account.type)
        val result = db.accountDao().getById(account.id)!!.toDomain()
        assertEquals("Updated Cash", result.name)
        assertEquals(25_000L, result.currentAmount)
        assertEquals(account.type, result.type)
    }

    @Test
    fun deleteAccount_shouldSoftDeleteAccountAndZeroBalance() = runTest {
        db.accountDao().insert(account.toEntity())
        repository.deleteAccount(account.id)
        val result = db.accountDao().getById(account.id)!!.toDomain()
        assertNotNull(result.deletedAt)
        assertTrue(result.isDeleted)
        assertEquals(0L, result.currentAmount)
    }

    @Test
    fun deleteAccount_withNonZeroBalance_shouldCreateAdjustmentTrx() = runTest {
        db.accountDao().insert(account.toEntity())
        repository.deleteAccount(account.id)
        val trxs = db.trxDao().getAllUpTo(Clock.System.now().toEpochMilliseconds() + 1_000L)
        val adjustment = trxs.single { it.type == TrxTypeEntity.Adjustment }
        assertEquals(-10_000L, adjustment.amount)
        assertEquals(account.id, adjustment.sourceAccountId)
    }

    @Test
    fun deleteAccount_withZeroBalance_shouldOnlyMarkDeleted() = runTest {
        db.accountDao().insert(account.copy(currentAmount = 0L).toEntity())
        repository.deleteAccount(account.id)
        val entity = db.accountDao().getById(account.id)!!
        assertNotNull(entity.deletedAt)
        assertTrue(db.trxDao().getAllUpTo(Clock.System.now().toEpochMilliseconds() + 1_000L).isEmpty())
    }

    @Test
    fun deleteAccount_whenAlreadyDeleted_shouldBeNoOp() = runTest {
        db.accountDao().insert(account.toEntity())
        repository.deleteAccount(account.id)
        repository.deleteAccount(account.id)
        val entity = db.accountDao().getById(account.id)!!
        assertNotNull(entity.deletedAt)
        assertEquals(0L, entity.currentAmount)
    }

    @Test
    fun deleteAccount_shouldThrowIfAccountNotFound() = runTest {
        val exception = assertFailsWith<NoSuchElementException> {
            repository.deleteAccount("non-existent-id")
        }
        assertEquals("Account not found", exception.message)
    }

    @Test
    fun getAllTrxAccounts_shouldExcludeDeletedAccounts() = runTest {
        db.accountDao().insert(account.toEntity())
        db.accountDao().insert(account.copy(id = "acc-2", name = "Bank").toEntity())
        repository.deleteAccount(account.id)
        val result = repository.getAllTrxAccounts()
        assertEquals(listOf("acc-2"), result.map { it.id })
    }

    @Test
    fun getAllTrxAccountsIncludingDeleted_shouldIncludeDeletedAccounts() = runTest {
        db.accountDao().insert(account.toEntity())
        db.accountDao().insert(account.copy(id = "acc-2", name = "Bank").toEntity())
        repository.deleteAccount(account.id)
        val result = repository.getAllTrxAccountsIncludingDeleted()
        assertEquals(setOf(account.id, "acc-2"), result.map { it.id }.toSet())
    }

    @Test
    fun getTotalBalance_shouldReturnSumOfAllCurrentAmounts() = runTest {
        db.accountDao().insert(account.toEntity())
        db.accountDao().insert(account.copy(id = "acc-2", currentAmount = 5_000L).toEntity())
        val total = repository.getTotalBalance()
        assertEquals(15_000L, total)
    }

    @Test
    fun getTotalBalance_shouldReturnZeroIfNoAccount() = runTest {
        val emptyDb = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        val emptyRepo = DefaultAccountRepository(emptyDb)
        val balance = emptyRepo.getTotalBalance()
        assertEquals(0, balance)
    }

    @Test
    fun getNetWorthHistory_withNoRecords_shouldReturnAllZeros() = runTest {
        val months = listOf(YearMonth(2025, 1), YearMonth(2025, 2), YearMonth(2025, 3))

        val result = repository.getNetWorthHistory(months)

        assertEquals(listOf(0L, 0L, 0L), result)
    }

    @Test
    fun getNetWorthHistory_withMissingMonths_shouldCarryForwardLastKnownValue() = runTest {
        db.monthlyNetWorthDao().upsert(
            MonthlyNetWorthEntity(year = 2025, month = 1, netWorth = 100L, createdAt = 1_000L, updatedAt = null)
        )
        db.monthlyNetWorthDao().upsert(
            MonthlyNetWorthEntity(year = 2025, month = 3, netWorth = 300L, createdAt = 2_000L, updatedAt = null)
        )

        val result = repository.getNetWorthHistory(
            listOf(YearMonth(2025, 1), YearMonth(2025, 2), YearMonth(2025, 3))
        )

        assertEquals(listOf(100L, 100L, 300L), result)
    }

    @Test
    fun getNetWorthHistory_leadingMissingMonths_shouldUseZeroUntilFirstRecord() = runTest {
        db.monthlyNetWorthDao().upsert(
            MonthlyNetWorthEntity(year = 2025, month = 2, netWorth = 200L, createdAt = 1_000L, updatedAt = null)
        )

        val result = repository.getNetWorthHistory(
            listOf(YearMonth(2025, 1), YearMonth(2025, 2), YearMonth(2025, 3))
        )

        assertEquals(listOf(0L, 200L, 200L), result)
    }

    @Test
    fun getNetWorthHistory_withExactMatches_shouldReturnCachedValues() = runTest {
        db.monthlyNetWorthDao().upsert(
            MonthlyNetWorthEntity(year = 2025, month = 1, netWorth = 100L, createdAt = 1_000L, updatedAt = null)
        )
        db.monthlyNetWorthDao().upsert(
            MonthlyNetWorthEntity(year = 2025, month = 2, netWorth = 200L, createdAt = 2_000L, updatedAt = null)
        )

        val result = repository.getNetWorthHistory(
            listOf(YearMonth(2025, 1), YearMonth(2025, 2))
        )

        assertEquals(listOf(100L, 200L), result)
    }

    @Test
    fun getNetWorthHistory_withEmptyMonths_shouldReturnEmptyList() = runTest {
        val result = repository.getNetWorthHistory(emptyList())

        assertEquals(emptyList(), result)
    }

    @Test
    fun ensureCurrentMonthNetWorth_withAccountsOnly_shouldComputeSum() = runTest {
        db.accountDao().insert(account.copy(id = "acc-1", currentAmount = 1_000L).toEntity())
        db.accountDao().insert(account.copy(id = "acc-2", currentAmount = 500L).toEntity())

        repository.ensureCurrentMonthNetWorth()

        assertEquals(1_500L, currentMonthRecord()?.netWorth)
    }

    @Test
    fun ensureCurrentMonthNetWorth_withCredits_shouldSubtractCreditBalance() = runTest {
        db.accountDao().insert(account.copy(id = "acc-1", currentAmount = 1_000L).toEntity())
        db.creditDao().insert(
            CreditEntity(id = "cred-1", name = "CC", limit = 5_000L, currentAmount = 300L, createdAt = 1_000L, updatedAt = null)
        )

        repository.ensureCurrentMonthNetWorth()

        assertEquals(700L, currentMonthRecord()?.netWorth)
    }

    @Test
    fun ensureCurrentMonthNetWorth_whenRecordExists_shouldPreserveCreatedAtAndUpdateUpdatedAt() = runTest {
        db.accountDao().insert(account.copy(id = "acc-1", currentAmount = 1_000L).toEntity())
        db.monthlyNetWorthDao().upsert(
            MonthlyNetWorthEntity(
                year = currentMonth.year,
                month = currentMonth.month.number,
                netWorth = 0L,
                createdAt = 100L,
                updatedAt = 100L
            )
        )

        repository.ensureCurrentMonthNetWorth()

        val record = currentMonthRecord()
        assertEquals(1_000L, record?.netWorth)
        assertEquals(100L, record?.createdAt)
        assertTrue((record?.updatedAt ?: 0L) > 100L)
    }

    @Test
    fun addAccount_shouldWriteCurrentMonthNetWorthRecord() = runTest {
        repository.addAccount(account.name, 10_000L, account.type)

        assertEquals(10_000L, currentMonthRecord()?.netWorth)
    }

    @Test
    fun addCredit_shouldSubtractCreditBalanceFromNetWorth() = runTest {
        repository.addAccount(account.name, 10_000L, account.type)
        repository.addCredit(name = "CC", limit = 5_000L, currentAmount = 3_000L)

        assertEquals(7_000L, currentMonthRecord()?.netWorth)
    }

    @Test
    fun updateCredit_shouldRefreshMonthlyNetWorthRecord() = runTest {
        repository.addAccount(account.name, 10_000L, account.type)
        repository.addCredit(name = "CC", limit = 5_000L, currentAmount = 3_000L)
        val creditId = db.creditDao().getAll().first().id

        repository.updateCredit(id = creditId, name = "CC", limit = 5_000L, currentAmount = 8_000L)

        assertEquals(2_000L, currentMonthRecord()?.netWorth)
    }

    @Test
    fun deleteCredit_shouldRefreshMonthlyNetWorthRecord() = runTest {
        repository.addAccount(account.name, 10_000L, account.type)
        repository.addCredit(name = "CC", limit = 5_000L, currentAmount = 3_000L)
        val creditId = db.creditDao().getAll().first().id

        repository.deleteCredit(creditId)

        assertEquals(10_000L, currentMonthRecord()?.netWorth)
    }
}
