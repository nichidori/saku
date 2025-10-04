package dev.nichidori.saku.data.dao

import androidx.room.Room
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val parentCategory = CategoryEntity(
    id = "cat-parent",
    name = "Food & Dining",
    type = TrxTypeEntity.Expense,
    parentId = null,
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

private val subcategoryRestaurant = CategoryEntity(
    id = "cat-restaurant",
    name = "Restaurant",
    type = TrxTypeEntity.Expense,
    parentId = "cat-parent",
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

private val subcategoryGrocery = CategoryEntity(
    id = "cat-grocery",
    name = "Grocery",
    type = TrxTypeEntity.Expense,
    parentId = "cat-parent",
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

private val independentCategory = CategoryEntity(
    id = "cat-transport",
    name = "Transportation",
    type = TrxTypeEntity.Expense,
    parentId = null,
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        categoryDao = db.categoryDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetById_shouldReturnMatchingCategory() = runTest {
        categoryDao.insert(parentCategory)

        val result = categoryDao.getById(parentCategory.id)

        assertNotNull(result)
        assertEquals(parentCategory.id, result.id)
        assertEquals(parentCategory.name, result.name)
        assertEquals(parentCategory.parentId, result.parentId)
        assertEquals(parentCategory.createdAt, result.createdAt)
        assertEquals(parentCategory.updatedAt, result.updatedAt)
    }

    @Test
    fun getById_withNonExistentId_shouldReturnNull() = runTest {
        val result = categoryDao.getById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun insert_withOnConflictReplace_shouldReplaceExistingCategory() = runTest {
        categoryDao.insert(parentCategory)

        val updatedCategory = parentCategory.copy(
            name = "Updated Food & Dining",
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insert(updatedCategory)

        val result = categoryDao.getById(parentCategory.id)
        assertNotNull(result)
        assertEquals("Updated Food & Dining", result.name)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun update_shouldModifyExistingCategory() = runTest {
        categoryDao.insert(parentCategory)

        val updatedCategory = parentCategory.copy(
            name = "Modified Food",
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.update(updatedCategory)

        val result = categoryDao.getById(parentCategory.id)
        assertNotNull(result)
        assertEquals("Modified Food", result.name)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun deleteById_shouldRemoveCategory() = runTest {
        categoryDao.insert(parentCategory)

        categoryDao.deleteById(parentCategory.id)

        val result = categoryDao.getById(parentCategory.id)
        assertNull(result)
    }

    @Test
    fun deleteById_withNonExistentId_shouldNotThrowException() = runTest {
        categoryDao.deleteById("non-existent-id")
    }

    @Test
    fun getAll_withMultipleCategories_shouldReturnAllOrderedByName() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(subcategoryRestaurant)
        categoryDao.insert(independentCategory)

        val results = categoryDao.getAll()

        assertEquals(3, results.size)
        assertEquals("Food & Dining", results[0].name)
        assertEquals("Restaurant", results[1].name)
        assertEquals("Transportation", results[2].name)
    }

    @Test
    fun getAll_withEmptyDatabase_shouldReturnEmptyList() = runTest {
        val results = categoryDao.getAll()
        assertTrue(results.isEmpty())
    }

    @Test
    fun getAll_withSingleCategory_shouldReturnSingleItemList() = runTest {
        categoryDao.insert(parentCategory)

        val results = categoryDao.getAll()

        assertEquals(1, results.size)
        assertEquals(parentCategory.id, results[0].id)
    }

    @Test
    fun getRootCategories_shouldReturnOnlyCategoriesWithNullParentIdOrderedByName() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(subcategoryRestaurant)
        categoryDao.insert(independentCategory)
        categoryDao.insert(subcategoryGrocery)

        val roots = categoryDao.getRootCategories()

        assertEquals(2, roots.size)
        assertTrue(roots.all { it.parentId == null })

        assertEquals("Food & Dining", roots[0].name)
        assertEquals("Transportation", roots[1].name)
    }

    @Test
    fun getSubcategories_withValidParentId_shouldReturnOrderedSubcategories() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(subcategoryRestaurant)
        categoryDao.insert(subcategoryGrocery)
        categoryDao.insert(independentCategory)

        val subcategories = categoryDao.getSubcategories(parentCategory.id)

        assertEquals(2, subcategories.size)
        assertEquals("Grocery", subcategories[0].name)
        assertEquals("Restaurant", subcategories[1].name)
        assertTrue(subcategories.all { it.parentId == parentCategory.id })
    }

    @Test
    fun getSubcategories_withNonExistentParentId_shouldReturnEmptyList() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(subcategoryRestaurant)

        val subcategories = categoryDao.getSubcategories("non-existent-parent")

        assertTrue(subcategories.isEmpty())
    }

    @Test
    fun getSubcategories_withParentWithoutChildren_shouldReturnEmptyList() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(independentCategory)

        val subcategories = categoryDao.getSubcategories(independentCategory.id)

        assertTrue(subcategories.isEmpty())
    }

    @Test
    fun insert_withParentAndChildCategories_shouldAllBeRetrievable() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(subcategoryRestaurant)
        categoryDao.insert(subcategoryGrocery)

        val parentResult = categoryDao.getById(parentCategory.id)
        val restaurantResult = categoryDao.getById(subcategoryRestaurant.id)
        val groceryResult = categoryDao.getById(subcategoryGrocery.id)

        assertNotNull(parentResult)
        assertNotNull(restaurantResult)
        assertNotNull(groceryResult)

        assertNull(parentResult.parentId)
        assertEquals(parentCategory.id, restaurantResult.parentId)
        assertEquals(parentCategory.id, groceryResult.parentId)
    }

    @Test
    fun getAll_withSameFirstLetter_shouldOrderAlphabetically() = runTest {
        val category1 = parentCategory.copy(id = "cat-1", name = "Apple Store")
        val category2 = parentCategory.copy(id = "cat-2", name = "Amazon")
        val category3 = parentCategory.copy(id = "cat-3", name = "Athletic Equipment")

        categoryDao.insert(category3)
        categoryDao.insert(category1)
        categoryDao.insert(category2)

        val results = categoryDao.getAll()

        assertEquals(3, results.size)
        assertEquals("Amazon", results[0].name)
        assertEquals("Apple Store", results[1].name)
        assertEquals("Athletic Equipment", results[2].name)
    }

    @Test
    fun getAll_withCaseInsensitiveOrdering_shouldOrderCorrectly() = runTest {
        val category1 = parentCategory.copy(id = "cat-1", name = "books")
        val category2 = parentCategory.copy(id = "cat-2", name = "Book Store")
        val category3 = parentCategory.copy(id = "cat-3", name = "BOOKMARKS")

        categoryDao.insert(category2)
        categoryDao.insert(category3)
        categoryDao.insert(category1)

        val results = categoryDao.getAll()

        assertEquals(3, results.size)
        assertTrue(results.all { it.name.lowercase().startsWith("book") })
    }

    @Test
    fun update_withSameId_shouldPreserveId() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(independentCategory)

        val updatedCategory = parentCategory.copy(
            name = "Completely Different Category",
            parentId = independentCategory.id
        )
        categoryDao.update(updatedCategory)

        val result = categoryDao.getById(parentCategory.id)
        assertNotNull(result)
        assertEquals(parentCategory.id, result.id)
        assertEquals("Completely Different Category", result.name)
        assertEquals(independentCategory.id, result.parentId)
    }

    @Test
    fun insert_withDuplicateNames_shouldAllowMultipleCategoriesWithSameName() = runTest {
        val category1 = parentCategory.copy(id = "cat-1", name = "My Category")
        val category2 = parentCategory.copy(id = "cat-2", name = "My Category")

        categoryDao.insert(category1)
        categoryDao.insert(category2)

        val results = categoryDao.getAll()
        assertEquals(2, results.size)
        assertTrue(results.all { it.name == "My Category" })
        assertTrue(results.map { it.id }.containsAll(listOf("cat-1", "cat-2")))
    }

    @Test
    fun getSubcategories_withMultipleParents_shouldReturnOnlyCorrectChildren() = runTest {
        val parent1 = parentCategory.copy(id = "parent-1", name = "Parent 1")
        val parent2 = parentCategory.copy(id = "parent-2", name = "Parent 2")
        val child1 = subcategoryRestaurant.copy(id = "child-1", name = "Child 1", parentId = "parent-1")
        val child2 = subcategoryRestaurant.copy(id = "child-2", name = "Child 2", parentId = "parent-1")
        val child3 = subcategoryRestaurant.copy(id = "child-3", name = "Child 3", parentId = "parent-2")

        categoryDao.insert(parent1)
        categoryDao.insert(parent2)
        categoryDao.insert(child1)
        categoryDao.insert(child2)
        categoryDao.insert(child3)

        val parent1Children = categoryDao.getSubcategories("parent-1")
        val parent2Children = categoryDao.getSubcategories("parent-2")

        assertEquals(2, parent1Children.size)
        assertEquals(1, parent2Children.size)
        assertTrue(parent1Children.all { it.parentId == "parent-1" })
        assertTrue(parent2Children.all { it.parentId == "parent-2" })
    }

    @Test
    fun getSubcategories_shouldOrderByNameAscending() = runTest {
        val parent = parentCategory.copy(id = "parent", name = "Parent")
        val childZ = subcategoryRestaurant.copy(id = "child-z", name = "Z Category", parentId = "parent")
        val childA = subcategoryRestaurant.copy(id = "child-a", name = "A Category", parentId = "parent")
        val childM = subcategoryRestaurant.copy(id = "child-m", name = "M Category", parentId = "parent")

        categoryDao.insert(parent)
        categoryDao.insert(childZ)
        categoryDao.insert(childA)
        categoryDao.insert(childM)

        val subcategories = categoryDao.getSubcategories("parent")

        assertEquals(3, subcategories.size)
        assertEquals("A Category", subcategories[0].name)
        assertEquals("M Category", subcategories[1].name)
        assertEquals("Z Category", subcategories[2].name)
    }

    @Test
    fun insert_withTimestamps_shouldPreserveTimestamps() = runTest {
        val specificTime = 1234567890L
        val categoryWithTimestamp = parentCategory.copy(
            createdAt = specificTime,
            updatedAt = specificTime + 1000
        )

        categoryDao.insert(categoryWithTimestamp)

        val result = categoryDao.getById(categoryWithTimestamp.id)
        assertNotNull(result)
        assertEquals(specificTime, result.createdAt)
        assertEquals(specificTime + 1000, result.updatedAt)
    }

    @Test
    fun insert_withNullParentId_shouldWork() = runTest {
        val rootCategory = parentCategory.copy(
            id = "root-category",
            name = "Root Category",
            parentId = null
        )

        categoryDao.insert(rootCategory)

        val result = categoryDao.getById(rootCategory.id)
        assertNotNull(result)
        assertNull(result.parentId)
    }

    @Test
    fun update_changingParentId_shouldUpdateCorrectly() = runTest {
        val parent1 = parentCategory.copy(id = "parent-1", name = "Parent 1")
        val parent2 = parentCategory.copy(id = "parent-2", name = "Parent 2")
        val child = subcategoryRestaurant.copy(id = "child", name = "Child", parentId = "parent-1")

        categoryDao.insert(parent1)
        categoryDao.insert(parent2)
        categoryDao.insert(child)

        val updatedChild = child.copy(parentId = "parent-2")
        categoryDao.update(updatedChild)

        val result = categoryDao.getById(child.id)
        assertNotNull(result)
        assertEquals("parent-2", result.parentId)

        val parent1Children = categoryDao.getSubcategories("parent-1")
        val parent2Children = categoryDao.getSubcategories("parent-2")

        assertTrue(parent1Children.isEmpty())
        assertEquals(1, parent2Children.size)
        assertEquals(child.id, parent2Children[0].id)
    }

    @Test
    fun update_changingParentIdToNull_shouldMakeRootCategory() = runTest {
        val parent = parentCategory.copy(id = "parent", name = "Parent")
        val child = subcategoryRestaurant.copy(id = "child", name = "Child", parentId = "parent")

        categoryDao.insert(parent)
        categoryDao.insert(child)

        val updatedChild = child.copy(parentId = null)
        categoryDao.update(updatedChild)

        val result = categoryDao.getById(child.id)
        assertNotNull(result)
        assertNull(result.parentId)

        val parentChildren = categoryDao.getSubcategories("parent")
        assertTrue(parentChildren.isEmpty())
    }

    @Test
    fun deleteById_withChildren_shouldOnlyDeleteSpecifiedCategory() = runTest {
        categoryDao.insert(parentCategory)
        categoryDao.insert(subcategoryRestaurant)
        categoryDao.insert(subcategoryGrocery)

        categoryDao.deleteById(parentCategory.id)

        val parentResult = categoryDao.getById(parentCategory.id)
        val restaurantResult = categoryDao.getById(subcategoryRestaurant.id)
        val groceryResult = categoryDao.getById(subcategoryGrocery.id)

        assertNull(parentResult)
        assertNotNull(restaurantResult)
        assertNotNull(groceryResult)
    }

    @Test
    fun getAll_withMixedParentAndChildCategories_shouldReturnAllOrdered() = runTest {
        categoryDao.insert(subcategoryRestaurant)
        categoryDao.insert(parentCategory)
        categoryDao.insert(independentCategory)
        categoryDao.insert(subcategoryGrocery)

        val results = categoryDao.getAll()

        assertEquals(4, results.size)
        val names = results.map { it.name }
        assertEquals(listOf("Food & Dining", "Grocery", "Restaurant", "Transportation"), names)
    }
}