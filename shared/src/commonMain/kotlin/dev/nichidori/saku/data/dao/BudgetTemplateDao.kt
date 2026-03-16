package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.nichidori.saku.data.entity.BudgetTemplateEntity
import dev.nichidori.saku.data.entity.BudgetTemplateWithCategoryEntity

@Dao
interface BudgetTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budgetTemplate: BudgetTemplateEntity)

    @Update
    suspend fun update(budgetTemplate: BudgetTemplateEntity)

    @Transaction
    @Query("SELECT * FROM budget_template WHERE id = :id")
    suspend fun getByIdWithCategory(id: String): BudgetTemplateWithCategoryEntity?

    @Transaction
    @Query("SELECT * FROM budget_template WHERE category_id = :categoryId")
    suspend fun getByCategoryIdWithCategory(categoryId: String): BudgetTemplateWithCategoryEntity?

    @Transaction
    @Query("SELECT * FROM budget_template")
    suspend fun getAllWithCategory(): List<BudgetTemplateWithCategoryEntity>

    @Query("DELETE FROM budget_template WHERE id = :id")
    suspend fun deleteById(id: String)
}
