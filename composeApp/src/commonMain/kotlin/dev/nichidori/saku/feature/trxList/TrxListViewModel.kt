package dev.nichidori.saku.feature.trxList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.util.log
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxFilter
import dev.nichidori.saku.domain.model.TrxType
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.CategoryRepository
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
    val stateByMonth: Map<YearMonth, MonthlyState> = emptyMap(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filterAccountIds: Set<String> = emptySet(),
    val filterAccountTypes: Set<AccountType> = emptySet(),
    val filterCategoryIds: Set<String> = emptySet(),
    val filterTrxTypes: Set<TrxType> = emptySet(),
) {
    val accountTypes: Set<AccountType> = AccountType.entries.toSet()
    val trxTypes: Set<TrxType> = TrxType.entries.toSet()
    val hasFilter: Boolean = filterAccountIds.isNotEmpty()
            || filterAccountTypes.isNotEmpty()
            || filterCategoryIds.isNotEmpty()
            || filterTrxTypes.isNotEmpty()

    data class MonthlyState(
        val loadStatus: Status<Unit, Exception> = Initial,
        val rawTrxs: List<Trx> = emptyList(),
        val trxRecordsByDate: Map<LocalDate, DailyTrxRecord> = emptyMap(),
    )
}

class TrxListViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val trxRepository: TrxRepository,
) : ViewModel() {
    init {
        loadAccounts()
        loadCategories()
    }

    private val _uiState = MutableStateFlow(TrxListUiState())
    val uiState: StateFlow<TrxListUiState> = _uiState.asStateFlow()

    fun loadTrxs(month: YearMonth) {
        viewModelScope.launch {
            try {
                updateMonthlyState(month) {
                    it.copy(loadStatus = Loading)
                }

                val trxs = trxRepository.getFilteredTrxs(TrxFilter(month = month))

                _uiState.update { currentState ->
                    val filteredRecords = filterAndGroupTransactions(
                        trxs = trxs,
                        accountIds = currentState.filterAccountIds,
                        accountTypes = currentState.filterAccountTypes,
                        categoryIds = currentState.filterCategoryIds,
                        trxTypes = currentState.filterTrxTypes,
                    )

                    val currentMonthlyState = currentState.stateByMonth[month] ?: TrxListUiState.MonthlyState()
                    currentState.copy(
                        stateByMonth = currentState.stateByMonth + (month to currentMonthlyState.copy(
                            loadStatus = Success(Unit),
                            rawTrxs = trxs,
                            trxRecordsByDate = filteredRecords
                        ))
                    )
                }
            } catch (e: Exception) {
                log(e)
                updateMonthlyState(month) {
                    it.copy(loadStatus = Failure(e))
                }
            }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            try {
                val accounts = accountRepository.getAllAccounts()

                _uiState.update {
                    it.copy(accounts = accounts)
                }
            } catch (e: Exception) {
                this@TrxListViewModel.log(e)
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = categoryRepository.getAllCategories()

                _uiState.update {
                    it.copy(categories = categories)
                }
            } catch (e: Exception) {
                this@TrxListViewModel.log(e)
            }
        }
    }

    fun applyFilters(
        accountIds: Set<String>,
        categoryIds: Set<String>,
        accountTypes: Set<AccountType>,
        trxTypes: Set<TrxType>,
    ) {
        _uiState.update {
            val newState = it.copy(
                filterAccountIds = accountIds,
                filterAccountTypes = accountTypes,
                filterCategoryIds = categoryIds,
                filterTrxTypes = trxTypes,
            )

            // Re-process every month currently in the state using the new filters
            val stateByMonth = newState.stateByMonth.mapValues { (_, monthlyState) ->
                filterAndGroupTransactions(
                    trxs = monthlyState.rawTrxs,
                    accountIds = accountIds,
                    accountTypes = accountTypes,
                    categoryIds = categoryIds,
                    trxTypes = trxTypes,
                ).let { filteredRecords ->
                    monthlyState.copy(trxRecordsByDate = filteredRecords)
                }
            }

            newState.copy(stateByMonth = stateByMonth)
        }
    }

    private fun filterAndGroupTransactions(
        trxs: List<Trx>,
        accountIds: Set<String>,
        accountTypes: Set<AccountType>,
        categoryIds: Set<String>,
        trxTypes: Set<TrxType>,
    ): Map<LocalDate, DailyTrxRecord> {
        return trxs.filter { trx ->
            val matchAccount = accountIds.isEmpty()
                    || accountIds.contains(trx.sourceAccount.id)
                    || (trx as? Trx.Transfer)?.let { accountIds.contains(it.targetAccount.id) } ?: false
            val matchCategory = categoryIds.isEmpty()
                    || trx.category?.let { categoryIds.contains(it.id) } ?: false
            val matchAccountType = accountTypes.isEmpty()
                    || accountTypes.contains(trx.sourceAccount.type)
                    || (trx as? Trx.Transfer)?.let { accountTypes.contains(it.targetAccount.type) } ?: false
            val matchTrxType = trxTypes.isEmpty() || trxTypes.contains(trx.type)

            matchAccount && matchCategory && matchAccountType && matchTrxType
        }.groupBy { trx ->
            trx.transactionAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        }.mapValues { (_, dailyTrxs) ->
            DailyTrxRecord(
                trxs = dailyTrxs,
                totalIncome = dailyTrxs.filterIsInstance<Trx.Income>().sumOf { it.amount },
                totalExpense = dailyTrxs.filterIsInstance<Trx.Expense>().sumOf { it.amount },
            )
        }
    }

    private fun updateMonthlyState(
        month: YearMonth,
        transform: (TrxListUiState.MonthlyState) -> TrxListUiState.MonthlyState
    ) {
        _uiState.update { currentState ->
            val currentMonthState = currentState.stateByMonth[month] ?: TrxListUiState.MonthlyState()
            currentState.copy(
                stateByMonth = currentState.stateByMonth + (month to transform(currentMonthState))
            )
        }
    }
}