package dev.nichidori.saku.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Initial
import dev.nichidori.saku.core.model.Status.Loading
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.repo.AccountRepository

data class AccountUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val balance: Long? = null,
    val type: AccountType? = null,
    val canDelete: Boolean = false,
    val saveStatus: Status<Unit, Exception> = Initial,
    val deleteStatus: Status<Unit, Exception> = Initial,
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
        id?.let { id ->
            viewModelScope.launch {
                _uiState.update {
                    it.copy(isLoading = true)
                }
                val account = accountRepository.getAccountById(id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = account?.name.orEmpty(),
                        balance = account?.currentAmount,
                        type = account?.type,
                        canDelete = account != null
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

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(deleteStatus = Loading)
                }
                accountRepository.deleteAccount(id!!)
                _uiState.update {
                    it.copy(deleteStatus = Success(Unit))
                }
            } catch (e: Exception) {
                this@AccountViewModel.log(e)
                _uiState.update {
                    it.copy(deleteStatus = Failure(e))
                }
            }
        }
    }
}
