package org.arraflydori.fin.domain.repo

import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.TrxType

interface CategoryRepository {
    suspend fun addCategory(name: String, type: TrxType, parent: Category?)
    suspend fun getCategoryById(id: String): Category?
    suspend fun getAllCategories(): List<Category>
    suspend fun getSubcategories(parentId: String): List<Category>
    suspend fun updateCategory(id: String, name: String, type: TrxType, parent: Category?)
    suspend fun deleteCategory(id: String)
}