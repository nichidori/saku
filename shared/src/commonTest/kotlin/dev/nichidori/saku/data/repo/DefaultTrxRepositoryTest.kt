package dev.nichidori.saku.data.repo

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.model.TrxType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultTrxRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultTrxRepository

    private val cashAccount = Account(
        id = "acc-1",
        name = "Cash",
        initialAmount = 10_000L,
        currentAmount = 10_000L,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val bankAccount = Account(
        id = "acc-2",
        name = "Bank",
        initialAmount = 20_000L,
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = expenseCategory,
            note = ""
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
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            category = transferCategory,
            note = ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
                sourceAccount = nonExistentAccount,
                targetAccount = null,
                category = incomeCategory,
                note = ""
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
                sourceAccount = cashAccount,
                targetAccount = nonExistentAccount,
                category = transferCategory,
                note = ""
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
                sourceAccount = cashAccount,
                targetAccount = null,
                category = nonExistentCategory,
                note = ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = "Paid in cash"
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
        )
        repository.addTrx(
            type = TrxType.Expense,
            transactionAt = Clock.System.now(),
            amount = 1_000L,
            description = "Food",
            sourceAccount = cashAccount,
            targetAccount = null,
            category = expenseCategory,
            note = ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
            note = addedTrx.note ?: ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = expenseCategory,
            note = ""
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
            note = addedTrx.note ?: ""
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
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            category = transferCategory,
            note = ""
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
            note = addedTrx.note ?: ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
            sourceAccount = bankAccount,
            targetAccount = null,
            category = addedTrx.category,
            note = addedTrx.note ?: ""
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
                sourceAccount = cashAccount,
                targetAccount = null,
                category = incomeCategory,
                note = ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
                note = addedTrx.note ?: ""
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
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            category = transferCategory,
            note = ""
        )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        db.accountDao().deleteById(bankAccount.id)
        assertFailsWith<IllegalStateException> {
            repository.updateTrx(
                id = addedTrx.id,
                type = TrxType.Transfer,
                transactionAt = addedTrx.transactionAt,
                amount = 2_000L,
                description = addedTrx.description,
                sourceAccount = addedTrx.sourceAccount,
                targetAccount = addedTrx.targetAccount,
                category = addedTrx.category,
                note = addedTrx.note ?: ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
                sourceAccount = nonExistentAccount,
                targetAccount = null,
                category = addedTrx.category,
                note = addedTrx.note ?: ""
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
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            category = transferCategory,
            note = ""
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
                targetAccount = nonExistentAccount,
                category = addedTrx.category,
                note = addedTrx.note ?: ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = expenseCategory,
            note = ""
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
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            category = transferCategory,
            note = ""
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
                sourceAccount = cashAccount,
                targetAccount = null,
                category = incomeCategory,
                note = ""
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
            sourceAccount = cashAccount,
            targetAccount = null,
            category = incomeCategory,
            note = ""
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
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            category = transferCategory,
            note = ""
        )
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain()
        db.accountDao().deleteById(bankAccount.id)
        assertFailsWith<IllegalStateException> {
            repository.deleteTrx(addedTrx.id)
        }
    }
}