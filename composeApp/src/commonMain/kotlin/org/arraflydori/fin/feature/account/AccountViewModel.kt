package org.arraflydori.fin.feature.account

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.arraflydori.fin.domain.model.AccountType

data class AccountUiState(
    val name: String = "",
    val balance: String = "",
    val type: AccountType? = null,
)

class AccountViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun onNameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(name = newValue)
    }

    fun onBalanceChange(newValue: String) {
        _uiState.value = _uiState.value.copy(balance = newValue)
    }

    fun onTypeChange(newValue: AccountType) {
        _uiState.value = _uiState.value.copy(type = newValue)
    }

    fun saveAccount() {
        // TODO: Save Account to database
    }
}
