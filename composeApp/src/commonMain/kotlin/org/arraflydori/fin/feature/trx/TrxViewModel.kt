package org.arraflydori.fin.feature.trx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.arraflydori.fin.core.model.Status
import org.arraflydori.fin.core.model.Status.Failure
import org.arraflydori.fin.core.model.Status.Initial
import org.arraflydori.fin.core.model.Status.Loading
import org.arraflydori.fin.core.model.Status.Success
import org.arraflydori.fin.core.util.log
import org.arraflydori.fin.core.util.toRupiah
import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.Trx
import org.arraflydori.fin.domain.model.TrxType
import org.arraflydori.fin.domain.repo.AccountRepository
import org.arraflydori.fin.domain.repo.CategoryRepository
import org.arraflydori.fin.domain.repo.TrxRepository
import kotlin.collections.orEmpty
import kotlin.text.orEmpty
import kotlin.time.Instant

data class TrxUiState(
    val isLoading: Boolean = false,
    val type: TrxType = TrxType.Expense,
    val time: Instant? = null,
    val amount: Long? = null,
    val name: String = "",
    val sourceAccount: Account? = null,
    val targetAccount: Account? = null,
    val category: Category? = null,
    val note: String = "",
    val accountOptions: List<Account> = listOf(),
    val categoryMap: Map<TrxType, List<Category>> = emptyMap(),
    val saveStatus: Status<Unit, Exception> = Initial,
) {
    val categoryOptions = categoryMap[type].orEmpty()
    val amountFormatted = amount?.toRupiah().orEmpty()
    val canSave = time != null
            && amount != null
            && name.isNotBlank()
            && sourceAccount != null
            && (if (type == TrxType.Transfer) targetAccount != null else true)
            && category != null
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
                        time = this?.transactionAt ?: it.time,
                        amount = this?.amount ?: it.amount,
                        name = this?.name ?: it.name,
                        sourceAccount = this?.sourceAccount ?: it.sourceAccount,
                        targetAccount = (this as? Trx.Transfer)?.targetAccount ?: it.targetAccount,
                        category = this?.category ?: it.category,
                        accountOptions = accounts,
                        categoryMap = categoriesMap
                    )
                }
            }
        }
    }

    fun onTypeChange(newValue: TrxType) {
        _uiState.value = _uiState.value.copy(type = newValue, targetAccount = null)
    }

    fun onTimeChange(newValue: Instant) {
        _uiState.value = _uiState.value.copy(time = newValue)
    }

    fun onAmountChange(newValue: String) {
        _uiState.value = _uiState.value.copy(amount = newValue.toLongOrNull())
    }

    fun onNameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(name = newValue)
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
                if (uiState.value.name.isBlank()) throw Exception("Name cannot be empty")
                if (uiState.value.amount == null) throw Exception("Amount cannot be empty")
                if (uiState.value.sourceAccount == null) throw Exception("Source account cannot be empty")
                if (uiState.value.type == TrxType.Transfer) {
                    if (uiState.value.targetAccount == null) throw Exception("Target account cannot be empty")
                }
                _uiState.update { it.copy(saveStatus = Loading) }
                if (id == null) {
                    _uiState.value.let {
                        trxRepository.addTrx(
                            type = it.type,
                            transactionAt = it.time!!,
                            amount = it.amount!!,
                            name = it.name,
                            sourceAccount = it.sourceAccount!!,
                            targetAccount = it.targetAccount,
                            category = it.category!!,
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
                            name = it.name,
                            sourceAccount = it.sourceAccount!!,
                            targetAccount = it.targetAccount,
                            category = it.category!!,
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
}
