package dev.nichidori.saku.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.Budget
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.BudgetRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.math.roundToLong
import kotlin.time.Clock

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
    val accounts: List<Account> = emptyList(),
    val budgets: List<ActiveBudget> = emptyList(),
    val trxs: List<Trx> = emptyList(),
    val monthlyBalancesByAccount: Map<String, List<Long>> = emptyMap(),
    val showBalance: Boolean = false,
) {
    val netWorthFormatted = if (showBalance) netWorth.toRupiah() else "****"
    val accountAndTrends = accounts.map { Pair(it, monthlyBalancesByAccount[it.id] ?: listOf()) }
}

fun Account.balanceFormatted(show: Boolean) = if (show) currentAmount.toRupiah() else "****"

class HomeViewModel(
    private val accountRepository: AccountRepository,
    private val trxRepository: TrxRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load(month: YearMonth) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(loadStatus = Loading, trxs = listOf())
                }
                budgetRepository.ensureBudgetsExist(month)
                accountRepository.ensureMonthlyBalancesExist(month)

                // Get the latest 12 months trend for net worth and account balances
                val startMonth = month.minus(11, DateTimeUnit.MONTH)
                val fullRange = generateSequence(startMonth) { it.plus(1, DateTimeUnit.MONTH) }
                    .takeWhile { it <= month }
                    .toList()

                val netWorthHistory = accountRepository.getNetWorthHistory(startMonth, month)
                val netWorthTrend = fullRange.map { netWorthHistory[it] ?: 0L }

                val accounts = accountRepository.getAllAccounts()
                val monthlyBalancesByAccount = accounts.associate { account ->
                    val history = accountRepository.getAccountBalanceHistory(
                        accountId = account.id,
                        startMonth = startMonth,
                        endMonth = month,
                    ).associateBy { it.yearMonth }

                    account.id to fullRange.map { history[it]?.balance ?: 0L }
                }

                val netWorth = accountRepository.getNetWorthByMonth(month)
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
                        monthlyBalancesByAccount = monthlyBalancesByAccount,
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