package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxAccount
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.model.TrxType
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
    )

    suspend fun getTrxById(id: String): Trx?
    suspend fun getFilteredTrxs(filter: TrxFilter): List<Trx>
    suspend fun updateTrx(
        id: String,
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        description: String,
        sourceAccount: TrxAccount,
        targetAccount: TrxAccount?,
        category: Category?,
    )

    suspend fun deleteTrx(id: String)
}
