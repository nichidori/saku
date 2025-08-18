package org.arraflydori.fin.feature.category

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
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.TrxType
import org.arraflydori.fin.domain.repo.CategoryRepository

data class CategoryUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val type: TrxType = TrxType.Income,
    val parent: Category? = null,
    val parentsMap: Map<TrxType, List<Category>> = emptyMap(),
    val saveStatus: Status<Unit, Exception> = Initial,
) {
    val canSave = name.isNotBlank()
    val parentOptions = parentsMap[type].orEmpty()
}

class CategoryViewModel(
    private val categoryRepository: CategoryRepository,
    private val id: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    val types = TrxType.entries

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val category = id?.let {
                categoryRepository.getCategoryById(id)
            }
            val parents = getParentlessCategories()
            val parentsMap = mutableMapOf<TrxType, List<Category>>()
            for (type in types) {
                parentsMap[type] = parents.filter { it.type == type }
            }
            _uiState.update {
                category?.let { c ->
                    it.copy(
                        name = c.name,
                        type = c.type,
                        parent = c.parent
                    )
                }
                it.copy(isLoading = false, parentsMap = parentsMap)
            }
        }
    }

    fun onNameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(name = newValue)
    }

    fun onTypeChange(newValue: TrxType) {
        _uiState.value = _uiState.value.copy(type = newValue, parent = null)
    }

    fun onParentChange(newValue: Category?) {
        _uiState.value = _uiState.value.copy(parent = newValue)
    }

    fun saveCategory() {
        viewModelScope.launch {
            try {
                if (uiState.value.name.isBlank()) throw Exception("Name cannot be empty")
                _uiState.update { it.copy(saveStatus = Loading) }
                if (id != null) {
                    categoryRepository.updateCategory(
                        id = id,
                        name = uiState.value.name,
                        type = uiState.value.type,
                        parent = uiState.value.parent
                    )
                } else {
                    categoryRepository.addCategory(
                        name = uiState.value.name,
                        type = uiState.value.type,
                        parent = uiState.value.parent
                    )
                }
                _uiState.update { it.copy(saveStatus = Success(Unit)) }
            } catch (e: Exception) {
                this@CategoryViewModel.log(e)
                _uiState.update { it.copy(saveStatus = Failure(e)) }
            }
        }
    }

    // TODO: Filter this in DB?
    private suspend fun getParentlessCategories(): List<Category> {
        return categoryRepository.getAllCategories().filter { it.parent == null }
    }
}
