package dev.nichidori.saku.domain.model

import kotlin.time.Instant

sealed class Trx(
    open val id: String,
    open val description: String,
    open val amount: Long,
    open val category: Category?,
    open val sourceAccount: TrxAccount,
    open val transactionAt: Instant,
    open val createdAt: Instant,
    open val updatedAt: Instant?
) {
    abstract val type: TrxType

    data class Income(
        override val id: String,
        override val description: String,
        override val amount: Long,
        override val category: Category?,
        override val sourceAccount: TrxAccount,
        override val transactionAt: Instant,
        override val createdAt: Instant,
        override val updatedAt: Instant?
    ) : Trx(id, description, amount, category, sourceAccount, transactionAt, createdAt, updatedAt) {
        override val type: TrxType = TrxType.Income
    }

    data class Expense(
        override val id: String,
        override val description: String,
        override val amount: Long,
        override val category: Category?,
        override val sourceAccount: TrxAccount,
        override val transactionAt: Instant,
        override val createdAt: Instant,
        override val updatedAt: Instant?
    ) : Trx(id, description, amount, category, sourceAccount, transactionAt, createdAt, updatedAt) {
        override val type: TrxType = TrxType.Expense
    }

    data class Transfer(
        override val id: String,
        override val description: String,
        override val amount: Long,
        override val category: Category?,
        override val sourceAccount: TrxAccount,
        val targetAccount: TrxAccount,
        override val transactionAt: Instant,
        override val createdAt: Instant,
        override val updatedAt: Instant?
    ) : Trx(id, description, amount, category, sourceAccount, transactionAt, createdAt, updatedAt) {
        override val type: TrxType = TrxType.Transfer
    }
}
