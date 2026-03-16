package dev.nichidori.saku.data.repo

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
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

class DefaultBudgetRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultBudgetRepository

    private val category = Category(
        id = "cat-food",
        name = "Food",
        type = TrxType.Expense,
        parent = null,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val budget = Budget(
        id = "budget-1",
        name = "Monthly Food Budget",
        category = category,
        month = 3,
        year = 2026,
        totalAmount = 5_000_000L,
        spentAmount = 1_000_000L,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        repository = DefaultBudgetRepository(db)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addBudget_shouldInsertBudgetWithGeneratedId() = runTest {
        db.categoryDao().insert(category.toEntity())
        repository.addBudget(
            budget.name,
            budget.category,
            budget.month,
            budget.year,
            budget.totalAmount,
            budget.spentAmount
        )
        
        val budgets = db.budgetDao().getByMonthAndYearWithCategory(3, 2026).map { it.toDomain() }
        assertEquals(1, budgets.size)
        assertNotEquals("budget-1", budgets.first().id)
        assertEquals(budget.name, budgets.first().name)
    }

    @Test
    fun getBudgetById_shouldReturnCorrectBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetDao().insert(budget.toEntity())

        val result = repository.getBudgetById(budget.id)
        assertNotNull(result)
        assertEquals(budget.name, result.name)
        assertEquals(category.id, result.category.id)
    }

    @Test
    fun getBudgetsByMonthAndYear_shouldReturnMatchingBudgets() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetDao().insert(budget.toEntity())

        val result = repository.getBudgetsByMonthAndYear(3, 2026)
        assertEquals(1, result.size)
        assertEquals(budget.name, result.first().name)
    }

    @Test
    fun updateBudget_shouldUpdateExistingBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetDao().insert(budget.toEntity())

        repository.updateBudget(
            budget.id,
            "Updated Budget",
            budget.category,
            budget.month,
            budget.year,
            6_000_000L,
            1_500_000L
        )

        val result = repository.getBudgetById(budget.id)
        assertNotNull(result)
        assertEquals("Updated Budget", result.name)
        assertEquals(6_000_000L, result.totalAmount)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun deleteBudget_shouldRemoveBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetDao().insert(budget.toEntity())

        repository.deleteBudget(budget.id)

        val result = repository.getBudgetById(budget.id)
        assertNull(result)
    }

    @Test
    fun deleteBudget_shouldThrowIfNotFound() = runTest {
        assertFailsWith<NoSuchElementException> {
            repository.deleteBudget("non-existent-id")
        }
    }
}
