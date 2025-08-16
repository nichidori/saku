package org.arraflydori.fin

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.getRoomDatabase
import org.arraflydori.fin.data.repo.DefaultAccountRepository
import org.arraflydori.fin.data.repo.DefaultTrxRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        // TODO: Build actual DB
        val db = getRoomDatabase(
            builder = Room.inMemoryDatabaseBuilder(this, AppDatabase::class.java)
        )

        setContent {
            App(
                accountRepository = DefaultAccountRepository(db = db),
                trxRepository = DefaultTrxRepository(db = db),
            )
        }
    }
}
