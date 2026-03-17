package dev.nichidori.saku.domain.model

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class Budget(
    val id: String,
    val templateId: String,
    val category: Category,
    val month: Int,
    val year: Int,
    val baseAmount: Long,
    val spentAmount: Long,
    val createdAt: Instant,
    val updatedAt: Instant?
) {
    val remainingAmount = baseAmount - spentAmount
}

enum class BudgetStatus {
    Past, Current, Future;

    val isActive: Boolean get() = this == Current
}

val Budget.status: BudgetStatus
    get() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val currentScore = (now.year * 12) + now.month.number
        val budgetScore = (this.year * 12) + this.month

        return when {
            budgetScore < currentScore -> BudgetStatus.Past
            budgetScore > currentScore -> BudgetStatus.Future
            else -> BudgetStatus.Current
        }
    }