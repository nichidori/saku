package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Credit

interface CreditRepository {
    suspend fun addCredit(name: String, limit: Long, initialAmount: Long)
    suspend fun getCreditById(id: String): Credit?
    suspend fun getAllCredits(): List<Credit>
    suspend fun updateCredit(id: String, name: String, limit: Long)
    suspend fun deleteCredit(id: String)
    suspend fun getTotalBalance(): Long
    suspend fun getTotalLimit(): Long
}
