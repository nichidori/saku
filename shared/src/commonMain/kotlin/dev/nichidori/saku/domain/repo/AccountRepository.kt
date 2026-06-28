package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Credit
import dev.nichidori.saku.domain.model.MonthlyAccountBalance
import dev.nichidori.saku.domain.model.MonthlyCreditBalance
import dev.nichidori.saku.domain.model.TrxAccount
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
    suspend fun getAllTrxAccounts(): List<TrxAccount>
    suspend fun getAccountBalancesByMonth(yearMonth: YearMonth): List<MonthlyAccountBalance>
    suspend fun getAccountBalanceHistory(accountId: String, startMonth: YearMonth, endMonth: YearMonth): List<MonthlyAccountBalance>
    suspend fun addCredit(name: String, limit: Long, currentAmount: Long)
    suspend fun getCreditById(id: String): Credit?
    suspend fun updateCredit(id: String, name: String, limit: Long, currentAmount: Long)
    suspend fun deleteCredit(id: String)
    suspend fun getCreditBalancesByMonth(yearMonth: YearMonth): List<MonthlyCreditBalance>
    suspend fun getCreditBalanceHistory(creditId: String, startMonth: YearMonth, endMonth: YearMonth): List<MonthlyCreditBalance>
    suspend fun updateMonthlySnapshots(yearMonth: YearMonth)
    suspend fun ensureMonthlyBalancesExist(now: YearMonth)
}