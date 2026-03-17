package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.nichidori.saku.data.entity.TrxEntity
import dev.nichidori.saku.data.entity.TrxTypeEntity
import dev.nichidori.saku.data.entity.TrxWithDetailsEntity

@Dao
interface TrxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trx: TrxEntity)

    @Update
    suspend fun update(trx: TrxEntity)

    @Query("DELETE FROM trx WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    @Query("SELECT * FROM trx WHERE id = :id")
    suspend fun getByIdWithDetails(id: String): TrxWithDetailsEntity?

    @Transaction
    @Query(
        """
    SELECT * FROM trx
    WHERE transaction_at >= :startTime
      AND transaction_at < :endTime
      AND (:type IS NULL OR type = :type)
      AND (:categoryId IS NULL OR category_id = :categoryId)
      AND (
          :accountId IS NULL OR 
          source_account_id = :accountId OR 
          target_account_id = :accountId
      )
    ORDER BY transaction_at DESC
"""
    )
    suspend fun getFilteredWithDetails(
        startTime: Long,
        endTime: Long,
        type: TrxTypeEntity? = null,
        categoryId: String? = null,
        accountId: String? = null
    ): List<TrxWithDetailsEntity>

    @Query(
        """
    SELECT SUM(t.amount) FROM trx t
    INNER JOIN category c ON t.category_id = c.id
    WHERE t.transaction_at >= :startTime
    AND t.transaction_at < :endTime
    AND t.type = :type
    AND (t.category_id = :categoryId OR c.parent_id = :categoryId)
"""
    )
    suspend fun getTotalAmount(startTime: Long, endTime: Long, categoryId: String, type: TrxTypeEntity): Long?
}
