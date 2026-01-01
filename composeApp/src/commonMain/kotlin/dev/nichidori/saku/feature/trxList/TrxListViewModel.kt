package dev.nichidori.saku.feature.trxList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime

data class DailyTrxRecord(
    val trxs: List<Trx>,
    val totalIncome: Long,
    val totalExpense: Long,
)

data class TrxListUiState(
    val stateByMonth: Map<YearMonth, MonthlyState> = emptyMap()
) {
    data class MonthlyState(
        val loadStatus: Status<Unit, Exception> = Initial,
        val trxRecordsByDate: Map<LocalDate, DailyTrxRecord> = emptyMap(),
    )
}

class TrxListViewModel(
    private val trxRepository: TrxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrxListUiState())
    val uiState: StateFlow<TrxListUiState> = _uiState.asStateFlow()

    fun load(month: YearMonth) {
        viewModelScope.launch {
            try {
                updateMonthlyState(month) {
                    it.copy(loadStatus = Loading)
                }

                val trxs = trxRepository.getFilteredTrxs(TrxFilter(month = month))

                val trxRecordsByDate = trxs.groupBy { trx ->
                    trx.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                }.mapValues { (_, dailyTrxs) ->
                    DailyTrxRecord(
                        trxs = dailyTrxs,
                        totalIncome = dailyTrxs.filterIsInstance<Trx.Income>().sumOf { it.amount },
                        totalExpense = dailyTrxs.filterIsInstance<Trx.Expense>().sumOf { it.amount },
                    )
                }

                updateMonthlyState(month) {
                    it.copy(
                        loadStatus = Success(Unit),
                        trxRecordsByDate = trxRecordsByDate
                    )
                }
            } catch (e: Exception) {
                this@TrxListViewModel.log(e)
                updateMonthlyState(month) {
                    it.copy(loadStatus = Failure(e))
                }
            }
        }
    }

    private fun updateMonthlyState(month: YearMonth, transform: (TrxListUiState.MonthlyState) -> TrxListUiState.MonthlyState) {
        _uiState.update { currentState ->
            val currentMonthState = currentState.stateByMonth[month] ?: TrxListUiState.MonthlyState()
            currentState.copy(
                stateByMonth = currentState.stateByMonth + (month to transform(currentMonthState))
            )
        }
    }
}