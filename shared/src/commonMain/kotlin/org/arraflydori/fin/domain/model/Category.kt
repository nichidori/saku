package org.arraflydori.fin.domain.model

data class Category(
    val id: String,
    val name: String,
    val type: TrxType,
    val parent: Category? = null,
    val createdAt: Long,
    val updatedAt: Long?
)