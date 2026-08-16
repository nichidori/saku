package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteException
import dev.nichidori.saku.core.event.AppEvent
import dev.nichidori.saku.core.event.AppEventBus
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.*
import dev.nichidori.saku.domain.model.*
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.datetime.*
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.TimeZone
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultTrxRepository(
    private val db: AppDatabase,
    private val appEventBus: AppEventBus = AppEventBus(),
) : TrxRepository {

    private val trxDao = db.trxDao()
    private val trxTemplateDao = db.trxTemplateDao()
    private val accountDao = db.accountDao()
    private val creditDao = db.creditDao()
    private val budgetDao = db.budgetDao()

    override suspend fun addTrx(
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
        installment: InstallmentInfo?,
    ): String {
        if (type == TrxType.Transfer && sourceAccount.id == targetAccount?.id) {
            error("Target account cannot be the same as source account")
        }

        val newId = UUID.randomUUID().toString()
        val currentTime = Clock.System.now()

        val trx = when (type) {
            TrxType.Income -> Trx.Income(
                id = newId,
                transactionAt = transactionAt,
                amount = amount,
                description = description,
                sourceAccount = sourceAccount,
                category = category ?: error("Category cannot be null"),
                createdAt = currentTime,
                updatedAt = null,
            )

            TrxType.Expense -> Trx.Expense(
                id = newId,
                transactionAt = transactionAt,
                amount = amount,
                description = description,
                sourceAccount = sourceAccount,
                category = category ?: error("Category cannot be null"),
                createdAt = currentTime,
                updatedAt = null,
                installment = installment,
            )

            TrxType.Transfer -> Trx.Transfer(
                id = newId,
                transactionAt = transactionAt,
                amount = amount,
                description = description,
                sourceAccount = sourceAccount,
                targetAccount = targetAccount ?: error("Target account cannot be null"),
                category = category,
                createdAt = currentTime,
                updatedAt = null,
            )

            TrxType.Adjustment -> Trx.Adjustment(
                id = newId,
                transactionAt = transactionAt,
                amount = amount,
                description = description,
                sourceAccount = sourceAccount,
                createdAt = currentTime,
                updatedAt = null,
            )
        }

        db.useWriterConnection {
            it.immediateTransaction {
                try {
                    trxDao.insert(trx.toEntity())
                } catch (e: SQLiteException) {
                    if (e.message?.contains("FOREIGN KEY constraint failed") == true) {
                        error("Referenced account or category not found")
                    }
                    throw e
                }

                when (trx) {
                    is Trx.Income -> {
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            balanceDelta(trx.sourceAccount, TrxType.Income, "source", trx.amount),
                            currentTime
                        )
                    }

                    is Trx.Expense -> {
                        if (installment !is InstallmentInfo.Installment) {
                            checkCreditLimit(trx.sourceAccount.id, trx.amount)
                            adjustAccountBalance(
                                trx.sourceAccount.id,
                                balanceDelta(trx.sourceAccount, TrxType.Expense, "source", trx.amount),
                                currentTime
                            )
                        }

                        if (installment == null || installment is InstallmentInfo.Installment) {
                            val date = transactionAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            val budgets = budgetDao.getByMonthAndYearWithCategory(
                                month = date.month.number,
                                year = date.year
                            )
                            val budget = budgets.firstOrNull { b -> b.category.id == trx.category?.id }
                            val parentBudget = budgets.firstOrNull { b -> b.category.id == trx.category?.parent?.id }

                            if (budget != null) {
                                budgetDao.update(
                                    budget.budget.copy(
                                        spentAmount = budget.budget.spentAmount + trx.amount,
                                        updatedAt = currentTime.toEpochMilliseconds(),
                                    )
                                )
                            }

                            if (parentBudget != null) {
                                budgetDao.update(
                                    parentBudget.budget.copy(
                                        spentAmount = parentBudget.budget.spentAmount + trx.amount,
                                        updatedAt = currentTime.toEpochMilliseconds(),
                                    )
                                )
                            }
                        }
                    }

                    is Trx.Transfer -> {
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            balanceDelta(trx.sourceAccount, TrxType.Transfer, "source", trx.amount),
                            currentTime
                        )
                        adjustAccountBalance(
                            trx.targetAccount.id,
                            balanceDelta(trx.targetAccount, TrxType.Transfer, "target", trx.amount),
                            currentTime
                        )
                    }

                    is Trx.Adjustment -> {
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            trx.amount,
                            currentTime
                        )
                    }
                }

                recalculateNetWorthFrom(transactionAt.toYearMonth())

            }
        }

        appEventBus.emit(AppEvent.TrxChanged.Created(trx))
        return newId
    }

    override suspend fun getTrxById(id: String): Trx? {
        return trxDao.getByIdWithDetails(id)?.toDomain()
    }

    override suspend fun getFilteredTrxs(filter: TrxFilter): List<Trx> {
        return db.useReaderConnection {
            trxDao.getFilteredWithDetails(
                startTime = filter.month.firstDay
                    .atStartOfDayIn(timeZone = TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
                endTime = filter.month.lastDay
                    .plus(1, DAY)
                    .atStartOfDayIn(timeZone = TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
                type = filter.type?.toEntity(),
                categoryId = filter.categoryId,
                accountId = filter.accountId,
                accountType = if (filter.accountType == AccountType.Credit)
                    null else filter.accountType?.toEntity(),
                isCredit = if (filter.accountType == AccountType.Credit)
                    true else null,
                excludeInstallmentCharges = filter.excludeInstallmentCharges,
            ).map { it.toDomain() }
        }
    }

    override suspend fun updateTrx(
        id: String,
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
        installment: InstallmentInfo?,
    ) {
        if (type == TrxType.Transfer && sourceAccount.id == targetAccount?.id) {
            error("Target account cannot be the same as source account")
        }

        var oldTrx: Trx? = null
        var newTrx: Trx? = null

        db.useWriterConnection {
            it.immediateTransaction {
                val existing = trxDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")
                if ((existing as? Trx.Expense)?.installment != null) {
                    throw UnsupportedOperationException("Installment transactions cannot be edited")
                }
                oldTrx = existing

                val currentTime = Clock.System.now()

                when (existing) {
                    is Trx.Income -> {
                        adjustAccountBalance(
                            existing.sourceAccount.id,
                            -balanceDelta(existing.sourceAccount, TrxType.Income, "source", existing.amount),
                            currentTime
                        )
                    }

                    is Trx.Expense -> {
                        adjustAccountBalance(
                            existing.sourceAccount.id,
                            -balanceDelta(existing.sourceAccount, TrxType.Expense, "source", existing.amount),
                            currentTime
                        )

                        val oldDate = existing.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault())
                        val budgets = budgetDao.getByMonthAndYearWithCategory(
                            month = oldDate.month.number,
                            year = oldDate.year
                        )
                        val oldBudget = budgets.firstOrNull { b -> b.category.id == existing.category?.id }
                        val parentBudget = budgets.firstOrNull { b -> b.category.id == existing.category?.parent?.id }

                        if (oldBudget != null) {
                            budgetDao.update(
                                oldBudget.budget.copy(
                                    spentAmount = oldBudget.budget.spentAmount - existing.amount,
                                    updatedAt = currentTime.toEpochMilliseconds(),
                                )
                            )
                        }

                        if (parentBudget != null) {
                            budgetDao.update(
                                parentBudget.budget.copy(
                                    spentAmount = parentBudget.budget.spentAmount - existing.amount,
                                    updatedAt = currentTime.toEpochMilliseconds(),
                                )
                            )
                        }
                    }

                    is Trx.Transfer -> {
                        adjustAccountBalance(
                            existing.sourceAccount.id,
                            -balanceDelta(existing.sourceAccount, TrxType.Transfer, "source", existing.amount),
                            currentTime
                        )
                        adjustAccountBalance(
                            existing.targetAccount.id,
                            -balanceDelta(existing.targetAccount, TrxType.Transfer, "target", existing.amount),
                            currentTime
                        )
                    }

                    is Trx.Adjustment -> {
                        adjustAccountBalance(
                            existing.sourceAccount.id,
                            -existing.amount,
                            currentTime
                        )
                    }
                }

                val updatedTrx = when (type) {
                    TrxType.Income -> Trx.Income(
                        id = id,
                        transactionAt = transactionAt,
                        amount = amount,
                        description = description,
                        sourceAccount = sourceAccount,
                        category = category ?: error("Category cannot be null"),
                        createdAt = existing.createdAt,
                        updatedAt = currentTime
                    )

                    TrxType.Expense -> Trx.Expense(
                        id = id,
                        transactionAt = transactionAt,
                        amount = amount,
                        description = description,
                        sourceAccount = sourceAccount,
                        category = category ?: error("Category cannot be null"),
                        createdAt = existing.createdAt,
                        updatedAt = currentTime,
                        installment = installment
                    )

                    TrxType.Transfer -> Trx.Transfer(
                        id = id,
                        transactionAt = transactionAt,
                        amount = amount,
                        description = description,
                        sourceAccount = sourceAccount,
                        targetAccount = targetAccount!!,
                        category = category,
                        createdAt = existing.createdAt,
                        updatedAt = currentTime
                    )

                    TrxType.Adjustment -> Trx.Adjustment(
                        id = id,
                        transactionAt = transactionAt,
                        amount = amount,
                        description = description,
                        sourceAccount = sourceAccount,
                        createdAt = existing.createdAt,
                        updatedAt = currentTime
                    )
                }

                newTrx = updatedTrx

                when (updatedTrx) {
                    is Trx.Income -> {
                        adjustAccountBalance(
                            updatedTrx.sourceAccount.id,
                            balanceDelta(updatedTrx.sourceAccount, TrxType.Income, "source", updatedTrx.amount),
                            currentTime
                        )
                    }

                    is Trx.Expense -> {
                        checkCreditLimit(updatedTrx.sourceAccount.id, updatedTrx.amount)
                        adjustAccountBalance(
                            updatedTrx.sourceAccount.id,
                            balanceDelta(updatedTrx.sourceAccount, TrxType.Expense, "source", updatedTrx.amount),
                            currentTime
                        )

                        val newDate = updatedTrx.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault())
                        val budgets = budgetDao.getByMonthAndYearWithCategory(
                            month = newDate.month.number,
                            year = newDate.year
                        )
                        val newBudget = budgets.firstOrNull { b -> b.category.id == updatedTrx.category?.id }
                        val parentBudget = budgets.firstOrNull { b -> b.category.id == updatedTrx.category?.parent?.id }

                        if (newBudget != null) {
                            budgetDao.update(
                                newBudget.budget.copy(
                                    spentAmount = newBudget.budget.spentAmount + updatedTrx.amount,
                                    updatedAt = currentTime.toEpochMilliseconds(),
                                )
                            )
                        }

                        if (parentBudget != null) {
                            budgetDao.update(
                                parentBudget.budget.copy(
                                    spentAmount = parentBudget.budget.spentAmount + updatedTrx.amount,
                                    updatedAt = currentTime.toEpochMilliseconds(),
                                )
                            )
                        }
                    }

                    is Trx.Transfer -> {
                        adjustAccountBalance(
                            updatedTrx.sourceAccount.id,
                            balanceDelta(updatedTrx.sourceAccount, TrxType.Transfer, "source", updatedTrx.amount),
                            currentTime
                        )
                        adjustAccountBalance(
                            updatedTrx.targetAccount.id,
                            balanceDelta(updatedTrx.targetAccount, TrxType.Transfer, "target", updatedTrx.amount),
                            currentTime
                        )
                    }

                    is Trx.Adjustment -> {
                        adjustAccountBalance(
                            updatedTrx.sourceAccount.id,
                            updatedTrx.amount,
                            currentTime
                        )
                    }
                }

                trxDao.update(updatedTrx.toEntity())

                val affectedMonth = minOf(
                    existing.transactionAt.toYearMonth(),
                    updatedTrx.transactionAt.toYearMonth()
                )
                recalculateNetWorthFrom(affectedMonth)
            }
        }

        appEventBus.emit(
            AppEvent.TrxChanged.Updated(
                before = oldTrx ?: error("Transaction not found"),
                after = newTrx ?: error("Transaction not found")
            )
        )
    }

    override suspend fun deleteTrx(id: String) {
        var deletedTrx: Trx? = null

        db.useWriterConnection {
            it.immediateTransaction {
                val trx = trxDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")
                deletedTrx = trx

                val currentTime = Clock.System.now()
                when (trx) {
                    is Trx.Income -> {
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            -balanceDelta(trx.sourceAccount, TrxType.Income, "source", trx.amount),
                            currentTime
                        )
                    }

                    is Trx.Expense -> {
                        if (trx.installment !is InstallmentInfo.Installment) {
                            adjustAccountBalance(
                                trx.sourceAccount.id,
                                -balanceDelta(trx.sourceAccount, TrxType.Expense, "source", trx.amount),
                                currentTime
                            )
                        }

                        if (trx.installment == null || trx.installment is InstallmentInfo.Installment) {
                            val date = trx.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            val budgets = budgetDao.getByMonthAndYearWithCategory(
                                month = date.month.number,
                                year = date.year
                            )
                            val budget = budgets.firstOrNull { b -> b.category.id == trx.category?.id }
                            val parentBudget = budgets.firstOrNull { b -> b.category.id == trx.category?.parent?.id }

                            if (budget != null) {
                                budgetDao.update(
                                    budget.budget.copy(
                                        spentAmount = budget.budget.spentAmount - trx.amount,
                                        updatedAt = currentTime.toEpochMilliseconds(),
                                    )
                                )
                            }

                            if (parentBudget != null) {
                                budgetDao.update(
                                    parentBudget.budget.copy(
                                        spentAmount = parentBudget.budget.spentAmount - trx.amount,
                                        updatedAt = currentTime.toEpochMilliseconds(),
                                    )
                                )
                            }
                        }
                    }

                    is Trx.Transfer -> {
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            -balanceDelta(trx.sourceAccount, TrxType.Transfer, "source", trx.amount),
                            currentTime
                        )
                        adjustAccountBalance(
                            trx.targetAccount.id,
                            -balanceDelta(trx.targetAccount, TrxType.Transfer, "target", trx.amount),
                            currentTime
                        )
                    }

                    is Trx.Adjustment -> {
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            -trx.amount,
                            currentTime
                        )
                    }
                }

                trxDao.deleteById(id)
                recalculateNetWorthFrom(trx.transactionAt.toYearMonth())
            }
        }

        deletedTrx?.let {
            appEventBus.emit(AppEvent.TrxChanged.Deleted(it))
        }
    }

    override suspend fun addTrxTemplate(
        name: String,
        type: TrxType,
        description: String,
        amount: Long,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
    ) {
        if (type == TrxType.Transfer && sourceAccount.id == targetAccount?.id) {
            error("Target account cannot be the same as source account")
        }

        val template = TrxTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            description = description,
            amount = amount,
            category = category,
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        db.useWriterConnection {
            it.immediateTransaction {
                try {
                    trxTemplateDao.insert(template.toEntity())
                } catch (e: SQLiteException) {
                    if (e.message?.contains("FOREIGN KEY constraint failed") == true) {
                        error("Referenced account or category not found")
                    }
                    throw e
                }
            }
        }
    }

    override suspend fun getTrxTemplateById(id: String): TrxTemplate? {
        return trxTemplateDao.getByIdWithDetails(id)?.toDomain()
    }

    override suspend fun getAllTrxTemplates(): List<TrxTemplate> {
        return db.useReaderConnection {
            trxTemplateDao.getAllWithDetails().map { it.toDomain() }
        }
    }

    override suspend fun updateTrxTemplate(
        id: String,
        name: String,
        type: TrxType,
        description: String,
        amount: Long,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
    ) {
        if (type == TrxType.Transfer && sourceAccount.id == targetAccount?.id) {
            error("Target account cannot be the same as source account")
        }

        db.useWriterConnection {
            it.immediateTransaction {
                val existing = trxTemplateDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction template not found")
                val updated = existing.copy(
                    name = name,
                    type = type,
                    description = description,
                    amount = amount,
                    sourceAccount = sourceAccount,
                    targetAccount = targetAccount,
                    category = category,
                    updatedAt = Clock.System.now()
                )
                trxTemplateDao.update(updated.toEntity())
            }
        }
    }

    override suspend fun deleteTrxTemplate(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                trxTemplateDao.getByIdWithDetails(id)
                    ?: throw NoSuchElementException("Transaction template not found")
                trxTemplateDao.deleteById(id)
            }
        }
    }

    private suspend fun adjustAccountBalance(accountId: String, delta: Long, updatedAt: Instant) {
        val account = accountDao.getById(accountId)
        if (account != null) {
            accountDao.update(
                account.copy(
                    currentAmount = account.currentAmount + delta,
                    updatedAt = updatedAt.toEpochMilliseconds()
                )
            )
            return
        }
        val credit = creditDao.getById(accountId)
        if (credit != null) {
            creditDao.update(
                credit.copy(
                    currentAmount = credit.currentAmount + delta,
                    updatedAt = updatedAt.toEpochMilliseconds()
                )
            )
            return
        }
        error("Account not found: $accountId")
    }

    private suspend fun checkCreditLimit(sourceId: String, expenseAmount: Long) {
        val credit = creditDao.getById(sourceId)?.toDomain()
        if (credit != null && credit.limit > 0) {
            val newBalance = credit.currentAmount + expenseAmount
            if (newBalance > credit.limit) {
                error("Transaction exceeds credit limit")
            }
        }
    }

    private fun balanceDelta(trxAccount: TrxAccount, type: TrxType, role: String, amount: Long): Long {
        return if (trxAccount is TrxAccount.Credit) {
            when (role) {
                "source" -> when (type) {
                    TrxType.Income -> -amount
                    TrxType.Adjustment -> amount
                    else -> amount
                }

                "target" -> -amount
                else -> amount
            }
        } else {
            when (role) {
                "source" -> when (type) {
                    TrxType.Income -> amount
                    TrxType.Adjustment -> amount
                    else -> -amount
                }

                "target" -> amount
                else -> amount
            }
        }
    }

    private suspend fun recalculateNetWorthFrom(startMonth: YearMonth) {
        val timeZone = TimeZone.currentSystemDefault()
        val currentMonth = Clock.System.now().toYearMonth()
        val months = generateSequence(startMonth) { it.plus(1, DateTimeUnit.MONTH) }
            .takeWhile { it <= currentMonth }
            .toList()
        if (months.isEmpty()) return

        val endTime = currentMonth
            .lastDay.plus(1, DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
        val allTrxs = db.trxDao().getAllUpTo(endTime)

        val accounts = accountDao.getAll().map { TrxAccount.Regular(it.toDomain()) }
        val credits = creditDao.getAll().map { TrxAccount.Credit(it.toDomain()) }
        val trxAccounts = accounts + credits
        if (trxAccounts.isEmpty()) return

        val accountTrxs = mutableMapOf<String, MutableList<TrxEntity>>()
        val initialBalances = mutableMapOf<String, Long>()
        val isCreditByAccount = mutableMapOf<String, Boolean>()
        val creationMonth = mutableMapOf<String, YearMonth>()

        for (account in trxAccounts) {
            accountTrxs[account.id] = mutableListOf()
            when (account) {
                is TrxAccount.Regular -> {
                    isCreditByAccount[account.id] = false
                    creationMonth[account.id] = account.account.createdAt.toYearMonth()
                }

                is TrxAccount.Credit -> {
                    isCreditByAccount[account.id] = true
                    creationMonth[account.id] = account.credit.createdAt.toYearMonth()
                }
            }
        }

        for (trx in allTrxs) {
            val sourceId = trx.sourceAccountId ?: trx.sourceCreditId
            if (sourceId in accountTrxs) accountTrxs[sourceId]!!.add(trx)
            val targetId = trx.targetAccountId ?: trx.targetCreditId
            if (targetId in accountTrxs) accountTrxs[targetId]!!.add(trx)
        }

        for (trxs in accountTrxs.values) {
            trxs.sortBy { it.transactionAt }
        }

        for (account in trxAccounts) {
            when (account) {
                is TrxAccount.Regular -> {
                    val trxs = accountTrxs[account.id]!!
                    var sumDeltas = 0L
                    for (trx in trxs) {
                        sumDeltas += accountDelta(account.id, trx, isCredit = false)
                    }
                    initialBalances[account.id] = account.account.currentAmount - sumDeltas
                }

                is TrxAccount.Credit -> {
                    val creditTrxs = db.trxDao().getAllByCreditId(account.id)
                    var sumDeltas = 0L
                    for (trx in creditTrxs) {
                        sumDeltas += accountDelta(account.id, trx, isCredit = true)
                    }
                    initialBalances[account.id] = account.credit.currentAmount - sumDeltas
                }
            }
        }

        val monthEnds = months.map { month ->
            month.lastDay.plus(1, DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        }

        val netWorthPerMonth = months.map { 0L }.toMutableList()

        for (account in trxAccounts) {
            val trxs = accountTrxs[account.id]!!
            val isCredit = isCreditByAccount[account.id]!!
            val creation = creationMonth[account.id]!!
            if (months.none { it >= creation }) continue

            var balance = initialBalances[account.id]!!
            var trxIndex = 0

            for (i in months.indices) {
                if (months[i] < creation) continue

                val end = monthEnds[i]
                while (trxIndex < trxs.size && trxs[trxIndex].transactionAt < end) {
                    balance += accountDelta(account.id, trxs[trxIndex], isCredit)
                    trxIndex++
                }

                val contribution = if (isCredit) -balance else balance
                netWorthPerMonth[i] = netWorthPerMonth[i] + contribution
            }
        }

        val currentTime = Clock.System.now()
        for (i in months.indices) {
            val existing = db.monthlyNetWorthDao().getByYearMonth(
                year = months[i].year,
                month = months[i].month.number
            )
            db.monthlyNetWorthDao().upsert(
                MonthlyNetWorthEntity(
                    year = months[i].year,
                    month = months[i].month.number,
                    netWorth = netWorthPerMonth[i],
                    createdAt = existing?.createdAt ?: currentTime.toEpochMilliseconds(),
                    updatedAt = currentTime.toEpochMilliseconds()
                )
            )
        }
    }

    private fun accountDelta(accountId: String, trx: TrxEntity, isCredit: Boolean): Long {
        if (trx.installmentId != null && trx.installmentIndex != null) return 0L

        val isSource = trx.sourceAccountId == accountId || trx.sourceCreditId == accountId
        val isTarget = trx.targetAccountId == accountId || trx.targetCreditId == accountId

        return when (trx.type) {
            TrxTypeEntity.Income -> {
                if (isSource) if (isCredit) -trx.amount else trx.amount else 0L
            }

            TrxTypeEntity.Expense -> {
                if (isSource) if (isCredit) trx.amount else -trx.amount else 0L
            }

            TrxTypeEntity.Transfer -> {
                when {
                    isSource && isTarget -> 0L
                    isSource -> if (isCredit) trx.amount else -trx.amount
                    isTarget -> if (isCredit) -trx.amount else trx.amount
                    else -> 0L
                }
            }

            TrxTypeEntity.Adjustment -> {
                if (isSource) trx.amount else 0L
            }
        }
    }

    private fun Instant.toYearMonth(): YearMonth {
        return toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }
    }
}
