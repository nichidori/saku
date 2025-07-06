package org.arraflydori.fin.domain.repo

import org.arraflydori.fin.domain.model.Account

interface AccountRepository {
    suspend fun addAccount(account: Account)
    suspend fun getAccountById(id: String): Account?
    suspend fun getAllAccounts(): List<Account>
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: String)
    suspend fun getTotalBalance(): Long
}