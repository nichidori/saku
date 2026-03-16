package dev.nichidori.saku.data


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.nichidori.saku.data.dao.AccountDao
import dev.nichidori.saku.data.dao.BudgetDao
import dev.nichidori.saku.data.dao.BudgetTemplateDao
import dev.nichidori.saku.data.dao.CategoryDao
import dev.nichidori.saku.data.dao.TrxDao
import dev.nichidori.saku.data.entity.AccountEntity
import dev.nichidori.saku.data.entity.BudgetEntity
import dev.nichidori.saku.data.entity.BudgetTemplateEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.TrxEntity
import kotlinx.coroutines.Dispatchers

@Database(entities = [AccountEntity::class, CategoryEntity::class, TrxEntity::class, BudgetEntity::class, BudgetTemplateEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun trxDao(): TrxDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetTemplateDao(): BudgetTemplateDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE category ADD COLUMN icon TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 4) {
    override fun migrate(connection: SQLiteConnection) {
        // Create budget_template
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `budget_template` (`id` TEXT NOT NULL, `category_id` TEXT NOT NULL, `start_month` INTEGER NOT NULL, `start_year` INTEGER NOT NULL, `default_amount` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_template_category_id` ON `budget_template` (`category_id`)")

        // Create budget table
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `budget` (`id` TEXT NOT NULL, `template_id` TEXT NOT NULL, `category_id` TEXT NOT NULL, `month` INTEGER NOT NULL, `year` INTEGER NOT NULL, `base_amount` INTEGER NOT NULL, `spent_amount` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`template_id`) REFERENCES `budget_template`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_category_id_month_year` ON `budget` (`category_id`, `month`, `year`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_template_id` ON `budget` (`template_id`)")
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
