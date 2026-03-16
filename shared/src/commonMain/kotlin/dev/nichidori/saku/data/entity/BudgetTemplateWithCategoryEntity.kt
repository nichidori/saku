package dev.nichidori.saku.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BudgetTemplateWithCategoryEntity(
    @Embedded val budgetTemplate: BudgetTemplateEntity,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity
)
