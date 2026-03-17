package dev.nichidori.saku.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.BudgetTemplate
import dev.nichidori.saku.domain.repo.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DefaultBudgetUiState(
    val loadStatus: Status<Unit, Exception> = Initial,
    val saveStatus: Status<Unit, Exception> = Initial,
    val template: BudgetTemplate? = null,
)

class DefaultBudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val templateId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(DefaultBudgetUiState())
    val uiState: StateFlow<DefaultBudgetUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loadStatus = Loading) }
                val template = budgetRepository.getBudgetTemplateById(templateId)
                    ?: throw NoSuchElementException("Budget template not found")
                _uiState.update {
                    it.copy(
                        loadStatus = Success(Unit),
                        template = template
                    )
                }
            } catch (e: Exception) {
                this@DefaultBudgetViewModel.log(e)
                _uiState.update { it.copy(loadStatus = Failure(e)) }
            }
        }
    }

    fun save(defaultAmount: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(saveStatus = Loading) }
                val currentTemplate = _uiState.value.template
                    ?: throw IllegalStateException("Template not loaded")
                
                budgetRepository.updateBudgetTemplate(
                    id = templateId,
                    category = currentTemplate.category,
                    defaultAmount = defaultAmount
                )
                _uiState.update { it.copy(saveStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@DefaultBudgetViewModel.log(e)
                _uiState.update { it.copy(saveStatus = Failure(e)) }
            }
        }
    }
}
