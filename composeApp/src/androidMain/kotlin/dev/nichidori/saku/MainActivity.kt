package dev.nichidori.saku

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.room.Room
import dev.nichidori.saku.core.platform.setToastActivityProvider
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.createDataStore
import dev.nichidori.saku.data.getDatabaseBuilder
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.data.repo.DefaultAccountRepository
import dev.nichidori.saku.data.repo.DefaultBudgetRepository
import dev.nichidori.saku.data.repo.DefaultCategoryRepository
import dev.nichidori.saku.data.repo.DefaultTrxRepository

const val useInMemoryDb = false

class MainActivity : ComponentActivity() {
    var themeInitialized by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !themeInitialized }
        setToastActivityProvider { this }
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val db = getRoomDatabase(
            builder = if (useInMemoryDb) {
                Room.inMemoryDatabaseBuilder(this, AppDatabase::class.java)
            } else {
                getDatabaseBuilder(this)
            }
        )
        val dataStore = createDataStore(this)

        setContent {
            val view = LocalView.current
            val window = LocalActivity.current?.window

            App(
                accountRepository = DefaultAccountRepository(db = db),
                categoryRepository = DefaultCategoryRepository(db = db),
                trxRepository = DefaultTrxRepository(db = db),
                budgetRepository = DefaultBudgetRepository(db = db),
                dataStore = dataStore,
                onDarkTheme = { darkTheme ->
                    if (!themeInitialized) {
                        themeInitialized = true
                    }
                    window?.let {
                        WindowInsetsControllerCompat(it, view).isAppearanceLightStatusBars = !darkTheme
                    }
                }
            )
        }
    }
}
