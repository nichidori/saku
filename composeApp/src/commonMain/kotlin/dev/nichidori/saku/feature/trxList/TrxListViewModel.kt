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
    val filterAccounts: Set<Account> = emptySet(),
    val filterAccountTypes: Set<AccountType> = emptySet(),
    val filterCategories: Set<Category> = emptySet(),
) {
    val accountTypes: Set<AccountType> = AccountType.entries.toSet()
    val hasFilter: Boolean = filterAccounts.isNotEmpty()
            || filterAccountTypes.isNotEmpty()
            || filterCategories.isNotEmpty()

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
                        accounts = currentState.filterAccounts,
                        accountTypes = currentState.filterAccountTypes,
                        categories = currentState.filterCategories,
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
        accounts: Set<Account>,
        categories: Set<Category>,
        accountTypes: Set<AccountType>
    ) {
        _uiState.update {
            val newState = it.copy(
                filterAccounts = accounts,
                filterAccountTypes = accountTypes,
                filterCategories = categories,
            )

            // Re-process every month currently in the state using the new filters
            val stateByMonth = newState.stateByMonth.mapValues { (_, monthlyState) ->
                filterAndGroupTransactions(
                    trxs = monthlyState.rawTrxs,
                    accounts = accounts,
                    accountTypes = accountTypes,
                    categories = categories,
                ).let { filteredRecords ->
                    monthlyState.copy(trxRecordsByDate = filteredRecords)
                }
            }

            newState.copy(stateByMonth = stateByMonth)
        }
    }

    private fun filterAndGroupTransactions(
        trxs: List<Trx>,
        accounts: Set<Account>,
        accountTypes: Set<AccountType>,
        categories: Set<Category>,
    ): Map<LocalDate, DailyTrxRecord> {
        return trxs.filter { trx ->
            val matchAccount = accounts.isEmpty()
                    || accounts.contains(trx.sourceAccount)
                    || (trx as? Trx.Transfer)?.let { accounts.contains(it.targetAccount) } ?: false
            val matchCategory = categories.isEmpty()
                    || categories.contains(trx.category)
            val matchType = accountTypes.isEmpty()
                    || accountTypes.contains(trx.sourceAccount.type)
                    || (trx as? Trx.Transfer)?.let { accountTypes.contains(it.targetAccount.type) } ?: false

            matchAccount && matchCategory && matchType
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