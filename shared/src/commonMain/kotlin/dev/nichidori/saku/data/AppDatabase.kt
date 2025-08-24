package dev.nichidori.saku.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import dev.nichidori.saku.data.dao.AccountDao
import dev.nichidori.saku.data.dao.CategoryDao
import dev.nichidori.saku.data.dao.TrxDao
import dev.nichidori.saku.data.entity.AccountEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxEntity

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
