package org.arraflydori.fin.domain.repo

import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.Trx
import org.arraflydori.fin.domain.model.TrxFilter
import org.arraflydori.fin.domain.model.TrxType
import kotlin.time.Instant

interface TrxRepository {
    suspend fun addTrx(
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        name: String,
        sourceAccount: Account,
        targetAccount: Account?,
        category: Category,
        note: String
    )

    suspend fun getTrxById(id: String): Trx?
    suspend fun getFilteredTrxs(filter: TrxFilter): List<Trx>
    suspend fun updateTrx(
        id: String,
        type: TrxType,
        transactionAt: Instant,
        amount: Long,
        name: String,
        sourceAccount: Account,
        targetAccount: Account?,
        category: Category,
        note: String
    )

    suspend fun deleteTrx(id: String)
}
