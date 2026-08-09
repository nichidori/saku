package dev.nichidori.saku.feature.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.event.AppEvent
import dev.nichidori.saku.core.event.AppEventBus
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.affectedMonths
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.*
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import kotlin.time.Duration.Companion.milliseconds

enum class StatisticGroupBy { Category, Account, AccountType }

sealed interface StatisticItemKey {
    data class ByCategory(val category: Category) : StatisticItemKey
    data class ByAccount(val account: TrxAccount) : StatisticItemKey
    data class ByAccountType(val type: AccountType) : StatisticItemKey
}

data class StatisticUiState(
    val stateByMonth: Map<YearMonth, MonthlyState> = emptyMap(),
    val groupBy: StatisticGroupBy = StatisticGroupBy.Category,
) {
    data class MonthlyState(
        val loadStatus: Status<Unit, Exception> = Initial,
        val incomesOfCategory: Map<Category, Long> = emptyMap(),
        val expensesOfCategory: Map<Category, Long> = emptyMap(),
        val incomesOfAccount: Map<TrxAccount, Long> = emptyMap(),
        val expensesOfAccount: Map<TrxAccount, Long> = emptyMap(),
        val incomesOfAccountType: Map<AccountType, Long> = emptyMap(),
        val expensesOfAccountType: Map<AccountType, Long> = emptyMap(),
        val expandedItemKey: StatisticItemKey? = null,
        val trxsStatusByItemKey: Map<StatisticItemKey, Status<List<Trx>, Exception>> = emptyMap(),
    ) {
        val totalIncome: Long = incomesOfCategory.values.sum()
        val totalExpense: Long = expensesOfCategory.values.sum()
    }
}

@OptIn(FlowPreview::class)
class StatisticViewModel(
    private val appEventBus: AppEventBus,
    private val trxRepository: TrxRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState: StateFlow<StatisticUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appEventBus.events
                .filterIsInstance<AppEvent.TrxChanged>()
                .debounce(150.milliseconds)
                .collect { event ->
                    event.affectedMonths()
                        .filter { it in _uiState.value.stateByMonth.keys }
                        .forEach { month -> load(month) }
                }
        }
    }

    fun setGroupBy(groupBy: StatisticGroupBy) {
        _uiState.update { it.copy(groupBy = groupBy) }
    }

    fun load(month: YearMonth) {
        viewModelScope.launch {
            try {
                updateMonthlyState(month) {
                    it.copy(loadStatus = Loading)
                }

                val incomes = trxRepository
                    .getFilteredTrxs(TrxFilter(month = month, type = TrxType.Income))
                val expenses = trxRepository
                    .getFilteredTrxs(TrxFilter(month = month, type = TrxType.Expense, excludeInstallmentCharges = true))

                val incomesOfCategory = incomes
                    .filter { it.category != null }
                    .groupBy({ it.category!! }, { it.amount })
                    .mapValues { it.value.sum() }
                val expensesOfCategory = expenses
                    .filter { it.category != null }
                    .groupBy({ it.category!! }, { it.amount })
                    .mapValues { it.value.sum() }

                val incomesOfAccount = incomes
                    .groupBy({ it.sourceAccount }, { it.amount })
                    .mapValues { it.value.sum() }
                val expensesOfAccount = expenses
                    .groupBy({ it.sourceAccount }, { it.amount })
                    .mapValues { it.value.sum() }

                val incomesOfAccountType = incomes
                    .map { trx ->
                        val type = when (val account = trx.sourceAccount) {
                            is TrxAccount.Regular -> account.account.type
                            is TrxAccount.Credit -> AccountType.Credit
                        }
                        type to trx.amount
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.sum() }
                val expensesOfAccountType = expenses
                    .map { trx ->
                        val type = when (val account = trx.sourceAccount) {
                            is TrxAccount.Regular -> account.account.type
                            is TrxAccount.Credit -> AccountType.Credit
                        }
                        type to trx.amount
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.sum() }

                _uiState.update { currentState ->
                    val currentMonthlyState = currentState.stateByMonth[month] ?: StatisticUiState.MonthlyState()
                    currentState.copy(
                        stateByMonth = currentState.stateByMonth + (month to currentMonthlyState.copy(
                            loadStatus = Success(Unit),
                            incomesOfCategory = incomesOfCategory
                                .toSortedMap(compareByDescending { c -> incomesOfCategory[c] }),
                            expensesOfCategory = expensesOfCategory
                                .toSortedMap(compareByDescending { c -> expensesOfCategory[c] }),
                            incomesOfAccount = incomesOfAccount
                                .toSortedMap(compareByDescending { a -> incomesOfAccount[a] }),
                            expensesOfAccount = expensesOfAccount
                                .toSortedMap(compareByDescending { a -> expensesOfAccount[a] }),
                            incomesOfAccountType = incomesOfAccountType
                                .toSortedMap(compareByDescending { at -> incomesOfAccountType[at] }),
                            expensesOfAccountType = expensesOfAccountType
                                .toSortedMap(compareByDescending { at -> expensesOfAccountType[at] }),
                        ))
                    )
                }
            } catch (e: Exception) {
                this@StatisticViewModel.log(e)
                updateMonthlyState(month) {
                    it.copy(loadStatus = Failure(e))
                }
            }
        }
    }

    fun onItemExpand(month: YearMonth, itemKey: StatisticItemKey, type: TrxType) {
        viewModelScope.launch {
            val filter = when (itemKey) {
                is StatisticItemKey.ByCategory -> TrxFilter(
                    month = month,
                    type = type,
                    categoryId = itemKey.category.id
                )
                is StatisticItemKey.ByAccount -> TrxFilter(
                    month = month,
                    type = type,
                    accountId = itemKey.account.id
                )
                is StatisticItemKey.ByAccountType -> TrxFilter(
                    month = month,
                    type = type,
                    accountType = itemKey.type
                )
            }

            updateMonthlyState(month) {
                it.copy(expandedItemKey = itemKey)
            }

            try {
                val trxs = trxRepository.getFilteredTrxs(filter)
                updateMonthlyState(month) {
                    it.copy(trxsStatusByItemKey = it.trxsStatusByItemKey + (itemKey to Success(trxs)))
                }
            } catch (e: Exception) {
                this@StatisticViewModel.log(e)
                updateMonthlyState(month) {
                    it.copy(trxsStatusByItemKey = it.trxsStatusByItemKey + (itemKey to Failure(e)))
                }
            }
        }
    }

    fun onItemCollapse(month: YearMonth) {
        updateMonthlyState(month) {
            it.copy(expandedItemKey = null)
        }
    }

    private fun updateMonthlyState(
        month: YearMonth,
        transform: (StatisticUiState.MonthlyState) -> StatisticUiState.MonthlyState
    ) {
        _uiState.update { currentState ->
            val currentMonthState = currentState.stateByMonth[month] ?: StatisticUiState.MonthlyState()
            currentState.copy(
                stateByMonth = currentState.stateByMonth + (month to transform(currentMonthState))
            )
        }
    }
}
