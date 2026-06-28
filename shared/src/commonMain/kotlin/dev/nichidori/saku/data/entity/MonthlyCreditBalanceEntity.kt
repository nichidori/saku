package dev.nichidori.saku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "monthly_credit_balance",
    primaryKeys = ["year", "month", "credit_id"],
    foreignKeys = [
        ForeignKey(
            entity = CreditEntity::class,
            parentColumns = ["id"],
            childColumns = ["credit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("credit_id")]
)
data class MonthlyCreditBalanceEntity(
    @ColumnInfo(name = "year") val year: Int,
    @ColumnInfo(name = "month") val month: Int,
    @ColumnInfo(name = "credit_id") val creditId: String,
    @ColumnInfo(name = "balance") val balance: Long
)
