package dev.nichidori.saku.domain.repo

import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Credit
import dev.nichidori.saku.domain.model.Installment
import kotlin.time.Instant

interface InstallmentRepository {
    suspend fun createInstallment(
        description: String,
        category: Category,
        credit: Credit,
        principal: Long,
        months: Int,
        monthlyRatePercent: Double,
        purchaseAt: Instant,
    ): String

    suspend fun getInstallmentById(id: String): Installment?
    suspend fun getAllInstallments(): List<Installment>
    suspend fun deleteInstallment(id: String)

    /**
     * Lazily materializes due installment Trxs. For every plan, creates a Trx for each
     * month whose due date has already arrived, starting from the plan's next index.
     */
    suspend fun processDueInstallments()
}