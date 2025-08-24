package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nichidori.saku.data.entity.AccountEntity

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM account ORDER BY name ASC")
    suspend fun getAll(): List<AccountEntity>

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT SUM(current_amount) FROM account")
    suspend fun getTotalBalance(): Long?
}
