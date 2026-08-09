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
    open val updatedAt: Instant?,
    open val installmentId: String? = null,
    open val installmentIndex: Int? = null,
) {
    abstract val type: TrxType

    val isInstallmentCharge: Boolean
        get() = installmentId != null && installmentIndex == null

    val isInstallmentChild: Boolean
        get() = installmentId != null && installmentIndex != null

    data class Income(
        override val id: String,
        override val description: String,
        override val amount: Long,
        override val category: Category?,
        override val sourceAccount: TrxAccount,
        override val transactionAt: Instant,
        override val createdAt: Instant,
        override val updatedAt: Instant?,
        override val installmentId: String? = null,
        override val installmentIndex: Int? = null,
    ) : Trx(id, description, amount, category, sourceAccount, transactionAt, createdAt, updatedAt, installmentId, installmentIndex) {
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
        override val updatedAt: Instant?,
        override val installmentId: String? = null,
        override val installmentIndex: Int? = null,
    ) : Trx(id, description, amount, category, sourceAccount, transactionAt, createdAt, updatedAt, installmentId, installmentIndex) {
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
        override val updatedAt: Instant?,
        override val installmentId: String? = null,
        override val installmentIndex: Int? = null,
    ) : Trx(id, description, amount, category, sourceAccount, transactionAt, createdAt, updatedAt, installmentId, installmentIndex) {
        override val type: TrxType = TrxType.Transfer
    }
}
