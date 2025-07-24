package org.arraflydori.fin.data.repo

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.entity.toEntity
import org.arraflydori.fin.data.getRoomDatabase
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.TrxType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultCategoryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultCategoryRepository

    private val incomeCategory = Category(
        id = "cat-1",
        name = "Salary",
        type = TrxType.Income,
        parent = null,
        createdAt = 0,
        updatedAt = null
    )

    private val subIncomeCategory = Category(
        id = "cat-2",
        name = "Bonus",
        type = TrxType.Income,
        parent = incomeCategory,
        createdAt = 0,
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        repository = DefaultCategoryRepository(db)
        runBlocking {
            db.categoryDao().insert(incomeCategory.toEntity())
            db.categoryDao().insert(subIncomeCategory.toEntity())
        }
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addCategory_shouldInsertNewCategory() = runTest {
        val category = Category(
            id = "",
            name = "Food",
            type = TrxType.Spending,
            parent = incomeCategory,
            createdAt = 0,
            updatedAt = null
        )
        repository.addCategory(category)
        val all = db.categoryDao().getAll()
        assertEquals(3, all.size)
        assertTrue(all.any { it.name == "Food" && it.parentId == incomeCategory.id })
    }

    @Test
    fun addCategory_shouldThrowIfParentIsNotFound() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Spending,
            createdAt = 0,
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Spending,
            parent = parent,
            createdAt = 0,
            updatedAt = null
        )
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        val exception = assertFailsWith<NoSuchElementException> {
            repository.addCategory(child)
        }
        assertEquals("Parent category not found", exception.message)
    }

    @Test
    fun addCategory_shouldThrowIfParentIsNested() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Spending,
            createdAt = 0,
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Spending,
            parent = parent,
            createdAt = 0,
            updatedAt = null
        )
        val grandChild = Category(
            id = "grand",
            name = "Grandchild",
            type = TrxType.Spending,
            parent = child,
            createdAt = 0,
            updatedAt = null
        )
        db.categoryDao().insert(parent.toEntity())
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.addCategory(grandChild)
        }
        assertEquals("Nested categories beyond one level are not allowed", exception.message)
    }

    @Test
    fun getCategoryById_shouldReturnCategory() = runTest {
        val result = repository.getCategoryById("cat-1")
        assertEquals("Salary", result?.name)
    }

    @Test
    fun getCategoryById_shouldReturnNullIfNotFound() = runTest {
        val result = repository.getCategoryById("unknown")
        assertNull(result)
    }

    @Test
    fun getAllCategories_shouldReturnAll() = runTest {
        val all = repository.getAllCategories()
        assertEquals(2, all.size)
    }

    @Test
    fun getSubcategories_shouldReturnListWithParentSet() = runTest {
        val result = repository.getSubcategories("cat-1")
        assertEquals(1, result.size)
        val sub = result.first()
        assertEquals("Bonus", sub.name)
        assertEquals("cat-1", sub.parent?.id)
        assertEquals("Salary", sub.parent?.name)
    }

    @Test
    fun getSubcategories_shouldReturnEmptyListIfNone() = runTest {
        val category = Category(
            id = "cat-empty",
            name = "Empty Parent",
            type = TrxType.Income,
            parent = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null
        )
        db.categoryDao().insert(category.toEntity())
        val result = repository.getSubcategories("cat-empty")
        assertTrue(result.isEmpty())
    }

    @Test
    fun getSubcategories_shouldThrowIfParentNotFound() = runTest {
        val exception = assertFailsWith<NoSuchElementException> {
            repository.getSubcategories("nonexistent-id")
        }
        assertEquals("Parent not found", exception.message)
    }

    @Test
    fun updateCategory_shouldUpdateData() = runTest {
        val beforeUpdate = db.categoryDao().getById("cat-1")!!
        val updated = incomeCategory.copy(name = "Updated Salary")
        repository.updateCategory(updated)
        val result = db.categoryDao().getById("cat-1")!!
        assertEquals("Updated Salary", result.name)
        assertTrue(result.updatedAt!! > (beforeUpdate.updatedAt ?: 0))
    }

    @Test
    fun updateCategory_shouldThrowIfParentIsNotFound() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Spending,
            createdAt = 0,
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Spending,
            parent = parent,
            createdAt = 0,
            updatedAt = null
        )
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        val exception = assertFailsWith<NoSuchElementException> {
            repository.updateCategory(child.copy(name = "new child"))
        }
        assertEquals("Parent category not found", exception.message)
    }

    @Test
    fun updateCategory_shouldThrowIfParentIsNested() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Spending,
            createdAt = System.currentTimeMillis(),
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Spending,
            parent = parent,
            createdAt = System.currentTimeMillis(),
            updatedAt = null
        )
        val grandchild = Category(
            id = "grandchild",
            name = "Grandchild",
            type = TrxType.Spending,
            parent = child,
            createdAt = System.currentTimeMillis(),
            updatedAt = null
        )
        db.categoryDao().insert(parent.toEntity())
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        db.categoryDao().insert(grandchild.copy(parent = child).toEntity())
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.updateCategory(grandchild)
        }
        assertEquals("Nested categories beyond one level are not allowed", exception.message)
    }

    @Test
    fun updateCategory_shouldThrowIfParentIsSelf() = runTest {
        val selfReferencing = incomeCategory.copy(parent = incomeCategory)
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.updateCategory(selfReferencing)
        }
        assertEquals("Category cannot be its own parent", exception.message)
    }

    @Test
    fun deleteCategory_shouldRemoveRow() = runTest {
        repository.deleteCategory("cat-1")
        val all = db.categoryDao().getAll()
        assertEquals(1, all.size)
        assertEquals("Bonus", all.first().name)
    }

    @Test
    fun deleteCategory_shouldThrowIfNotFound() = runTest {
        val exception = assertFailsWith<NoSuchElementException> {
            repository.deleteCategory("invalid-id")
        }
        assertEquals("Category not found", exception.message)
    }
}
