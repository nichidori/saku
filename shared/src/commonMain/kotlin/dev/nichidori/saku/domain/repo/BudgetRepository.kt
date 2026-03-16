package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.Category

interface BudgetRepository {
    suspend fun addBudget(
        name: String,
        category: Category,
        month: Int,
        year: Int,
        totalAmount: Long,
        spentAmount: Long
    )
    suspend fun getBudgetById(id: String): Budget?
    suspend fun getBudgetsByMonthAndYear(month: Int, year: Int): List<Budget>
    suspend fun getBudgetsByCategory(categoryId: String): List<Budget>
    suspend fun updateBudget(
        id: String,
        name: String,
        category: Category,
        month: Int,
        year: Int,
        totalAmount: Long,
        spentAmount: Long
    )
    suspend fun deleteBudget(id: String)
}
