package org.arraflydori.fin.feature.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.arraflydori.fin.core.util.log
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.repo.CategoryRepository

data class StatisticUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
)

class StatisticViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState: StateFlow<StatisticUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isLoading = true)
                }
                val categories = categoryRepository.getAllCategories()
                _uiState.update {
                    it.copy(categories = categories, isLoading = false)
                }
            } catch (e: Exception) {
                this@StatisticViewModel.log(e)
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }
}
