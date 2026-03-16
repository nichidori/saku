package dev.nichidori.saku.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BudgetWithCategoryEntity(
    @Embedded val budget: BudgetEntity,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity
)
