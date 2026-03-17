package dev.nichidori.saku.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.BudgetTemplate
import dev.nichidori.saku.domain.repo.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonthBudgetUiState(
    val loadStatus: Status<Unit, Exception> = Initial,
    val saveStatus: Status<Unit, Exception> = Initial,
    val template: BudgetTemplate? = null,
    val budget: Budget? = null,
)

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
                val template = budgetRepository.getBudgetTemplateByCategoryId(budget.category.id)
                
                _uiState.update {
                    it.copy(
                        loadStatus = Success(Unit),
                        budget = budget,
                        template = template
                    )
                }
            } catch (e: Exception) {
                this@MonthBudgetViewModel.log(e)
                _uiState.update { it.copy(loadStatus = Failure(e)) }
            }
        }
    }

    fun save(baseAmount: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(saveStatus = Loading) }
                val currentBudget = _uiState.value.budget
                    ?: throw IllegalStateException("Budget not loaded")
                
                budgetRepository.updateBudget(
                    id = budgetId,
                    baseAmount = baseAmount,
                    spentAmount = currentBudget.spentAmount
                )
                _uiState.update { it.copy(saveStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@MonthBudgetViewModel.log(e)
                _uiState.update { it.copy(saveStatus = Failure(e)) }
            }
        }
    }
}
