package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteException
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.*
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.datetime.*
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.YearMonth
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultTrxRepository(
    private val db: AppDatabase,
) : TrxRepository {

    private val trxDao = db.trxDao()
    private val accountDao = db.accountDao()
    private val creditDao = db.creditDao()
    private val budgetDao = db.budgetDao()
    private val monthlyAccountBalanceDao = db.monthlyAccountBalanceDao()
    private val monthlyCreditBalanceDao = db.monthlyCreditBalanceDao()

    private suspend fun updateSnapshot(yearMonth: YearMonth) {
        val accounts = accountDao.getAll()
        val snapshots = accounts.map { account ->
            MonthlyAccountBalance(
                yearMonth = yearMonth,
                accountId = account.id,
                balance = account.currentAmount
            ).toEntity()
        }
        monthlyAccountBalanceDao.insertAll(snapshots)

        val credits = creditDao.getAll()
        val creditSnapshots = credits.map { credit ->
            MonthlyCreditBalance(
                yearMonth = yearMonth,
                creditId = credit.id,
                balance = credit.currentAmount
            ).toEntity()
        }
        monthlyCreditBalanceDao.insertAll(creditSnapshots)
    }

    override suspend fun addTrx(
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
    ) {
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
                updatedAt = null
            )

            TrxType.Expense -> Trx.Expense(
                id = newId,
                transactionAt = transactionAt,
                amount = amount,
                description = description,
                sourceAccount = sourceAccount,
                category = category ?: error("Category cannot be null"),
                createdAt = currentTime,
                updatedAt = null
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
                updatedAt = null
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
                        checkCreditLimit(trx.sourceAccount.id, trx.amount)
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            balanceDelta(trx.sourceAccount, TrxType.Expense, "source", trx.amount),
                            currentTime
                        )

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
                }

                val transactionMonth = YearMonth(
                    transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).year,
                    transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).month
                )
                updateSnapshot(transactionMonth)
            }
        }
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
                accountType = filter.accountType?.toEntity(),
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
    ) {
        if (type == TrxType.Transfer && sourceAccount.id == targetAccount?.id) {
            error("Target account cannot be the same as source account")
        }

        db.useWriterConnection {
            it.immediateTransaction {
                val existing = trxDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")

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
                        updatedAt = currentTime
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
                }

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
                }

                trxDao.update(updatedTrx.toEntity())

                val transactionMonth = YearMonth(
                    transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).year,
                    transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).month
                )
                updateSnapshot(transactionMonth)
            }
        }
    }

    override suspend fun deleteTrx(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                val trx = trxDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")

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
                        adjustAccountBalance(
                            trx.sourceAccount.id,
                            -balanceDelta(trx.sourceAccount, TrxType.Expense, "source", trx.amount),
                            currentTime
                        )

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
                }

                val transactionMonth = YearMonth(
                    trx.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).year,
                    trx.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).month
                )
                updateSnapshot(transactionMonth)

                trxDao.deleteById(id)
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
                    else -> amount
                }

                "target" -> -amount
                else -> amount
            }
        } else {
            when (role) {
                "source" -> when (type) {
                    TrxType.Income -> amount
                    else -> -amount
                }

                "target" -> amount
                else -> amount
            }
        }
    }
}
