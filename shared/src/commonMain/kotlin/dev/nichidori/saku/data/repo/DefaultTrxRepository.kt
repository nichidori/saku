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
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultTrxRepository(
    private val db: AppDatabase,
) : TrxRepository {

    private val trxDao = db.trxDao()
    private val accountDao = db.accountDao()
    private val budgetDao = db.budgetDao()

    override suspend fun addTrx(
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: Account,
        targetAccount: Account?,
        category: Category?,
        note: String
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
                note = note,
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
                note = note,
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
                note = note,
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

                val source = accountDao.getById(sourceAccount.id)?.toDomain()
                    ?: error("Source account not found")
                val target = when (trx) {
                    is Trx.Transfer -> accountDao.getById(trx.targetAccount.id)?.toDomain()
                    else -> null
                }

                when (trx) {
                    is Trx.Income -> {
                        accountDao.update(
                            source.copy(
                                currentAmount = source.currentAmount + trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }

                    is Trx.Expense -> {
                        accountDao.update(
                            source.copy(
                                currentAmount = source.currentAmount - trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
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
                        accountDao.update(
                            source.copy(
                                currentAmount = source.currentAmount - trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                        accountDao.update(
                            target!!.copy(
                                currentAmount = target.currentAmount + trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }
                }
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
                accountId = filter.accountId
            ).map { it.toDomain() }
        }
    }

    override suspend fun updateTrx(
        id: String,
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: Account,
        targetAccount: Account?,
        category: Category?,
        note: String
    ) {
        if (type == TrxType.Transfer && sourceAccount.id == targetAccount?.id) {
            error("Target account cannot be the same as source account")
        }

        db.useWriterConnection {
            it.immediateTransaction {
                val existing = trxDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")

                val oldSource = accountDao.getById(existing.sourceAccount.id)?.toDomain()
                    ?: error("Old source account not found")
                val oldTarget = when (existing) {
                    is Trx.Transfer -> accountDao.getById(existing.targetAccount.id)?.toDomain()
                    else -> null
                }

                val currentTime = Clock.System.now()

                when (existing) {
                    is Trx.Income -> {
                        accountDao.update(
                            oldSource.copy(
                                currentAmount = oldSource.currentAmount - existing.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }

                    is Trx.Expense -> {
                        accountDao.update(
                            oldSource.copy(
                                currentAmount = oldSource.currentAmount + existing.amount,
                                updatedAt = currentTime
                            ).toEntity()
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
                        accountDao.update(
                            oldSource.copy(
                                currentAmount = oldSource.currentAmount + existing.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                        accountDao.update(
                            oldTarget!!.copy(
                                currentAmount = oldTarget.currentAmount - existing.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }
                }

                val newSource = accountDao.getById(sourceAccount.id)?.toDomain()
                    ?: error("New source account not found")
                val newTarget = when (type) {
                    TrxType.Transfer -> accountDao.getById(targetAccount!!.id)?.toDomain()
                        ?: error("New target account not found")

                    else -> null
                }

                val updatedTrx = when (type) {
                    TrxType.Income -> Trx.Income(
                        id = id,
                        transactionAt = transactionAt,
                        amount = amount,
                        description = description,
                        sourceAccount = sourceAccount,
                        category = category ?: error("Category cannot be null"),
                        note = note,
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
                        note = note,
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
                        note = note,
                        createdAt = existing.createdAt,
                        updatedAt = currentTime
                    )
                }

                when (updatedTrx) {
                    is Trx.Income -> {
                        accountDao.update(
                            newSource.copy(
                                currentAmount = newSource.currentAmount + updatedTrx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }

                    is Trx.Expense -> {
                        accountDao.update(
                            newSource.copy(
                                currentAmount = newSource.currentAmount - updatedTrx.amount,
                                updatedAt = currentTime
                            ).toEntity()
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
                        accountDao.update(
                            newSource.copy(
                                currentAmount = newSource.currentAmount - updatedTrx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                        accountDao.update(
                            newTarget!!.copy(
                                currentAmount = newTarget.currentAmount + updatedTrx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }
                }

                trxDao.update(updatedTrx.toEntity())
            }
        }
    }

    override suspend fun deleteTrx(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                val trx = trxDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")

                val source = accountDao.getById(trx.sourceAccount.id)?.toDomain()
                    ?: error("Source account not found")
                val target = when (trx) {
                    is Trx.Transfer -> accountDao.getById(trx.targetAccount.id)?.toDomain()
                        ?: error("Target account not found")

                    else -> null
                }

                val currentTime = Clock.System.now()
                when (trx) {
                    is Trx.Income -> {
                        accountDao.update(
                            source.copy(
                                currentAmount = source.currentAmount - trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }

                    is Trx.Expense -> {
                        accountDao.update(
                            source.copy(
                                currentAmount = source.currentAmount + trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
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
                        accountDao.update(
                            source.copy(
                                currentAmount = source.currentAmount + trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                        accountDao.update(
                            target!!.copy(
                                currentAmount = target.currentAmount - trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }
                }

                trxDao.deleteById(id)
            }
        }
    }
}