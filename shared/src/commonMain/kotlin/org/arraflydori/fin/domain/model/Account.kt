package org.arraflydori.fin.domain.model

data class Account(
    val id: String,
    val name: String,
    val initialAmount: Long,
    val currentAmount: Long,
    val type: AccountType,
    val createdAt: Long,
    val updatedAt: Long?
)
