package dev.nichidori.saku.data.entity

import dev.nichidori.saku.domain.model.*
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
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

fun CreditEntity.toDomain(): Credit = Credit(
    id = id,
    name = name,
    limit = limit,
    currentAmount = currentAmount,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun Credit.toEntity(): CreditEntity = CreditEntity(
    id = id,
    name = name,
    limit = limit,
    currentAmount = currentAmount,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)

fun InstallmentEntity.toDomain(category: Category, credit: Credit): Installment = Installment(
    id = id,
    description = description,
    category = category,
    credit = credit,
    principal = principal,
    months = months,
    monthlyRatePercent = monthlyRatePercent,
    totalAmount = totalAmount,
    monthlyPayment = monthlyPayment,
    lastPayment = lastPayment,
    startAt = Instant.fromEpochMilliseconds(startAt),
    dueDay = dueDay,
    nextIndex = nextIndex,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun Installment.toEntity(): InstallmentEntity = InstallmentEntity(
    id = id,
    description = description,
    categoryId = category.id,
    creditId = credit.id,
    principal = principal,
    months = months,
    monthlyRatePercent = monthlyRatePercent,
    totalAmount = totalAmount,
    monthlyPayment = monthlyPayment,
    lastPayment = lastPayment,
    startAt = startAt.toEpochMilliseconds(),
    dueDay = dueDay,
    nextIndex = nextIndex,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)

fun MonthlyNetWorthEntity.toDomain(): MonthlyNetWorth = MonthlyNetWorth(
    month = YearMonth(year, month),
    netWorth = netWorth,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun MonthlyNetWorth.toEntity(): MonthlyNetWorthEntity = MonthlyNetWorthEntity(
    year = month.year,
    month = month.month.number,
    netWorth = netWorth,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)

fun CategoryEntity.toDomain(parent: Category? = null): Category = Category(
    id = id,
    name = name,
    type = type.toDomain(),
    parent = parent,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) },
    icon = icon
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type.toEntity(),
    parentId = parent?.id,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds(),
    icon = icon
)

fun AccountTypeEntity.toDomain(): AccountType = when (this) {
    AccountTypeEntity.Cash -> AccountType.Cash
    AccountTypeEntity.Bank -> AccountType.Bank
    AccountTypeEntity.Ewallet -> AccountType.Ewallet
    AccountTypeEntity.Emoney -> AccountType.Emoney
}

fun AccountType.toEntity(): AccountTypeEntity = when (this) {
    AccountType.Cash -> AccountTypeEntity.Cash
    AccountType.Bank -> AccountTypeEntity.Bank
    AccountType.Credit -> throw IllegalStateException("Credit type cannot be persisted to account table")
    AccountType.Ewallet -> AccountTypeEntity.Ewallet
    AccountType.Emoney -> AccountTypeEntity.Emoney
}

fun TrxEntity.toDomain(
    category: Category?,
    sourceAccount: TrxAccount,
    targetAccount: TrxAccount? = null,
): Trx = when (type) {
    TrxTypeEntity.Income -> Trx.Income(
        id = id,
        description = description,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        transactionAt = Instant.fromEpochMilliseconds(transactionAt),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) },
        installmentId = installmentId,
        installmentIndex = installmentIndex
    )

    TrxTypeEntity.Expense -> Trx.Expense(
        id = id,
        description = description,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        transactionAt = Instant.fromEpochMilliseconds(transactionAt),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) },
        installmentId = installmentId,
        installmentIndex = installmentIndex
    )

    TrxTypeEntity.Transfer -> Trx.Transfer(
        id = id,
        description = description,
        amount = amount,
        category = category,
        sourceAccount = sourceAccount,
        targetAccount = requireNotNull(targetAccount),
        transactionAt = Instant.fromEpochMilliseconds(transactionAt),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) },
        installmentId = installmentId,
        installmentIndex = installmentIndex
    )
}

fun Trx.toEntity(): TrxEntity = when (this) {
    is Trx.Income -> TrxEntity(
        id = id,
        description = description,
        amount = amount,
        categoryId = category?.id,
        sourceAccountId = (sourceAccount as? TrxAccount.Regular)?.account?.id,
        sourceCreditId = (sourceAccount as? TrxAccount.Credit)?.credit?.id,
        targetAccountId = null,
        targetCreditId = null,
        transactionAt = transactionAt.toEpochMilliseconds(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt?.toEpochMilliseconds(),
        type = TrxTypeEntity.Income,
        installmentId = installmentId,
        installmentIndex = installmentIndex
    )
    is Trx.Expense -> TrxEntity(
        id = id,
        description = description,
        amount = amount,
        categoryId = category?.id,
        sourceAccountId = (sourceAccount as? TrxAccount.Regular)?.account?.id,
        sourceCreditId = (sourceAccount as? TrxAccount.Credit)?.credit?.id,
        targetAccountId = null,
        targetCreditId = null,
        transactionAt = transactionAt.toEpochMilliseconds(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt?.toEpochMilliseconds(),
        type = TrxTypeEntity.Expense,
        installmentId = installmentId,
        installmentIndex = installmentIndex
    )
    is Trx.Transfer -> TrxEntity(
        id = id,
        description = description,
        amount = amount,
        categoryId = null,
        sourceAccountId = (sourceAccount as? TrxAccount.Regular)?.account?.id,
        sourceCreditId = (sourceAccount as? TrxAccount.Credit)?.credit?.id,
        targetAccountId = (targetAccount as? TrxAccount.Regular)?.account?.id,
        targetCreditId = (targetAccount as? TrxAccount.Credit)?.credit?.id,
        transactionAt = transactionAt.toEpochMilliseconds(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt?.toEpochMilliseconds(),
        type = TrxTypeEntity.Transfer,
        installmentId = installmentId,
        installmentIndex = installmentIndex
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
    val source = resolveSourceAccount()
        ?: error("Source account not found for trx ${trx.id}")
    val target = resolveTargetAccount()

    return when (trx.type) {
        TrxTypeEntity.Income -> Trx.Income(
            id = trx.id,
            description = trx.description,
            amount = trx.amount,
            sourceAccount = source,
            transactionAt = Instant.fromEpochMilliseconds(trx.transactionAt),
            category = categoryWithParent?.category?.toDomain(
                parent = categoryWithParent.parent?.toDomain()
            ),
            createdAt = Instant.fromEpochMilliseconds(trx.createdAt),
            updatedAt = trx.updatedAt?.let { Instant.fromEpochMilliseconds(it) },
            installmentId = trx.installmentId,
            installmentIndex = trx.installmentIndex
        )

        TrxTypeEntity.Expense -> Trx.Expense(
            id = trx.id,
            description = trx.description,
            amount = trx.amount,
            sourceAccount = source,
            transactionAt = Instant.fromEpochMilliseconds(trx.transactionAt),
            category = categoryWithParent?.category?.toDomain(
                parent = categoryWithParent.parent?.toDomain()
            ),
            createdAt = Instant.fromEpochMilliseconds(trx.createdAt),
            updatedAt = trx.updatedAt?.let { Instant.fromEpochMilliseconds(it) },
            installmentId = trx.installmentId,
            installmentIndex = trx.installmentIndex
        )

        TrxTypeEntity.Transfer -> Trx.Transfer(
            id = trx.id,
            description = trx.description,
            amount = trx.amount,
            sourceAccount = source,
            targetAccount = checkNotNull(target),
            transactionAt = Instant.fromEpochMilliseconds(trx.transactionAt),
            category = null,
            createdAt = Instant.fromEpochMilliseconds(trx.createdAt),
            updatedAt = trx.updatedAt?.let { Instant.fromEpochMilliseconds(it) },
            installmentId = trx.installmentId,
            installmentIndex = trx.installmentIndex
        )
    }
}

private fun TrxWithDetailsEntity.resolveSourceAccount(): TrxAccount? {
    return sourceAccount?.toDomain()?.let { TrxAccount.Regular(it) }
        ?: sourceCredit?.toDomain()?.let { TrxAccount.Credit(it) }
}

private fun TrxWithDetailsEntity.resolveTargetAccount(): TrxAccount? {
    return targetAccount?.toDomain()?.let { TrxAccount.Regular(it) }
        ?: targetCredit?.toDomain()?.let { TrxAccount.Credit(it) }
}

fun BudgetEntity.toDomain(category: Category): Budget = Budget(
    id = id,
    templateId = templateId,
    category = category,
    month = YearMonth(year, month),
    baseAmount = baseAmount,
    spentAmount = spentAmount,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun BudgetWithCategoryEntity.toDomain(): Budget {
    return budget.toDomain(category.toDomain())
}

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    templateId = templateId,
    categoryId = category.id,
    month = month.month.number,
    year = month.year,
    baseAmount = baseAmount,
    spentAmount = spentAmount,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)

fun TrxTemplateEntity.toDomain(
    category: Category?,
    sourceAccount: TrxAccount,
    targetAccount: TrxAccount?,
): TrxTemplate = TrxTemplate(
    id = id,
    name = name,
    type = type.toDomain(),
    description = description,
    amount = amount,
    category = category,
    sourceAccount = sourceAccount,
    targetAccount = targetAccount,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun TrxTemplateWithDetailsEntity.toDomain(): TrxTemplate {
    val source = resolveSourceAccount()
        ?: error("Source account not found for trx template ${trxTemplate.id}")
    val target = resolveTargetAccount()

    return TrxTemplate(
        id = trxTemplate.id,
        name = trxTemplate.name,
        type = trxTemplate.type.toDomain(),
        description = trxTemplate.description,
        amount = trxTemplate.amount,
        category = categoryWithParent?.category?.toDomain(
            parent = categoryWithParent.parent?.toDomain()
        ),
        sourceAccount = source,
        targetAccount = target,
        createdAt = Instant.fromEpochMilliseconds(trxTemplate.createdAt),
        updatedAt = trxTemplate.updatedAt?.let { Instant.fromEpochMilliseconds(it) }
    )
}

fun TrxTemplate.toEntity(): TrxTemplateEntity = TrxTemplateEntity(
    id = id,
    name = name,
    type = type.toEntity(),
    description = description,
    amount = amount,
    categoryId = category?.id,
    sourceAccountId = (sourceAccount as? TrxAccount.Regular)?.account?.id,
    sourceCreditId = (sourceAccount as? TrxAccount.Credit)?.credit?.id,
    targetAccountId = (targetAccount as? TrxAccount.Regular)?.account?.id,
    targetCreditId = (targetAccount as? TrxAccount.Credit)?.credit?.id,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)

private fun TrxTemplateWithDetailsEntity.resolveSourceAccount(): TrxAccount? {
    return sourceAccount?.toDomain()?.let { TrxAccount.Regular(it) }
        ?: sourceCredit?.toDomain()?.let { TrxAccount.Credit(it) }
}

private fun TrxTemplateWithDetailsEntity.resolveTargetAccount(): TrxAccount? {
    return targetAccount?.toDomain()?.let { TrxAccount.Regular(it) }
        ?: targetCredit?.toDomain()?.let { TrxAccount.Credit(it) }
}

fun BudgetTemplateEntity.toDomain(category: Category): BudgetTemplate = BudgetTemplate(
    id = id,
    category = category,
    defaultAmount = defaultAmount,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
)

fun BudgetTemplateWithCategoryEntity.toDomain(): BudgetTemplate {
    return budgetTemplate.toDomain(category.toDomain())
}

fun BudgetTemplate.toEntity(): BudgetTemplateEntity = BudgetTemplateEntity(
    id = id,
    categoryId = category.id,
    defaultAmount = defaultAmount,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt?.toEpochMilliseconds()
)


