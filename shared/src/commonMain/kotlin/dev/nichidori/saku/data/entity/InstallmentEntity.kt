package dev.nichidori.saku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installment",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CreditEntity::class,
            parentColumns = ["id"],
            childColumns = ["credit_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["credit_id"]),
    ]
)
data class InstallmentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "credit_id") val creditId: String,
    @ColumnInfo(name = "principal") val principal: Long,
    @ColumnInfo(name = "months") val months: Int,
    @ColumnInfo(name = "monthly_rate") val monthlyRatePercent: Double,
    @ColumnInfo(name = "total_amount") val totalAmount: Long,
    @ColumnInfo(name = "monthly_payment") val monthlyPayment: Long,
    @ColumnInfo(name = "last_payment") val lastPayment: Long,
    @ColumnInfo(name = "start_at") val startAt: Long,
    @ColumnInfo(name = "due_day") val dueDay: Int,
    @ColumnInfo(name = "next_index") val nextIndex: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long?,
)