package dev.nichidori.saku.domain.model

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
    val remainingAmount = baseAmount - spentAmount;
}
