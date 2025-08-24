package dev.nichidori.saku.domain.model

import kotlin.time.Instant

data class Account(
    val id: String,
    val name: String,
    val initialAmount: Long,
    val currentAmount: Long,
    val type: AccountType,
    val createdAt: Instant,
    val updatedAt: Instant?
)
