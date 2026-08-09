package dev.nichidori.saku.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TrxWithDetailsEntity(
    @Embedded val trx: TrxEntity,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "id",
        entity = CategoryEntity::class
    )
    val categoryWithParent: CategoryWithParentEntity?,

    @Relation(
        parentColumn = "source_account_id",
        entityColumn = "id"
    )
    val sourceAccount: AccountEntity?,

    @Relation(
        parentColumn = "source_credit_id",
        entityColumn = "id"
    )
    val sourceCredit: CreditEntity?,

    @Relation(
        parentColumn = "target_account_id",
        entityColumn = "id"
    )
    val targetAccount: AccountEntity?,

    @Relation(
        parentColumn = "target_credit_id",
        entityColumn = "id"
    )
    val targetCredit: CreditEntity?,

    @Relation(
        parentColumn = "installment_id",
        entityColumn = "id",
        entity = InstallmentEntity::class
    )
    val installmentPlan: InstallmentEntity? = null,
)
