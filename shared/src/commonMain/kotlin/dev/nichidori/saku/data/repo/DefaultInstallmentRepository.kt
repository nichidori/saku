package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.core.event.AppEvent
import dev.nichidori.saku.core.event.AppEventBus
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.*
import dev.nichidori.saku.domain.model.*
import dev.nichidori.saku.domain.repo.InstallmentRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.datetime.*
import kotlinx.datetime.DateTimeUnit.Companion.MONTH
import java.util.*
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultInstallmentRepository(
    private val db: AppDatabase,
    private val trxRepository: TrxRepository,
    private val appEventBus: AppEventBus = AppEventBus(),
) : InstallmentRepository {

    private val installmentDao = db.installmentDao()
    private val trxDao = db.trxDao()
    private val budgetDao = db.budgetDao()
    private val categoryDao = db.categoryDao()
    private val creditDao = db.creditDao()

    override suspend fun createInstallment(
        description: String,
        category: Category,
        credit: Credit,
        principal: Long,
        months: Int,
        monthlyRatePercent: Double,
        purchaseAt: Instant,
    ): String {
        require(principal > 0) { "Principal must be greater than zero" }
        require(months > 0) { "Months must be greater than zero" }
        require(monthlyRatePercent >= 0.0) { "Interest rate cannot be negative" }
        require(category.type == TrxType.Expense) { "Installments are only available for Expense categories" }

        val totalAmount = computeTotalAmount(principal, months, monthlyRatePercent)
        val monthlyPayment = totalAmount / months
        val lastPayment = monthlyPayment + (totalAmount % months)
        val purchaseDay = purchaseAt.toLocalDateTime(TimeZone.currentSystemDefault()).day

        val installment = Installment(
            id = UUID.randomUUID().toString(),
            description = description,
            category = category,
            credit = credit,
            principal = principal,
            months = months,
            monthlyRatePercent = monthlyRatePercent,
            totalAmount = totalAmount,
            monthlyPayment = monthlyPayment,
            lastPayment = lastPayment,
            startAt = purchaseAt,
            dueDay = minOf(purchaseDay, 28),
            nextIndex = 0,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        db.useWriterConnection {
            it.immediateTransaction {
                installmentDao.insert(installment.toEntity())
            }
        }

        try {
            trxRepository.addTrx(
                type = TrxType.Expense,
                transactionAt = purchaseAt,
                amount = totalAmount,
                description = description,
                sourceAccount = TrxAccount.Credit(credit),
                targetAccount = null,
                category = category,
                installmentId = installment.id,
                installmentIndex = null
            )
        } catch (e: Exception) {
            db.useWriterConnection {
                it.immediateTransaction {
                    installmentDao.deleteById(installment.id)
                }
            }
            throw e
        }

        processDueInstallments()

        return installment.id
    }

    override suspend fun getInstallmentById(id: String): Installment? {
        return db.useReaderConnection {
            val entity = installmentDao.getById(id) ?: return@useReaderConnection null
            entity.toDomain(
                category = resolveCategory(entity.categoryId),
                credit = resolveCredit(entity.creditId)
            )
        }
    }

    override suspend fun getAllInstallments(): List<Installment> {
        return db.useReaderConnection {
            installmentDao.getAll().map { entity ->
                entity.toDomain(
                    category = resolveCategory(entity.categoryId),
                    credit = resolveCredit(entity.creditId)
                )
            }
        }
    }

    override suspend fun deleteInstallment(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                installmentDao.getById(id) ?: throw NoSuchElementException("Installment not found")

                val trxs = trxDao.getByInstallmentId(id)
                val charge = trxs.firstOrNull { it.installmentIndex == null }
                val children = trxs.filter { it.installmentIndex != null }
                    .sortedBy { it.installmentIndex }

                children.forEach { child ->
                    trxRepository.deleteTrx(child.id)
                }
                charge?.let { trxRepository.deleteTrx(it.id) }

                installmentDao.deleteById(id)
            }
        }
    }

    override suspend fun processDueInstallments() {
        val timeZone = TimeZone.currentSystemDefault()
        val currentMonth = Clock.System.now()
            .toLocalDateTime(timeZone)
            .let { YearMonth(it.year, it.month) }

        val plans = db.useReaderConnection { installmentDao.getAll() }
        if (plans.isEmpty()) return

        for (rawPlan in plans) {
            val plan = rawPlan.toDomain(
                resolveCategory(rawPlan.categoryId),
                resolveCredit(rawPlan.creditId)
            )
            val startMonth = plan.startAt.toLocalDateTime(timeZone)
                .let { YearMonth(it.year, it.month) }

            var index = rawPlan.nextIndex
            while (index < plan.months) {
                val dueMonth = startMonth.plus(index, MONTH)
                if (dueMonth > currentMonth) break

                createDueChild(rawPlan, plan, dueMonth, index)
                index++
            }
        }
    }

    private suspend fun createDueChild(
        rawPlan: InstallmentEntity,
        plan: Installment,
        dueMonth: YearMonth,
        index: Int,
    ) {
        val timeZone = TimeZone.currentSystemDefault()
        val dueDate = LocalDate(
            year = dueMonth.year,
            month = dueMonth.month,
            day = minOf(plan.dueDay, dueMonth.days.size)
        ).atStartOfDayIn(timeZone)

        val amount = plan.paymentAmountForIndex(index)
        val currentTime = Clock.System.now()
        val newId = UUID.randomUUID().toString()

        db.useWriterConnection {
            it.immediateTransaction {
                val existing = trxDao.getByInstallmentIndex(plan.id, index)
                if (existing == null) {
                    trxDao.insert(
                        TrxEntity(
                            id = newId,
                            description = plan.description,
                            amount = amount,
                            categoryId = plan.category.id,
                            sourceAccountId = null,
                            sourceCreditId = plan.credit.id,
                            targetAccountId = null,
                            targetCreditId = null,
                            transactionAt = dueDate.toEpochMilliseconds(),
                            createdAt = currentTime.toEpochMilliseconds(),
                            updatedAt = null,
                            type = TrxTypeEntity.Expense,
                            installmentId = plan.id,
                            installmentIndex = index
                        )
                    )

                    applyExpenseBudget(
                        categoryId = plan.category.id,
                        parentCategoryId = plan.category.parent?.id,
                        year = dueMonth.year,
                        month = dueMonth.month.number,
                        delta = amount,
                        updatedAt = currentTime
                    )

                    installmentDao.update(
                        rawPlan.copy(nextIndex = index + 1, updatedAt = currentTime.toEpochMilliseconds())
                    )

                    appEventBus.emit(
                        AppEvent.TrxChanged.Created(
                            Trx.Expense(
                                id = newId,
                                description = plan.description,
                                amount = amount,
                                category = plan.category,
                                sourceAccount = TrxAccount.Credit(plan.credit),
                                transactionAt = dueDate,
                                createdAt = currentTime,
                                updatedAt = null,
                                installmentId = plan.id,
                                installmentIndex = index
                            )
                        )
                    )
                }
            }
        }
    }

    private suspend fun applyExpenseBudget(
        categoryId: String,
        parentCategoryId: String?,
        year: Int,
        month: Int,
        delta: Long,
        updatedAt: Instant,
    ) {
        val budgets = budgetDao.getByMonthAndYearWithCategory(month = month, year = year)
        val budget = budgets.firstOrNull { it.category.id == categoryId }
        val parentBudget = budgets.firstOrNull { it.category.id == parentCategoryId }

        if (budget != null) {
            budgetDao.update(
                budget.budget.copy(
                    spentAmount = budget.budget.spentAmount + delta,
                    updatedAt = updatedAt.toEpochMilliseconds()
                )
            )
        }
        if (parentBudget != null) {
            budgetDao.update(
                parentBudget.budget.copy(
                    spentAmount = parentBudget.budget.spentAmount + delta,
                    updatedAt = updatedAt.toEpochMilliseconds()
                )
            )
        }
    }

    private fun computeTotalAmount(principal: Long, months: Int, monthlyRatePercent: Double): Long {
        val total = principal * (1.0 + monthlyRatePercent / 100.0 * months)
        return total.roundToLong()
    }

    private suspend fun resolveCategory(categoryId: String): Category {
        return requireNotNull(categoryDao.getById(categoryId)) {
            "Category not found: $categoryId"
        }.toDomain()
    }

    private suspend fun resolveCredit(creditId: String): Credit {
        return requireNotNull(creditDao.getById(creditId)) {
            "Credit not found: $creditId"
        }.toDomain()
    }
}