package dev.nichidori.saku.feature.trxList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth

data class TrxListUiState(
    val isLoading: Boolean = false,
    val trxs: List<Trx> = emptyList(),
)

class TrxListViewModel(
    private val trxRepository: TrxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrxListUiState())
    val uiState: StateFlow<TrxListUiState> = _uiState.asStateFlow()

    fun load(month: YearMonth) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isLoading = true)
                }
                val trxs = trxRepository.getFilteredTrxs(TrxFilter(
                    month = month
                ))
                _uiState.update {
                    it.copy(
                        trxs = trxs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                this@TrxListViewModel.log(e)
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }
}
