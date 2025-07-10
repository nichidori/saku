package org.arraflydori.fin.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "initial_amount") val initialAmount: Long,
    @ColumnInfo(name = "current_amount") val currentAmount: Long,
    @ColumnInfo(name = "type") val type: AccountTypeEntity,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long?
)

