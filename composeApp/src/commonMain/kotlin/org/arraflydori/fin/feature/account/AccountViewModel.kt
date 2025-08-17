package org.arraflydori.fin.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.arraflydori.fin.core.model.Status
import org.arraflydori.fin.core.model.Status.*
import org.arraflydori.fin.core.util.log
import org.arraflydori.fin.core.util.toRupiah
import org.arraflydori.fin.domain.model.AccountType
import org.arraflydori.fin.domain.repo.AccountRepository

data class AccountUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val balance: Long? = null,
    val type: AccountType? = null,
    val saveStatus: Status<Unit, Exception> = Initial,
) {
    val canSave = name.isNotBlank() && balance != null && type != null
    val balanceFormatted = balance?.toRupiah().orEmpty()
}

class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val id: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    val typeOptions = AccountType.entries.toList()

    init {
        id?.let {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(isLoading = true)
                }
                val account = accountRepository.getAccountById(it)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = account?.name.orEmpty(),
                        balance = account?.currentAmount,
                        type = account?.type
                    )
                }
            }
        }
    }

    fun onNameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(name = newValue)
    }

    fun onBalanceChange(newValue: String) {
        if (newValue.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(balance = newValue.toLongOrNull())
        }
    }

    fun onTypeChange(newValue: AccountType) {
        _uiState.value = _uiState.value.copy(type = newValue)
    }

    fun saveAccount() {
        viewModelScope.launch {
            try {
                if (uiState.value.name.isBlank()) throw  Exception("Name cannot be empty")
                if (uiState.value.balance == null) throw  Exception("Balance cannot be empty")
                if (uiState.value.type == null) throw  Exception("Type cannot be empty")
                _uiState.update {
                    it.copy(saveStatus = Loading)
                }
                if (id != null) {
                    accountRepository.updateAccount(
                        id = id,
                        name = uiState.value.name,
                        initialAmount = uiState.value.balance!!,
                        type = uiState.value.type!!
                    )
                } else {
                    accountRepository.addAccount(
                        name = uiState.value.name,
                        initialAmount = uiState.value.balance!!,
                        type = uiState.value.type!!
                    )
                }
                _uiState.update {
                    it.copy(saveStatus = Success(Unit))
                }
            } catch (e: Exception) {
                this@AccountViewModel.log(e)
                _uiState.update {
                    it.copy(saveStatus = Failure(e))
                }
            }
        }
    }
}
