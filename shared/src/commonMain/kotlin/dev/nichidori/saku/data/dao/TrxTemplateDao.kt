package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.nichidori.saku.data.entity.TrxTemplateEntity
import dev.nichidori.saku.data.entity.TrxTemplateWithDetailsEntity

@Dao
interface TrxTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trxTemplate: TrxTemplateEntity)

    @Update
    suspend fun update(trxTemplate: TrxTemplateEntity)

    @Transaction
    @Query("SELECT * FROM trx_template WHERE id = :id")
    suspend fun getByIdWithDetails(id: String): TrxTemplateWithDetailsEntity?

    @Transaction
    @Query("SELECT * FROM trx_template")
    suspend fun getAllWithDetails(): List<TrxTemplateWithDetailsEntity>

    @Query("DELETE FROM trx_template WHERE id = :id")
    suspend fun deleteById(id: String)
}
