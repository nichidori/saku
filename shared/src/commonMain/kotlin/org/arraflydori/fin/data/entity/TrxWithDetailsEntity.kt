package org.arraflydori.fin.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TrxWithDetailsEntity(
    @Embedded val trx: TrxEntity,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity,

    @Relation(
        parentColumn = "source_account_id",
        entityColumn = "id"
    )
    val sourceAccount: AccountEntity,

    @Relation(
        parentColumn = "target_account_id",
        entityColumn = "id"
    )
    val targetAccount: AccountEntity?
)
