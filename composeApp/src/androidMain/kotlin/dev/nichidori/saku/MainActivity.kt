package dev.nichidori.saku

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import dev.nichidori.saku.core.platform.setToastActivityProvider
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.getDatabaseBuilder
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.data.repo.DefaultAccountRepository
import dev.nichidori.saku.data.repo.DefaultCategoryRepository
import dev.nichidori.saku.data.repo.DefaultTrxRepository

const val useInMemoryDb = false

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        setToastActivityProvider { this }

        val db = getRoomDatabase(
            builder = if (useInMemoryDb) {
                Room.inMemoryDatabaseBuilder(this, AppDatabase::class.java)
            } else {
                getDatabaseBuilder(this)
            }
        )
        setContent {
            App(
                accountRepository = DefaultAccountRepository(db = db),
                categoryRepository = DefaultCategoryRepository(db = db),
                trxRepository = DefaultTrxRepository(db = db),
            )
        }
    }
}
