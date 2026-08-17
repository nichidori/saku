package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Credit
import dev.nichidori.saku.domain.model.TrxAccount
import kotlinx.datetime.YearMonth

interface AccountRepository {
    suspend fun addAccount(name: String, currentAmount: Long, type: AccountType)
    suspend fun getAccountById(id: String): Account?
    suspend fun getAllAccounts(): List<Account>
    suspend fun updateAccount(id: String, name: String, type: AccountType)
    suspend fun deleteAccount(id: String)
    suspend fun getTotalBalance(): Long
    suspend fun getAllTrxAccounts(): List<TrxAccount>
    suspend fun getAllTrxAccountsIncludingDeleted(): List<TrxAccount>
    suspend fun getNetWorthHistory(months: List<YearMonth>): List<Long>
    suspend fun ensureCurrentMonthNetWorth()
    suspend fun addCredit(name: String, limit: Long, currentAmount: Long)
    suspend fun getCreditById(id: String): Credit?
    suspend fun updateCredit(id: String, name: String, limit: Long, currentAmount: Long)
    suspend fun deleteCredit(id: String)
    suspend fun getPendingInstallmentCount(creditId: String): Int
}
