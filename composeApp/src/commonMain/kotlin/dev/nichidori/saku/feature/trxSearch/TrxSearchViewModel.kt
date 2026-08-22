package dev.nichidori.saku.feature.trxSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.repo.TrxRepository
import dev.nichidori.saku.feature.trxList.DailyTrxRecord
import dev.nichidori.saku.feature.trxList.groupTrxsByDate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.milliseconds

data class TrxSearchUiState(
    val status: Status<Unit, Exception> = Initial,
    val recordsByDate: Map<LocalDate, DailyTrxRecord> = emptyMap(),
)

@OptIn(FlowPreview::class)
class TrxSearchViewModel(
    private val trxRepository: TrxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrxSearchUiState())
    val uiState: StateFlow<TrxSearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var searchDebounceJob: Job? = null

    fun onQueryChange(query: String) {
        _query.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300.milliseconds)
            if (query.isBlank()) {
                _uiState.value = TrxSearchUiState()
            } else {
                doSearch(query.trim())
            }
        }
    }

    fun clearQuery() {
        searchDebounceJob?.cancel()
        searchDebounceJob = null
        _query.value = ""
        _uiState.value = TrxSearchUiState()
    }

    private fun doSearch(query: String) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(status = Loading)
                }

                val trxs = trxRepository.searchTrxsByDescription(query)

                _uiState.update {
                    it.copy(
                        status = Success(Unit),
                        recordsByDate = groupTrxsByDate(trxs),
                    )
                }
            } catch (e: Exception) {
                this@TrxSearchViewModel.log(e)
                _uiState.update {
                    it.copy(status = Failure(e))
                }
            }
        }
    }
}
