package dev.nichidori.saku.domain.model

import kotlin.time.Instant

data class BudgetTemplate(
    val id: String,
    val category: Category,
    val startMonth: Int,
    val startYear: Int,
    val defaultAmount: Long,
    val createdAt: Instant,
    val updatedAt: Instant?
)
