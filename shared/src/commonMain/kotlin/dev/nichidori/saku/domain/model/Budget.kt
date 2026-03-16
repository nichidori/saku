package dev.nichidori.saku.domain.model

import kotlin.time.Instant

data class Budget(
    val id: String,
    val name: String,
    val category: Category,
    val month: Int,
    val year: Int,
    val totalAmount: Long,
    val spentAmount: Long,
    val createdAt: Instant,
    val updatedAt: Instant?
)
