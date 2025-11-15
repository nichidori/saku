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
    val loadStatus: Status<YearMonth, Exception> = Initial,
    val trxRecordsByDate: Map<LocalDate, DailyTrxRecord> = emptyMap(),
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
                    it.copy(loadStatus = Loading)
                }
                val trxs = trxRepository.getFilteredTrxs(TrxFilter(month = month))
                _uiState.update {
                    it.copy(
                        loadStatus = Success(month),
                        trxRecordsByDate = trxs.groupBy { trx ->
                            trx.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        }.mapValues { (_, dailyTrxs) ->
                            DailyTrxRecord(
                                trxs = dailyTrxs,
                                totalIncome = dailyTrxs
                                    .filter { trx -> trx is Trx.Income }
                                    .sumOf { trx -> trx.amount },
                                totalExpense = dailyTrxs
                                    .filter { trx -> trx is Trx.Expense }
                                    .sumOf { trx -> trx.amount },
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                this@TrxListViewModel.log(e)
                _uiState.update {
                    it.copy(loadStatus = Failure(e))
                }
            }
        }
    }
}
