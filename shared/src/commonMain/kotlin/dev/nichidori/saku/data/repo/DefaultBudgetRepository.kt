package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.repo.BudgetRepository
import java.util.UUID
import kotlin.time.Clock

class DefaultBudgetRepository(
    private val db: AppDatabase,
) : BudgetRepository {
    override suspend fun addBudget(
        name: String,
        category: Category,
        month: Int,
        year: Int,
        totalAmount: Long,
        spentAmount: Long
    ) {
        val budget = Budget(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            month = month,
            year = year,
            totalAmount = totalAmount,
            spentAmount = spentAmount,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.useWriterConnection {
            db.budgetDao().insert(budget.toEntity())
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

    override suspend fun updateBudget(
        id: String,
        name: String,
        category: Category,
        month: Int,
        year: Int,
        totalAmount: Long,
        spentAmount: Long
    ) {
        db.useWriterConnection {
            it.immediateTransaction {
                val updatedBudget = db.budgetDao().getByIdWithCategory(id)?.toDomain()
                    ?.copy(
                        name = name,
                        category = category,
                        month = month,
                        year = year,
                        totalAmount = totalAmount,
                        spentAmount = spentAmount,
                        updatedAt = Clock.System.now()
                    )
                    ?: throw NoSuchElementException("Budget not found")
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
