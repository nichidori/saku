package dev.nichidori.saku.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun createDataStore(): DataStore<Preferences> = createDataStore(
    producePath = {
        val userHome = System.getProperty("user.home")
        val file = File(userHome, dataStoreFileName)
        file.absolutePath
    }
)