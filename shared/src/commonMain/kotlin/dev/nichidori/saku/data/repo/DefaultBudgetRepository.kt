package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.BudgetTemplate
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.repo.BudgetRepository
import java.util.*
import kotlin.time.Clock

class DefaultBudgetRepository(
    private val db: AppDatabase,
) : BudgetRepository {

    // Budget Template methods
    override suspend fun addBudgetTemplate(
        category: Category,
        defaultAmount: Long
    ) {
        val template = BudgetTemplate(
            id = UUID.randomUUID().toString(),
            category = category,
            defaultAmount = defaultAmount,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.useWriterConnection {
            db.budgetTemplateDao().insert(template.toEntity())
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

    override suspend fun updateBudgetTemplate(
        id: String,
        category: Category,
        defaultAmount: Long
    ) {
        db.useWriterConnection {
            it.immediateTransaction {
                val existing = db.budgetTemplateDao().getByIdWithCategory(id)?.toDomain()
                    ?: throw NoSuchElementException("Budget template not found")
                val updated = existing.copy(
                    category = category,
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
    override suspend fun addBudget(
        templateId: String,
        category: Category,
        month: Int,
        year: Int,
        baseAmount: Long,
        spentAmount: Long
    ) {
        val budget = Budget(
            id = UUID.randomUUID().toString(),
            category = category,
            month = month,
            year = year,
            baseAmount = baseAmount,
            spentAmount = spentAmount,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.useWriterConnection {
            db.budgetDao().insert(budget.toEntity(templateId))
        }
    }

    override suspend fun getBudgetById(id: String): Budget? {
        return db.useReaderConnection {
            db.budgetDao().getByIdWithCategory(id)?.toDomain()
        }
    }

    override suspend fun getBudgetsByMonthAndYear(month: Int, year: Int): List<Budget> {
        return db.useReaderConnection {
            db.budgetDao().getByMonthAndYearWithCategory(month, year).map { it.toDomain() }
        }
    }

    override suspend fun getBudgetsByCategory(categoryId: String): List<Budget> {
        return db.useReaderConnection {
            db.budgetDao().getByCategoryIdWithCategory(categoryId).map { it.toDomain() }
        }
    }

    override suspend fun updateBudget(
        id: String,
        templateId: String,
        category: Category,
        month: Int,
        year: Int,
        baseAmount: Long,
        spentAmount: Long
    ) {
        db.useWriterConnection {
            it.immediateTransaction {
                val updatedBudget = db.budgetDao().getByIdWithCategory(id)?.toDomain()
                    ?.copy(
                        category = category,
                        month = month,
                        year = year,
                        baseAmount = baseAmount,
                        spentAmount = spentAmount,
                        updatedAt = Clock.System.now()
                    )
                    ?: throw NoSuchElementException("Budget not found")
                db.budgetDao().update(updatedBudget.toEntity(templateId))
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
