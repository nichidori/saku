package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.TrxEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Credit
import dev.nichidori.saku.domain.model.TrxAccount
import dev.nichidori.saku.domain.repo.AccountRepository
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
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
    }

    override suspend fun getAccountById(id: String): Account? {
        return db.accountDao().getById(id)?.toDomain()
    }

    override suspend fun getAllAccounts(): List<Account> {
        return db.useReaderConnection {
            db.accountDao().getAll().map { it.toDomain() }
        }
    }

    override suspend fun getAllTrxAccounts(): List<TrxAccount> {
        return db.useReaderConnection {
            val accounts = db.accountDao().getAll().map { TrxAccount.Regular(it.toDomain()) }
            val credits = db.creditDao().getAll().map { TrxAccount.Credit(it.toDomain()) }
            accounts + credits
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

    override suspend fun getBalanceHistory(months: List<YearMonth>, timeZone: TimeZone): List<Long> {
        if (months.isEmpty()) return emptyList()

        val endTime = months.last()
            .lastDay.plus(1, DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
        val allTrxs = db.trxDao().getAllUpTo(endTime)

        val accounts = db.accountDao().getAll().map { TrxAccount.Regular(it.toDomain()) }
        val credits = db.creditDao().getAll().map { TrxAccount.Credit(it.toDomain()) }
        val trxAccounts = accounts + credits
        if (trxAccounts.isEmpty()) return months.map { 0L }

        val accountTrxs = mutableMapOf<String, MutableList<TrxEntity>>()
        val initialBalances = mutableMapOf<String, Long>()
        val isCreditByAccount = mutableMapOf<String, Boolean>()

        for (account in trxAccounts) {
            accountTrxs[account.id] = mutableListOf()
            when (account) {
                is TrxAccount.Regular -> {
                    initialBalances[account.id] = account.account.initialAmount
                    isCreditByAccount[account.id] = false
                }

                is TrxAccount.Credit -> {
                    initialBalances[account.id] = 0L
                    isCreditByAccount[account.id] = true
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

        val monthEnds = months.map { month ->
            month.lastDay.plus(1, DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        }

        val netWorthPerMonth = months.map { 0L }.toMutableList()

        for (account in trxAccounts) {
            val trxs = accountTrxs[account.id]!!
            val isCredit = isCreditByAccount[account.id]!!
            var balance = initialBalances[account.id]!!
            var trxIndex = 0

            for (i in months.indices) {
                val end = monthEnds[i]
                while (trxIndex < trxs.size && trxs[trxIndex].transactionAt < end) {
                    val trx = trxs[trxIndex]
                    val isSource = trx.sourceAccountId == account.id || trx.sourceCreditId == account.id
                    val isTarget = trx.targetAccountId == account.id || trx.targetCreditId == account.id

                    balance += when (trx.type) {
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
                    }

                    trxIndex++
                }

                val contribution = if (isCredit) -balance else balance
                netWorthPerMonth[i] = netWorthPerMonth[i] + contribution
            }
        }

        return netWorthPerMonth
    }

    override suspend fun addCredit(name: String, limit: Long, currentAmount: Long) {
        val currentTime = Clock.System.now()
        val credit = Credit(
            id = UUID.randomUUID().toString(),
            name = name,
            limit = limit,
            currentAmount = currentAmount,
            createdAt = currentTime,
            updatedAt = null
        )
        db.useWriterConnection {
            db.creditDao().insert(credit.toEntity())
        }
    }

    override suspend fun getCreditById(id: String): Credit? {
        return db.useReaderConnection {
            db.creditDao().getById(id)?.toDomain()
        }
    }

    override suspend fun updateCredit(id: String, name: String, limit: Long, currentAmount: Long) {
        val currentTime = Clock.System.now()
        db.useWriterConnection {
            it.immediateTransaction {
                val updated = db.creditDao().getById(id)?.toDomain()
                    ?.copy(
                        name = name,
                        limit = limit,
                        currentAmount = currentAmount,
                        updatedAt = currentTime
                    )
                    ?: throw NoSuchElementException("Credit not found")
                db.creditDao().update(updated.toEntity())
            }
        }
    }

    override suspend fun deleteCredit(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.creditDao().getById(id) ?: throw NoSuchElementException("Credit not found")
                db.creditDao().deleteById(id)
            }
        }
    }
}
