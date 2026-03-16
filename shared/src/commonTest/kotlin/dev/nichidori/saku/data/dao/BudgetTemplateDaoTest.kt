package dev.nichidori.saku.data.dao

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.BudgetTemplateEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BudgetTemplateDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BudgetTemplateDao
    private lateinit var categoryDao: CategoryDao

    private val category = CategoryEntity(
        id = "cat-food",
        name = "Food",
        type = TrxTypeEntity.Expense,
        parentId = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    private val template = BudgetTemplateEntity(
        id = "tmpl-1",
        categoryId = "cat-food",
        defaultAmount = 5_000_000L,
        createdAt = System.currentTimeMillis(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        dao = db.budgetTemplateDao()
        categoryDao = db.categoryDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetByIdWithCategory() = runTest {
        categoryDao.insert(category)
        dao.insert(template)

        val result = dao.getByIdWithCategory(template.id)
        assertNotNull(result)
        assertEquals(category.name, result.category.name)
    }

    @Test
    fun getByCategoryIdWithCategory() = runTest {
        categoryDao.insert(category)
        dao.insert(template)

        val result = dao.getByCategoryIdWithCategory(category.id)
        assertNotNull(result)
        assertEquals(template.id, result.budgetTemplate.id)
    }

    @Test
    fun getAllWithCategory() = runTest {
        categoryDao.insert(category)
        dao.insert(template)

        val results = dao.getAllWithCategory()
        assertEquals(1, results.size)
    }

    @Test
    fun update() = runTest {
        categoryDao.insert(category)
        dao.insert(template)

        val updated = template.copy(defaultAmount = 10_000_000L)
        dao.update(updated)

        val result = dao.getByIdWithCategory(template.id)
        assertEquals(10_000_000L, result?.budgetTemplate?.defaultAmount)
    }

    @Test
    fun deleteById() = runTest {
        categoryDao.insert(category)
        dao.insert(template)

        dao.deleteById(template.id)
        assertNull(dao.getByIdWithCategory(template.id))
    }
}
