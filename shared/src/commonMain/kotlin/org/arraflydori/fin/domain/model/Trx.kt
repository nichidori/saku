package org.arraflydori.fin.domain.model

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

fun Trx.withId(id: String): Trx = when (this) {
    is Trx.Income -> copy(id = id)
    is Trx.Expense -> copy(id = id)
    is Trx.Transfer -> copy(id = id)
}

fun Trx.withCreatedAt(createdAt: Instant): Trx = when (this) {
    is Trx.Income -> copy(createdAt = createdAt)
    is Trx.Expense -> copy(createdAt = createdAt)
    is Trx.Transfer -> copy(createdAt = createdAt)
}

fun Trx.withUpdatedAt(updatedAt: Instant): Trx = when (this) {
    is Trx.Income -> copy(updatedAt = updatedAt)
    is Trx.Expense -> copy(updatedAt = updatedAt)
    is Trx.Transfer -> copy(updatedAt = updatedAt)
}
