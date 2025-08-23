package org.arraflydori.fin.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.entity.toDomain
import org.arraflydori.fin.data.entity.toEntity
import org.arraflydori.fin.domain.model.Trx
import org.arraflydori.fin.domain.model.TrxFilter
import org.arraflydori.fin.domain.model.withCreatedAt
import org.arraflydori.fin.domain.model.withId
import org.arraflydori.fin.domain.model.withUpdatedAt
import org.arraflydori.fin.domain.repo.TrxRepository
import java.util.UUID
import kotlin.time.Clock

class DefaultTrxRepository(
    private val db: AppDatabase,
) : TrxRepository {

    private val trxDao = db.trxDao()
    private val accountDao = db.accountDao()

    override suspend fun addTrx(trx: Trx) {
        val trxWithId = trx
            .withId(UUID.randomUUID().toString())
            .withCreatedAt(Clock.System.now())

        db.useWriterConnection {
            it.immediateTransaction {
                try {
                    trxDao.insert(trxWithId.toEntity())
                } catch (e: SQLiteException) {
                    if (e.message?.contains("FOREIGN KEY constraint failed") == true) {
                        error("Referenced account or category not found")
                    }
                    throw e
                }

                // Re-fetch accounts.
                val source = accountDao.getById(trxWithId.sourceAccount.id)?.toDomain()
                    ?: error("Source account not found")
                val target = when (trxWithId) {
                    is Trx.Transfer -> accountDao.getById(trxWithId.targetAccount.id)?.toDomain()
                    else -> null
                }

                // Update balance.
                val currentTime = Clock.System.now()
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

    override suspend fun updateTrx(trx: Trx) {
        db.useWriterConnection {
            it.immediateTransaction {
                val existing = trxDao.getByIdWithDetails(trx.id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")

                // Re-fetch involved accounts for revert.
                val oldSource = accountDao.getById(existing.sourceAccount.id)?.toDomain()
                    ?: error("Old source account not found")
                val oldTarget = when (existing) {
                    is Trx.Transfer -> accountDao.getById(existing.targetAccount.id)?.toDomain()
                    else -> null
                }

                val currentTime = Clock.System.now()

                // Revert balances.
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

                // Re-fetch involved accounts for apply.
                val newSource = accountDao.getById(trx.sourceAccount.id)?.toDomain()
                    ?: error("New source account not found")
                val newTarget = when (trx) {
                    is Trx.Transfer -> accountDao.getById(trx.targetAccount.id)?.toDomain()
                        ?: error("New target account not found")

                    else -> null
                }

                // Apply new balances.
                when (trx) {
                    is Trx.Income -> {
                        accountDao.update(
                            newSource.copy(
                                currentAmount = newSource.currentAmount + trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }

                    is Trx.Expense -> {
                        accountDao.update(
                            newSource.copy(
                                currentAmount = newSource.currentAmount - trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }

                    is Trx.Transfer -> {
                        accountDao.update(
                            newSource.copy(
                                currentAmount = newSource.currentAmount - trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                        accountDao.update(
                            newTarget!!.copy(
                                currentAmount = newTarget.currentAmount + trx.amount,
                                updatedAt = currentTime
                            ).toEntity()
                        )
                    }
                }

                trxDao.update(trx.withUpdatedAt(currentTime).toEntity())
            }
        }
    }

    override suspend fun deleteTrx(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                val trx = trxDao.getByIdWithDetails(id)?.toDomain()
                    ?: throw NoSuchElementException("Transaction not found")

                // Re-fetch accounts.
                val source = accountDao.getById(trx.sourceAccount.id)?.toDomain()
                    ?: error("Source account not found")
                val target = when (trx) {
                    is Trx.Transfer -> accountDao.getById(trx.targetAccount.id)?.toDomain()
                        ?: error("Target account not found")

                    else -> null
                }

                // Revert balances.
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
