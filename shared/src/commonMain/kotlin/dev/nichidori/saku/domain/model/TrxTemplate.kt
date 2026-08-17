package dev.nichidori.saku.domain.model

import kotlin.time.Instant

data class TrxTemplate(
    val id: String,
    val name: String,
    val type: TrxType,
    val description: String,
    val amount: Long,
    val category: Category?,
    val sourceAccount: TrxAccount,
    val targetAccount: TrxAccount?,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    val hasDeletedAccount: Boolean
        get() = sourceAccount.isDeleted || (targetAccount?.isDeleted == true)
}
