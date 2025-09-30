package dev.nichidori.saku.feature.categoryList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.repo.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryListUiState(
    val isLoading: Boolean = false,
    val categoriesByParent: Map<Category, List<Category>> = emptyMap(),
)

class CategoryListViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isLoading = true)
                }
                val categories = categoryRepository.getAllCategories()
                val parents = categories.filter { it.parent == null }
                val categoriesByParent = parents.associateWith { parent ->
                    categories.filter { it.parent?.id == parent.id }
                }
                _uiState.update {
                    it.copy(categoriesByParent = categoriesByParent, isLoading = false)
                }
            } catch (e: Exception) {
                this@CategoryListViewModel.log(e)
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }
}
