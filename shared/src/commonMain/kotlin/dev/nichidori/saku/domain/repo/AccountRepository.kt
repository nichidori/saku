package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.MonthlyAccountBalance
import kotlinx.datetime.YearMonth

interface AccountRepository {
    suspend fun addAccount(name: String, initialAmount: Long, type: AccountType)
    suspend fun getAccountById(id: String): Account?
    suspend fun getAllAccounts(): List<Account>
    suspend fun updateAccount(id: String, name: String, initialAmount: Long, type: AccountType)
    suspend fun deleteAccount(id: String)
    suspend fun getTotalBalance(): Long
    suspend fun getNetWorthByMonth(yearMonth: YearMonth): Long
    suspend fun getNetWorthHistory(startMonth: YearMonth, endMonth: YearMonth): Map<YearMonth, Long>
    suspend fun getAccountBalancesByMonth(yearMonth: YearMonth): List<MonthlyAccountBalance>
    suspend fun getAccountBalanceHistory(accountId: String, startMonth: YearMonth, endMonth: YearMonth): List<MonthlyAccountBalance>
    suspend fun updateMonthlySnapshots(yearMonth: YearMonth)
    suspend fun ensureMonthlyBalancesExist(now: YearMonth)
}