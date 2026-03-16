package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.nichidori.saku.data.entity.BudgetEntity
import dev.nichidori.saku.data.entity.BudgetWithCategoryEntity

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Transaction
    @Query("SELECT * FROM budget WHERE id = :id")
    suspend fun getByIdWithCategory(id: String): BudgetWithCategoryEntity?

    @Transaction
    @Query("SELECT * FROM budget WHERE month = :month AND year = :year")
    suspend fun getByMonthAndYearWithCategory(month: Int, year: Int): List<BudgetWithCategoryEntity>

    @Query("DELETE FROM budget WHERE id = :id")
    suspend fun deleteById(id: String)
}
