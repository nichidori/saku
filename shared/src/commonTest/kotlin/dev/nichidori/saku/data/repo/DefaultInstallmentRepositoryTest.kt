package dev.nichidori.saku.data.repo

import androidx.room.Room
import dev.nichidori.saku.core.event.AppEventBus
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.*
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlinx.datetime.DateTimeUnit.Companion.MONTH
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultInstallmentRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultInstallmentRepository
    private lateinit var trxRepository: DefaultTrxRepository

    private val creditId = "credit-1"
    private val expenseCategoryId = "cat-exp"

    private val timeZone: TimeZone
        get() = TimeZone.currentSystemDefault()

    private val currentMonth: YearMonth
        get() = Clock.System.now().toLocalDateTime(timeZone).let { YearMonth(it.year, it.month) }

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        trxRepository = DefaultTrxRepository(db, AppEventBus())
        repository = DefaultInstallmentRepository(db, trxRepository, AppEventBus())
        runTest {
            val now = Clock.System.now().toEpochMilliseconds()
            db.creditDao().insert(
                CreditEntity(
                    id = creditId,
                    name = "My Credit",
                    limit = 100_000_000L,
                    currentAmount = 0L,
                    createdAt = now,
                    updatedAt = null
                )
            )
            db.categoryDao().insert(
                CategoryEntity(
                    id = expenseCategoryId,
                    name = "Gadget",
                    type = TrxTypeEntity.Expense,
                    parentId = null,
                    createdAt = now,
                    updatedAt = null,
                    icon = null
                )
            )
        }
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun credit(): Credit {
        return db.creditDao().getById(creditId)!!.toDomain()
    }

    private suspend fun expenseCategory(): Category {
        return db.categoryDao().getById(expenseCategoryId)!!.toDomain()
    }

    private fun purchasesAt(month: YearMonth): Instant {
        return month.firstDay.atStartOfDayIn(timeZone)
    }

    @Test
    fun createInstallment_shouldCreateChargeTrxAndIncreaseCreditBalance() = runTest {
        val id = repository.createInstallment(
            description = "iPhone 15",
            category = expenseCategory(),
            credit = credit(),
            principal = 12_000_000L,
            months = 12,
            monthlyRatePercent = 0.0,
            purchaseAt = Clock.System.now(),
        )

        assertEquals(12_000_000L, credit().currentAmount)

        val trxs = db.trxDao().getByInstallmentId(id)
        val charge = trxs.first { it.installmentIndex == null }
        assertEquals(12_000_000L, charge.amount)
        assertEquals(creditId, charge.sourceCreditId)
        assertEquals(TrxTypeEntity.Expense, charge.type)

        val plans = repository.getAllInstallments()
        assertEquals(1, plans.size)
        assertEquals(12, plans.first().months)
        assertEquals(1_000_000L, plans.first().monthlyPayment)
    }

    @Test
    fun createInstallment_withInterest_shouldIncludeInterestInTotal() = runTest {
        val id = repository.createInstallment(
            description = "Laptop",
            category = expenseCategory(),
            credit = credit(),
            principal = 10_000_000L,
            months = 10,
            monthlyRatePercent = 1.0,
            purchaseAt = Clock.System.now(),
        )

        val plan = repository.getInstallmentById(id)!!
        assertEquals(11_000_000L, plan.totalAmount)
        assertEquals(1_100_000L, plan.monthlyPayment)
        assertEquals(1_100_000L, plan.lastPayment)
        assertEquals(11_000_000L, credit().currentAmount)
    }

    @Test
    fun createInstallment_withRemainder_shouldPutRemainderOnLastPayment() = runTest {
        val id = repository.createInstallment(
            description = "Item",
            category = expenseCategory(),
            credit = credit(),
            principal = 10L,
            months = 6,
            monthlyRatePercent = 0.0,
            purchaseAt = Clock.System.now(),
        )

        val plan = repository.getInstallmentById(id)!!
        assertEquals(10L, plan.totalAmount)
        assertEquals(1L, plan.monthlyPayment)
        assertEquals(5L, plan.lastPayment)
    }

    @Test
    fun createInstallment_shouldOnlyCountFirstInstallmentInBudget_notTheCharge() = runTest {
        addBudgetFor(expenseCategoryId, currentMonth)

        repository.createInstallment(
            description = "TV",
            category = expenseCategory(),
            credit = credit(),
            principal = 2_400_000L,
            months = 12,
            monthlyRatePercent = 0.0,
            purchaseAt = Clock.System.now(),
        )

        assertEquals(200_000L, budgetSpent(expenseCategoryId, currentMonth))
    }

    @Test
    fun createInstallment_shouldThrowForNonExpenseCategory() = runTest {
        db.categoryDao().insert(
            CategoryEntity(
                id = "cat-inc",
                name = "Salary",
                type = TrxTypeEntity.Income,
                parentId = null,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = null,
                icon = null
            )
        )
        val incomeCategory = db.categoryDao().getById("cat-inc")!!.toDomain()

        assertFailsWith<IllegalArgumentException> {
            repository.createInstallment(
                description = "Expense only",
                category = incomeCategory,
                credit = credit(),
                principal = 100L,
                months = 3,
                monthlyRatePercent = 0.0,
                purchaseAt = Clock.System.now(),
            )
        }
    }

    @Test
    fun processDueInstallments_shouldCreateDueChildrenLazilyAndBeIdempotent() = runTest {
        val startMonth = currentMonth.minus(2, MONTH)
        val id = repository.createInstallment(
            description = "Phone",
            category = expenseCategory(),
            credit = credit(),
            principal = 3_000_000L,
            months = 12,
            monthlyRatePercent = 0.0,
            purchaseAt = purchasesAt(startMonth),
        )

        assertEquals(3_000_000L, credit().currentAmount)

        repository.processDueInstallments()

        val children = db.trxDao().getByInstallmentId(id)
            .filter { it.installmentIndex != null }
            .sortedBy { it.installmentIndex }
        assertEquals(3, children.size)
        children.forEach { assertEquals(250_000L, it.amount) }

        assertEquals(setOf(0, 1, 2), children.map { it.installmentIndex!! }.toSet())
        assertEquals(3_000_000L, credit().currentAmount)

        repository.processDueInstallments()

        val childrenAfter = db.trxDao().getByInstallmentId(id)
            .filter { it.installmentIndex != null }
        assertEquals(3, childrenAfter.size)
    }

    @Test
    fun processDueInstallments_shouldIncludeChildrenInTrxListAndExcludeChargeWhenRequested() = runTest {
        val id = repository.createInstallment(
            description = "Phone",
            category = expenseCategory(),
            credit = credit(),
            principal = 1_200_000L,
            months = 12,
            monthlyRatePercent = 0.0,
            purchaseAt = Clock.System.now(),
        )
        repository.processDueInstallments()

        val filter = TrxFilter(month = currentMonth)
        val all = trxRepository.getFilteredTrxs(filter)
        val filtered = trxRepository.getFilteredTrxs(filter.copy(excludeInstallmentCharges = true))

        val isCharge = { t: Trx -> (t as? Trx.Expense)?.installment is InstallmentInfo.Charge }
        val isChildAt = { t: Trx, index: Int -> (t as? Trx.Expense)?.installment == InstallmentInfo.Installment(installmentId = id, index = index, totalMonths = 12) }

        assertTrue(all.any { isCharge(it) })
        assertFalse(filtered.any { isCharge(it) })
        assertTrue(filtered.any { isChildAt(it, 0) })

        val child = filtered.first { isChildAt(it, 0) } as Trx.Expense
        assertEquals(12, (child.installment as InstallmentInfo.Installment).totalMonths)
        val charge = all.first { isCharge(it) } as Trx.Expense
        assertEquals(12, (charge.installment as InstallmentInfo.Charge).totalMonths)
    }

    @Test
    fun createInstallment_shouldCreateFirstInstallmentImmediatelyInPurchaseMonth() = runTest {
        addBudgetFor(expenseCategoryId, currentMonth)

        val id = repository.createInstallment(
            description = "Phone",
            category = expenseCategory(),
            credit = credit(),
            principal = 1_200_000L,
            months = 12,
            monthlyRatePercent = 0.0,
            purchaseAt = Clock.System.now(),
        )

        val children = db.trxDao().getByInstallmentId(id)
            .filter { it.installmentIndex != null }
        assertEquals(1, children.size)
        assertEquals(setOf(0), children.map { it.installmentIndex!! }.toSet())
        assertEquals(100_000L, children.first().amount)
        assertEquals(100_000L, budgetSpent(expenseCategoryId, currentMonth))

        repository.processDueInstallments()

        assertEquals(1, db.trxDao().getByInstallmentId(id).count { it.installmentIndex != null })
        assertEquals(100_000L, budgetSpent(expenseCategoryId, currentMonth))
    }

    @Test
    fun deleteInstallment_shouldRemovePlanChildrenAndChargeAndRevertEffects() = runTest {
        val id = repository.createInstallment(
            description = "Phone",
            category = expenseCategory(),
            credit = credit(),
            principal = 1_200_000L,
            months = 12,
            monthlyRatePercent = 0.0,
            purchaseAt = Clock.System.now(),
        )
        repository.processDueInstallments()
        assertEquals(1_200_000L, credit().currentAmount)

        repository.deleteInstallment(id)

        assertNull(repository.getInstallmentById(id))
        assertTrue(db.trxDao().getByInstallmentId(id).isEmpty())
        assertEquals(0L, credit().currentAmount)
    }

    @Test
    fun deleteInstallment_shouldRevertBudgetForChildren() = runTest {
        addBudgetFor(expenseCategoryId, currentMonth)

        val id = repository.createInstallment(
            description = "Phone",
            category = expenseCategory(),
            credit = credit(),
            principal = 1_200_000L,
            months = 12,
            monthlyRatePercent = 0.0,
            purchaseAt = Clock.System.now(),
        )
        repository.processDueInstallments()
        assertEquals(100_000L, budgetSpent(expenseCategoryId, currentMonth))

        repository.deleteInstallment(id)

        assertEquals(0L, budgetSpent(expenseCategoryId, currentMonth))
        assertEquals(0L, credit().currentAmount)
    }

    private suspend fun addBudgetFor(categoryId: String, month: YearMonth) {
        val now = Clock.System.now()
        db.budgetTemplateDao().insert(
            BudgetTemplateEntity(
                id = "template-$categoryId",
                categoryId = categoryId,
                defaultAmount = 10_000_000L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )
        db.budgetDao().insert(
            BudgetEntity(
                id = "budget-$categoryId-${month.year}-${month.month.number}",
                templateId = "template-$categoryId",
                categoryId = categoryId,
                month = month.month.number,
                year = month.year,
                baseAmount = 10_000_000L,
                spentAmount = 0L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = null
            )
        )
    }

    private suspend fun budgetSpent(categoryId: String, month: YearMonth): Long {
        return db.budgetDao().getByMonthAndYearWithCategory(
            month = month.month.number,
            year = month.year
        ).firstOrNull { it.budget.categoryId == categoryId }
            ?.budget?.spentAmount ?: 0L
    }
}