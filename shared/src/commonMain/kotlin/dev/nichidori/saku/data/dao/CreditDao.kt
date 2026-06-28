package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nichidori.saku.data.entity.CreditEntity

@Dao
interface CreditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credit: CreditEntity)

    @Query("SELECT * FROM credit WHERE id = :id")
    suspend fun getById(id: String): CreditEntity?

    @Query("SELECT * FROM credit ORDER BY name ASC")
    suspend fun getAll(): List<CreditEntity>

    @Update
    suspend fun update(credit: CreditEntity)

    @Query("DELETE FROM credit WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT SUM(current_amount) FROM credit")
    suspend fun getTotalBalance(): Long?

    @Query("SELECT SUM(`limit`) FROM credit")
    suspend fun getTotalLimit(): Long?
}
