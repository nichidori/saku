package dev.nichidori.saku.feature.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth

data class StatisticUiState(
    val isLoading: Boolean = false,
    val incomesOfCategory: Map<Category, Long> = emptyMap(),
    val expensesOfCategory: Map<Category, Long> = emptyMap(),
) {
    val totalIncome: Long = incomesOfCategory.values.sum()
    val totalExpense: Long = expensesOfCategory.values.sum()
}

class StatisticViewModel(
    private val trxRepository: TrxRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState: StateFlow<StatisticUiState> = _uiState.asStateFlow()

    fun load(month: YearMonth) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isLoading = true)
                }
                val incomes = trxRepository
                    .getFilteredTrxs(TrxFilter(month = month, type = TrxType.Income))
                    .associateBy({ it.category }, { it.amount })
                val expenses = trxRepository
                    .getFilteredTrxs(TrxFilter(month = month, type = TrxType.Expense))
                    .associateBy({ it.category }, { it.amount })
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        incomesOfCategory = incomes
                            .toSortedMap(compareByDescending { c -> incomes[c] }),
                        expensesOfCategory = expenses
                            .toSortedMap(compareByDescending { c -> expenses[c] }),
                    )
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
