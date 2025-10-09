package dev.nichidori.saku.feature.trx

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
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.CategoryRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlin.collections.orEmpty
import kotlin.text.orEmpty
import kotlin.time.Clock
import kotlin.time.Instant

data class TrxUiState(
    val isLoading: Boolean = false,
    val type: TrxType = TrxType.Expense,
    val time: Instant? = null,
    val amount: Long? = null,
    val description: String = "",
    val sourceAccount: Account? = null,
    val targetAccount: Account? = null,
    val category: Category? = null,
    val note: String = "",
    val accountOptions: List<Account> = listOf(),
    val categoryMap: Map<TrxType, List<Category>> = emptyMap(),
    val canDelete: Boolean = false,
    val saveStatus: Status<Unit, Exception> = Initial,
    val deleteStatus: Status<Unit, Exception> = Initial,
) {
    val categoryOptions = categoryMap[type].orEmpty()
    val amountFormatted = amount?.toRupiah().orEmpty()
    val canSave = time != null
            && amount != null
            && sourceAccount != null
            && (if (type == TrxType.Transfer) targetAccount != null else true)
            && (if (type != TrxType.Transfer) category != null else true)
}

class TrxViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val trxRepository: TrxRepository,
    private val id: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrxUiState())
    val uiState: StateFlow<TrxUiState> = _uiState.asStateFlow()

    val types = TrxType.entries

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val accounts = accountRepository.getAllAccounts()
            val categories = categoryRepository.getAllCategories()
            val categoriesMap = mutableMapOf<TrxType, List<Category>>()
            for (type in types) {
                categoriesMap[type] = categories.filter { it.type == type }
            }
            val trx = id?.let { trxRepository.getTrxById(id) }
            _uiState.update {
                with(trx) {
                    it.copy(
                        isLoading = false,
                        type = when (this) {
                            is Trx.Income -> TrxType.Income
                            is Trx.Expense -> TrxType.Expense
                            is Trx.Transfer -> TrxType.Transfer
                            null -> it.type
                        },
                        time = this?.transactionAt ?: Clock.System.now(),
                        amount = this?.amount ?: it.amount,
                        description = this?.description ?: it.description,
                        sourceAccount = this?.sourceAccount ?: it.sourceAccount,
                        targetAccount = (this as? Trx.Transfer)?.targetAccount ?: it.targetAccount,
                        category = this?.category ?: it.category,
                        accountOptions = accounts,
                        categoryMap = categoriesMap,
                        canDelete = this != null
                    )
                }
            }
        }
    }

    fun onTypeChange(newValue: TrxType) {
        _uiState.value = _uiState.value.copy(
            type = newValue,
            category = null,
            targetAccount = null
        )
    }

    fun onTimeChange(newValue: Instant) {
        _uiState.value = _uiState.value.copy(time = newValue)
    }

    fun onAmountChange(newValue: String) {
        _uiState.value = _uiState.value.copy(amount = newValue.toLongOrNull())
    }

    fun onDescriptionChange(newValue: String) {
        _uiState.value = _uiState.value.copy(description = newValue)
    }

    fun onSourceAccountChange(newValue: Account) {
        _uiState.value = _uiState.value.copy(sourceAccount = newValue)
    }

    fun onTargetAccountChange(newValue: Account) {
        _uiState.value = _uiState.value.copy(targetAccount = newValue)
    }

    fun onCategoryChange(newValue: Category) {
        _uiState.value = _uiState.value.copy(category = newValue)
    }

    fun onNoteChange(newValue: String) {
        _uiState.value = _uiState.value.copy(note = newValue)
    }

    fun saveTrx() {
        viewModelScope.launch {
            try {
                if (uiState.value.time == null) throw Exception("Time cannot be empty")
                if (uiState.value.amount == null) throw Exception("Amount cannot be empty")
                if (uiState.value.sourceAccount == null) throw Exception("Source account cannot be empty")
                if (uiState.value.type == TrxType.Transfer) {
                    if (uiState.value.targetAccount == null) throw Exception("Target account cannot be empty")
                } else {
                    if (uiState.value.category == null) throw Exception("Category cannot be empty")
                }
                _uiState.update { it.copy(saveStatus = Loading) }
                if (id == null) {
                    _uiState.value.let {
                        trxRepository.addTrx(
                            type = it.type,
                            transactionAt = it.time!!,
                            amount = it.amount!!,
                            description = it.description,
                            sourceAccount = it.sourceAccount!!,
                            targetAccount = it.targetAccount,
                            category = it.category,
                            note = it.note
                        )
                    }
                } else {
                    _uiState.value.let {
                        trxRepository.updateTrx(
                            id = id,
                            type = it.type,
                            transactionAt = it.time!!,
                            amount = it.amount!!,
                            description = it.description,
                            sourceAccount = it.sourceAccount!!,
                            targetAccount = it.targetAccount,
                            category = it.category,
                            note = it.note
                        )
                    }
                }
                _uiState.update { it.copy(saveStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@TrxViewModel.log(e)
                _uiState.update { it.copy(saveStatus = Failure(e)) }
            }
        }
    }

    fun deleteTrx() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(deleteStatus = Loading)
                }
                trxRepository.deleteTrx(id!!)
                _uiState.update {
                    it.copy(deleteStatus = Success(Unit))
                }
            } catch (e: Exception) {
                this@TrxViewModel.log(e)
                _uiState.update {
                    it.copy(deleteStatus = Failure(e))
                }
            }
        }
    }
}
