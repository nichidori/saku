package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.MonthlyAccountBalance
import dev.nichidori.saku.domain.repo.AccountRepository
import kotlinx.datetime.*
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.DateTimeUnit.Companion.MONTH
import kotlinx.datetime.TimeZone
import java.util.*
import kotlin.time.Clock

class DefaultAccountRepository(
    private val db: AppDatabase,
) : AccountRepository {
    override suspend fun addAccount(name: String, initialAmount: Long, type: AccountType) {
        val currentTime = Clock.System.now()
        val account = Account(
            id = UUID.randomUUID().toString(),
            name = name,
            initialAmount = initialAmount,
            currentAmount = initialAmount,
            type = type,
            createdAt = currentTime,
            updatedAt = null
        )
        db.useWriterConnection {
            db.accountDao().insert(account.toEntity())
        }
        val currentMonth = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }
        updateMonthlySnapshots(currentMonth)
    }

    override suspend fun getAccountById(id: String): Account? {
        return db.accountDao().getById(id)?.toDomain()
    }

    override suspend fun getAllAccounts(): List<Account> {
        return db.useReaderConnection {
            db.accountDao().getAll().map { it.toDomain() }
        }
    }

    override suspend fun updateAccount(
        id: String, name: String, initialAmount: Long, type: AccountType
    ) {
        val currentTime = Clock.System.now()
        db.useWriterConnection {
            it.immediateTransaction {
                val updatedAccount = db.accountDao().getById(id)?.toDomain()
                    ?.copy(
                        name = name,
                        initialAmount = initialAmount,
                        currentAmount = initialAmount,
                        type = type,
                        updatedAt = currentTime
                    )
                    ?: throw NoSuchElementException("Account not found")
                db.accountDao().update(updatedAccount.toEntity())
            }
        }
        val currentMonth = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }
        updateMonthlySnapshots(currentMonth)
    }

    override suspend fun deleteAccount(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.accountDao().getById(id) ?: throw NoSuchElementException("Account not found")
                db.accountDao().deleteById(id)
            }
        }
    }

    override suspend fun getTotalBalance(): Long {
        return db.useReaderConnection {
            db.accountDao().getTotalBalance() ?: 0
        }
    }

    override suspend fun getNetWorthByMonth(yearMonth: YearMonth): Long {
        return db.useReaderConnection {
            db.monthlyAccountBalanceDao().getNetWorthByYearMonth(yearMonth.year, yearMonth.month.number) ?: 0L
        }
    }

    override suspend fun getNetWorthHistory(
        startMonth: YearMonth,
        endMonth: YearMonth
    ): Map<YearMonth, Long> {
        return db.useReaderConnection {
            val balances = db.monthlyAccountBalanceDao().getByYearMonthRange(
                startMonth.year,
                startMonth.month.number,
                endMonth.year,
                endMonth.month.number
            )
            balances.groupBy(
                keySelector = { YearMonth(it.year, it.month) },
                valueTransform = { it.balance }
            ).mapValues { (_, balances) -> balances.sum() }
        }
    }

    override suspend fun getAccountBalancesByMonth(yearMonth: YearMonth): List<MonthlyAccountBalance> {
        return db.useReaderConnection {
            db.monthlyAccountBalanceDao().getByYearMonth(yearMonth.year, yearMonth.month.number)
                .map { it.toDomain() }
        }
    }

    override suspend fun getAccountBalanceHistory(
        accountId: String,
        startMonth: YearMonth,
        endMonth: YearMonth
    ): List<MonthlyAccountBalance> {
        return db.useReaderConnection {
            db.monthlyAccountBalanceDao().getByAccountIdAndYearMonthRange(
                accountId,
                startMonth.year,
                startMonth.month.number,
                endMonth.year,
                endMonth.month.number
            ).map { it.toDomain() }
        }
    }

    override suspend fun updateMonthlySnapshots(yearMonth: YearMonth) {
        val accounts = db.useReaderConnection { db.accountDao().getAll() }
        val snapshots = accounts.map { account ->
            MonthlyAccountBalance(
                yearMonth = yearMonth,
                accountId = account.id,
                balance = account.currentAmount
            )
        }
        db.useWriterConnection {
            db.monthlyAccountBalanceDao().insertAll(snapshots.map { it.toEntity() })
        }
    }

    override suspend fun ensureMonthlyBalancesExist(now: YearMonth) {
        val timeZone = TimeZone.currentSystemDefault()

        db.useWriterConnection {
            it.immediateTransaction {
                val accounts = db.accountDao().getAll()

                for (account in accounts) {
                    val accountDomain = account.toDomain()
                    val accountCreatedMonth = accountDomain.createdAt
                        .toLocalDateTime(timeZone)
                        .let { month -> YearMonth(month.year, month.month) }

                    val earliestTrx = db.trxDao().getEarliestByAccountId(account.id)?.toDomain()
                    val earliestTrxMonth = earliestTrx?.transactionAt
                        ?.toLocalDateTime(timeZone)
                        ?.let { month -> YearMonth(month.year, month.month) }

                    val startMonth = if (earliestTrxMonth != null && earliestTrxMonth < accountCreatedMonth) {
                        earliestTrxMonth
                    } else {
                        accountCreatedMonth
                    }

                    val existingBalances = db.monthlyAccountBalanceDao()
                        .getByAccountIdAndYearMonthRange(
                            accountId = account.id,
                            startYear = startMonth.year,
                            startMonth = startMonth.month.number,
                            endYear = now.year,
                            endMonth = now.month.number
                        )
                        .map { month -> YearMonth(month.year, month.month) }
                        .toHashSet()

                    for (month in startMonth..<now.plus(1, MONTH)) {
                        if (month !in existingBalances || month == now) {
                            val balance = calculateHistoricalBalance(
                                accountId = account.id,
                                startAmount = accountDomain.initialAmount,
                                month = month,
                                timeZone = timeZone
                            )
                            db.monthlyAccountBalanceDao().insert(
                                MonthlyAccountBalance(
                                    yearMonth = month,
                                    accountId = account.id,
                                    balance = balance
                                ).toEntity()
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun calculateHistoricalBalance(
        accountId: String,
        startAmount: Long,
        month: YearMonth,
        timeZone: TimeZone
    ): Long {
        val endTime = month.lastDay
            .plus(1, DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

        val trxs = db.trxDao().getFilteredWithDetails(
            startTime = 0,
            endTime = endTime,
            accountId = accountId
        )

        var balance = startAmount
        for (trxWithDetails in trxs) {
            val trx = trxWithDetails.trx
            when (trx.type) {
                TrxTypeEntity.Income -> {
                    if (trx.sourceAccountId == accountId) balance += trx.amount
                }

                TrxTypeEntity.Expense -> {
                    if (trx.sourceAccountId == accountId) balance -= trx.amount
                }

                TrxTypeEntity.Transfer -> {
                    if (trx.sourceAccountId == accountId) balance -= trx.amount
                    if (trx.targetAccountId == accountId) balance += trx.amount
                }
            }
        }
        return balance
    }
}