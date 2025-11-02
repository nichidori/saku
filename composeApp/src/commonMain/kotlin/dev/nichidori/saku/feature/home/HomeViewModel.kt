package dev.nichidori.saku.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Initial
import dev.nichidori.saku.core.model.Status.Loading
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth

data class HomeUiState(
    val loadStatus: Status<YearMonth, Exception> = Initial,
    val netWorth: Long = 0,
    val netWorthTrend: List<Float> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val trxs: List<Trx> = emptyList(),
    val showBalance: Boolean = false,
) {
    val netWorthFormatted = if (showBalance) netWorth.toRupiah() else "****"
}

fun Account.balanceFormatted(show: Boolean) = if (show) currentAmount.toRupiah() else "****"

class HomeViewModel(
    private val accountRepository: AccountRepository,
    private val trxRepository: TrxRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load(month: YearMonth) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(loadStatus = Loading, trxs = listOf())
                }
                val accounts = accountRepository.getAllAccounts()
                val netWorth = accountRepository.getTotalBalance()
                val trxs = trxRepository.getFilteredTrxs(TrxFilter(month = month))
                _uiState.update {
                    it.copy(
                        loadStatus = Success(month),
                        netWorth = netWorth,
                        accounts = accounts,
                        trxs = trxs,
                    )
                }
            } catch (e: Exception) {
                this@HomeViewModel.log(e)
                _uiState.update {
                    it.copy(loadStatus = Failure(e))
                }
            }
        }
    }

    fun onBalanceToggle() {
        _uiState.update {
            it.copy(showBalance = !it.showBalance)
        }
    }
}