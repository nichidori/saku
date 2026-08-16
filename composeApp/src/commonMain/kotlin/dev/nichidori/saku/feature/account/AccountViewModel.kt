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
import dev.nichidori.saku.domain.model.TrxAccount
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlin.time.Clock

data class AccountUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val balance: Long? = null,
    val initialBalance: Long? = null,
    val limit: Long? = null,
    val type: AccountType? = null,
    val account: TrxAccount? = null,
    val isEditing: Boolean = false,
    val saveStatus: Status<Unit, Exception> = Initial,
    val deleteStatus: Status<Unit, Exception> = Initial,
) {
    val showLimitInput = type == AccountType.Credit
    val canSave = name.isNotBlank() && balance != null && type != null
            && (if (type == AccountType.Credit) limit != null else true)
    val balanceFormatted: String
        get() {
            val b = balance ?: return ""
            val displayAmount = if (type == AccountType.Credit) -b else b
            return displayAmount.toRupiah()
        }
    val limitFormatted = limit?.toRupiah().orEmpty()
}

class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val trxRepository: TrxRepository,
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
                val credit = if (account == null) accountRepository.getCreditById(id) else null
                val balance = account?.currentAmount ?: credit?.currentAmount
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = account?.name ?: credit?.name.orEmpty(),
                        balance = balance,
                        initialBalance = balance,
                        limit = credit?.limit,
                        type = if (credit != null) AccountType.Credit else account?.type,
                        account = account?.let { TrxAccount.Regular(it) }
                            ?: credit?.let { TrxAccount.Credit(it) },
                        isEditing = account != null || credit != null
                    )
                }
            }
        }
    }

    fun onNameChange(newValue: String) {
        _uiState.update { it.copy(name = newValue) }
    }

    fun onBalanceChange(newValue: String) {
        if (newValue.isEmpty() || newValue.matches(Regex("-?\\d+"))) {
            _uiState.update { it.copy(balance = newValue.toLongOrNull()) }
        }
    }

    fun onTypeChange(newValue: AccountType) {
        _uiState.update {
            if (it.isEditing) return
            it.copy(
                type = newValue,
                limit = if (newValue != AccountType.Credit) null else it.limit
            )
        }
    }

    fun onLimitChange(newValue: String) {
        if (newValue.all { it.isDigit() }) {
            _uiState.update { it.copy(limit = newValue.toLongOrNull()) }
        }
    }

    fun saveAccount() {
        viewModelScope.launch {
            try {
                if (uiState.value.name.isBlank()) throw Exception("Name cannot be empty")
                if (uiState.value.balance == null) throw Exception("Balance cannot be empty")
                if (uiState.value.type == null) throw Exception("Type cannot be empty")
                if (uiState.value.type == AccountType.Credit && uiState.value.limit == null) throw Exception("Limit cannot be empty")
                _uiState.update {
                    it.copy(saveStatus = Loading)
                }
                val delta = if (uiState.value.isEditing
                    && uiState.value.balance != null
                    && uiState.value.initialBalance != null
                ) {
                    uiState.value.balance!! - uiState.value.initialBalance!!
                } else {
                    0L
                }
                if (uiState.value.type == AccountType.Credit) {
                    if (id != null) {
                        accountRepository.updateCredit(
                            id = id,
                            name = uiState.value.name,
                            limit = uiState.value.limit!!,
                            currentAmount = uiState.value.initialBalance ?: uiState.value.balance!!
                        )
                    } else {
                        accountRepository.addCredit(
                            name = uiState.value.name,
                            limit = uiState.value.limit!!,
                            currentAmount = uiState.value.balance!!
                        )
                    }
                } else {
                    if (id != null) {
                        accountRepository.updateAccount(
                            id = id,
                            name = uiState.value.name,
                            type = uiState.value.type!!
                        )
                    } else {
                        accountRepository.addAccount(
                            name = uiState.value.name,
                            currentAmount = uiState.value.balance!!,
                            type = uiState.value.type!!
                        )
                    }
                }
                if (delta != 0L) {
                    trxRepository.addTrx(
                        type = TrxType.Adjustment,
                        transactionAt = Clock.System.now(),
                        amount = delta,
                        description = "Balance adjustment",
                        sourceAccount = uiState.value.account
                            ?: throw Exception("Account not found"),
                        targetAccount = null,
                        category = null,
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
                if (uiState.value.type == AccountType.Credit) {
                    accountRepository.deleteCredit(id!!)
                } else {
                    accountRepository.deleteAccount(id!!)
                }
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
