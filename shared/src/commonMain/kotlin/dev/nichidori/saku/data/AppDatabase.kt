package dev.nichidori.saku.data


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.nichidori.saku.data.dao.*
import dev.nichidori.saku.data.entity.*
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TrxEntity::class,
        BudgetEntity::class,
        BudgetTemplateEntity::class,
        CreditEntity::class,
        TrxTemplateEntity::class,
    ],
    version = 8,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun trxDao(): TrxDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetTemplateDao(): BudgetTemplateDao
    abstract fun creditDao(): CreditDao
    abstract fun trxTemplateDao(): TrxTemplateDao
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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // 1. Create credit table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `credit` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `limit` INTEGER NOT NULL DEFAULT 0,
                `current_amount` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // 2. Copy CREDIT rows from account → credit
        connection.execSQL(
            """
            INSERT INTO `credit` (`id`, `name`, `limit`, `current_amount`, `created_at`, `updated_at`)
            SELECT `id`, `name`, 0, 0, `created_at`, `updated_at`
            FROM `account` WHERE `type` = 'Credit'
            """.trimIndent()
        )

        // 3. Create monthly_credit_balance table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `monthly_credit_balance` (
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `credit_id` TEXT NOT NULL,
                `balance` INTEGER NOT NULL,
                PRIMARY KEY(`year`, `month`, `credit_id`),
                FOREIGN KEY(`credit_id`) REFERENCES `credit`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_monthly_credit_balance_credit_id` ON `monthly_credit_balance` (`credit_id`)")

        // 4. Recreate trx with separate source/target columns + FKs CASCADE
        connection.execSQL("ALTER TABLE `trx` RENAME TO `trx_old`")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `trx` (
                `id` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `category_id` TEXT,
                `source_account_id` TEXT,
                `source_credit_id` TEXT,
                `target_account_id` TEXT,
                `target_credit_id` TEXT,
                `transaction_at` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER,
                `type` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`source_account_id`) REFERENCES `account`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`source_credit_id`) REFERENCES `credit`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`target_account_id`) REFERENCES `account`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`target_credit_id`) REFERENCES `credit`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`category_id`) REFERENCES `category`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO `trx` (
                `id`, `description`, `amount`, `category_id`,
                `source_account_id`, `source_credit_id`,
                `target_account_id`, `target_credit_id`,
                `transaction_at`, `created_at`, `updated_at`, `type`
            )
            SELECT
                t.`id`, t.`description`, t.`amount`, t.`category_id`,
                CASE WHEN c.`id` IS NULL THEN t.`source_account_id` ELSE NULL END,
                c.`id`,
                CASE WHEN ct.`id` IS NULL THEN t.`target_account_id` ELSE NULL END,
                ct.`id`,
                t.`transaction_at`, t.`created_at`, t.`updated_at`, t.`type`
            FROM `trx_old` t
            LEFT JOIN `credit` c ON t.`source_account_id` = c.`id`
            LEFT JOIN `credit` ct ON t.`target_account_id` = ct.`id`
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE `trx_old`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_category_id` ON `trx` (`category_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_source_account_id` ON `trx` (`source_account_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_source_credit_id` ON `trx` (`source_credit_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_target_account_id` ON `trx` (`target_account_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_target_credit_id` ON `trx` (`target_credit_id`)")

        // 5. Move credit history from monthly_account_balance to monthly_credit_balance
        connection.execSQL(
            """
            INSERT INTO `monthly_credit_balance` (`year`, `month`, `credit_id`, `balance`)
            SELECT `year`, `month`, `account_id`, `balance`
            FROM `monthly_account_balance`
            WHERE `account_id` IN (SELECT `id` FROM `credit`)
            """.trimIndent()
        )

        // 6. Remove credit rows from monthly_account_balance
        connection.execSQL(
            """
            DELETE FROM `monthly_account_balance`
            WHERE `account_id` IN (SELECT `id` FROM `credit`)
            """.trimIndent()
        )

        // 6. Delete Credit rows from account
        connection.execSQL("DELETE FROM `account` WHERE `type` = 'Credit'")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS monthly_account_balance")
        connection.execSQL("DROP TABLE IF EXISTS monthly_credit_balance")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `trx_template` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `category_id` TEXT,
                `source_account_id` TEXT,
                `source_credit_id` TEXT,
                `target_account_id` TEXT,
                `target_credit_id` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`category_id`) REFERENCES `category`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`source_account_id`) REFERENCES `account`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`source_credit_id`) REFERENCES `credit`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`target_account_id`) REFERENCES `account`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`target_credit_id`) REFERENCES `credit`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_template_category_id` ON `trx_template` (`category_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_template_source_account_id` ON `trx_template` (`source_account_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_template_source_credit_id` ON `trx_template` (`source_credit_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_template_target_account_id` ON `trx_template` (`target_account_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_template_target_credit_id` ON `trx_template` (`target_credit_id`)")
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8
        )
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
