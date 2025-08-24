package dev.nichidori.saku.domain.model

import kotlin.time.Instant

sealed class Trx(
    open val id: String,
    open val name: String,
    open val amount: Long,
    open val category: Category,
    open val sourceAccount: Account,
    open val transactionAt: Instant,
    open val note: String?,
    open val createdAt: Instant,
    open val updatedAt: Instant?
) {
    data class Income(
        override val id: String,
        override val name: String,
        override val amount: Long,
        override val category: Category,
        override val sourceAccount: Account,
        override val transactionAt: Instant,
        override val note: String?,
        override val createdAt: Instant,
        override val updatedAt: Instant?
    ) : Trx(id, name, amount, category, sourceAccount, transactionAt, note, createdAt, updatedAt)

    data class Expense(
        override val id: String,
        override val name: String,
        override val amount: Long,
        override val category: Category,
        override val sourceAccount: Account,
        override val transactionAt: Instant,
        override val note: String?,
        override val createdAt: Instant,
        override val updatedAt: Instant?
    ) : Trx(id, name, amount, category, sourceAccount, transactionAt, note, createdAt, updatedAt)

    data class Transfer(
        override val id: String,
        override val name: String,
        override val amount: Long,
        override val category: Category,
        override val sourceAccount: Account,
        val targetAccount: Account,
        override val transactionAt: Instant,
        override val note: String?,
        override val createdAt: Instant,
        override val updatedAt: Instant?
    ) : Trx(id, name, amount, category, sourceAccount, transactionAt, note, createdAt, updatedAt)
}
