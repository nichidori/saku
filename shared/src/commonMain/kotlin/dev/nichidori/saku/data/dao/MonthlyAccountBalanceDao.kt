package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.nichidori.saku.data.entity.MonthlyAccountBalanceEntity

data class NetWorthTuple(
    val year: Int,
    val month: Int,
    val netWorth: Long
)

@Dao
interface MonthlyAccountBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MonthlyAccountBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MonthlyAccountBalanceEntity>)

    @Update
    suspend fun update(entity: MonthlyAccountBalanceEntity)

    @Update
    suspend fun updateAll(entities: List<MonthlyAccountBalanceEntity>)

    @Query(
        """
        SELECT * FROM monthly_account_balance
        WHERE year = :year AND month = :month
        ORDER BY account_id ASC
        """
    )
    suspend fun getByYearMonth(year: Int, month: Int): List<MonthlyAccountBalanceEntity>

    @Query(
        """
        SELECT * FROM monthly_account_balance
        WHERE (year > :startYear OR (year = :startYear AND month >= :startMonth))
        AND (year < :endYear OR (year = :endYear AND month <= :endMonth))
        ORDER BY year ASC, month ASC, account_id ASC
        """
    )
    suspend fun getByYearMonthRange(
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): List<MonthlyAccountBalanceEntity>

    @Query(
        """
        SELECT * FROM monthly_account_balance
        WHERE account_id = :accountId
        AND (year > :startYear OR (year = :startYear AND month >= :startMonth))
        AND (year < :endYear OR (year = :endYear AND month <= :endMonth))
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getByAccountIdAndYearMonthRange(
        accountId: String,
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): List<MonthlyAccountBalanceEntity>

    @Query("SELECT SUM(balance) FROM monthly_account_balance WHERE year = :year AND month = :month")
    suspend fun getNetWorthByYearMonth(year: Int, month: Int): Long?

    @Query(
        """
        SELECT year, month, SUM(balance) as netWorth FROM monthly_account_balance
        WHERE (year > :startYear OR (year = :startYear AND month >= :startMonth))
        AND (year < :endYear OR (year = :endYear AND month <= :endMonth))
        GROUP BY year, month
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getNetWorthsByYearMonthRange(
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): List<NetWorthTuple>
}
