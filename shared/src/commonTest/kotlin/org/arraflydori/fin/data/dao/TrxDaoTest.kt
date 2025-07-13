package org.arraflydori.fin.data.dao

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.entity.AccountEntity
import org.arraflydori.fin.data.entity.AccountTypeEntity
import org.arraflydori.fin.data.entity.CategoryEntity
import org.arraflydori.fin.data.entity.TrxEntity
import org.arraflydori.fin.data.entity.TrxTypeEntity
import org.arraflydori.fin.data.getRoomDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val sourceAccount = AccountEntity(
    id = "acc-1",
    name = "Cash",
    initialAmount = 1_000_000,
    currentAmount = 1_000_000,
    type = AccountTypeEntity.Cash,
    createdAt = 0L,
    updatedAt = null
)

private val targetAccount = AccountEntity(
    id = "acc-2",
    name = "Bank",
    initialAmount = 2_000_000,
    currentAmount = 2_000_000,
    type = AccountTypeEntity.Bank,
    createdAt = 0L,
    updatedAt = null
)

private val incomeCategory = CategoryEntity(
    id = "cat-1",
    name = "Salary",
    type = TrxTypeEntity.Income,
    parentId = null,
    createdAt = 0L,
    updatedAt = null
)

private val spendingCategory = CategoryEntity(
    id = "cat-2",
    name = "Food",
    type = TrxTypeEntity.Spending,
    parentId = null,
    createdAt = 0L,
    updatedAt = null
)

private val transferCategory = CategoryEntity(
    id = "cat-transfer",
    name = "Transfer",
    type = TrxTypeEntity.Transfer,
    parentId = null,
    createdAt = 0L,
    updatedAt = null
)

private val transferTrx = TrxEntity(
    id = "trx-transfer",
    name = "Move to Bank",
    amount = 500_000,
    categoryId = transferCategory.id,
    sourceAccountId = sourceAccount.id,
    targetAccountId = targetAccount.id,
    transactionAt = System.currentTimeMillis(),
    note = "Internal transfer",
    createdAt = System.currentTimeMillis(),
    updatedAt = null,
    type = TrxTypeEntity.Transfer
)

private val incomeTrx = TrxEntity(
    id = "trx-1",
    name = "July Salary",
    amount = 10_000_000,
    categoryId = incomeCategory.id,
    sourceAccountId = sourceAccount.id,
    targetAccountId = null,
    transactionAt = System.currentTimeMillis(),
    note = "Payslip",
    createdAt = System.currentTimeMillis(),
    updatedAt = null,
    type = TrxTypeEntity.Income
)

class TrxDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var trxDao: TrxDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        trxDao = db.trxDao()
        accountDao = db.accountDao()
        categoryDao = db.categoryDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    private suspend fun setupBasicData() {
        accountDao.insert(sourceAccount)
        accountDao.insert(targetAccount)
        categoryDao.insert(incomeCategory)
        categoryDao.insert(spendingCategory)
        categoryDao.insert(transferCategory)
    }

    @Test
    fun insertIncomeAndGetByIdWithDetails_shouldReturnMatchingTransaction() = runTest {
        accountDao.insert(sourceAccount)
        categoryDao.insert(incomeCategory)
        trxDao.insert(incomeTrx)

        val result = trxDao.getByIdWithDetails(incomeTrx.id)

        assertNotNull(result)
        assertEquals(incomeTrx.id, result.trx.id)
        assertEquals(incomeCategory.id, result.category.id)
        assertEquals(sourceAccount.id, result.sourceAccount.id)
        assertNull(result.targetAccount)
    }

    @Test
    fun insertTransferAndGetByIdWithDetails_shouldReturnWithSourceAndTargetAccount() = runTest {
        accountDao.insert(sourceAccount)
        accountDao.insert(targetAccount)
        categoryDao.insert(transferCategory)
        trxDao.insert(transferTrx)

        val result = trxDao.getByIdWithDetails(transferTrx.id)

        assertNotNull(result)
        assertEquals(transferTrx.id, result.trx.id)
        assertEquals(transferCategory.id, result.category.id)
        assertEquals(sourceAccount.id, result.sourceAccount.id)
        assertEquals(targetAccount.id, result.targetAccount?.id)
    }

    @Test
    fun getFilteredWithDetails_shouldRespectTypeAndCategory() = runTest {
        accountDao.insert(sourceAccount)
        categoryDao.insert(incomeCategory)
        trxDao.insert(incomeTrx)

        val now = System.currentTimeMillis()
        val results = trxDao.getFilteredWithDetails(
            startTime = now - 1000,
            endTime = now + 1000,
            type = TrxTypeEntity.Income,
            categoryId = incomeCategory.id,
            accountId = sourceAccount.id
        )

        assertEquals(1, results.size)
        assertEquals(incomeTrx.id, results[0].trx.id)
    }

    @Test
    fun getFilteredWithDetails_shouldReturnTransferByTypeAndAccount() = runTest {
        accountDao.insert(sourceAccount)
        accountDao.insert(targetAccount)
        categoryDao.insert(transferCategory)
        trxDao.insert(transferTrx)

        val now = System.currentTimeMillis()
        val results = trxDao.getFilteredWithDetails(
            startTime = now - 1000,
            endTime = now + 1000,
            type = TrxTypeEntity.Transfer,
            categoryId = null,
            accountId = sourceAccount.id
        )

        assertEquals(1, results.size)
        assertEquals(transferTrx.id, results[0].trx.id)
    }

    @Test
    fun update_shouldModifyExistingTransaction() = runTest {
        accountDao.insert(sourceAccount)
        categoryDao.insert(incomeCategory)
        trxDao.insert(incomeTrx)

        val updated = incomeTrx.copy(name = "Updated Income")
        trxDao.update(updated)

        val result = trxDao.getByIdWithDetails(incomeTrx.id)
        assertEquals("Updated Income", result?.trx?.name)
    }

    @Test
    fun deleteById_shouldRemoveTransaction() = runTest {
        accountDao.insert(sourceAccount)
        categoryDao.insert(incomeCategory)
        trxDao.insert(incomeTrx)

        trxDao.deleteById(incomeTrx.id)

        val result = trxDao.getByIdWithDetails(incomeTrx.id)
        assertNull(result)
    }

    @Test
    fun getByIdWithDetails_withNonExistentId_shouldReturnNull() = runTest {
        val result = trxDao.getByIdWithDetails("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getFilteredWithDetails_withNoResults_shouldReturnEmptyList() = runTest {
        val results = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = 1000,
            type = TrxTypeEntity.Income,
            categoryId = "non-existent-category",
            accountId = "non-existent-account"
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun getFilteredWithDetails_withNullFilters_shouldReturnAllTransactions() = runTest {
        setupBasicData()
        trxDao.insert(incomeTrx)
        trxDao.insert(transferTrx)

        val results = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = null,
            categoryId = null,
            accountId = null
        )

        assertEquals(2, results.size)
    }

    @Test
    fun getFilteredWithDetails_shouldReturnInCorrectOrder() = runTest {
        setupBasicData()

        val baseTime = System.currentTimeMillis()
        val olderTrx = incomeTrx.copy(
            id = "older-trx",
            transactionAt = baseTime - 10000,
            createdAt = baseTime - 10000
        )
        val newerTrx = incomeTrx.copy(
            id = "newer-trx",
            transactionAt = baseTime + 10000,
            createdAt = baseTime + 10000
        )

        trxDao.insert(olderTrx)
        trxDao.insert(newerTrx)

        val results = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = null,
            categoryId = null,
            accountId = null
        )

        assertTrue(results[0].trx.transactionAt >= results[1].trx.transactionAt)
        assertEquals(newerTrx.id, results[0].trx.id)
        assertEquals(olderTrx.id, results[1].trx.id)
    }

    @Test
    fun getFilteredWithDetails_withTimeRange_shouldFilterCorrectly() = runTest {
        setupBasicData()

        val baseTime = System.currentTimeMillis()
        val outsideRangeTrx = incomeTrx.copy(
            id = "outside-range",
            transactionAt = baseTime - 100000,
            createdAt = baseTime - 100000
        )
        val insideRangeTrx = incomeTrx.copy(
            id = "inside-range",
            transactionAt = baseTime,
            createdAt = baseTime
        )

        trxDao.insert(outsideRangeTrx)
        trxDao.insert(insideRangeTrx)

        val results = trxDao.getFilteredWithDetails(
            startTime = baseTime - 1000,
            endTime = baseTime + 1000,
            type = null,
            categoryId = null,
            accountId = null
        )

        assertEquals(1, results.size)
        assertEquals(insideRangeTrx.id, results[0].trx.id)
    }

    @Test
    fun getFilteredWithDetails_withAccountFilter_shouldFindTransactionsBySourceAccount() = runTest {
        setupBasicData()

        val differentAccount = sourceAccount.copy(id = "different-account")
        accountDao.insert(differentAccount)

        val trxWithDifferentAccount = incomeTrx.copy(
            id = "different-account-trx",
            sourceAccountId = differentAccount.id
        )

        trxDao.insert(incomeTrx)
        trxDao.insert(trxWithDifferentAccount)

        val results = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = null,
            categoryId = null,
            accountId = sourceAccount.id
        )

        assertEquals(1, results.size)
        assertEquals(incomeTrx.id, results[0].trx.id)
    }

    @Test
    fun getFilteredWithDetails_withAccountFilter_shouldFindTransactionsByTargetAccount() = runTest {
        setupBasicData()
        trxDao.insert(transferTrx)

        val results = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = null,
            categoryId = null,
            accountId = targetAccount.id
        )

        assertEquals(1, results.size)
        assertEquals(transferTrx.id, results[0].trx.id)
    }

    @Test
    fun getFilteredWithDetails_withTypeFilter_shouldReturnOnlyMatchingTypes() = runTest {
        setupBasicData()

        val expenseTrx = TrxEntity(
            id = "expense-trx",
            name = "Lunch",
            amount = 50_000,
            categoryId = spendingCategory.id,
            sourceAccountId = sourceAccount.id,
            targetAccountId = null,
            transactionAt = System.currentTimeMillis(),
            note = "Daily meal",
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            type = TrxTypeEntity.Spending
        )

        trxDao.insert(incomeTrx)
        trxDao.insert(expenseTrx)
        trxDao.insert(transferTrx)

        val incomeResults = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = TrxTypeEntity.Income,
            categoryId = null,
            accountId = null
        )

        val expenseResults = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = TrxTypeEntity.Spending,
            categoryId = null,
            accountId = null
        )

        assertEquals(1, incomeResults.size)
        assertEquals(incomeTrx.id, incomeResults[0].trx.id)

        assertEquals(1, expenseResults.size)
        assertEquals(expenseTrx.id, expenseResults[0].trx.id)
    }

    @Test
    fun getFilteredWithDetails_withCategoryFilter_shouldReturnOnlyMatchingCategory() = runTest {
        setupBasicData()

        val anotherIncomeCategory = incomeCategory.copy(id = "another-income-cat")
        categoryDao.insert(anotherIncomeCategory)

        val anotherIncomeTrx = incomeTrx.copy(
            id = "another-income-trx",
            categoryId = anotherIncomeCategory.id
        )

        trxDao.insert(incomeTrx)
        trxDao.insert(anotherIncomeTrx)

        val results = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = null,
            categoryId = incomeCategory.id,
            accountId = null
        )

        assertEquals(1, results.size)
        assertEquals(incomeTrx.id, results[0].trx.id)
    }

    @Test
    fun getFilteredWithDetails_withMultipleFilters_shouldApplyAllFilters() = runTest {
        setupBasicData()

        val baseTime = System.currentTimeMillis()
        val matchingTrx = incomeTrx.copy(
            id = "matching-trx",
            transactionAt = baseTime,
            createdAt = baseTime
        )

        val nonMatchingTypeTrx = transferTrx.copy(
            id = "non-matching-type",
            transactionAt = baseTime,
            createdAt = baseTime
        )

        trxDao.insert(matchingTrx)
        trxDao.insert(nonMatchingTypeTrx)

        val results = trxDao.getFilteredWithDetails(
            startTime = baseTime - 1000,
            endTime = baseTime + 1000,
            type = TrxTypeEntity.Income,
            categoryId = incomeCategory.id,
            accountId = sourceAccount.id
        )

        assertEquals(1, results.size)
        assertEquals(matchingTrx.id, results[0].trx.id)
    }

    @Test
    fun insert_withOnConflictReplace_shouldReplaceExistingTransaction() = runTest {
        setupBasicData()
        trxDao.insert(incomeTrx)

        val replacementTrx = incomeTrx.copy(
            name = "Replaced Transaction",
            amount = 999_999
        )
        trxDao.insert(replacementTrx)

        val result = trxDao.getByIdWithDetails(incomeTrx.id)
        assertNotNull(result)
        assertEquals("Replaced Transaction", result.trx.name)
        assertEquals(999_999, result.trx.amount)
    }

    @Test
    fun deleteById_withNonExistentId_shouldNotThrowException() = runTest {
        trxDao.deleteById("non-existent-id")
    }

    @Test
    fun getFilteredWithDetails_withEmptyDatabase_shouldReturnEmptyList() = runTest {
        val results = trxDao.getFilteredWithDetails(
            startTime = 0,
            endTime = Long.MAX_VALUE,
            type = null,
            categoryId = null,
            accountId = null
        )

        assertTrue(results.isEmpty())
    }
}