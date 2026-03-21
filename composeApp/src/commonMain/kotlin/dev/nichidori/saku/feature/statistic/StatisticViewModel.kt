package dev.nichidori.saku.feature.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.*
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.TrxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth

enum class StatisticGroupBy { Category, Account, AccountType }

sealed interface StatisticItemKey {
    data class ByCategory(val category: Category) : StatisticItemKey
    data class ByAccount(val account: Account) : StatisticItemKey
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
        val incomesOfAccount: Map<Account, Long> = emptyMap(),
        val expensesOfAccount: Map<Account, Long> = emptyMap(),
        val incomesOfAccountType: Map<AccountType, Long> = emptyMap(),
        val expensesOfAccountType: Map<AccountType, Long> = emptyMap(),
        val expandedItemKey: StatisticItemKey? = null,
        val trxsStatusByItemKey: Map<StatisticItemKey, Status<List<Trx>, Exception>> = emptyMap(),
    ) {
        val totalIncome: Long = incomesOfCategory.values.sum()
        val totalExpense: Long = expensesOfCategory.values.sum()
    }
}

class StatisticViewModel(
    private val trxRepository: TrxRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState: StateFlow<StatisticUiState> = _uiState.asStateFlow()

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
                    .getFilteredTrxs(TrxFilter(month = month, type = TrxType.Expense))

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
                    .groupBy({ it.sourceAccount.type }, { it.amount })
                    .mapValues { it.value.sum() }
                val expensesOfAccountType = expenses
                    .groupBy({ it.sourceAccount.type }, { it.amount })
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

    fun onItemExpand(month: YearMonth, itemKey: StatisticItemKey) {
        viewModelScope.launch {
            val filter = when (itemKey) {
                is StatisticItemKey.ByCategory -> TrxFilter(
                    month = month,
                    categoryId = itemKey.category.id
                )
                is StatisticItemKey.ByAccount -> TrxFilter(
                    month = month,
                    accountId = itemKey.account.id
                )
                is StatisticItemKey.ByAccountType -> TrxFilter(
                    month = month
                )
            }

            updateMonthlyState(month) {
                it.copy(
                    expandedItemKey = itemKey,
                    trxsStatusByItemKey = it.trxsStatusByItemKey + (itemKey to Loading),
                )
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
