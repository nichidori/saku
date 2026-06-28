package dev.nichidori.saku.domain.model

import kotlin.time.Instant

data class Credit(
    val id: String,
    val name: String,
    val limit: Long,
    val currentAmount: Long,
    val createdAt: Instant,
    val updatedAt: Instant?
) {
    val availableCredit = limit - currentAmount
}
