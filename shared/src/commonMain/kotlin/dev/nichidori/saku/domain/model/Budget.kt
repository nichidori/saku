package dev.nichidori.saku.domain.model

import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class Budget(
    val id: String,
    val templateId: String,
    val category: Category,
    val month: YearMonth,
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
        val currentMonth = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }

        return when {
            month < currentMonth -> BudgetStatus.Past
            month > currentMonth -> BudgetStatus.Future
            else -> BudgetStatus.Current
        }
    }