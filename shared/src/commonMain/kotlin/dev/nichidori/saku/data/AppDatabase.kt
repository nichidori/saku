package dev.nichidori.saku.data


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.nichidori.saku.data.dao.AccountDao
import dev.nichidori.saku.data.dao.CategoryDao
import dev.nichidori.saku.data.dao.TrxDao
import dev.nichidori.saku.data.entity.AccountEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxEntity
import kotlinx.coroutines.Dispatchers

@Database(entities = [AccountEntity::class, CategoryEntity::class, TrxEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun trxDao(): TrxDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE category ADD COLUMN icon TEXT")
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
