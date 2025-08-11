package org.arraflydori.fin.data.repo

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.entity.toDomain
import org.arraflydori.fin.data.entity.toEntity
import org.arraflydori.fin.data.getRoomDatabase
import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.AccountType
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.Trx
import org.arraflydori.fin.domain.model.TrxFilter
import org.arraflydori.fin.domain.model.TrxType
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

    private val spendingCategory = Category(
        id = "cat-2",
        name = "Food",
        type = TrxType.Spending,
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
            db.categoryDao().insert(spendingCategory.toEntity())
            db.categoryDao().insert(transferCategory.toEntity())
        }
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addTrx_shouldInsertIncomeAndAddToBalance() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "July Salary",
            amount = 5_000L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(15_000L, updatedAccount.currentAmount)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        assertEquals("July Salary", addedTrx.name)
        assertEquals(5_000L, addedTrx.amount)
    }

    @Test
    fun addTrx_shouldInsertSpendingAndSubtractFromBalance() = runTest {
        val trx = Trx.Spending(
            id = "",
            name = "Groceries",
            amount = 2_000L,
            category = spendingCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(8_000L, updatedAccount.currentAmount)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Spending
        assertEquals("Groceries", addedTrx.name)
        assertEquals(2_000L, addedTrx.amount)
    }

    @Test
    fun addTrx_shouldInsertTransferAndUpdateBothBalances() = runTest {
        val trx = Trx.Transfer(
            id = "",
            name = "Cash to Bank",
            amount = 3_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val updatedCash = db.accountDao().getById(cashAccount.id)!!.toDomain()
        val updatedBank = db.accountDao().getById(bankAccount.id)!!.toDomain()
        assertEquals(7_000L, updatedCash.currentAmount)
        assertEquals(23_000L, updatedBank.currentAmount)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        assertEquals("Cash to Bank", addedTrx.name)
        assertEquals(3_000L, addedTrx.amount)
    }

    @Test
    fun addTrx_shouldHandleZeroAmountTransactions() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "Zero Income",
            amount = 0L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
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
        val trx = Trx.Income(
            id = "",
            name = "Income",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = nonExistentAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        assertFailsWith<IllegalStateException> {
            repository.addTrx(trx)
        }
    }

    @Test
    fun addTrx_shouldThrowWhenTargetAccountNotFoundForTransfer() = runTest {
        val nonExistentAccount = bankAccount.copy(id = "non-existent-id")
        val trx = Trx.Transfer(
            id = "",
            name = "Transfer",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            targetAccount = nonExistentAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        assertFailsWith<IllegalStateException> {
            repository.addTrx(trx)
        }
    }

    @Test
    fun addTrx_shouldThrowWhenCategoryNotFound() = runTest {
        val nonExistentCategory = transferCategory.copy(id = "non-existent-id")
        val trx = Trx.Income(
            id = "",
            name = "Income",
            amount = 1_000L,
            category = nonExistentCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        assertFailsWith<Exception> {
            repository.addTrx(trx)
        }
    }

    @Test
    fun getTrxById_shouldReturnMatchingTrx() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "Side Job",
            amount = 1_000L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = "Paid in cash",
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val trxs = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE)
        val loadedTrx = repository.getTrxById(trxs.first().toDomain().id)
        assertNotNull(loadedTrx)
        assertEquals("Side Job", loadedTrx.name)
    }

    @Test
    fun getTrxById_shouldReturnNullForNonExistentTrx() = runTest {
        val result = repository.getTrxById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getFilteredTrxs_shouldReturnFilteredResults() = runTest {
        val income = Trx.Income(
            id = "",
            name = "Salary",
            amount = 5_000L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        val expense = Trx.Spending(
            id = "",
            name = "Food",
            amount = 1_000L,
            category = spendingCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(income)
        repository.addTrx(expense)
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
        assertEquals("Salary", results.first().name)
    }

    @Test
    fun updateTrx_shouldUpdateIncomeAndAdjustBalance() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "Bonus",
            amount = 2_000L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        repository.updateTrx(addedTrx.copy(amount = 4_000L))
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(14_000L, updatedAccount.currentAmount)
        val updatedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        assertEquals(4_000L, updatedTrx.amount)
    }

    @Test
    fun updateTrx_shouldUpdateSpendingAndAdjustBalance() = runTest {
        val trx = Trx.Spending(
            id = "",
            name = "Shopping",
            amount = 1_000L,
            category = spendingCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Spending
        repository.updateTrx(addedTrx.copy(amount = 1_500L))
        val updatedAccount = db.accountDao().getById(cashAccount.id)!!.toDomain()
        assertEquals(8_500L, updatedAccount.currentAmount)
        val updatedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Spending
        assertEquals(1_500L, updatedTrx.amount)
    }

    @Test
    fun updateTrx_shouldUpdateTransferAndAdjustBothBalances() = runTest {
        val trx = Trx.Transfer(
            id = "",
            name = "Transfer",
            amount = 2_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        val initialCashUpdatedAt = db.accountDao().getById(cashAccount.id)!!.toDomain().updatedAt
        val initialBankUpdatedAt = db.accountDao().getById(bankAccount.id)!!.toDomain().updatedAt
        repository.addTrx(trx)
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
        repository.updateTrx(addedTrx.copy(amount = 3_000L))
        val updatedCash = db.accountDao().getById(cashAccount.id)!!.toDomain()
        val updatedBank = db.accountDao().getById(bankAccount.id)!!.toDomain()
        assertEquals(7_000L, updatedCash.currentAmount)
        assertEquals(23_000L, updatedBank.currentAmount)
        assertTrue(updatedCash.updatedAt!! > beforeUpdateCashTime)
        assertTrue(updatedBank.updatedAt!! > beforeUpdateBankTime)
    }

    @Test
    fun updateTrx_shouldHandleAccountChange() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "Freelance",
            amount = 1_000L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        repository.updateTrx(addedTrx.copy(sourceAccount = bankAccount))
        val updatedCash = db.accountDao().getById(cashAccount.id)!!.toDomain()
        val updatedBank = db.accountDao().getById(bankAccount.id)!!.toDomain()
        assertEquals(10_000L, updatedCash.currentAmount)
        assertEquals(21_000L, updatedBank.currentAmount)
    }

    @Test
    fun updateTrx_shouldThrowWhenTransactionNotFound() = runTest {
        val nonExistentTrx = Trx.Income(
            id = "non-existent-id",
            name = "Income",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        assertFailsWith<NoSuchElementException> {
            repository.updateTrx(nonExistentTrx)
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenOldSourceAccountNotFound() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "Income",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        db.accountDao().deleteById(cashAccount.id)
        assertFailsWith<NoSuchElementException> {
            repository.updateTrx(addedTrx.copy(amount = 2_000L))
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenOldTargetAccountNotFoundForTransfer() = runTest {
        val trx = Trx.Transfer(
            id = "",
            name = "Transfer",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        db.accountDao().deleteById(bankAccount.id)
        assertFailsWith<IllegalStateException> {
            repository.updateTrx(addedTrx.copy(amount = 2_000L))
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenNewSourceAccountNotFound() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "Income",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        val nonExistentAccount = cashAccount.copy(id = "non-existent-id")
        assertFailsWith<IllegalStateException> {
            repository.updateTrx(addedTrx.copy(sourceAccount = nonExistentAccount))
        }
    }

    @Test
    fun updateTrx_shouldThrowWhenNewTargetAccountNotFoundForTransfer() = runTest {
        val trx = Trx.Transfer(
            id = "",
            name = "Transfer",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        val nonExistentAccount = bankAccount.copy(id = "non-existent-id")
        assertFailsWith<IllegalStateException> {
            repository.updateTrx(addedTrx.copy(targetAccount = nonExistentAccount))
        }
    }

    @Test
    fun deleteTrx_shouldDeleteIncomeAndRevertBalance() = runTest {
        val trx = Trx.Income(
            id = "",
            name = "Salary",
            amount = 2_000L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        repository.deleteTrx(addedTrx.id)
        val deletedTrx = repository.getTrxById(addedTrx.id)
        assertNull(deletedTrx)
        val updatedAccount = db.accountDao().getById(cashAccount.id)?.toDomain()
        assertEquals(10_000L, updatedAccount?.currentAmount)
    }

    @Test
    fun deleteTrx_shouldDeleteSpendingAndRevertBalance() = runTest {
        val trx = Trx.Spending(
            id = "",
            name = "Food",
            amount = 1_500L,
            category = spendingCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Spending
        repository.deleteTrx(addedTrx.id)
        val deletedTrx = repository.getTrxById(addedTrx.id)
        assertNull(deletedTrx)
        val updatedAccount = db.accountDao().getById(cashAccount.id)?.toDomain()
        assertEquals(10_000L, updatedAccount?.currentAmount)
    }

    @Test
    fun deleteTrx_shouldDeleteTransferAndRevertBothBalances() = runTest {
        val trx = Trx.Transfer(
            id = "",
            name = "Transfer",
            amount = 2_500L,
            category = transferCategory,
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
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
        val nonExistentTrx = Trx.Income(
            id = "non-existent",
            name = "Test",
            amount = 1_000L,
            category = incomeCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        assertFailsWith<NoSuchElementException> {
            repository.updateTrx(nonExistentTrx)
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
        val trx = Trx.Income(
            id = "",
            name = "Income",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Income
        db.accountDao().deleteById(cashAccount.id)
        assertFailsWith<NoSuchElementException> {
            repository.deleteTrx(addedTrx.id)
        }
    }

    @Test
    fun deleteTrx_shouldThrowWhenTargetAccountNotFoundForTransfer() = runTest {
        val trx = Trx.Transfer(
            id = "",
            name = "Transfer",
            amount = 1_000L,
            category = transferCategory,
            sourceAccount = cashAccount,
            targetAccount = bankAccount,
            transactionAt = Clock.System.now(),
            note = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
        )
        repository.addTrx(trx)
        val addedTrx = db.trxDao()
            .getFilteredWithDetails(startTime = 0, endTime = Long.MAX_VALUE).first()
            .toDomain() as Trx.Transfer
        db.accountDao().deleteById(bankAccount.id)
        assertFailsWith<IllegalStateException> {
            repository.deleteTrx(addedTrx.id)
        }
    }
}