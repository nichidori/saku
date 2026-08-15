package dev.nichidori.saku.data.repo

import androidx.room.Room
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.BudgetEntity
import dev.nichidori.saku.data.entity.BudgetTemplateEntity
import dev.nichidori.saku.data.entity.MonthlyNetWorthEntity
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class DefaultTrxRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultTrxRepository

    private val cashAccount = Account(
        id = "acc-1",
        name = "Cash",
        currentAmount = 10_000L,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val bankAccount = Account(
        id = "acc-2",
        name = "Bank",
        currentAmount = 20_000L,
        type = AccountType.Bank,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val incomeCategory = Category(
        id = "cat-1",
        name = "Salary",
        type = TrxType.Income,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val expenseCategory = Category(
        id = "cat-2",
        name = "Food",
        type = TrxType.Expense,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val transferCategory = Category(
        id = "cat-3",
        name = "Deposit",
        type = TrxType.Transfer,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        repository = DefaultTrxRepository(db)
        runBlocking {
            db.accountDao().insert(cashAccount.toEntity())
            db.accountDao().insert(bankAccount.toEntity())
            db.categoryDao().insert(incomeCategory.toEntity())
            db.categoryDao().insert(expenseCategory.toEntity())
            db.categoryDao().insert(transferCategory.toEntity())
        }
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addTrx_shouldInsertIncomeAndAddToBalance() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 5_000L,
            description = "July Salary",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(15_000L, updatedAccount.currentAmount)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        assertEquals("July Salary", addedTrx.description)
        assertEquals(5_000L, addedTrx.amount)
    }

    @Test
    fun addTrx_shouldInsertExpenseAndSubtractFromBalance() = runTest {
        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = Clock.System.now(),
            amount = 2_000L,
            description = "Groceries",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(8_000L, updatedAccount.currentAmount)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Expense
        assertEquals("Groceries", addedTrx.description)
        assertEquals(2_000L, addedTrx.amount)
    }

    @Test
    fun addTrx_shouldInsertTransferAndUpdateBothBalances() = runTest {
        repository.addTrx(
            type = TrxType.Transfer,
            transactionAt = Clock.System.now(),
            amount = 3_000L,
            description = "Cash to Bank",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = TrxAccount.Regular(bankAccount),
            category = transferCategory,

            )
        val updatedCash = db.accountDao().getById(cashAccount.id)!!.toDomain()
        val updatedBank = db.accountDao().getById(bankAccount.id)!!.toDomain()
        assertEquals(7_000L, updatedCash.currentAmount)
        assertEquals(23_000L, updatedBank.currentAmount)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        assertEquals("Cash to Bank", addedTrx.description)
        assertEquals(3_000L, addedTrx.amount)
    }

    @Test
    fun addTrx_shouldHandleZeroAmountTransactions() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 0L,
            description = "Zero Income",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(10_000L, updatedAccount.currentAmount)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        assertEquals(0L, addedTrx.amount)
    }

    @Test
    fun addTrx_shouldThrowWhenSourceAccountNotFound() = runTest {
        val nonExistentAccount = cashAccount.copy(id = "non-existent-id")
        assertFailsWith<IllegalStateException> {
            repository.addTrx(
                type = TrxType.Income,
                transactionAt = Clock.System.now(),
                amount = 1_000L,
                description = "Income",
                sourceAccount = TrxAccount.Regular(nonExistentAccount),
                targetAccount = null,
                category = incomeCategory,

                )
        }
    }

    @Test
    fun addTrx_shouldThrowWhenTargetAccountNotFoundForTransfer() = runTest {
        val nonExistentAccount = bankAccount.copy(id = "non-existent-id")
        assertFailsWith<IllegalStateException> {
            repository.addTrx(
                type = TrxType.Transfer,
                transactionAt = Clock.System.now(),
                amount = 1_000L,
                description = "Transfer",
                sourceAccount = TrxAccount.Regular(cashAccount),
                targetAccount = TrxAccount.Regular(nonExistentAccount),
                category = transferCategory,

                )
        }
    }

    @Test
    fun addTrx_shouldThrowWhenCategoryNotFound() = runTest {
        val nonExistentCategory = incomeCategory.copy(id = "non-existent-id")
        assertFailsWith<Exception> {
            repository.addTrx(
                type = TrxType.Income,
                transactionAt = Clock.System.now(),
                amount = 1_000L,
                description = "Income",
                sourceAccount = TrxAccount.Regular(cashAccount),
                targetAccount = null,
                category = nonExistentCategory,

                )
        }
    }

    @Test
    fun getTrxById_shouldReturnMatchingTrx() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Side Job",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val trxs = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE)
        val loadedTrx = repository.getTrxById(trxs.first().toDomain().id)
        assertNotNull(loadedTrx)
        assertEquals("Side Job", loadedTrx.description)
    }

    @Test
    fun getTrxById_shouldReturnNullForNonExistentTrx() = runTest {
        val result = repository.getTrxById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getFilteredTrxs_shouldReturnFilteredResults() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 5_000L,
            description = "Salary",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Food",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )
        val filter = TrxFilter(
            month = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let {
                YearMonth(it.year, it.month)
            },
            type = TrxType.Income,
            categoryId = null,
            accountId = null
        )
        val results = repository.getFilteredTrxs(filter)
        assertEquals(1, results.size)
        assertEquals("Salary", results.first().description)
    }

    @Test
    fun updateTrx_shouldUpdateIncomeAndAdjustBalance() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 2_000L,
            description = "Bonus",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        repository.updateTrx(
            id = addedTrx.id,
            type = TrxType.Income,
            transactionAt = addedTrx.transactionAt,
            amount = 4_000L,
            description = addedTrx.description,
            sourceAccount = addedTrx.sourceAccount,
            targetAccount = null,
            category = addedTrx.category,

            )
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(14_000L, updatedAccount.currentAmount)
        val updatedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        assertEquals(4_000L, updatedTrx.amount)
    }

    @Test
    fun updateTrx_shouldUpdateExpenseAndAdjustBalance() = runTest {
        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Shopping",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        repository.updateTrx(
            id = addedTrx.id,
            type = TrxType.Expense,
            transactionAt = addedTrx.transactionAt,
            amount = 1_500L,
            description = addedTrx.description,
            sourceAccount = addedTrx.sourceAccount,
            targetAccount = null,
            category = addedTrx.category,

            )
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(8_500L, updatedAccount.currentAmount)
        val updatedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Expense
        assertEquals(1_500L, updatedTrx.amount)
    }

    @Test
    fun updateTrx_shouldUpdateTransferAndAdjustBothBalances() = runTest {
        val initialCashUpdatedAt = db.accountDao().getById(cashAccount.id)!!.toDomain().updatedAt
        val initialBankUpdatedAt = db.accountDao().getById(bankAccount.id)!!.toDomain().updatedAt
        repository.addTrx(
            type = TrxType.Transfer,
            transactionAt = Clock.System.now(),
            amount = 2_000L,
            description = "Transfer",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = TrxAccount.Regular(bankAccount),
            category = transferCategory,

            )
        val afterAddCash = db.accountDao().getById(cashAccount.id)!!.toDomain()
        val afterAddBank = db.accountDao().getById(bankAccount.id)!!.toDomain()
        assertTrue(
            afterAddCash.updatedAt!! > (initialCashUpdatedAt ?: Instant.fromEpochMilliseconds(0))
        )
        assertTrue(
            afterAddBank.updatedAt!! > (initialBankUpdatedAt ?: Instant.fromEpochMilliseconds(0))
        )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        val beforeUpdateCashTime = afterAddCash.updatedAt
        val beforeUpdateBankTime = afterAddBank.updatedAt
        repository.updateTrx(
            id = addedTrx.id,
            type = TrxType.Transfer,
            transactionAt = addedTrx.transactionAt,
            amount = 3_000L,
            description = addedTrx.description,
            sourceAccount = addedTrx.sourceAccount,
            targetAccount = addedTrx.targetAccount,
            category = addedTrx.category,

            )
        val updatedCash = db.accountDao().getById(cashAccount.id)!!.toDomain()
        val updatedBank = db.accountDao().getById(bankAccount.id)!!.toDomain()
        assertEquals(7_000L, updatedCash.currentAmount)
        assertEquals(23_000L, updatedBank.currentAmount)
        assertTrue(updatedCash.updatedAt!! > beforeUpdateCashTime)
        assertTrue(updatedBank.updatedAt!! > beforeUpdateBankTime)
    }

    @Test
    fun updateTrx_shouldHandleAccountChange() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Freelance",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        repository.updateTrx(
            id = addedTrx.id,
            type = TrxType.Income,
            transactionAt = addedTrx.transactionAt,
            amount = addedTrx.amount,
            description = addedTrx.description,
            sourceAccount = TrxAccount.Regular(bankAccount),
            targetAccount = null,
            category = addedTrx.category,

            )
        val updatedCash = db.accountDao().getById(cashAccount.id)!!.toDomain()
        val updatedBank = db.accountDao().getById(bankAccount.id)!!.toDomain()
        assertEquals(10_000L, updatedCash.currentAmount)
        assertEquals(21_000L, updatedBank.currentAmount)
    }

    @Test
    fun updateTrx_shouldThrowWhenTransactionNotFound() = runTest {
        assertFailsWith<NoSuchElementException> {
            repository.updateTrx(
                id = "non-existent-id",
                type = TrxType.Income,
                transactionAt = Clock.System.now(),
                amount = 1_000L,
                description = "Income",
                sourceAccount = TrxAccount.Regular(cashAccount),
                targetAccount = null,
                category = incomeCategory,

                )
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenOldSourceAccountNotFound() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Income",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        db.accountDao().deleteById(cashAccount.id)
        assertFailsWith<NoSuchElementException> {
            repository.updateTrx(
                id = addedTrx.id,
                type = TrxType.Income,
                transactionAt = addedTrx.transactionAt,
                amount = 2_000L,
                description = addedTrx.description,
                sourceAccount = addedTrx.sourceAccount,
                targetAccount = null,
                category = addedTrx.category,

                )
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenOldTargetAccountNotFoundForTransfer() = runTest {
        repository.addTrx(
            type = TrxType.Transfer,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Transfer",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = TrxAccount.Regular(bankAccount),
            category = transferCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        db.accountDao().deleteById(bankAccount.id)
        assertFailsWith<NoSuchElementException> {
            repository.updateTrx(
                id = addedTrx.id,
                type = TrxType.Transfer,
                transactionAt = addedTrx.transactionAt,
                amount = 2_000L,
                description = addedTrx.description,
                sourceAccount = addedTrx.sourceAccount,
                targetAccount = addedTrx.targetAccount,
                category = addedTrx.category,

                )
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenNewSourceAccountNotFound() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Income",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        val nonExistentAccount = cashAccount.copy(id = "non-existent-id")
        assertFailsWith<IllegalStateException> {
            repository.updateTrx(
                id = addedTrx.id,
                type = TrxType.Income,
                transactionAt = addedTrx.transactionAt,
                amount = addedTrx.amount,
                description = addedTrx.description,
                sourceAccount = TrxAccount.Regular(nonExistentAccount),
                targetAccount = null,
                category = addedTrx.category,

                )
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenNewTargetAccountNotFoundForTransfer() = runTest {
        repository.addTrx(
            type = TrxType.Transfer,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Transfer",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = TrxAccount.Regular(bankAccount),
            category = transferCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        val nonExistentAccount = bankAccount.copy(id = "non-existent-id")
        assertFailsWith<IllegalStateException> {
            repository.updateTrx(
                id = addedTrx.id,
                type = TrxType.Transfer,
                transactionAt = addedTrx.transactionAt,
                amount = addedTrx.amount,
                description = addedTrx.description,
                sourceAccount = addedTrx.sourceAccount,
                targetAccount = TrxAccount.Regular(nonExistentAccount),
                category = addedTrx.category,

                )
        }
    }

    @Test
    fun deleteTrx_shouldDeleteIncomeAndRevertBalance() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 2_000L,
            description = "Salary",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        repository.deleteTrx(addedTrx.id)
        val deletedTrx = repository.getTrxById(addedTrx.id)
        assertNull(deletedTrx)
        val updatedAccount = db.accountDao().getById(cashAccount.id)?.toDomain()
        assertEquals(10_000L, updatedAccount?.currentAmount)
    }

    @Test
    fun deleteTrx_shouldDeleteExpenseAndRevertBalance() = runTest {
        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = Clock.System.now(),
            amount = 1_500L,
            description = "Food",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        repository.deleteTrx(addedTrx.id)
        val deletedTrx = repository.getTrxById(addedTrx.id)
        assertNull(deletedTrx)
        val updatedAccount = db.accountDao().getById(cashAccount.id)?.toDomain()
        assertEquals(10_000L, updatedAccount?.currentAmount)
    }

    @Test
    fun deleteTrx_shouldDeleteTransferAndRevertBothBalances() = runTest {
        repository.addTrx(
            type = TrxType.Transfer,
            transactionAt = Clock.System.now(),
            amount = 2_500L,
            description = "Transfer",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = TrxAccount.Regular(bankAccount),
            category = transferCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        repository.deleteTrx(addedTrx.id)
        val deletedTrx = repository.getTrxById(addedTrx.id)
        assertNull(deletedTrx)
        val updatedCash = db.accountDao().getById(cashAccount.id)?.toDomain()
        val updatedBank = db.accountDao().getById(bankAccount.id)?.toDomain()
        assertEquals(10_000L, updatedCash?.currentAmount)
        assertEquals(20_000L, updatedBank?.currentAmount)
    }

    @Test
    fun updateTrx_shouldThrowWhenTrxNotFound() = runTest {
        assertFailsWith<NoSuchElementException> {
            repository.updateTrx(
                id = "non-existent",
                type = TrxType.Income,
                transactionAt = Clock.System.now(),
                amount = 1_000L,
                description = "Test",
                sourceAccount = TrxAccount.Regular(cashAccount),
                targetAccount = null,
                category = incomeCategory,

                )
        }
    }

    @Test
    fun deleteTrx_shouldThrowWhenTrxNotFound() = runTest {
        assertFailsWith<NoSuchElementException> {
            repository.deleteTrx("non-existent-id")
        }
    }

    @Test
    fun deleteTrx_shouldThrowWhenSourceAccountNotFound() = runTest {
        repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Income",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        db.accountDao().deleteById(cashAccount.id)
        assertFailsWith<NoSuchElementException> {
            repository.deleteTrx(addedTrx.id)
        }
    }

    @Test
    fun deleteTrx_shouldThrowWhenTargetAccountNotFoundForTransfer() = runTest {
        repository.addTrx(
            type = TrxType.Transfer,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Transfer",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = TrxAccount.Regular(bankAccount),
            category = transferCategory,

            )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        db.accountDao().deleteById(bankAccount.id)
        assertFailsWith<NoSuchElementException> {
            repository.deleteTrx(addedTrx.id)
        }
    }

    @Test
    fun addTrx_shouldUpdateBudgetSpentAmountForExpense() = runTest {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault())

        db.budgetTemplateDao().insert(
            BudgetTemplateEntity(
                id = "template-1",
                categoryId = expenseCategory.id,
                defaultAmount = 5000L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        db.budgetDao().insert(
            BudgetEntity(
                id = "budget-1",
                templateId = "template-1",
                categoryId = expenseCategory.id,
                month = localDate.month.number,
                year = localDate.year,
                baseAmount = 5000L,
                spentAmount = 1000L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )

        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = now,
            amount = 2_000L,
            description = "Groceries",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )

        val updatedBudget = db.budgetDao().getByMonthAndYearWithCategory(
            year = localDate.year,
            month = localDate.month.number
        ).first { it.budget.categoryId == expenseCategory.id }

        assertEquals(3000L, updatedBudget.budget.spentAmount)
    }

    @Test
    fun updateTrx_shouldUpdateBudgetSpentAmountForExpense() = runTest {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault())

        db.budgetTemplateDao().insert(
            BudgetTemplateEntity(
                id = "template-1",
                categoryId = expenseCategory.id,
                defaultAmount = 5000L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        db.budgetDao().insert(
            BudgetEntity(
                id = "budget-1",
                templateId = "template-1",
                categoryId = expenseCategory.id,
                month = localDate.month.number,
                year = localDate.year,
                baseAmount = 5000L,
                spentAmount = 0L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )

        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = now,
            amount = 1_000L,
            description = "Groceries",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )

        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()

        repository.updateTrx(
            id = addedTrx.id,
            type = TrxType.Expense,
            transactionAt = now,
            amount = 2_500L,
            description = addedTrx.description,
            sourceAccount = addedTrx.sourceAccount,
            targetAccount = null,
            category = addedTrx.category,

            )

        val updatedBudget = db.budgetDao().getByMonthAndYearWithCategory(
            year = localDate.year,
            month = localDate.month.number
        ).first { it.budget.categoryId == expenseCategory.id }

        assertEquals(2500L, updatedBudget.budget.spentAmount)
    }

    @Test
    fun updateTrx_shouldUpdateBudgetsWhenDateChangesForExpense() = runTest {
        val now = Clock.System.now()
        val currentLocalDate = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val nextMonthDate = now.plus(40.days)
        val nextLocalDate = nextMonthDate.toLocalDateTime(TimeZone.currentSystemDefault())

        db.budgetTemplateDao().insert(
            BudgetTemplateEntity(
                id = "template-1",
                categoryId = expenseCategory.id,
                defaultAmount = 5000L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        db.budgetDao().insert(
            BudgetEntity(
                id = "budget-current",
                templateId = "template-1",
                categoryId = expenseCategory.id,
                month = currentLocalDate.month.number,
                year = currentLocalDate.year,
                baseAmount = 5000L,
                spentAmount = 0L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )

        // Make sure we only insert if they don't collide, but realistically plus 40 days won't land in the same month and year
        if (currentLocalDate.month != nextLocalDate.month || currentLocalDate.year != nextLocalDate.year) {
            db.budgetDao().insert(
                BudgetEntity(
                    id = "budget-next",
                    templateId = "template-1",
                    categoryId = expenseCategory.id,
                    month = nextLocalDate.month.number,
                    year = nextLocalDate.year,
                    baseAmount = 5000L,
                    spentAmount = 0L,
                    createdAt = now.toEpochMilliseconds(),
                    updatedAt = null
                )
            )
        }

        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = now,
            amount = 1_000L,
            description = "Groceries",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )

        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()

        repository.updateTrx(
            id = addedTrx.id,
            type = TrxType.Expense,
            transactionAt = nextMonthDate,
            amount = 1_000L,
            description = addedTrx.description,
            sourceAccount = addedTrx.sourceAccount,
            targetAccount = null,
            category = addedTrx.category,

            )

        val currentBudget = db.budgetDao().getByMonthAndYearWithCategory(
            year = currentLocalDate.year,
            month = currentLocalDate.month.number
        ).first { it.budget.categoryId == expenseCategory.id }

        val nextBudget = db.budgetDao().getByMonthAndYearWithCategory(
            year = nextLocalDate.year,
            month = nextLocalDate.month.number
        ).first { it.budget.categoryId == expenseCategory.id }

        if (currentLocalDate.month != nextLocalDate.month || currentLocalDate.year != nextLocalDate.year) {
            assertEquals(0L, currentBudget.budget.spentAmount)
            assertEquals(1000L, nextBudget.budget.spentAmount)
        } else {
            // Unlikely, but if they fall in the same month, spent amount remains 1000.
            assertEquals(1000L, currentBudget.budget.spentAmount)
        }
    }

    @Test
    fun deleteTrx_shouldDecreaseBudgetSpentAmountForExpense() = runTest {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault())

        db.budgetTemplateDao().insert(
            BudgetTemplateEntity(
                id = "template-1",
                categoryId = expenseCategory.id,
                defaultAmount = 5000L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        db.budgetDao().insert(
            BudgetEntity(
                id = "budget-1",
                templateId = "template-1",
                categoryId = expenseCategory.id,
                month = localDate.month.number,
                year = localDate.year,
                baseAmount = 5000L,
                spentAmount = 0L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )

        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = now,
            amount = 2_000L,
            description = "Groceries",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,

            )

        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()

        repository.deleteTrx(addedTrx.id)

        val updatedBudget = db.budgetDao().getByMonthAndYearWithCategory(
            year = localDate.year,
            month = localDate.month.number
        ).first { it.budget.categoryId == expenseCategory.id }

        assertEquals(0L, updatedBudget.budget.spentAmount)
    }

    // TrxTemplate tests

    @Test
    fun addTrxTemplate_shouldInsertWithGeneratedId() = runTest {
        repository.addTrxTemplate(
            name = "Groceries",
            type = TrxType.Expense,
            description = "Weekly groceries",
            amount = 500_000L,
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,
        )

        val templates = repository.getAllTrxTemplates()
        assertEquals(1, templates.size)
        assertEquals("Groceries", templates[0].name)
        assertEquals(TrxType.Expense, templates[0].type)
        assertEquals("Weekly groceries", templates[0].description)
        assertEquals(500_000L, templates[0].amount)
        assertEquals(expenseCategory.id, templates[0].category?.id)
        assertEquals(cashAccount.id, templates[0].sourceAccount.id)
        assertNull(templates[0].targetAccount)
        assertNotNull(templates[0].id)
    }

    @Test
    fun addTrxTemplate_shouldThrowWhenSourceAndTargetAreSame() = runTest {
        assertFailsWith<IllegalStateException> {
            repository.addTrxTemplate(
                name = "Self Transfer",
                type = TrxType.Transfer,
                description = "Transfer to self",
                amount = 100_000L,
                sourceAccount = TrxAccount.Regular(cashAccount),
                targetAccount = TrxAccount.Regular(cashAccount),
                category = null,
            )
        }
    }

    @Test
    fun getTrxTemplateById_shouldReturnTemplate() = runTest {
        repository.addTrxTemplate(
            name = "Salary",
            type = TrxType.Income,
            description = "Monthly salary",
            amount = 5_000_000L,
            sourceAccount = TrxAccount.Regular(bankAccount),
            targetAccount = null,
            category = incomeCategory,
        )

        val all = repository.getAllTrxTemplates()
        val template = repository.getTrxTemplateById(all[0].id)

        assertNotNull(template)
        assertEquals("Salary", template.name)
        assertEquals(TrxType.Income, template.type)
        assertEquals(5_000_000L, template.amount)
        assertEquals(bankAccount.id, template.sourceAccount.id)
    }

    @Test
    fun getTrxTemplateById_withNonExistentId_shouldReturnNull() = runTest {
        val result = repository.getTrxTemplateById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getAllTrxTemplates_shouldReturnAllTemplates() = runTest {
        repository.addTrxTemplate(
            name = "Salary",
            type = TrxType.Income,
            description = "Monthly salary",
            amount = 5_000_000L,
            sourceAccount = TrxAccount.Regular(bankAccount),
            targetAccount = null,
            category = incomeCategory,
        )
        repository.addTrxTemplate(
            name = "Groceries",
            type = TrxType.Expense,
            description = "Weekly groceries",
            amount = 500_000L,
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,
        )
        repository.addTrxTemplate(
            name = "Deposit",
            type = TrxType.Transfer,
            description = "Transfer to bank",
            amount = 1_000_000L,
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = TrxAccount.Regular(bankAccount),
            category = null,
        )

        val templates = repository.getAllTrxTemplates()
        assertEquals(3, templates.size)
    }

    @Test
    fun updateTrxTemplate_shouldUpdateFields() = runTest {
        repository.addTrxTemplate(
            name = "Groceries",
            type = TrxType.Expense,
            description = "Weekly groceries",
            amount = 500_000L,
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,
        )

        val template = repository.getAllTrxTemplates()[0]

        repository.updateTrxTemplate(
            id = template.id,
            name = "Monthly Groceries",
            type = TrxType.Expense,
            description = "Monthly groceries budget",
            amount = 2_000_000L,
            sourceAccount = TrxAccount.Regular(bankAccount),
            targetAccount = null,
            category = expenseCategory,
        )

        val updated = repository.getTrxTemplateById(template.id)
        assertNotNull(updated)
        assertEquals("Monthly Groceries", updated.name)
        assertEquals("Monthly groceries budget", updated.description)
        assertEquals(2_000_000L, updated.amount)
        assertEquals(bankAccount.id, updated.sourceAccount.id)
        assertNotNull(updated.updatedAt)
    }

    @Test
    fun updateTrxTemplate_shouldThrowIfNotFound() = runTest {
        assertFailsWith<NoSuchElementException> {
            repository.updateTrxTemplate(
                id = "non-existent-id",
                name = "Updated",
                type = TrxType.Expense,
                description = "Updated",
                amount = 100L,
                sourceAccount = TrxAccount.Regular(cashAccount),
                targetAccount = null,
                category = expenseCategory,
            )
        }
    }

    @Test
    fun deleteTrxTemplate_shouldDelete() = runTest {
        repository.addTrxTemplate(
            name = "Groceries",
            type = TrxType.Expense,
            description = "Weekly groceries",
            amount = 500_000L,
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,
        )

        val template = repository.getAllTrxTemplates()[0]
        repository.deleteTrxTemplate(template.id)

        assertNull(repository.getTrxTemplateById(template.id))
        assertEquals(0, repository.getAllTrxTemplates().size)
    }

    @Test
    fun deleteTrxTemplate_shouldThrowIfNotFound() = runTest {
        assertFailsWith<NoSuchElementException> {
            repository.deleteTrxTemplate("non-existent-id")
        }
    }

    @Test
    fun addTrx_shouldWriteMonthlyNetWorthRecord() = runTest {
        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = Clock.System.now(),
            amount = 2_000L,
            description = "Groceries",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = expenseCategory,
        )

        val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }
        val record = db.monthlyNetWorthDao().getByYearMonth(
            year = currentMonth.year,
            month = currentMonth.month.number
        )
        assertNotNull(record)
        assertEquals(28_000L, record.netWorth)
    }

}