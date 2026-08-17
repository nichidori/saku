package dev.nichidori.saku.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.event.AppEvent
import dev.nichidori.saku.core.event.AppEventBus
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxAccount
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.BudgetRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

data class ActiveBudget(
    val budget: Budget,
    val remainingDays: Int,
    val dailyAllowance: Long
)

data class HomeUiState(
    val loadStatus: Status<Unit, Exception> = Initial,
    val month: YearMonth? = null,
    val netWorth: Long = 0,
    val netWorthTrend: List<Long> = emptyList(),
    val accounts: List<TrxAccount> = emptyList(),
    val budgets: List<ActiveBudget> = emptyList(),
    val trxs: List<Trx> = emptyList(),
    val showBalance: Boolean = false,
) {
    val netWorthFormatted = if (showBalance) netWorth.toRupiah() else "****"
}

fun TrxAccount.balanceFormatted(show: Boolean) = if (show) {
    val displayAmount = when (this) {
        is TrxAccount.Credit -> -currentAmount
        is TrxAccount.Regular -> currentAmount
    }
    displayAmount.toRupiah()
} else "****"

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val appEventBus: AppEventBus,
    private val accountRepository: AccountRepository,
    private val trxRepository: TrxRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appEventBus.events
                .filterIsInstance<AppEvent.TrxChanged>()
                .debounce(150.milliseconds)
                .collect {
                    if (_uiState.value.loadStatus is Loading) return@collect
                    val month = _uiState.value.month ?: return@collect
                    load(month)
                }
        }
        viewModelScope.launch {
            appEventBus.events
                .filterIsInstance<AppEvent.AccountChanged>()
                .debounce(150.milliseconds)
                .collect {
                    if (_uiState.value.loadStatus is Loading) return@collect
                    val month = _uiState.value.month ?: return@collect
                    load(month)
                }
        }
    }

    fun load(month: YearMonth) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(loadStatus = Loading, trxs = listOf())
                }
                budgetRepository.ensureBudgetsExist(month)

                val startMonth = month.minus(11, DateTimeUnit.MONTH)
                val fullRange = generateSequence(startMonth) { it.plus(1, DateTimeUnit.MONTH) }
                    .takeWhile { it <= month }
                    .toList()

                val accounts = accountRepository.getAllTrxAccounts()
                val netWorthTrend = accountRepository.getNetWorthHistory(fullRange)

                val netWorth = netWorthTrend.lastOrNull() ?: 0L
                val trxs = trxRepository.getFilteredTrxs(TrxFilter(month = month))

                val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val currentMonth = YearMonth(currentDate.year, currentDate.month)
                val daysInMonth = currentMonth.days.size
                val dayOfMonth = currentDate.day
                val remainingDays = daysInMonth - dayOfMonth + 1

                val budgets = budgetRepository.getBudgetsByYearMonth(month)
                    .map { budget ->
                        ActiveBudget(
                            budget = budget,
                            remainingDays = remainingDays,
                            dailyAllowance = if (remainingDays > 0) {
                                (budget.remainingAmount.coerceAtLeast(0)
                                    .toDouble() / remainingDays.toLong()).roundToLong()
                            } else {
                                0L
                            }
                        )
                    }
                _uiState.update {
                    it.copy(
                        loadStatus = Success(Unit),
                        month = month,
                        netWorth = netWorth,
                        netWorthTrend = netWorthTrend,
                        accounts = accounts,
                        budgets = budgets,
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