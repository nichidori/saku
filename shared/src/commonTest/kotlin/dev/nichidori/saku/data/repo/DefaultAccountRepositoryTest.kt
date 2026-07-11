package dev.nichidori.saku.data.repo

import androidx.room.Room
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.*
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultAccountRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultAccountRepository

    private val account = Account(
        id = "acc-1",
        name = "Cash",
        initialAmount = 10_000L,
        currentAmount = 10_000L,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        repository = DefaultAccountRepository(db)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addAccount_shouldInsertAccountWithGeneratedIdAndCreatedAt() = runTest {
        repository.addAccount(account.name, account.initialAmount, account.type)
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
        repository.updateAccount(account.id, "Updated Cash", account.initialAmount, account.type)
        val result = db.accountDao().getById(account.id)!!.toDomain()
        assertEquals("Updated Cash", result.name)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun deleteAccount_shouldDeleteAccountById() = runTest {
        db.accountDao().insert(account.toEntity())
        repository.deleteAccount(account.id)
        val result = db.accountDao().getById(account.id)
        assertNull(result)
    }

    @Test
    fun deleteAccount_shouldThrowIfAccountNotFound() = runTest {
        val exception = assertFailsWith<NoSuchElementException> {
            repository.deleteAccount("non-existent-id")
        }
        assertEquals("Account not found", exception.message)
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
    fun getBalanceHistory_withSingleAccountAndIncome_shouldReturnCorrectBalances() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        val testAccount = account.copy(
            id = "acc-hist",
            initialAmount = 10_000L,
            currentAmount = 10_000L,
            createdAt = Instant.fromEpochMilliseconds(0),
        )
        db.accountDao().insert(testAccount.toEntity())

        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-income",
                name = "Salary",
                type = TrxTypeEntity.Income,
                parentId = null,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )

        val currentMonthStart = LocalDate(current.year, current.month, 1)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()

        db.trxDao().insert(
            TrxEntity(
                id = "trx-income",
                description = "Salary",
                amount = 5_000L,
                categoryId = "cat-income",
                sourceAccountId = "acc-hist",
                sourceCreditId = null,
                targetAccountId = null,
                targetCreditId = null,
                transactionAt = currentMonthStart,
                createdAt = currentMonthStart,
                updatedAt = null,
                type = TrxTypeEntity.Income
            )
        )

        val currentMonth = YearMonth(current.year, current.month)
        val previousMonth = currentMonth.minus(1, DateTimeUnit.MONTH)
        val months = listOf(previousMonth, currentMonth)

        val result = repository.getBalanceHistory(months, TimeZone.UTC)

        assertEquals(2, result.size)
        assertEquals(10_000L, result[0])
        assertEquals(15_000L, result[1])
    }

    @Test
    fun getBalanceHistory_withExpenseAndTransfer_shouldCalculateCorrectly() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        val currentMonthStart = LocalDate(current.year, current.month, 1)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()

        val source = account.copy(
            id = "acc-source",
            initialAmount = 20_000L,
            currentAmount = 20_000L,
            createdAt = Instant.fromEpochMilliseconds(0),
        )
        val target = account.copy(
            id = "acc-target",
            initialAmount = 5_000L,
            currentAmount = 5_000L,
            createdAt = Instant.fromEpochMilliseconds(0),
        )
        db.accountDao().insert(source.toEntity())
        db.accountDao().insert(target.toEntity())

        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-exp",
                name = "Food",
                type = TrxTypeEntity.Expense,
                parentId = null,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )

        db.trxDao().insert(
            TrxEntity(
                id = "trx-expense",
                description = "Groceries",
                amount = 3_000L,
                categoryId = "cat-exp",
                sourceAccountId = "acc-source",
                sourceCreditId = null,
                targetAccountId = null,
                targetCreditId = null,
                transactionAt = currentMonthStart,
                createdAt = currentMonthStart,
                updatedAt = null,
                type = TrxTypeEntity.Expense
            )
        )

        db.trxDao().insert(
            TrxEntity(
                id = "trx-transfer",
                description = "Transfer",
                amount = 2_000L,
                categoryId = null,
                sourceAccountId = "acc-source",
                sourceCreditId = null,
                targetAccountId = "acc-target",
                targetCreditId = null,
                transactionAt = currentMonthStart + 1000,
                createdAt = currentMonthStart + 1000,
                updatedAt = null,
                type = TrxTypeEntity.Transfer
            )
        )

        val currentMonth = YearMonth(current.year, current.month)
        val previousMonth = currentMonth.minus(1, DateTimeUnit.MONTH)
        val months = listOf(previousMonth, currentMonth)

        val result = repository.getBalanceHistory(months, TimeZone.UTC)

        assertEquals(2, result.size)
        assertEquals(25_000L, result[0])
        assertEquals(22_000L, result[1])
    }

    @Test
    fun getBalanceHistory_withEmptyAccounts_shouldReturnAllZeros() = runTest {
        val month = YearMonth(2025, 1)
        val result = repository.getBalanceHistory(
            months = listOf(month),
            timeZone = TimeZone.UTC,
        )

        assertEquals(listOf(0L), result)
    }

    @Test
    fun getBalanceHistory_withNoTransactions_shouldUseInitialBalances() = runTest {
        db.accountDao().insert(account.copy(createdAt = Instant.fromEpochMilliseconds(0)).toEntity())

        val month = YearMonth(2025, 1)
        val months = listOf(month)

        val result = repository.getBalanceHistory(months, TimeZone.UTC)

        assertEquals(listOf(10_000L), result)
    }
}
