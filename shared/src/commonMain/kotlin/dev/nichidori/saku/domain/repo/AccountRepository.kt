package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType

interface AccountRepository {
    suspend fun addAccount(name: String, initialAmount: Long, type: AccountType)
    suspend fun getAccountById(id: String): Account?
    suspend fun getAllAccounts(): List<Account>
    suspend fun updateAccount(id: String, name: String, initialAmount: Long, type: AccountType)
    suspend fun deleteAccount(id: String)
    suspend fun getTotalBalance(): Long
}