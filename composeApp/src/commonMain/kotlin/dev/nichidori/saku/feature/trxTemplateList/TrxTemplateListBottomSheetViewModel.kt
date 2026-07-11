package dev.nichidori.saku.feature.trxTemplateList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.TrxTemplate
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class TrxTemplateListBottomSheetUiState(
    val isLoading: Boolean = false,
    val templates: List<TrxTemplate> = emptyList(),
)

class TrxTemplateListBottomSheetViewModel(
    private val trxRepository: TrxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrxTemplateListBottomSheetUiState())
    val uiState: StateFlow<TrxTemplateListBottomSheetUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val templates = trxRepository.getAllTrxTemplates()
                _uiState.update {
                    it.copy(templates = templates, isLoading = false)
                }
            } catch (e: Exception) {
                this@TrxTemplateListBottomSheetViewModel.log(e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    suspend fun createTrxFromTemplate(templateId: String): String {
        val template = trxRepository.getTrxTemplateById(templateId)
            ?: throw NoSuchElementException("Template not found")
        return trxRepository.addTrx(
            type = template.type,
            transactionAt = Clock.System.now(),
            amount = template.amount,
            description = template.description,
            sourceAccount = template.sourceAccount,
            targetAccount = template.targetAccount,
            category = template.category,
        )
    }
}
