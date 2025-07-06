package org.arraflydori.fin.domain.repo

import org.arraflydori.fin.domain.model.Category

interface CategoryRepository {
    suspend fun addCategory(category: Category)
    suspend fun getCategoryById(id: String): Category?
    suspend fun getAllCategories(): List<Category>
    suspend fun getSubcategories(parentId: String): List<Category>
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: String)
}