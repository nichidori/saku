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

data class CategoryBudgetUiState(
    val loadStatus: Status<Unit, Exception> = Initial,
    val deleteStatus: Status<Unit, Exception> = Initial,
    val template: BudgetTemplate? = null,
    val budgets: List<Budget> = emptyList(),
)

class CategoryBudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val templateId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryBudgetUiState())
    val uiState: StateFlow<CategoryBudgetUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loadStatus = Loading) }
                val template = budgetRepository.getBudgetTemplateById(templateId)
                    ?: throw NoSuchElementException("Budget template not found")
                val budgets = budgetRepository.getBudgetsByCategory(template.category.id)
                    .sortedWith(compareByDescending<Budget> { it.year }.thenByDescending { it.month })
                
                _uiState.update {
                    it.copy(
                        loadStatus = Success(Unit),
                        template = template,
                        budgets = budgets
                    )
                }
            } catch (e: Exception) {
                this@CategoryBudgetViewModel.log(e)
                _uiState.update { it.copy(loadStatus = Failure(e)) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(deleteStatus = Loading) }
                budgetRepository.deleteBudgetTemplate(templateId)
                _uiState.update { it.copy(deleteStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@CategoryBudgetViewModel.log(e)
                _uiState.update { it.copy(deleteStatus = Failure(e)) }
            }
        }
    }
}
