package dev.nichidori.saku.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Initial
import dev.nichidori.saku.core.model.Status.Loading
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val type: TrxType = TrxType.Expense,
    val canChooseType: Boolean = true,
    val parent: Category? = null,
    val parentsOfType: Map<TrxType, List<Category>> = emptyMap(),
    val children: List<Category> = emptyList(),
    val canDelete: Boolean = false,
    val saveStatus: Status<Unit, Exception> = Initial,
    val deleteStatus: Status<Unit, Exception> = Initial,
) {
    val canSave = name.isNotBlank()
    val parentOptions = parentsOfType[type].orEmpty()
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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    canChooseType = id == null,
                    canDelete = id != null
                )
            }
            val category = id?.let { categoryRepository.getCategoryById(id) }
            val children = category?.let {
                categoryRepository.getSubcategories(it.id)
            } ?: emptyList()
            val parents = if (children.isEmpty()) {
                categoryRepository.getRootCategories().filter { it.id != id }
            } else {
                emptyList()
            }
            val parentsOfType = types.associateWith { type ->
                parents.filter { it.type == type }
            }
            _uiState.update {
                it.copy(
                    name = category?.name ?: it.name,
                    type = category?.type ?: it.type,
                    parent = category?.parent ?: it.parent,
                    isLoading = false,
                    parentsOfType = parentsOfType
                )
            }
        }
    }

    fun onNameChange(newValue: String) {
        _uiState.update { it.copy(name = newValue) }
    }

    fun onTypeChange(newValue: TrxType) {
        _uiState.update { it.copy(type = newValue, parent = null) }
    }

    fun onParentChange(newValue: Category?) {
        _uiState.update { it.copy(parent = newValue) }
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

    fun deleteCategory() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(deleteStatus = Loading)
                }
                categoryRepository.deleteCategory(id!!)
                _uiState.update {
                    it.copy(deleteStatus = Success(Unit))
                }
            } catch (e: Exception) {
                this@CategoryViewModel.log(e)
                _uiState.update {
                    it.copy(deleteStatus = Failure(e))
                }
            }
        }
    }
}
