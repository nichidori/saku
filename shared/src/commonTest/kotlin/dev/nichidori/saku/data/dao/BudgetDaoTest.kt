package dev.nichidori.saku.data.dao

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.BudgetEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BudgetDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var budgetDao: BudgetDao
    private lateinit var categoryDao: CategoryDao

    private val category = CategoryEntity(
        id = "cat-food",
        name = "Food",
        type = TrxTypeEntity.Expense,
        parentId = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    private val budget = BudgetEntity(
        id = "budget-1",
        name = "Monthly Food Budget",
        categoryId = "cat-food",
        month = 3,
        year = 2026,
        totalAmount = 5_000_000L,
        spentAmount = 1_000_000L,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        budgetDao = db.budgetDao()
        categoryDao = db.categoryDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetByIdWithCategory_shouldReturnMatchingBudgetWithCategory() = runTest {
        categoryDao.insert(category)
        budgetDao.insert(budget)

        val result = budgetDao.getByIdWithCategory(budget.id)

        assertNotNull(result)
        assertEquals(budget.id, result.budget.id)
        assertEquals(budget.name, result.budget.name)
        assertEquals(category.id, result.category.id)
        assertEquals(category.name, result.category.name)
    }

    @Test
    fun getByMonthAndYearWithCategory_shouldReturnBudgetsForSpecificMonth() = runTest {
        categoryDao.insert(category)
        budgetDao.insert(budget)
        
        val anotherBudget = budget.copy(id = "budget-2", month = 4)
        budgetDao.insert(anotherBudget)

        val results = budgetDao.getByMonthAndYearWithCategory(3, 2026)

        assertEquals(1, results.size)
        assertEquals(budget.id, results[0].budget.id)
    }

    @Test
    fun getByCategoryIdWithCategory_shouldReturnBudgetsForSpecificCategory() = runTest {
        categoryDao.insert(category)
        budgetDao.insert(budget)

        val anotherCategory = category.copy(id = "cat-other", name = "Other")
        categoryDao.insert(anotherCategory)
        val anotherBudget = budget.copy(id = "budget-2", categoryId = "cat-other")
        budgetDao.insert(anotherBudget)

        val results = budgetDao.getByCategoryIdWithCategory(category.id)

        assertEquals(1, results.size)
        assertEquals(budget.id, results[0].budget.id)
    }

    @Test
    fun update_shouldUpdateBudgetDetails() = runTest {
        categoryDao.insert(category)
        budgetDao.insert(budget)

        val updatedBudget = budget.copy(name = "Updated Budget", totalAmount = 6_000_000L)
        budgetDao.update(updatedBudget)

        val result = budgetDao.getByIdWithCategory(budget.id)
        assertNotNull(result)
        assertEquals("Updated Budget", result.budget.name)
        assertEquals(6_000_000L, result.budget.totalAmount)
    }

    @Test
    fun deleteById_shouldRemoveBudget() = runTest {
        categoryDao.insert(category)
        budgetDao.insert(budget)

        budgetDao.deleteById(budget.id)

        val result = budgetDao.getByIdWithCategory(budget.id)
        assertNull(result)
    }

    @Test
    fun insertDuplicateUnique_shouldReplaceExisting() = runTest {
        categoryDao.insert(category)
        budgetDao.insert(budget)

        val duplicateBudget = budget.copy(id = "budget-new-id", name = "New Name")
        budgetDao.insert(duplicateBudget)

        val results = budgetDao.getByMonthAndYearWithCategory(3, 2026)
        assertEquals(1, results.size)
        assertEquals("budget-new-id", results[0].budget.id)
        assertEquals("New Name", results[0].budget.name)
    }
}
