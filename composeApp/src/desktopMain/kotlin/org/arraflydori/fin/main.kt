package org.arraflydori.fin

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.room.Room
import org.arraflydori.fin.data.getDatabaseBuilder
import org.arraflydori.fin.data.getRoomDatabase
import org.arraflydori.fin.data.repo.DefaultAccountRepository
import org.arraflydori.fin.data.repo.DefaultCategoryRepository
import org.arraflydori.fin.data.repo.DefaultTrxRepository

const val useInMemoryDb = false

fun main() = application {
    val state = rememberWindowState(
        size = DpSize(
            width = 360.dp,
            height = 720.dp,
        )
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fin",
        state = state,
    ) {
        val db = getRoomDatabase(
            builder = if (useInMemoryDb) {
                Room.inMemoryDatabaseBuilder()
            } else {
                getDatabaseBuilder()
            }
        )
        App(
            accountRepository = DefaultAccountRepository(db = db),
            categoryRepository = DefaultCategoryRepository(db = db),
            trxRepository = DefaultTrxRepository(db = db),
        )
    }
}