package dev.nichidori.saku.feature.trxTemplateList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.TrxTemplate
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrxTemplateListUiState(
    val isLoading: Boolean = false,
    val selectedType: TrxType = TrxType.Expense,
    val templates: List<TrxTemplate> = emptyList(),
) {
    val filteredTemplates: List<TrxTemplate>
        get() = templates.filter { it.type == selectedType }
}

class TrxTemplateListViewModel(
    private val trxRepository: TrxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrxTemplateListUiState())
    val uiState: StateFlow<TrxTemplateListUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val templates = trxRepository.getAllTrxTemplates()
                _uiState.update {
                    it.copy(templates = templates, isLoading = false)
                }
            } catch (e: Exception) {
                this@TrxTemplateListViewModel.log(e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSelectedTypeChange(type: TrxType) {
        _uiState.update { it.copy(selectedType = type) }
    }
}
