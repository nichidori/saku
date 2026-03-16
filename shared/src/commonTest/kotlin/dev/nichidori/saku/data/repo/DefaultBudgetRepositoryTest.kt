package dev.nichidori.saku.data.repo

import androidx.room.Room
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.BudgetTemplate
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*
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
        defaultAmount = 5_000_000L,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val budget = Budget(
        id = "budget-1",
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
    fun ensureBudgetsExist_shouldCreateBudgets() = runTest {
        val templateCreatedAt = Clock.System.now()
        val templateWithDate = template.copy(createdAt = templateCreatedAt)
        
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(templateWithDate.toEntity())

        val timeZone = TimeZone.currentSystemDefault()
        val startDateTime = templateCreatedAt.toLocalDateTime(timeZone)
        val startYearMonth = YearMonth(startDateTime.year, startDateTime.month)
        
        val nextMonth = startYearMonth.plusMonth()
        val twoMonthsLater = nextMonth.plusMonth()

        repository.ensureBudgetsExist(twoMonthsLater)

        val startBudget = repository.getBudgetsByMonthAndYear(startYearMonth.month.number, startYearMonth.year)
        val nextMonthBudget = repository.getBudgetsByMonthAndYear(nextMonth.month.number, nextMonth.year)
        val twoMonthsLaterBudget = repository.getBudgetsByMonthAndYear(twoMonthsLater.month.number, twoMonthsLater.year)

        assertEquals(1, startBudget.size)
        assertEquals(1, nextMonthBudget.size)
        assertEquals(1, twoMonthsLaterBudget.size)
        
        assertEquals(templateWithDate.defaultAmount, startBudget.first().baseAmount)
        assertEquals(0L, startBudget.first().spentAmount)
    }

    @Test
    fun ensureBudgetsExist_withTransactions_shouldSetSpentAmount() = runTest {
        val templateCreatedAt = Clock.System.now()
        val templateWithDate = template.copy(createdAt = templateCreatedAt)
        
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(templateWithDate.toEntity())

        val timeZone = TimeZone.currentSystemDefault()
        val startDateTime = templateCreatedAt.toLocalDateTime(timeZone)
        val startYearMonth = YearMonth(startDateTime.year, startDateTime.month)
        
        val sourceAccount = dev.nichidori.saku.data.entity.AccountEntity(
            id = "acc-1",
            name = "Cash",
            initialAmount = 1_000_000,
            currentAmount = 1_000_000,
            type = dev.nichidori.saku.data.entity.AccountTypeEntity.Cash,
            createdAt = 0L,
            updatedAt = null
        )
        db.accountDao().insert(sourceAccount)

        val trxTime = startYearMonth.firstDay.atStartOfDayIn(timeZone).toEpochMilliseconds() + 1000L
        val trx = dev.nichidori.saku.data.entity.TrxEntity(
            id = "trx-1",
            description = "Lunch",
            amount = 50_000,
            categoryId = category.id,
            sourceAccountId = sourceAccount.id,
            targetAccountId = null,
            transactionAt = trxTime,
            note = null,
            createdAt = 0L,
            updatedAt = null,
            type = dev.nichidori.saku.data.entity.TrxTypeEntity.Expense
        )
        db.trxDao().insert(trx)

        repository.ensureBudgetsExist(startYearMonth)

        val startBudget = repository.getBudgetsByMonthAndYear(startYearMonth.month.number, startYearMonth.year)
        
        assertEquals(1, startBudget.size)
        assertEquals(50_000L, startBudget.first().spentAmount)
    }

    @Test
    fun getBudgetById_shouldReturnCorrectBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity("tmpl-1"))

        val result = repository.getBudgetById(budget.id)
        assertNotNull(result)
        assertEquals(category.id, result.category.id)
    }

    @Test
    fun getBudgetsByMonthAndYear_shouldReturnMatchingBudgets() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity("tmpl-1"))

        val result = repository.getBudgetsByMonthAndYear(3, 2026)
        assertEquals(1, result.size)
    }

    @Test
    fun getBudgetsByCategory_shouldReturnMatchingBudgets() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity("tmpl-1"))

        val result = repository.getBudgetsByCategory(category.id)
        assertEquals(1, result.size)
    }

    @Test
    fun updateBudget_shouldUpdateExistingBudget() = runTest {
        db.categoryDao().insert(category.toEntity())
        db.budgetTemplateDao().insert(template.toEntity())
        db.budgetDao().insert(budget.toEntity("tmpl-1"))

        repository.updateBudget(
            budget.id,
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
        db.budgetDao().insert(budget.toEntity("tmpl-1"))

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
