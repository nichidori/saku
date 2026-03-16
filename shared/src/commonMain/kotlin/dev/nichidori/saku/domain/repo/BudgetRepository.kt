package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.BudgetTemplate
import dev.nichidori.saku.domain.model.Category

interface BudgetRepository {
    // Budget Template methods
    suspend fun addBudgetTemplate(
        category: Category,
        defaultAmount: Long
    )
    suspend fun getBudgetTemplateById(id: String): BudgetTemplate?
    suspend fun getBudgetTemplateByCategoryId(categoryId: String): BudgetTemplate?
    suspend fun getAllBudgetTemplates(): List<BudgetTemplate>
    suspend fun updateBudgetTemplate(
        id: String,
        category: Category,
        defaultAmount: Long
    )
    suspend fun deleteBudgetTemplate(id: String)

    // Budget methods
    suspend fun addBudget(
        templateId: String,
        category: Category,
        month: Int,
        year: Int,
        baseAmount: Long,
        spentAmount: Long
    )
    suspend fun getBudgetById(id: String): Budget?
    suspend fun getBudgetsByMonthAndYear(month: Int, year: Int): List<Budget>
    suspend fun getBudgetsByCategory(categoryId: String): List<Budget>
    suspend fun updateBudget(
        id: String,
        templateId: String,
        category: Category,
        month: Int,
        year: Int,
        baseAmount: Long,
        spentAmount: Long
    )
    suspend fun deleteBudget(id: String)
}
