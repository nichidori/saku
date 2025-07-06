package org.arraflydori.fin.domain.repo

import org.arraflydori.fin.domain.model.Trx
import org.arraflydori.fin.domain.model.TrxFilter

interface TrxRepository {
    suspend fun addTrx(trx: Trx)
    suspend fun getTrxById(id: String): Trx?
    suspend fun getFilteredTrxs(filter: TrxFilter): List<Trx>
    suspend fun updateTrx(trx: Trx)
    suspend fun deleteTrx(id: String)
}
