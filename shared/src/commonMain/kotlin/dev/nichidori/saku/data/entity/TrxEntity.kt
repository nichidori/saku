package dev.nichidori.saku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trx",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["target_account_id"],
            onDelete = ForeignKey.SET_NULL
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["source_account_id"]),
        Index(value = ["target_account_id"])
    ]
)
data class TrxEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "source_account_id") val sourceAccountId: String,
    @ColumnInfo(name = "target_account_id") val targetAccountId: String?,
    @ColumnInfo(name = "transaction_at") val transactionAt: Long,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long?,
    @ColumnInfo(name = "type") val type: TrxTypeEntity
)
