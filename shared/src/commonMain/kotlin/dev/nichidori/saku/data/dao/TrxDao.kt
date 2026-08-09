package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.nichidori.saku.data.entity.AccountTypeEntity
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
          AND (:type IS NULL OR trx.type = :type)
          AND (:categoryId IS NULL OR category_id = :categoryId)
          AND (
              :accountId IS NULL OR 
              source_account_id = :accountId OR 
              source_credit_id = :accountId OR
              target_account_id = :accountId OR
              target_credit_id = :accountId
          )
          AND (
              (:accountType IS NULL AND :isCredit IS NULL)
              OR
              (:accountType IS NOT NULL AND (
                  source_account_id IN (SELECT id FROM account WHERE type = :accountType)
                  OR
                  target_account_id IN (SELECT id FROM account WHERE type = :accountType)
              ))
              OR
              (:isCredit = 1 AND (source_credit_id IS NOT NULL OR target_credit_id IS NOT NULL))
          )
          AND (:excludeInstallmentCharges = 0
               OR NOT (installment_id IS NOT NULL AND installment_index IS NULL))
        ORDER BY transaction_at DESC
        """
    )
    suspend fun getFilteredWithDetails(
        startTime: Long,
        endTime: Long,
        type: TrxTypeEntity? = null,
        categoryId: String? = null,
        accountId: String? = null,
        accountType: AccountTypeEntity? = null,
        isCredit: Boolean? = null,
        excludeInstallmentCharges: Boolean = false,
    ): List<TrxWithDetailsEntity>

    @Query("SELECT * FROM trx WHERE transaction_at < :endTime ORDER BY transaction_at ASC")
    suspend fun getAllUpTo(endTime: Long): List<TrxEntity>

    @Query(
        """
        SELECT * FROM trx
        WHERE source_credit_id = :creditId OR target_credit_id = :creditId
        ORDER BY transaction_at ASC
        """
    )
    suspend fun getAllByCreditId(creditId: String): List<TrxEntity>

    @Query(
        """
        SELECT * FROM trx
        WHERE installment_id = :installmentId
        ORDER BY installment_index ASC
        """
    )
    suspend fun getByInstallmentId(installmentId: String): List<TrxEntity>

    @Query(
        """
        SELECT * FROM trx
        WHERE installment_id = :installmentId
          AND installment_index = :index
        LIMIT 1
        """
    )
    suspend fun getByInstallmentIndex(installmentId: String, index: Int): TrxEntity?

    @Query(
        """
        SELECT SUM(t.amount) FROM trx t
        INNER JOIN category c ON t.category_id = c.id
        WHERE t.transaction_at >= :startTime
        AND t.transaction_at < :endTime
        AND t.type = :type
        AND (t.category_id = :categoryId OR c.parent_id = :categoryId)
        AND (t.installment_id IS NULL OR t.installment_index IS NOT NULL)
        """
    )
    suspend fun getTotalAmount(startTime: Long, endTime: Long, categoryId: String, type: TrxTypeEntity): Long?

    @Transaction
    @Query(
        """
        SELECT * FROM trx
        WHERE source_account_id = :accountId
           OR source_credit_id = :accountId
           OR target_account_id = :accountId
           OR target_credit_id = :accountId
        ORDER BY transaction_at ASC
        LIMIT 1
        """
    )
    suspend fun getEarliestByAccountId(accountId: String): TrxWithDetailsEntity?
}
