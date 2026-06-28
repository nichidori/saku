package dev.nichidori.saku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.nichidori.saku.data.entity.MonthlyCreditBalanceEntity

@Dao
interface MonthlyCreditBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MonthlyCreditBalanceEntity>)

    @Query(
        """
        SELECT * FROM monthly_credit_balance
        WHERE credit_id = :creditId
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getByCreditId(creditId: String): List<MonthlyCreditBalanceEntity>

    @Query(
        """
        SELECT * FROM monthly_credit_balance
        WHERE year = :year AND month = :month
        ORDER BY credit_id ASC
        """
    )
    suspend fun getByYearMonth(year: Int, month: Int): List<MonthlyCreditBalanceEntity>

    @Query(
        """
        SELECT * FROM monthly_credit_balance
        WHERE (year > :startYear OR (year = :startYear AND month >= :startMonth))
        AND (year < :endYear OR (year = :endYear AND month <= :endMonth))
        ORDER BY year ASC, month ASC, credit_id ASC
        """
    )
    suspend fun getByYearMonthRange(
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): List<MonthlyCreditBalanceEntity>

    @Query(
        """
        SELECT * FROM monthly_credit_balance
        WHERE credit_id = :creditId
        AND (year > :startYear OR (year = :startYear AND month >= :startMonth))
        AND (year < :endYear OR (year = :endYear AND month <= :endMonth))
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getByCreditIdAndYearMonthRange(
        creditId: String,
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): List<MonthlyCreditBalanceEntity>

    @Query("SELECT SUM(balance) FROM monthly_credit_balance WHERE year = :year AND month = :month")
    suspend fun getTotalByYearMonth(year: Int, month: Int): Long?

    @Query(
        """
        SELECT year, month, SUM(balance) as totalBalance FROM monthly_credit_balance
        WHERE (year > :startYear OR (year = :startYear AND month >= :startMonth))
        AND (year < :endYear OR (year = :endYear AND month <= :endMonth))
        GROUP BY year, month
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getTotalsByYearMonthRange(
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): List<BalanceTotalTuple>
}

data class BalanceTotalTuple(
    val year: Int,
    val month: Int,
    val totalBalance: Long
)
