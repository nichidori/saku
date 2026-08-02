package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.nichidori.saku.data.entity.MonthlyNetWorthEntity

@Dao
interface MonthlyNetWorthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MonthlyNetWorthEntity)

    @Query("SELECT * FROM monthly_net_worth WHERE year = :year AND month = :month")
    suspend fun getByYearMonth(year: Int, month: Int): MonthlyNetWorthEntity?

    @Query("SELECT * FROM monthly_net_worth ORDER BY year ASC, month ASC")
    suspend fun getAll(): List<MonthlyNetWorthEntity>

    @Query(
        """
        SELECT * FROM monthly_net_worth
        WHERE year > :year OR (year = :year AND month >= :month)
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getFromYearMonth(year: Int, month: Int): List<MonthlyNetWorthEntity>

    @Query("DELETE FROM monthly_net_worth")
    suspend fun deleteAll()
}
