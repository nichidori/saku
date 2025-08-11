package org.arraflydori.fin.domain.model

import kotlin.time.Instant

data class Category(
    val id: String,
    val name: String,
    val type: TrxType,
    val parent: Category? = null,
    val createdAt: Instant,
    val updatedAt: Instant?
)