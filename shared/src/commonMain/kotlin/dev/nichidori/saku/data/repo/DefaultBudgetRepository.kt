package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.BudgetTemplate
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.repo.BudgetRepository
import kotlinx.datetime.*
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultBudgetRepository(
    private val db: AppDatabase,
) : BudgetRepository {

    // Budget Template methods
    override suspend fun addBudgetTemplate(category: Category, defaultAmount: Long) {
        val template = BudgetTemplate(
            id = UUID.randomUUID().toString(),
            category = category,
            defaultAmount = defaultAmount,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.useWriterConnection {
            it.immediateTransaction {
                db.budgetTemplateDao().insert(template.toEntity())

                val currentTime = Clock.System.now()
                val timeZone = TimeZone.currentSystemDefault()
                val month = currentTime
                    .toLocalDateTime(timeZone = timeZone)
                    .let { date -> YearMonth(date.year, date.month) }

                val spentAmount = db.trxDao().getTotalAmount(
                    startTime = month.firstDay
                        .atStartOfDayIn(timeZone = timeZone)
                        .toEpochMilliseconds(),
                    endTime = month.lastDay
                        .plus(1, DAY)
                        .atStartOfDayIn(timeZone = timeZone)
                        .toEpochMilliseconds(),
                    categoryId = category.id,
                    type = TrxTypeEntity.Expense,
                ) ?: 0

                val budget = Budget(
                    id = UUID.randomUUID().toString(),
                    templateId = template.id,
                    category = category,
                    month = month,
                    baseAmount = template.defaultAmount,
                    spentAmount = spentAmount,
                    createdAt = currentTime,
                    updatedAt = null
                )
                db.budgetDao().insert(budget.toEntity())
            }
        }
    }

    override suspend fun getBudgetTemplateById(id: String): BudgetTemplate? {
        return db.useReaderConnection {
            db.budgetTemplateDao().getByIdWithCategory(id)?.toDomain()
        }
    }

    override suspend fun getBudgetTemplateByCategoryId(categoryId: String): BudgetTemplate? {
        return db.useReaderConnection {
            db.budgetTemplateDao().getByCategoryIdWithCategory(categoryId)?.toDomain()
        }
    }

    override suspend fun getAllBudgetTemplates(): List<BudgetTemplate> {
        return db.useReaderConnection {
            db.budgetTemplateDao().getAllWithCategory().map { it.toDomain() }
        }
    }

    override suspend fun updateBudgetTemplate(id: String, defaultAmount: Long) {
        db.useWriterConnection {
            it.immediateTransaction {
                val existing = db.budgetTemplateDao().getByIdWithCategory(id)?.toDomain()
                    ?: throw NoSuchElementException("Budget template not found")
                val updated = existing.copy(
                    defaultAmount = defaultAmount,
                    updatedAt = Clock.System.now()
                )
                db.budgetTemplateDao().update(updated.toEntity())
            }
        }
    }

    override suspend fun deleteBudgetTemplate(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.budgetTemplateDao().getByIdWithCategory(id)
                    ?: throw NoSuchElementException("Budget template not found")
                db.budgetTemplateDao().deleteById(id)
            }
        }
    }

    // Budget methods
    override suspend fun ensureBudgetsExist(now: YearMonth) {
        db.useWriterConnection {
            val templates = db.budgetTemplateDao().getAllWithCategory()
            val timeZone = TimeZone.currentSystemDefault()

            for (template in templates) {
                val category = template.category.toDomain()
                val start = Instant.fromEpochMilliseconds(template.budgetTemplate.createdAt)
                    .toLocalDateTime(timeZone = timeZone)
                    .let { d -> YearMonth(d.year, d.month) }
                val existingMonths = db.budgetDao()
                    .getByCategoryIdWithCategory(categoryId = category.id)
                    .map { d -> YearMonth(d.budget.year, d.budget.month) }
                    .toHashSet()

                it.immediateTransaction {
                    for (month in start.rangeUntil(now.plusMonth())) {
                        if (month !in existingMonths) {
                            val spentAmount = db.trxDao().getTotalAmount(
                                startTime = month.firstDay
                                    .atStartOfDayIn(timeZone = timeZone)
                                    .toEpochMilliseconds(),
                                endTime = month.lastDay
                                    .plus(1, DAY)
                                    .atStartOfDayIn(timeZone = timeZone)
                                    .toEpochMilliseconds(),
                                categoryId = category.id,
                                type = TrxTypeEntity.Expense,
                            ) ?: 0

                            val budget = Budget(
                                id = UUID.randomUUID().toString(),
                                templateId = template.budgetTemplate.id,
                                category = category,
                                month = month,
                                baseAmount = template.budgetTemplate.defaultAmount,
                                spentAmount = spentAmount,
                                createdAt = Clock.System.now(),
                                updatedAt = null
                            )
                            db.budgetDao().insert(budget.toEntity())
                        }
                    }
                }
            }
        }
    }

    override suspend fun getBudgetById(id: String): Budget? {
        return db.useReaderConnection {
            db.budgetDao().getByIdWithCategory(id)?.toDomain()
        }
    }

    override suspend fun getBudgetsByYearMonth(month: YearMonth): List<Budget> {
        return db.useReaderConnection {
            db.budgetDao()
                .getByMonthAndYearWithCategory(month = month.month.number, year = month.year)
                .map { it.toDomain() }
        }
    }

    override suspend fun getBudgetsByCategory(categoryId: String): List<Budget> {
        return db.useReaderConnection {
            db.budgetDao().getByCategoryIdWithCategory(categoryId).map { it.toDomain() }
        }
    }

    override suspend fun updateBudget(
        id: String,
        baseAmount: Long,
        spentAmount: Long
    ) {
        db.useWriterConnection {
            it.immediateTransaction {
                val existingBudget = db.budgetDao().getByIdWithCategory(id)
                    ?: throw NoSuchElementException("Budget not found")

                val updatedBudget = existingBudget.toDomain().copy(
                    baseAmount = baseAmount,
                    spentAmount = spentAmount,
                    updatedAt = Clock.System.now()
                )
                db.budgetDao().update(updatedBudget.toEntity())
            }
        }
    }

    override suspend fun deleteBudget(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.budgetDao().getByIdWithCategory(id) ?: throw NoSuchElementException("Budget not found")
                db.budgetDao().deleteById(id)
            }
        }
    }
}
