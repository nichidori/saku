package dev.nichidori.saku.domain.model

import kotlin.time.Instant

data class Account(
    val id: String,
    val name: String,
    val currentAmount: Long,
    val type: AccountType,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val deletedAt: Instant? = null,
) {
    val isDeleted: Boolean get() = deletedAt != null
}
