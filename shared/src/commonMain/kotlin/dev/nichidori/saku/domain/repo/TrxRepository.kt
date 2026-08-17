package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.*
import kotlin.time.Instant

interface TrxRepository {
    suspend fun addTrx(
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
        installment: InstallmentInfo? = null,
    ): String

    suspend fun getTrxById(id: String): Trx?
    suspend fun getFilteredTrxs(filter: TrxFilter): List<Trx>
    suspend fun searchTrxsByDescription(keyword: String): List<Trx>
    suspend fun updateTrx(
        id: String,
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
        installment: InstallmentInfo? = null,
    )

    suspend fun deleteTrx(id: String)
    suspend fun addTrxTemplate(
        name: String,
        type: TrxType,
        description: String,
        amount: Long,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
    )

    suspend fun getTrxTemplateById(id: String): TrxTemplate?
    suspend fun getAllTrxTemplates(): List<TrxTemplate>
    suspend fun updateTrxTemplate(
        id: String,
        name: String,
        type: TrxType,
        description: String,
        amount: Long,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
    )

    suspend fun deleteTrxTemplate(id: String)
}
