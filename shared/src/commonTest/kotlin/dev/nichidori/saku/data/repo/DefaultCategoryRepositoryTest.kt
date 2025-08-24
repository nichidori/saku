package dev.nichidori.saku.data.repo

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class DefaultCategoryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultCategoryRepository

    private val incomeCategory = Category(
        id = "cat-1",
        name = "Salary",
        type = TrxType.Income,
        parent = null,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val subIncomeCategory = Category(
        id = "cat-2",
        name = "Bonus",
        type = TrxType.Income,
        parent = incomeCategory,
        createdAt = Clock.System.now(),
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
        repository.addCategory(
            name = "Food",
            type = TrxType.Expense,
            parent = incomeCategory
        )
        val all = db.categoryDao().getAll()
        assertEquals(3, all.size)
        assertTrue(all.any { it.name == "Food" && it.parentId == incomeCategory.id })
    }

    @Test
    fun addCategory_shouldThrowIfParentIsNotFound() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Expense,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Expense,
            parent = parent,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        val exception = assertFailsWith<NoSuchElementException> {
            repository.addCategory(
                name = child.name,
                type = child.type,
                parent = child.parent
            )
        }
        assertEquals("Parent category not found", exception.message)
    }

    @Test
    fun addCategory_shouldThrowIfParentIsNested() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Expense,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Expense,
            parent = parent,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val grandChild = Category(
            id = "grand",
            name = "Grandchild",
            type = TrxType.Expense,
            parent = child,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.categoryDao().insert(parent.toEntity())
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.addCategory(
                name = grandChild.name,
                type = grandChild.type,
                parent = grandChild.parent
            )
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
            createdAt = Clock.System.now(),
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
        repository.updateCategory(
            id = incomeCategory.id,
            name = "Updated Salary",
            type = incomeCategory.type,
            parent = incomeCategory.parent
        )
        val result = db.categoryDao().getById("cat-1")!!
        assertEquals("Updated Salary", result.name)
        assertTrue(result.updatedAt!! > (beforeUpdate.updatedAt ?: 0))
    }

    @Test
    fun updateCategory_shouldThrowIfParentIsNotFound() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Expense,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Expense,
            parent = parent,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        val exception = assertFailsWith<NoSuchElementException> {
            repository.updateCategory(
                id = child.id,
                name = "new child",
                type = child.type,
                parent = child.parent
            )
        }
        assertEquals("Parent category not found", exception.message)
    }

    @Test
    fun updateCategory_shouldThrowIfParentIsNested() = runTest {
        val parent = Category(
            id = "parent",
            name = "Parent",
            type = TrxType.Expense,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val child = Category(
            id = "child",
            name = "Child",
            type = TrxType.Expense,
            parent = parent,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val grandchild = Category(
            id = "grandchild",
            name = "Grandchild",
            type = TrxType.Expense,
            parent = child,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.categoryDao().insert(parent.toEntity())
        db.categoryDao().insert(child.copy(parent = parent).toEntity())
        db.categoryDao().insert(grandchild.copy(parent = child).toEntity())
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.updateCategory(
                id = grandchild.id,
                name = grandchild.name,
                type = grandchild.type,
                parent = grandchild.parent
            )
        }
        assertEquals("Nested categories beyond one level are not allowed", exception.message)
    }

    @Test
    fun updateCategory_shouldThrowIfParentIsSelf() = runTest {
        val selfReferencing = incomeCategory.copy(parent = incomeCategory)
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.updateCategory(
                id = selfReferencing.id,
                name = selfReferencing.name,
                type = selfReferencing.type,
                parent = selfReferencing
            )
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
