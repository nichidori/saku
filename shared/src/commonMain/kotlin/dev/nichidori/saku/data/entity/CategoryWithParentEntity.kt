package dev.nichidori.saku.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithParentEntity(
    @Embedded val category: CategoryEntity,

    @Relation(
        parentColumn = "parent_id",
        entityColumn = "id"
    )
    val parent: CategoryEntity?
)
