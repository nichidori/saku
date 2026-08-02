package dev.nichidori.saku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "monthly_net_worth",
    primaryKeys = ["year", "month"]
)
data class MonthlyNetWorthEntity(
    @ColumnInfo(name = "year") val year: Int,
    @ColumnInfo(name = "month") val month: Int,
    @ColumnInfo(name = "net_worth") val netWorth: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long?,
)
