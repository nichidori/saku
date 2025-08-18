package org.arraflydori.fin

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.room.Room
import org.arraflydori.fin.data.getRoomDatabase
import org.arraflydori.fin.data.repo.DefaultAccountRepository
import org.arraflydori.fin.data.repo.DefaultCategoryRepository
import org.arraflydori.fin.data.repo.DefaultTrxRepository

fun main() = application {
    val state = rememberWindowState(size = DpSize(
        width = 360.dp,
        height = 720.dp,
    ))
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fin",
        state = state,
    ) {
        // TODO: Build actual DB
        val db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder())
        App(
            accountRepository = DefaultAccountRepository(db = db),
            categoryRepository = DefaultCategoryRepository(db = db),
            trxRepository = DefaultTrxRepository(db = db),
        )
    }
}