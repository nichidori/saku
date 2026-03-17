package dev.nichidori.saku.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.repo.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonthBudgetUiState(
    val loadStatus: Status<Unit, Exception> = Initial,
    val saveStatus: Status<Unit, Exception> = Initial,
    val defaultAmount: Long? = null,
    val budget: Budget? = null,
    val amount: Long? = null,
) {
    val canSave: Boolean = amount != null && amount > 0
    val amountFormatted = amount?.toRupiah().orEmpty()
    val defaultAmountFormatted = defaultAmount?.toRupiah().orEmpty()
}

class MonthBudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val budgetId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(MonthBudgetUiState())
    val uiState: StateFlow<MonthBudgetUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loadStatus = Loading) }
                val budget = budgetRepository.getBudgetById(budgetId)
                    ?: throw NoSuchElementException("Budget not found")
                val template = budgetRepository.getBudgetTemplateById(budget.templateId)

                _uiState.update {
                    it.copy(
                        loadStatus = Success(Unit),
                        budget = budget,
                        defaultAmount = template?.defaultAmount,
                        amount = budget.baseAmount
                    )
                }
            } catch (e: Exception) {
                this@MonthBudgetViewModel.log(e)
                _uiState.update { it.copy(loadStatus = Failure(e)) }
            }
        }
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

                val budget = _uiState.value.budget ?: throw Exception("Budget not found")
                val baseAmount = _uiState.value.amount ?: throw Exception("Amount cannot be empty")

                budgetRepository.updateBudget(
                    id = budgetId,
                    baseAmount = baseAmount,
                    spentAmount = budget.spentAmount
                )
                _uiState.update { it.copy(saveStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@MonthBudgetViewModel.log(e)
                _uiState.update { it.copy(saveStatus = Failure(e)) }
            }
        }
    }
}
