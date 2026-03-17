package dev.nichidori.saku.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.BudgetRepository
import dev.nichidori.saku.domain.repo.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DefaultBudgetUiState(
    val loadStatus: Status<Unit, Exception> = Initial,
    val saveStatus: Status<Unit, Exception> = Initial,
    val categoriesByParent: Map<Category, List<Category>> = emptyMap(),
    val category: Category? = null,
    val amount: Long? = null,
    val newBudget: Boolean? = null,
) {
    val canSave = category != null && amount != null && amount > 0
    val amountFormatted = amount?.toRupiah().orEmpty()
}

class DefaultBudgetViewModel(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val templateId: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(DefaultBudgetUiState())
    val uiState: StateFlow<DefaultBudgetUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loadStatus = Loading, newBudget = templateId == null) }

                if (templateId != null) {
                    val template = budgetRepository.getBudgetTemplateById(templateId)
                        ?: throw NoSuchElementException("Budget template not found")

                    _uiState.update {
                        it.copy(
                            loadStatus = Success(Unit),
                            category = template.category,
                            amount = template.defaultAmount,
                        )
                    }
                } else {
                    val categories = categoryRepository.getAllCategories()
                    val (parents, children) = categories.partition { it.parent == null }
                    val childrenByParentId = children.groupBy { it.parent?.id }
                    val expensesByParent = parents
                        .filter { it.type == TrxType.Expense }
                        .associateWith { childrenByParentId[it.id].orEmpty() }

                    _uiState.update {
                        it.copy(
                            loadStatus = Success(Unit),
                            categoriesByParent = expensesByParent,
                        )
                    }
                }
            } catch (e: Exception) {
                this@DefaultBudgetViewModel.log(e)
                _uiState.update { it.copy(loadStatus = Failure(e)) }
            }
        }
    }

    fun onCategoryChange(newValue: Category) {
        _uiState.update { it.copy(category = newValue) }
    }

    fun onAmountChange(newValue: String) {
        if (newValue.all { it.isDigit() }) {
            _uiState.update { it.copy(amount = newValue.toLongOrNull()) }
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(saveStatus = Loading) }

                val category = _uiState.value.category ?: throw Exception("Category cannot be empty")
                val amount = _uiState.value.amount ?: throw Exception("Amount cannot be empty")

                if (templateId != null) {
                    budgetRepository.updateBudgetTemplate(
                        id = templateId,
                        defaultAmount = amount
                    )
                } else {
                    budgetRepository.addBudgetTemplate(
                        category = category,
                        defaultAmount = amount
                    )
                }

                _uiState.update { it.copy(saveStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@DefaultBudgetViewModel.log(e)
                _uiState.update { it.copy(saveStatus = Failure(e)) }
            }
        }
    }
}
