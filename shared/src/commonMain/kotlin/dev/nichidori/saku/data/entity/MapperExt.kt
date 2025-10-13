package dev.nichidori.saku.data.entity

import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxType
import kotlin.time.Instant

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    initialAmount = initialAmount,
    currentAmount = currentAmount,
    type = type.toDomain(),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    initialAmount = initialAmount,
    currentAmount = currentAmount,
    type = type.toEntity(),
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)

fun CategoryEntity.toDomain(parent: Category? = null): Category = Category(
    id = id,
    name = name,
    type = type.toDomain(),
    parent = parent,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type.toEntity(),
    parentId = parent?.id,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)

fun AccountTypeEntity.toDomain(): AccountType = when (this) {
    AccountTypeEntity.Cash -> AccountType.Cash
    AccountTypeEntity.Bank -> AccountType.Bank
    AccountTypeEntity.Credit -> AccountType.Credit
    AccountTypeEntity.Ewallet -> AccountType.Ewallet
    AccountTypeEntity.Emoney -> AccountType.Emoney
}

fun AccountType.toEntity(): AccountTypeEntity = when (this) {
    AccountType.Cash -> AccountTypeEntity.Cash
    AccountType.Bank -> AccountTypeEntity.Bank
    AccountType.Credit -> AccountTypeEntity.Credit
    AccountType.Ewallet -> AccountTypeEntity.Ewallet
    AccountType.Emoney -> AccountTypeEntity.Emoney
}

fun TrxEntity.toDomain(
    category: Category?,
    sourceAccount: Account,
    targetAccount: Account? = null,
): Trx = when (type) {
    TrxTypeEntity.Income -> Trx.Income(
        id = id,
        description = description,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        transactionAt = Instant.fromEpochMilliseconds(transactionAt),
        note = note,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
    )

    TrxTypeEntity.Expense -> Trx.Expense(
        id = id,
        description = description,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        transactionAt = Instant.fromEpochMilliseconds(transactionAt),
        note = note,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
    )

    TrxTypeEntity.Transfer -> Trx.Transfer(
        id = id,
        description = description,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        targetAccount = requireNotNull(targetAccount),
        transactionAt = Instant.fromEpochMilliseconds(transactionAt),
        note = note,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
    )
}

fun Trx.toEntity(): TrxEntity = when (this) {
    is Trx.Income -> TrxEntity(
        id = id,
        description = description,
        amount = amount,
        categoryId = category?.id,
        sourceAccountId = sourceAccount.id,
        targetAccountId = null,
        transactionAt = transactionAt.toEpochMilliseconds(),
        note = note,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt?.toEpochMilliseconds(),
        type = TrxTypeEntity.Income
    )
    is Trx.Expense -> TrxEntity(
        id = id,
        description = description,
        amount = amount,
        categoryId = category?.id,
        sourceAccountId = sourceAccount.id,
        targetAccountId = null,
        transactionAt = transactionAt.toEpochMilliseconds(),
        note = note,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt?.toEpochMilliseconds(),
        type = TrxTypeEntity.Expense
    )
    is Trx.Transfer -> TrxEntity(
        id = id,
        description = description,
        amount = amount,
        categoryId = null,
        sourceAccountId = sourceAccount.id,
        targetAccountId = targetAccount.id,
        transactionAt = transactionAt.toEpochMilliseconds(),
        note = note,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt?.toEpochMilliseconds(),
        type = TrxTypeEntity.Transfer
    )
}

fun TrxTypeEntity.toDomain(): TrxType = when (this) {
    TrxTypeEntity.Income -> TrxType.Income
    TrxTypeEntity.Expense -> TrxType.Expense
    TrxTypeEntity.Transfer -> TrxType.Transfer
}

fun TrxType.toEntity(): TrxTypeEntity = when (this) {
    TrxType.Income -> TrxTypeEntity.Income
    TrxType.Expense -> TrxTypeEntity.Expense
    TrxType.Transfer -> TrxTypeEntity.Transfer
}

fun TrxWithDetailsEntity.toDomain(): Trx {
    return when (trx.type) {
        TrxTypeEntity.Income -> Trx.Income(
            id = trx.id,
            description = trx.description,
            amount = trx.amount,
            sourceAccount = sourceAccount.toDomain(),
            transactionAt = Instant.fromEpochMilliseconds(trx.transactionAt),
            category = categoryWithParent?.category?.toDomain(
                parent = categoryWithParent.parent?.toDomain()
            ),
            note = trx.note,
            createdAt = Instant.fromEpochMilliseconds(trx.createdAt),
            updatedAt = trx.updatedAt?.let { Instant.fromEpochMilliseconds(it) }
        )

        TrxTypeEntity.Expense -> Trx.Expense(
            id = trx.id,
            description = trx.description,
            amount = trx.amount,
            sourceAccount = sourceAccount.toDomain(),
            transactionAt = Instant.fromEpochMilliseconds(trx.transactionAt),
            category = categoryWithParent?.category?.toDomain(
                parent = categoryWithParent.parent?.toDomain()
            ),
            note = trx.note,
            createdAt = Instant.fromEpochMilliseconds(trx.createdAt),
            updatedAt = trx.updatedAt?.let { Instant.fromEpochMilliseconds(it) }
        )

        TrxTypeEntity.Transfer -> Trx.Transfer(
            id = trx.id,
            description = trx.description,
            amount = trx.amount,
            sourceAccount = sourceAccount.toDomain(),
            targetAccount = checkNotNull(targetAccount).toDomain(),
            transactionAt = Instant.fromEpochMilliseconds(trx.transactionAt),
            category = null,
            note = trx.note,
            createdAt = Instant.fromEpochMilliseconds(trx.createdAt),
            updatedAt = trx.updatedAt?.let { Instant.fromEpochMilliseconds(it) }
        )
    }
}
