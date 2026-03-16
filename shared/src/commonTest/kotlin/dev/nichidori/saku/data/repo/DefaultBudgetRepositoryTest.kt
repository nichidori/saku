package dev.nichidori.saku.data.repo

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.BudgetTemplate
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
import kotlin.time.Clock

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

    private val template = BudgetTemplate(
        id = "tmpl-1",
        category = category,
        startMonth = 1,
        startYear = 2026,
        defaultAmount = 5_000_000L,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val budget = Budget(
        id = "budget-1",
        templateId = "tmpl-1",
        category = category,
        month = 3,
        year = 2026,
        baseAmount = 5_000_000L,
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

    // Budget Template tests

    @Test
    fun addAndGetTemplate() = runTest {
        db.categoryDao().insert(category.toEntity())
        repository.addBudgetTemplate(
            template.category,
            template.startMonth,
            template.startYear,
            template.defaultAmount
        )

        val all = repository.getAllBudgetTemplates()
        assertEquals(1, all.size)
    }

    @Test
    fun getTemplateByCategoryId() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())

        val result = repository.getBudgetTemplateByCategoryId(category.id)
        assertNotNull(result)
        assertEquals(template.id, result.id)
    }

    @Test
    fun updateTemplate() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())

        repository.updateBudgetTemplate(
            template.id,
            template.category,
            template.startMonth,
            template.startYear,
            10_000_000L
        )

        val result = repository.getBudgetTemplateById(template.id)
        assertEquals(10_000_000L, result?.defaultAmount)
    }

    @Test
    fun deleteTemplate() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())

        repository.deleteBudgetTemplate(template.id)
        assertNull(repository.getBudgetTemplateById(template.id))
    }

    // Budget tests

    @Test
    fun addBudget_shouldInsertBudgetWithGeneratedId() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        repository.addBudget(
            budget.templateId,
            budget.category,
            budget.month,
            budget.year,
            budget.baseAmount,
            budget.spentAmount
        )
        
        val budgets = db.budgetDao().getByMonthAndYearWithCategory(3, 2026).map { it.toDomain() }
        assertEquals(1, budgets.size)
        assertNotEquals("budget-1", budgets.first().id)
        assertEquals("tmpl-1", budgets.first().templateId)
    }

    @Test
    fun getBudgetById_shouldReturnCorrectBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity())

        val result = repository.getBudgetById(budget.id)
        assertNotNull(result)
        assertEquals(category.id, result.category.id)
    }

    @Test
    fun getBudgetsByMonthAndYear_shouldReturnMatchingBudgets() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity())

        val result = repository.getBudgetsByMonthAndYear(3, 2026)
        assertEquals(1, result.size)
    }

    @Test
    fun getBudgetsByCategory_shouldReturnMatchingBudgets() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity())

        val result = repository.getBudgetsByCategory(category.id)
        assertEquals(1, result.size)
    }

    @Test
    fun updateBudget_shouldUpdateExistingBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity())

        repository.updateBudget(
            budget.id,
            budget.templateId,
            budget.category,
            budget.month,
            budget.year,
            6_000_000L,
            1_500_000L
        )

        val result = repository.getBudgetById(budget.id)
        assertNotNull(result)
        assertEquals(6_000_000L, result.baseAmount)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun deleteBudget_shouldRemoveBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
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
