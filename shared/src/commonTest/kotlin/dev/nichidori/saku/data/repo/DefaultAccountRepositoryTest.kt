package dev.nichidori.saku.data.repo

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.data.entity.MonthlyAccountBalanceEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days

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
    fun addAccount_shouldInsertMonthlyAccountBalance() = runTest {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault())

        repository.addAccount("New Account", 50_000L, AccountType.Cash)

        val balanceRecords = db.monthlyAccountBalanceDao().getByYearMonth(
            localDate.year,
            localDate.month.number
        )
        val newAccountBalance = balanceRecords.first { it.accountId != account.id }
        assertEquals(50_000L, newAccountBalance.balance)
    }

    @Test
    fun updateAccount_shouldUpdateMonthlyAccountBalance() = runTest {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault())

        db.accountDao().insert(account.toEntity())
        db.monthlyAccountBalanceDao().insert(
            MonthlyAccountBalanceEntity(
                year = localDate.year,
                month = localDate.month.number,
                accountId = account.id,
                balance = 10_000L
            )
        )

        repository.updateAccount(account.id, "Updated Cash", 20_000L, account.type)

        val balanceRecords = db.monthlyAccountBalanceDao().getByYearMonth(
            localDate.year,
            localDate.month.number
        )
        val updatedBalance = balanceRecords.first { it.accountId == account.id }
        assertEquals(20_000L, updatedBalance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldFillMissingHistoricalMonths() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        val twoMonthsAgo = now.minus(60.days)
        val twoMonthsAgoDate = twoMonthsAgo.toLocalDateTime(TimeZone.UTC)
        
        val accountWithPastDate = account.copy(
            id = "acc-historical",
            createdAt = twoMonthsAgo
        )
        db.accountDao().insert(accountWithPastDate.toEntity())
        
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-income",
                name = "Salary",
                type = TrxTypeEntity.Income,
                parentId = null,
                createdAt = twoMonthsAgo.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        
        val month2Start = LocalDate(twoMonthsAgoDate.year, twoMonthsAgoDate.month, 1)
            .plus(1, DateTimeUnit.MONTH)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
        
        db.trxDao().insert(
            TrxEntity(
                id = "trx-1",
                description = "Salary",
                amount = 5_000L,
                categoryId = "cat-income",
                sourceAccountId = "acc-historical",
                targetAccountId = null,
                transactionAt = month2Start,
                createdAt = month2Start,
                updatedAt = null,
                type = TrxTypeEntity.Income
            )
        )
        
        val nowYearMonth = YearMonth(current.year, current.month)
        repository.ensureMonthlyBalancesExist(nowYearMonth)
        
        val balances = db.monthlyAccountBalanceDao().getByAccountIdAndYearMonthRange(
            accountId = "acc-historical",
            startYear = twoMonthsAgoDate.year,
            startMonth = twoMonthsAgoDate.month.number,
            endYear = current.year,
            endMonth = current.month.number
        )
        
        assertTrue(balances.isNotEmpty())
        val latestBalance = balances.maxByOrNull { it.year * 12 + it.month }!!
        assertEquals(15_000L, latestBalance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldNotDuplicateExistingMonths() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        val lastMonth = now.minus(40.days)
        val lastMonthDate = lastMonth.toLocalDateTime(TimeZone.UTC)
        
        val accountWithPastDate = account.copy(
            id = "acc-existing",
            createdAt = lastMonth
        )
        db.accountDao().insert(accountWithPastDate.toEntity())
        
        db.monthlyAccountBalanceDao().insert(
            MonthlyAccountBalanceEntity(
                year = lastMonthDate.year,
                month = lastMonthDate.month.number,
                accountId = "acc-existing",
                balance = 20_000L
            )
        )
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val balances = db.monthlyAccountBalanceDao().getByAccountIdAndYearMonthRange(
            accountId = "acc-existing",
            startYear = lastMonthDate.year,
            startMonth = lastMonthDate.month.number,
            endYear = current.year,
            endMonth = current.month.number
        )
        
        assertEquals(2, balances.size)
        val existingMonthBalance = balances.first { it.year == lastMonthDate.year && it.month == lastMonthDate.month.number }
        assertEquals(20_000L, existingMonthBalance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldUpdateCurrentMonth() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        
        val existingAccount = account.copy(
            id = "acc-current",
            currentAmount = 30_000L
        )
        db.accountDao().insert(existingAccount.toEntity())
        
        db.monthlyAccountBalanceDao().insert(
            MonthlyAccountBalanceEntity(
                year = current.year,
                month = current.month.number,
                accountId = "acc-current",
                balance = 5_000L
            )
        )
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val balance = db.monthlyAccountBalanceDao().getByYearMonth(current.year, current.month.number)
            .first { it.accountId == "acc-current" }
        
        assertEquals(10_000L, balance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldCalculateCorrectBalanceFromTransactions() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        val twoMonthsAgo = now.minus(60.days)
        val twoMonthsAgoDate = twoMonthsAgo.toLocalDateTime(TimeZone.UTC)
        
        val testAccount = account.copy(
            id = "acc-calculate",
            createdAt = twoMonthsAgo,
            initialAmount = 10_000L,
            currentAmount = 10_000L
        )
        db.accountDao().insert(testAccount.toEntity())
        
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-income-calc",
                name = "Salary",
                type = TrxTypeEntity.Income,
                parentId = null,
                createdAt = twoMonthsAgo.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-expense-calc",
                name = "Food",
                type = TrxTypeEntity.Expense,
                parentId = null,
                createdAt = twoMonthsAgo.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        
        val month2Start = LocalDate(twoMonthsAgoDate.year, twoMonthsAgoDate.month, 1)
            .plus(1, DateTimeUnit.MONTH)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
        
        db.trxDao().insert(
            TrxEntity(
                id = "trx-income-calc",
                description = "Salary",
                amount = 5_000L,
                categoryId = "cat-income-calc",
                sourceAccountId = "acc-calculate",
                targetAccountId = null,
                transactionAt = month2Start,
                createdAt = month2Start,
                updatedAt = null,
                type = TrxTypeEntity.Income
            )
        )
        
        db.trxDao().insert(
            TrxEntity(
                id = "trx-expense-calc",
                description = "Groceries",
                amount = 2_000L,
                categoryId = "cat-expense-calc",
                sourceAccountId = "acc-calculate",
                targetAccountId = null,
                transactionAt = month2Start + 1000,
                createdAt = month2Start + 1000,
                updatedAt = null,
                type = TrxTypeEntity.Expense
            )
        )
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val balances = db.monthlyAccountBalanceDao().getByAccountIdAndYearMonthRange(
            accountId = "acc-calculate",
            startYear = twoMonthsAgoDate.year,
            startMonth = twoMonthsAgoDate.month.number,
            endYear = current.year,
            endMonth = current.month.number
        )
        
        val latestBalance = balances.maxByOrNull { it.year * 12 + it.month }!!
        assertEquals(13_000L, latestBalance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldHandleAccountWithNoTransactions() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        val twoMonthsAgo = now.minus(60.days)
        val twoMonthsAgoDate = twoMonthsAgo.toLocalDateTime(TimeZone.UTC)
        
        val noTrxAccount = account.copy(
            id = "acc-no-trx",
            createdAt = twoMonthsAgo,
            initialAmount = 10_000L,
            currentAmount = 10_000L
        )
        db.accountDao().insert(noTrxAccount.toEntity())
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val balances = db.monthlyAccountBalanceDao().getByAccountIdAndYearMonthRange(
            accountId = "acc-no-trx",
            startYear = twoMonthsAgoDate.year,
            startMonth = twoMonthsAgoDate.month.number,
            endYear = current.year,
            endMonth = current.month.number
        )
        
        assertTrue(balances.isNotEmpty())
        assertTrue(balances.all { it.balance == 10_000L })
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldHandleMultipleAccounts() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        val oneMonthAgo = now.minus(40.days)
        val oneMonthAgoDate = oneMonthAgo.toLocalDateTime(TimeZone.UTC)
        
        val account1 = account.copy(id = "acc-multi-1", createdAt = oneMonthAgo, initialAmount = 10_000L, currentAmount = 10_000L)
        val account2 = account.copy(id = "acc-multi-2", createdAt = oneMonthAgo, initialAmount = 20_000L, currentAmount = 20_000L)
        db.accountDao().insert(account1.toEntity())
        db.accountDao().insert(account2.toEntity())
        
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-income-multi",
                name = "Salary",
                type = TrxTypeEntity.Income,
                parentId = null,
                createdAt = oneMonthAgo.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        
        val monthStart = LocalDate(oneMonthAgoDate.year, oneMonthAgoDate.month, 1)
            .plus(1, DateTimeUnit.MONTH)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
        
        db.trxDao().insert(
            TrxEntity(
                id = "trx-multi-1",
                description = "Income",
                amount = 5_000L,
                categoryId = "cat-income-multi",
                sourceAccountId = "acc-multi-1",
                targetAccountId = null,
                transactionAt = monthStart,
                createdAt = monthStart,
                updatedAt = null,
                type = TrxTypeEntity.Income
            )
        )
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val balances = db.monthlyAccountBalanceDao().getByYearMonth(current.year, current.month.number)
        
        val acc1Balance = balances.first { it.accountId == "acc-multi-1" }
        val acc2Balance = balances.first { it.accountId == "acc-multi-2" }
        
        assertEquals(15_000L, acc1Balance.balance)
        assertEquals(20_000L, acc2Balance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldHandleAccountCreatedInCurrentMonth() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        
        val newAccount = account.copy(
            id = "acc-current-month",
            createdAt = now,
            initialAmount = 50_000L,
            currentAmount = 50_000L
        )
        db.accountDao().insert(newAccount.toEntity())
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val balances = db.monthlyAccountBalanceDao().getByYearMonth(current.year, current.month.number)
        val newAccountBalance = balances.first { it.accountId == "acc-current-month" }
        
        assertEquals(50_000L, newAccountBalance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldHandleTransactionOnFirstDayOfMonth() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        
        val testAccount = account.copy(
            id = "acc-first-day",
            createdAt = now.minus(40.days),
            initialAmount = 10_000L,
            currentAmount = 10_000L
        )
        db.accountDao().insert(testAccount.toEntity())
        
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-first-day",
                name = "Salary",
                type = TrxTypeEntity.Income,
                parentId = null,
                createdAt = now.minus(40.days).toEpochMilliseconds(),
                updatedAt = null
            )
        )
        
        val firstDayOfCurrentMonth = LocalDate(current.year, current.month, 1)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
        
        db.trxDao().insert(
            TrxEntity(
                id = "trx-first-day",
                description = "Salary",
                amount = 3_000L,
                categoryId = "cat-first-day",
                sourceAccountId = "acc-first-day",
                targetAccountId = null,
                transactionAt = firstDayOfCurrentMonth,
                createdAt = firstDayOfCurrentMonth,
                updatedAt = null,
                type = TrxTypeEntity.Income
            )
        )
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val balance = db.monthlyAccountBalanceDao().getByYearMonth(current.year, current.month.number)
            .first { it.accountId == "acc-first-day" }
        
        assertEquals(13_000L, balance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldHandleTransactionOnLastDayOfMonth() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        
        val testAccount = account.copy(
            id = "acc-last-day",
            createdAt = now.minus(40.days),
            initialAmount = 10_000L,
            currentAmount = 10_000L
        )
        db.accountDao().insert(testAccount.toEntity())
        
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-last-day",
                name = "Expense",
                type = TrxTypeEntity.Expense,
                parentId = null,
                createdAt = now.minus(40.days).toEpochMilliseconds(),
                updatedAt = null
            )
        )
        
        val previousMonth = if (current.month.number == 1) YearMonth(current.year - 1, 12) else YearMonth(current.year, current.month.number - 1)
        val dayInPreviousMonth = LocalDate(previousMonth.year, previousMonth.month, 15)
            .plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds() - 1
        
        db.trxDao().insert(
            TrxEntity(
                id = "trx-last-day",
                description = "Expense",
                amount = 1_500L,
                categoryId = "cat-last-day",
                sourceAccountId = "acc-last-day",
                targetAccountId = null,
                transactionAt = dayInPreviousMonth,
                createdAt = dayInPreviousMonth,
                updatedAt = null,
                type = TrxTypeEntity.Expense
            )
        )
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val previousMonthBalance = db.monthlyAccountBalanceDao().getByYearMonth(previousMonth.year, previousMonth.month.number)
            .first { it.accountId == "acc-last-day" }
        
        assertEquals(8_500L, previousMonthBalance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldHandleTransferBetweenAccounts() = runTest {
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        
        val sourceAccount = account.copy(
            id = "acc-transfer-source",
            createdAt = now.minus(40.days),
            initialAmount = 10_000L,
            currentAmount = 10_000L
        )
        val targetAccount = account.copy(
            id = "acc-transfer-target",
            createdAt = now.minus(40.days),
            initialAmount = 5_000L,
            currentAmount = 5_000L
        )
        db.accountDao().insert(sourceAccount.toEntity())
        db.accountDao().insert(targetAccount.toEntity())
        
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-transfer",
                name = "Transfer",
                type = TrxTypeEntity.Transfer,
                parentId = null,
                createdAt = now.minus(40.days).toEpochMilliseconds(),
                updatedAt = null
            )
        )
        
        val monthStart = LocalDate(current.year, current.month, 1)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
        
        db.trxDao().insert(
            TrxEntity(
                id = "trx-transfer",
                description = "Transfer",
                amount = 2_000L,
                categoryId = "cat-transfer",
                sourceAccountId = "acc-transfer-source",
                targetAccountId = "acc-transfer-target",
                transactionAt = monthStart,
                createdAt = monthStart,
                updatedAt = null,
                type = TrxTypeEntity.Transfer
            )
        )
        
        repository.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val sourceBalance = db.monthlyAccountBalanceDao().getByYearMonth(current.year, current.month.number)
            .first { it.accountId == "acc-transfer-source" }
        val targetBalance = db.monthlyAccountBalanceDao().getByYearMonth(current.year, current.month.number)
            .first { it.accountId == "acc-transfer-target" }
        
        assertEquals(8_000L, sourceBalance.balance)
        assertEquals(7_000L, targetBalance.balance)
    }

    @Test
    fun ensureMonthlyBalancesExist_shouldHandleEmptyDatabase() = runTest {
        val emptyDb = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        val emptyRepo = DefaultAccountRepository(emptyDb)
        
        val now = Clock.System.now()
        val current = now.toLocalDateTime(TimeZone.UTC)
        
        emptyRepo.ensureMonthlyBalancesExist(YearMonth(current.year, current.month))
        
        val allAccounts = emptyDb.accountDao().getAll()
        assertTrue(allAccounts.isEmpty())
        
        emptyDb.close()
    }
}
