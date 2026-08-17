package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nichidori.saku.data.entity.InstallmentEntity

@Dao
interface InstallmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(installment: InstallmentEntity)

    @Update
    suspend fun update(installment: InstallmentEntity)

    @Query("SELECT * FROM installment WHERE id = :id")
    suspend fun getById(id: String): InstallmentEntity?

    @Query("SELECT * FROM installment")
    suspend fun getAll(): List<InstallmentEntity>

    @Query("SELECT COUNT(*) FROM installment WHERE credit_id = :creditId AND next_index < months")
    suspend fun getPendingCountByCreditId(creditId: String): Int

    @Query("DELETE FROM installment WHERE id = :id")
    suspend fun deleteById(id: String)
}