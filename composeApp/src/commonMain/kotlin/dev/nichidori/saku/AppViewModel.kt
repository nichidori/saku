package dev.nichidori.saku

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.util.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

data class AppUiState(
    val darkTheme: Boolean = false,
)

class AppViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data
                .map { it[DARK_THEME_KEY] ?: false }.first()
                .let { dark ->
                    _uiState.update {
                        it.copy(darkTheme = dark)
                    }
                }
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            try {
                val newDarkTheme = !_uiState.value.darkTheme
                dataStore.edit { prefs ->
                    prefs[DARK_THEME_KEY] = newDarkTheme
                }
                _uiState.update {
                    it.copy(darkTheme = newDarkTheme)
                }
            } catch (e: Exception) {
                this@AppViewModel.log(e)
            }
        }
    }
}
