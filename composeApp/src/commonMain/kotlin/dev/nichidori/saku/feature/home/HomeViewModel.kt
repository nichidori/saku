package dev.nichidori.saku.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlin.time.Instant

data class HomeUiState(
    val isLoading: Boolean = false,
    val currentMonth: YearMonth? = null,
    val netWorth: Long = 0,
    val netWorthTrend: List<Float> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val trxs: List<Trx> = emptyList(),
) {
    val netWorthFormatted = netWorth.toRupiah()
}

fun Account.balanceFormatted() = currentAmount.toRupiah()

class HomeViewModel(
    private val accountRepository: AccountRepository,
    private val trxRepository: TrxRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load(month: YearMonth) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val accounts = accountRepository.getAllAccounts()
            val netWorth = accountRepository.getTotalBalance()
            val trxs = trxRepository.getFilteredTrxs(TrxFilter(month = month))
            _uiState.update {
                it.copy(
                    netWorth = netWorth,
                    accounts = accounts,
                    trxs = trxs,
                    isLoading = false
                )
            }
        }
    }
}

fun Instant.toYearMonth(): YearMonth {
    return toLocalDateTime(TimeZone.currentSystemDefault())
        .let { YearMonth(it.year, it.month) }
}