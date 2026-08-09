package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.*
import dev.nichidori.saku.domain.model.Credit
import dev.nichidori.saku.domain.model.TrxAccount
import dev.nichidori.saku.domain.repo.CreditRepository
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultCreditRepository(
    private val db: AppDatabase,
) : CreditRepository {

    override suspend fun addCredit(name: String, limit: Long, initialAmount: Long) {
        val currentTime = Clock.System.now()
        val credit = Credit(
            id = UUID.randomUUID().toString(),
            name = name,
            limit = limit,
            currentAmount = initialAmount,
            createdAt = currentTime,
            updatedAt = null
        )
        db.useWriterConnection {
            it.immediateTransaction {
                db.creditDao().insert(credit.toEntity())
                recalculateNetWorthFrom(credit.createdAt.toYearMonth())
            }
        }
    }

    override suspend fun getCreditById(id: String): Credit? {
        return db.creditDao().getById(id)?.toDomain()
    }

    override suspend fun getAllCredits(): List<Credit> {
        return db.useReaderConnection {
            db.creditDao().getAll().map { it.toDomain() }
        }
    }

    override suspend fun updateCredit(id: String, name: String, limit: Long) {
        val currentTime = Clock.System.now()
        db.useWriterConnection {
            it.immediateTransaction {
                val updated = db.creditDao().getById(id)?.toDomain()
                    ?.copy(
                        name = name,
                        limit = limit,
                        updatedAt = currentTime
                    )
                    ?: throw NoSuchElementException("Credit not found")
                db.creditDao().update(updated.toEntity())
                recalculateNetWorthFrom(updated.createdAt.toYearMonth())
            }
        }
    }

    override suspend fun deleteCredit(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.creditDao().getById(id) ?: throw NoSuchElementException("Credit not found")
                db.creditDao().deleteById(id)
                recalculateNetWorthFrom(Clock.System.now().toYearMonth())
            }
        }
    }

    override suspend fun getTotalBalance(): Long {
        return db.useReaderConnection {
            db.creditDao().getTotalBalance() ?: 0
        }
    }

    override suspend fun getTotalLimit(): Long {
        return db.useReaderConnection {
            db.creditDao().getTotalLimit() ?: 0
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
            .lastDay.plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
        val allTrxs = db.trxDao().getAllUpTo(endTime)

        val accounts = db.accountDao().getAll().map { TrxAccount.Regular(it.toDomain()) }
        val credits = db.creditDao().getAll().map { TrxAccount.Credit(it.toDomain()) }
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
                    initialBalances[account.id] = account.account.initialAmount
                    isCreditByAccount[account.id] = false
                    creationMonth[account.id] = account.account.createdAt.toYearMonth()
                }

                is TrxAccount.Credit -> {
                    initialBalances[account.id] = 0L
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
            if (account is TrxAccount.Credit) {
                val creditTrxs = db.trxDao().getAllByCreditId(account.id)
                var sumDeltas = 0L
                for (trx in creditTrxs) {
                    sumDeltas += accountDelta(account.id, trx, isCredit = true)
                }
                initialBalances[account.id] = account.credit.currentAmount - sumDeltas
            }
        }

        val monthEnds = months.map { month ->
            month.lastDay.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
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
        }
    }

    private fun Instant.toYearMonth(): YearMonth {
        return toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }
    }
}
