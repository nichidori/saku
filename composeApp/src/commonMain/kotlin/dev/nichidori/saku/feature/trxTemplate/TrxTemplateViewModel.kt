package dev.nichidori.saku.feature.trxTemplate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxAccount
import dev.nichidori.saku.domain.model.TrxTemplate
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.CategoryRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrxTemplateUiState(
    val isLoading: Boolean = false,
    val type: TrxType = TrxType.Expense,
    val name: String = "",
    val amount: Long? = null,
    val description: String = "",
    val sourceAccount: TrxAccount? = null,
    val targetAccount: TrxAccount? = null,
    val category: Category? = null,
    val accountOptions: List<TrxAccount> = listOf(),
    val incomesByParent: Map<Category, List<Category>> = emptyMap(),
    val expensesByParent: Map<Category, List<Category>> = emptyMap(),
    val canDelete: Boolean = false,
    val saveStatus: Status<Unit, Exception> = Initial,
    val deleteStatus: Status<TrxTemplate, Exception> = Initial,
) {
    val categoriesByParent = when (type) {
        TrxType.Income -> incomesByParent
        TrxType.Expense -> expensesByParent
        else -> emptyMap()
    }
    val amountFormatted = (amount ?: 0).toRupiah()
    val canSave = name.isNotBlank()
            && amount != null
            && sourceAccount != null
            && (if (type == TrxType.Transfer) targetAccount != null else true)
            && (if (type != TrxType.Transfer) category != null else true)
}

class TrxTemplateViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val trxRepository: TrxRepository,
    private val id: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrxTemplateUiState())
    val uiState: StateFlow<TrxTemplateUiState> = _uiState.asStateFlow()

    val types = TrxType.entries

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val accounts = accountRepository.getAllTrxAccounts()
            val categories = categoryRepository.getAllCategories()
            val (parents, children) = categories.partition { it.parent == null }
            val childrenByParentId = children.groupBy { it.parent?.id }
            val incomesByParent = parents
                .filter { it.type == TrxType.Income }
                .associateWith { childrenByParentId[it.id].orEmpty() }
            val expensesByParent = parents
                .filter { it.type == TrxType.Expense }
                .associateWith { childrenByParentId[it.id].orEmpty() }
            val template = id?.let { trxRepository.getTrxTemplateById(id) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    type = template?.type ?: it.type,
                    name = template?.name ?: it.name,
                    amount = template?.amount ?: it.amount,
                    description = template?.description ?: it.description,
                    sourceAccount = template?.sourceAccount ?: it.sourceAccount,
                    targetAccount = template?.targetAccount ?: it.targetAccount,
                    category = template?.category ?: it.category,
                    accountOptions = accounts,
                    incomesByParent = incomesByParent,
                    expensesByParent = expensesByParent,
                    canDelete = template != null
                )
            }
        }
    }

    fun onTypeChange(newValue: TrxType) {
        _uiState.update {
            it.copy(
                type = newValue,
                category = null,
                targetAccount = null
            )
        }
    }

    fun onNameChange(newValue: String) {
        _uiState.update { it.copy(name = newValue) }
    }

    fun onAmountChange(change: (String) -> String) {
        _uiState.update { currState ->
            val current = currState.amount?.toString().orEmpty()
            currState.copy(amount = change(current).toLongOrNull())
        }
    }

    fun onDescriptionChange(newValue: String) {
        _uiState.update { it.copy(description = newValue) }
    }

    fun onSourceAccountChange(newValue: TrxAccount) {
        _uiState.update { it.copy(sourceAccount = newValue) }
    }

    fun onTargetAccountChange(newValue: TrxAccount) {
        _uiState.update { it.copy(targetAccount = newValue) }
    }

    fun onCategoryChange(newValue: Category) {
        _uiState.update { it.copy(category = newValue) }
    }

    fun saveTemplate() {
        viewModelScope.launch {
            try {
                if (uiState.value.name.isBlank()) throw Exception("Name cannot be empty")
                if (uiState.value.amount == null) throw Exception("Amount cannot be empty")
                if (uiState.value.sourceAccount == null) throw Exception("Source account cannot be empty")
                if (uiState.value.type == TrxType.Transfer) {
                    if (uiState.value.targetAccount == null) throw Exception("Target account cannot be empty")
                    if (uiState.value.targetAccount == uiState.value.sourceAccount) throw Exception("Target account cannot be same as source account")
                } else {
                    if (uiState.value.category == null) throw Exception("Category cannot be empty")
                }
                _uiState.update { it.copy(saveStatus = Loading) }
                if (id == null) {
                    _uiState.value.let {
                        trxRepository.addTrxTemplate(
                            name = it.name,
                            type = it.type,
                            description = it.description,
                            amount = it.amount!!,
                            sourceAccount = it.sourceAccount!!,
                            targetAccount = it.targetAccount,
                            category = it.category,
                        )
                    }
                } else {
                    _uiState.value.let {
                        trxRepository.updateTrxTemplate(
                            id = id,
                            name = it.name,
                            type = it.type,
                            description = it.description,
                            amount = it.amount!!,
                            sourceAccount = it.sourceAccount!!,
                            targetAccount = it.targetAccount,
                            category = it.category,
                        )
                    }
                }
                _uiState.update { it.copy(saveStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@TrxTemplateViewModel.log(e)
                _uiState.update { it.copy(saveStatus = Failure(e)) }
            }
        }
    }

    fun deleteTemplate() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(deleteStatus = Loading)
                }
                val templateId = id ?: throw Exception("Template id is null")
                val template = trxRepository.getTrxTemplateById(templateId) ?: throw Exception("Template not found")
                trxRepository.deleteTrxTemplate(templateId)
                _uiState.update {
                    it.copy(deleteStatus = Success(template))
                }
            } catch (e: Exception) {
                this@TrxTemplateViewModel.log(e)
                _uiState.update {
                    it.copy(deleteStatus = Failure(e))
                }
            }
        }
    }
}
