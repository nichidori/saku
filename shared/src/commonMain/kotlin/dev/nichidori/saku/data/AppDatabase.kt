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
import dev.nichidori.saku.data.dao.MonthlyAccountBalanceDao
import dev.nichidori.saku.data.dao.TrxDao
import dev.nichidori.saku.data.entity.AccountEntity
import dev.nichidori.saku.data.entity.BudgetEntity
import dev.nichidori.saku.data.entity.BudgetTemplateEntity
import dev.nichidori.saku.data.entity.CategoryEntity
import dev.nichidori.saku.data.entity.MonthlyAccountBalanceEntity
import dev.nichidori.saku.data.entity.TrxEntity
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TrxEntity::class,
        BudgetEntity::class,
        BudgetTemplateEntity::class,
        MonthlyAccountBalanceEntity::class,
    ],
    version = 5,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun trxDao(): TrxDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetTemplateDao(): BudgetTemplateDao
    abstract fun monthlyAccountBalanceDao(): MonthlyAccountBalanceDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE category ADD COLUMN icon TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        // Create budget_template
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budget_template` (
                `id` TEXT NOT NULL,
                `category_id` TEXT NOT NULL,
                `default_amount` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`category_id`) REFERENCES `category`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_template_category_id`
            ON `budget_template` (`category_id`)
            """.trimIndent()
        )

        // Create budget table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budget` (
                `id` TEXT NOT NULL,
                `template_id` TEXT NOT NULL,
                `category_id` TEXT NOT NULL,
                `month` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `base_amount` INTEGER NOT NULL,
                `spent_amount` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`category_id`) REFERENCES `category`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`template_id`) REFERENCES `budget_template`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_category_id_month_year`
            ON `budget` (`category_id`, `month`, `year`)
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_budget_template_id`
            ON `budget` (`template_id`)
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `monthly_account_balance` (
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `account_id` TEXT NOT NULL,
                `balance` INTEGER NOT NULL,
                PRIMARY KEY(`year`, `month`, `account_id`),
                FOREIGN KEY(`account_id`) REFERENCES `account`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_monthly_account_balance_account_id`
            ON `monthly_account_balance` (`account_id`)
            """.trimIndent()
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            UPDATE `trx`
            SET `description` = `description` || ' ' || `note`
            WHERE `note` IS NOT NULL AND `note` != ''
            """.trimIndent()
        )
        connection.execSQL("ALTER TABLE `trx` DROP COLUMN `note`")
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
