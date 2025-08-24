package org.arraflydori.fin.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.arraflydori.fin.data.dao.AccountDao
import org.arraflydori.fin.data.dao.CategoryDao
import org.arraflydori.fin.data.dao.TrxDao
import org.arraflydori.fin.data.entity.AccountEntity
import org.arraflydori.fin.data.entity.CategoryEntity
import org.arraflydori.fin.data.entity.TrxEntity

@Database(entities = [AccountEntity::class, CategoryEntity::class, TrxEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun trxDao(): TrxDao
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
