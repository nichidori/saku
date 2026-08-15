package dev.nichidori.saku.data.dao

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.AccountEntity
import dev.nichidori.saku.data.entity.AccountTypeEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxTemplateEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TrxTemplateDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TrxTemplateDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var accountDao: AccountDao

    private val category = CategoryEntity(
        id = "cat-food",
        name = "Food",
        type = TrxTypeEntity.Expense,
        parentId = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    private val cashAccount = AccountEntity(
        id = "acc-cash",
        name = "Cash",
        currentAmount = 10_000L,
        type = AccountTypeEntity.Cash,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    private val bankAccount = AccountEntity(
        id = "acc-bank",
        name = "Bank",
        currentAmount = 20_000L,
        type = AccountTypeEntity.Bank,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    private val incomeTemplate = TrxTemplateEntity(
        id = "tmpl-income",
        name = "Salary",
        type = TrxTypeEntity.Income,
        description = "Monthly salary",
        amount = 5_000_000L,
        categoryId = "cat-food",
        sourceAccountId = "acc-bank",
        sourceCreditId = null,
        targetAccountId = null,
        targetCreditId = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    private val expenseTemplate = TrxTemplateEntity(
        id = "tmpl-expense",
        name = "Groceries",
        type = TrxTypeEntity.Expense,
        description = "Weekly groceries",
        amount = 500_000L,
        categoryId = "cat-food",
        sourceAccountId = "acc-cash",
        sourceCreditId = null,
        targetAccountId = null,
        targetCreditId = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    private val transferTemplate = TrxTemplateEntity(
        id = "tmpl-transfer",
        name = "Deposit",
        type = TrxTypeEntity.Transfer,
        description = "Transfer to bank",
        amount = 1_000_000L,
        categoryId = null,
        sourceAccountId = "acc-cash",
        sourceCreditId = null,
        targetAccountId = "acc-bank",
        targetCreditId = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        dao = db.trxTemplateDao()
        categoryDao = db.categoryDao()
        accountDao = db.accountDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetByIdWithDetails_incomeTemplate() = runTest {
        categoryDao.insert(category)
        accountDao.insert(bankAccount)
        dao.insert(incomeTemplate)

        val result = dao.getByIdWithDetails(incomeTemplate.id)
        assertNotNull(result)
        assertEquals("Salary", result.trxTemplate.name)
        assertEquals(TrxTypeEntity.Income, result.trxTemplate.type)
        assertEquals("Monthly salary", result.trxTemplate.description)
        assertEquals(5_000_000L, result.trxTemplate.amount)
        assertEquals("Food", result.categoryWithParent?.category?.name)
        assertEquals("Bank", result.sourceAccount?.name)
        assertNull(result.targetAccount)
    }

    @Test
    fun insertAndGetByIdWithDetails_expenseTemplate() = runTest {
        categoryDao.insert(category)
        accountDao.insert(cashAccount)
        dao.insert(expenseTemplate)

        val result = dao.getByIdWithDetails(expenseTemplate.id)
        assertNotNull(result)
        assertEquals("Groceries", result.trxTemplate.name)
        assertEquals(TrxTypeEntity.Expense, result.trxTemplate.type)
        assertEquals("Food", result.categoryWithParent?.category?.name)
        assertEquals("Cash", result.sourceAccount?.name)
        assertNull(result.targetAccount)
    }

    @Test
    fun insertAndGetByIdWithDetails_transferTemplate() = runTest {
        accountDao.insert(cashAccount)
        accountDao.insert(bankAccount)
        dao.insert(transferTemplate)

        val result = dao.getByIdWithDetails(transferTemplate.id)
        assertNotNull(result)
        assertEquals("Deposit", result.trxTemplate.name)
        assertEquals(TrxTypeEntity.Transfer, result.trxTemplate.type)
        assertNull(result.categoryWithParent)
        assertEquals("Cash", result.sourceAccount?.name)
        assertEquals("Bank", result.targetAccount?.name)
    }

    @Test
    fun insertAndGetByIdWithDetails_nullCategory() = runTest {
        accountDao.insert(cashAccount)
        accountDao.insert(bankAccount)
        val templateNoCategory = transferTemplate.copy(categoryId = null)
        dao.insert(templateNoCategory)

        val result = dao.getByIdWithDetails(templateNoCategory.id)
        assertNotNull(result)
        assertNull(result.categoryWithParent)
    }

    @Test
    fun insertAndGetByIdWithDetails_nullTargetAccount() = runTest {
        categoryDao.insert(category)
        accountDao.insert(cashAccount)
        val templateNoTarget = expenseTemplate.copy(targetAccountId = null, targetCreditId = null)
        dao.insert(templateNoTarget)

        val result = dao.getByIdWithDetails(templateNoTarget.id)
        assertNotNull(result)
        assertNull(result.targetAccount)
    }

    @Test
    fun getAllWithDetails() = runTest {
        categoryDao.insert(category)
        accountDao.insert(cashAccount)
        accountDao.insert(bankAccount)
        dao.insert(incomeTemplate)
        dao.insert(expenseTemplate)
        dao.insert(transferTemplate)

        val results = dao.getAllWithDetails()
        assertEquals(3, results.size)
    }

    @Test
    fun update() = runTest {
        categoryDao.insert(category)
        accountDao.insert(cashAccount)
        dao.insert(expenseTemplate)

        val updated = expenseTemplate.copy(
            name = "Weekly Groceries",
            amount = 750_000L,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)

        val result = dao.getByIdWithDetails(expenseTemplate.id)
        assertNotNull(result)
        assertEquals("Weekly Groceries", result.trxTemplate.name)
        assertEquals(750_000L, result.trxTemplate.amount)
    }

    @Test
    fun deleteById() = runTest {
        categoryDao.insert(category)
        accountDao.insert(cashAccount)
        dao.insert(expenseTemplate)

        dao.deleteById(expenseTemplate.id)
        assertNull(dao.getByIdWithDetails(expenseTemplate.id))
    }

    @Test
    fun getByIdWithDetails_nonExistentId_shouldReturnNull() = runTest {
        val result = dao.getByIdWithDetails("non-existent-id")
        assertNull(result)
    }
}
