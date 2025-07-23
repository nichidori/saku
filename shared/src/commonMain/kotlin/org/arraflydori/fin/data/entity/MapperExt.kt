package org.arraflydori.fin.data.entity

import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.AccountType
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.Trx
import org.arraflydori.fin.domain.model.TrxType

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    initialAmount = initialAmount,
    currentAmount = currentAmount,
    type = type.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    initialAmount = initialAmount,
    currentAmount = currentAmount,
    type = type.toEntity(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CategoryEntity.toDomain(parent: Category? = null): Category = Category(
    id = id,
    name = name,
    type = type.toDomain(),
    parent = parent,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type.toEntity(),
    parentId = parent?.id,
    createdAt = createdAt,
    updatedAt = updatedAt
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
    category: Category,
    sourceAccount: Account,
    targetAccount: Account? = null
): Trx = when (type) {
    TrxTypeEntity.Income -> Trx.Income(
        id = id,
        name = name,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        transactionAt = transactionAt,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    TrxTypeEntity.Spending -> Trx.Spending(
        id = id,
        name = name,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        transactionAt = transactionAt,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    TrxTypeEntity.Transfer -> Trx.Transfer(
        id = id,
        name = name,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        targetAccount = requireNotNull(targetAccount),
        transactionAt = transactionAt,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Trx.toEntity(): TrxEntity = when (this) {
    is Trx.Income -> TrxEntity(
        id = id,
        name = name,
        amount = amount,
        categoryId = category.id,
        sourceAccountId = sourceAccount.id,
        targetAccountId = null,
        transactionAt = transactionAt,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        type = TrxTypeEntity.Income
    )
    is Trx.Spending -> TrxEntity(
        id = id,
        name = name,
        amount = amount,
        categoryId = category.id,
        sourceAccountId = sourceAccount.id,
        targetAccountId = null,
        transactionAt = transactionAt,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        type = TrxTypeEntity.Spending
    )
    is Trx.Transfer -> TrxEntity(
        id = id,
        name = name,
        amount = amount,
        categoryId = category.id,
        sourceAccountId = sourceAccount.id,
        targetAccountId = targetAccount.id,
        transactionAt = transactionAt,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        type = TrxTypeEntity.Transfer
    )
}

fun TrxTypeEntity.toDomain(): TrxType = when (this) {
    TrxTypeEntity.Income -> TrxType.Income
    TrxTypeEntity.Spending -> TrxType.Spending
    TrxTypeEntity.Transfer -> TrxType.Transfer
}

fun TrxType.toEntity(): TrxTypeEntity = when (this) {
    TrxType.Income -> TrxTypeEntity.Income
    TrxType.Spending -> TrxTypeEntity.Spending
    TrxType.Transfer -> TrxTypeEntity.Transfer
}

fun TrxWithDetailsEntity.toDomain(): Trx {
    return when (trx.type) {
        TrxTypeEntity.Income -> Trx.Income(
            id = trx.id,
            name = trx.name,
            amount = trx.amount,
            sourceAccount = sourceAccount.toDomain(),
            transactionAt = trx.transactionAt,
            category = category.toDomain(),
            note = trx.note,
            createdAt = trx.createdAt,
            updatedAt = trx.updatedAt,
        )

        TrxTypeEntity.Spending -> Trx.Spending(
            id = trx.id,
            name = trx.name,
            amount = trx.amount,
            sourceAccount = sourceAccount.toDomain(),
            transactionAt = trx.transactionAt,
            category = category.toDomain(),
            note = trx.note,
            createdAt = trx.createdAt,
            updatedAt = trx.updatedAt,
        )

        TrxTypeEntity.Transfer -> Trx.Transfer(
            id = trx.id,
            name = trx.name,
            amount = trx.amount,
            sourceAccount = sourceAccount.toDomain(),
            targetAccount = checkNotNull(targetAccount).toDomain(),
            transactionAt = trx.transactionAt,
            category = category.toDomain(),
            note = trx.note,
            createdAt = trx.createdAt,
            updatedAt = trx.updatedAt,
        )
    }
}
