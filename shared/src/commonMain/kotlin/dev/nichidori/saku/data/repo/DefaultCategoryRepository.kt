package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.CategoryRepository
import java.util.UUID
import kotlin.time.Clock

class DefaultCategoryRepository(
    private val db: AppDatabase,
) : CategoryRepository {
    override suspend fun addCategory(name: String, type: TrxType, parent: Category?) {
        db.useWriterConnection {
            it.immediateTransaction {
                val parentId = parent?.id
                if (parentId != null) {
                    val parentEntity = db.categoryDao().getById(parentId)
                        ?: throw NoSuchElementException("Parent category not found")
                    if (parentEntity.parentId != null) {
                        throw IllegalArgumentException("Nested categories beyond one level are not allowed")
                    }
                }

                val category = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    parent = parent,
                    type = type,
                    createdAt = Clock.System.now(),
                    updatedAt = null
                )
                db.categoryDao().insert(category.toEntity())
            }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return db.useReaderConnection {
            val entity = db.categoryDao().getById(id) ?: return@useReaderConnection null
            val parent = entity.parentId?.let {
                db.categoryDao().getById(it)?.toDomain()
            }
            entity.toDomain(parent)
        }
    }

    override suspend fun getAllCategories(): List<Category> {
        return db.useReaderConnection {
            val entities = db.categoryDao().getAll()
            val byId = entities.associateBy { it.id }

            entities.map { entity ->
                val parent = entity.parentId?.let { byId[it]?.toDomain(null) }
                entity.toDomain(parent)
            }
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

    override suspend fun updateCategory(
        id: String,
        name: String,
        type: TrxType,
        parent: Category?
    ) {
        db.useWriterConnection {
            it.immediateTransaction {
                val parentId = parent?.id
                val parentEntity = if (parentId != null) {
                    if (parentId == id) {
                        throw IllegalArgumentException("Category cannot be its own parent")
                    }
                    val entity = db.categoryDao().getById(parentId)
                        ?: throw NoSuchElementException("Parent category not found")
                    if (entity.parentId != null) {
                        throw IllegalArgumentException("Nested categories beyond one level are not allowed")
                    }
                    entity
                } else {
                    null
                }

                val parent = parentEntity?.toDomain(parent = null)
                val category = db.categoryDao().getById(id)?.toDomain(parent = parent)
                    ?: throw NoSuchElementException("Category not found")
                val updatedCategory = category.copy(
                    name = name,
                    type = type,
                    parent = parent,
                    updatedAt = Clock.System.now()
                )
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