package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.MonthlyAccountBalance
import dev.nichidori.saku.domain.repo.AccountRepository
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

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
}