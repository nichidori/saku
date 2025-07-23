package org.arraflydori.fin.domain.model

sealed class Trx(
    open val id: String,
    open val name: String,
    open val amount: Long,
    open val category: Category,
    open val sourceAccount: Account,
    open val transactionAt: Long,
    open val note: String?,
    open val createdAt: Long,
    open val updatedAt: Long?
) {
    data class Income(
        override val id: String,
        override val name: String,
        override val amount: Long,
        override val category: Category,
        override val sourceAccount: Account,
        override val transactionAt: Long,
        override val note: String?,
        override val createdAt: Long,
        override val updatedAt: Long?
    ) : Trx(id, name, amount, category, sourceAccount, transactionAt, note, createdAt, updatedAt)

    data class Spending(
        override val id: String,
        override val name: String,
        override val amount: Long,
        override val category: Category,
        override val sourceAccount: Account,
        override val transactionAt: Long,
        override val note: String?,
        override val createdAt: Long,
        override val updatedAt: Long?
    ) : Trx(id, name, amount, category, sourceAccount, transactionAt, note, createdAt, updatedAt)

    data class Transfer(
        override val id: String,
        override val name: String,
        override val amount: Long,
        override val category: Category,
        override val sourceAccount: Account,
        val targetAccount: Account,
        override val transactionAt: Long,
        override val note: String?,
        override val createdAt: Long,
        override val updatedAt: Long?
    ) : Trx(id, name, amount, category, sourceAccount, transactionAt, note, createdAt, updatedAt)
}

fun Trx.withId(id: String): Trx = when (this) {
    is Trx.Income -> copy(id = id)
    is Trx.Spending -> copy(id = id)
    is Trx.Transfer -> copy(id = id)
}

fun Trx.withCreatedAt(createdAt: Long): Trx = when (this) {
    is Trx.Income -> copy(createdAt = createdAt)
    is Trx.Spending -> copy(createdAt = createdAt)
    is Trx.Transfer -> copy(createdAt = createdAt)
}

fun Trx.withUpdatedAt(updatedAt: Long): Trx = when (this) {
    is Trx.Income -> copy(updatedAt = updatedAt)
    is Trx.Spending -> copy(updatedAt = updatedAt)
    is Trx.Transfer -> copy(updatedAt = updatedAt)
}
