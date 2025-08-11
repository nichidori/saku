package org.arraflydori.fin.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.entity.toDomain
import org.arraflydori.fin.data.entity.toEntity
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.repo.CategoryRepository
import java.util.UUID
import kotlin.time.Clock

class DefaultCategoryRepository(
    private val db: AppDatabase,
) : CategoryRepository {
    override suspend fun addCategory(category: Category) {
        db.useWriterConnection {
            it.immediateTransaction {
                val parentId = category.parent?.id
                if (parentId != null) {
                    val parentEntity = db.categoryDao().getById(parentId)
                        ?: throw NoSuchElementException("Parent category not found")
                    if (parentEntity.parentId != null) {
                        throw IllegalArgumentException("Nested categories beyond one level are not allowed")
                    }
                }

                val categoryWithId = category.copy(
                    id = UUID.randomUUID().toString(),
                    createdAt = Clock.System.now()
                )
                db.categoryDao().insert(categoryWithId.toEntity())
            }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return db.categoryDao().getById(id)?.toDomain()
    }

    override suspend fun getAllCategories(): List<Category> {
        return db.useReaderConnection {
            db.categoryDao().getAll().map { it.toDomain() }
        }
    }

    override suspend fun getSubcategories(parentId: String): List<Category> {
        return db.useReaderConnection {
            val parent = db.categoryDao().getById(parentId)
                ?: throw NoSuchElementException("Parent not found")
            db.categoryDao().getSubcategories(parentId).map {
                it.toDomain(parent = parent.toDomain())
            }
        }
    }

    override suspend fun updateCategory(category: Category) {
        db.useWriterConnection {
            it.immediateTransaction {
                val parentId = category.parent?.id
                if (parentId != null) {
                    if (parentId == category.id) {
                        throw IllegalArgumentException("Category cannot be its own parent")
                    }

                    val parentEntity = db.categoryDao().getById(parentId)
                        ?: throw NoSuchElementException("Parent category not found")
                    if (parentEntity.parentId != null) {
                        throw IllegalArgumentException("Nested categories beyond one level are not allowed")
                    }
                }

                val updatedCategory = category.copy(updatedAt = Clock.System.now())
                db.categoryDao().update(updatedCategory.toEntity())
            }
        }
    }

    override suspend fun deleteCategory(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.categoryDao().getById(id) ?: throw NoSuchElementException("Category not found")
                db.categoryDao().deleteById(id)
            }
        }
    }
}