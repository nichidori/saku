package dev.nichidori.saku.data

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.nichidori.saku.data.entity.AccountTypeEntity
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppMigrationTest {

    private lateinit var dbFile: File

    @BeforeTest
    fun setup() {
        dbFile = createTempFile(suffix = ".db").toFile().apply { delete() }
    }

    @AfterTest
    fun teardown() {
        dbFile.delete()
    }

    @Test
    fun migrateFromVersion10_shouldDropInitialAmountAndKeepForeignKeys() = runTest {
        seedVersion10Database()
        var db: AppDatabase? = null
        try {
            db = getRoomDatabase(builder = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath))
            val accounts = db.accountDao().getAll()
            assertEquals(1, accounts.size)
            assertEquals("Cash", accounts.first().name)
            assertEquals(10_000L, accounts.first().currentAmount)
        } finally {
            db?.close()
        }

        val driver = BundledSQLiteDriver()
        val connection = driver.open(dbFile.absolutePath)
        try {
            val columnNames = queryColumnNames(connection, "PRAGMA table_info(`account`)", 1)
            assertFalse("initial_amount" in columnNames, "account should not have initial_amount column")
            assertTrue("current_amount" in columnNames)

            val referencedTables = queryColumnNames(connection, "PRAGMA foreign_key_list(`trx`)", 2)
            assertTrue("account" in referencedTables, "trx foreign keys should reference account")
            assertFalse("account_old" in referencedTables, "trx foreign keys must not reference account_old")
        } finally {
            connection.close()
        }
    }

    private fun seedVersion10Database() {
        val driver = BundledSQLiteDriver()
        val connection = driver.open(dbFile.absolutePath)
        try {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `account` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `initial_amount` INTEGER NOT NULL,
                    `current_amount` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `category` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `parent_id` TEXT,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER,
                    `icon` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`parent_id`) REFERENCES `category`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_category_parent_id` ON `category` (`parent_id`)")
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `credit` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `limit` INTEGER NOT NULL,
                    `current_amount` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `installment` (
                    `id` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `category_id` TEXT NOT NULL,
                    `credit_id` TEXT NOT NULL,
                    `principal` INTEGER NOT NULL,
                    `months` INTEGER NOT NULL,
                    `monthly_rate` REAL NOT NULL,
                    `total_amount` INTEGER NOT NULL,
                    `monthly_payment` INTEGER NOT NULL,
                    `last_payment` INTEGER NOT NULL,
                    `start_at` INTEGER NOT NULL,
                    `due_day` INTEGER NOT NULL,
                    `next_index` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`category_id`) REFERENCES `category`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`credit_id`) REFERENCES `credit`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_category_id` ON `installment` (`category_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_credit_id` ON `installment` (`credit_id`)")
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
                    `installment_id` TEXT,
                    `installment_index` INTEGER,
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
                        ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(`installment_id`) REFERENCES `installment`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_source_account_id` ON `trx` (`source_account_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_source_credit_id` ON `trx` (`source_credit_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_target_account_id` ON `trx` (`target_account_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_target_credit_id` ON `trx` (`target_credit_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_category_id` ON `trx` (`category_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_trx_installment_id` ON `trx` (`installment_id`)")
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
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_category_id_month_year` ON `budget` (`category_id`, `month`, `year`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_template_id` ON `budget` (`template_id`)")
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
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `monthly_net_worth` (
                    `year` INTEGER NOT NULL,
                    `month` INTEGER NOT NULL,
                    `net_worth` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER,
                    PRIMARY KEY(`year`, `month`)
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                INSERT INTO `account` (`id`, `name`, `initial_amount`, `current_amount`, `type`, `created_at`, `updated_at`)
                VALUES ('acc-1', 'Cash', 10_000, 10_000, '${AccountTypeEntity.Cash}', 1_000_000_000, NULL)
                """.trimIndent()
            )
            connection.execSQL("PRAGMA user_version = 10")
        } finally {
            connection.close()
        }
    }

    private fun queryColumnNames(connection: SQLiteConnection, sql: String, columnIndex: Int): List<String> {
        val names = mutableListOf<String>()
        val statement = connection.prepare(sql)
        try {
            while (statement.step()) {
                names.add(statement.getText(columnIndex))
            }
        } finally {
            statement.close()
        }
        return names
    }
}