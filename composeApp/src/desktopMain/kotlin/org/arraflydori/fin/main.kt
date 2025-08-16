package org.arraflydori.fin

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.room.Room
import org.arraflydori.fin.data.getRoomDatabase
import org.arraflydori.fin.data.repo.DefaultAccountRepository
import org.arraflydori.fin.data.repo.DefaultTrxRepository

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fin",
    ) {
        // TODO: Build actual DB
        val db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder())
        App(
            accountRepository = DefaultAccountRepository(db = db),
            trxRepository = DefaultTrxRepository(db = db),
        )
    }
}