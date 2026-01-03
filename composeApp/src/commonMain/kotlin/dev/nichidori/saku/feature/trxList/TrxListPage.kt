package dev.nichidori.saku.feature.trxList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Filter
import com.composables.icons.lucide.Lucide
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyMonthChipRow
import dev.nichidori.saku.core.composable.MyNoData
import dev.nichidori.saku.core.composable.label
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.format
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.Trx
import kotlinx.datetime.*
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlin.math.absoluteValue
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrxListPage(
    initialMonth: YearMonth,
    viewModel: TrxListViewModel,
    onMonthChange: (YearMonth) -> Unit,
    onTrxClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterOption by remember { mutableStateOf(false) }

    val earliestMonth = YearMonth(2025, 1)
    val currentMonth = Clock.System.now().toYearMonth()
    val pagerState = rememberPagerState(
        initialPage = earliestMonth.until(initialMonth, unit = DateTimeUnit.MONTH).toInt(),
        pageCount = { earliestMonth.until(currentMonth, unit = DateTimeUnit.MONTH).toInt() + 1 }
    )

    LaunchedEffect(initialMonth) {
        val page = earliestMonth.until(initialMonth, unit = DateTimeUnit.MONTH).toInt()
        if (pagerState.currentPage != page) {
            pagerState.scrollToPage(page)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val month = earliestMonth.plus(page, unit = DateTimeUnit.MONTH)
            viewModel.loadTrxs(month = month)
            onMonthChange(month)
        }
    }

    if (showFilterOption) {
        ModalBottomSheet(
            onDismissRequest = { showFilterOption = false },
            sheetState = sheetState,
            shape = MyDefaultShape.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        ) {
            var selectedAccounts by remember { mutableStateOf(uiState.filterAccounts) }
            var selectedAccountTypes by remember { mutableStateOf(uiState.filterAccountTypes) }
            var selectedCategories by remember { mutableStateOf(uiState.filterCategories) }

            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = {
                            selectedAccounts = emptySet()
                            selectedAccountTypes = emptySet()
                            selectedCategories = emptySet()
                        }
                    ) {
                        Text("Reset")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Account
                Text(
                    "Account",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    uiState.accounts.forEach {
                        val selected = selectedAccounts.contains(it)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedAccounts = if (selected) {
                                    selectedAccounts - it
                                } else {
                                    selectedAccounts + it
                                }
                            },
                            label = {
                                Text(it.name)
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Account type
                Text(
                    "Account Type",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    uiState.accountTypes.forEach {
                        val selected = selectedAccountTypes.contains(it)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedAccountTypes = if (selected) {
                                    selectedAccountTypes - it
                                } else {
                                    selectedAccountTypes + it
                                }
                            },
                            label = {
                                Text(it.label())
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Category
                Text(
                    "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    uiState.categories.forEach {
                        val selected = selectedCategories.contains(it)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedCategories = if (selected) {
                                    selectedCategories - it
                                } else {
                                    selectedCategories + it
                                }
                            },
                            label = {
                                Text(it.name)
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.applyFilters(
                            accounts = selectedAccounts,
                            accountTypes = selectedAccountTypes,
                            categories = selectedCategories
                        )
                        showFilterOption = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .height(60.dp)
            ) {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = {
                        showFilterOption = true
                        viewModel.loadAccounts()
                        viewModel.loadCategories()
                    }
                ) {
                    Box(modifier = Modifier.padding(2.dp)) {
                        Icon(
                            imageVector = Lucide.Filter,
                            contentDescription = "Filter transactions"
                        )

                        if (uiState.hasFilter) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .padding(2.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier,
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            MyMonthChipRow(
                selectedMonth = initialMonth,
                earliestMonth = earliestMonth,
                latestMonth = currentMonth,
                onMonthSelect = onMonthChange
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageMonth = earliestMonth.plus(page, unit = DateTimeUnit.MONTH)
                TrxListContent(
                    uiState = uiState.stateByMonth[pageMonth] ?: TrxListUiState.MonthlyState(),
                    onTrxClick = onTrxClick,
                )
            }
        }
    }
}

@Composable
fun TrxListContent(
    uiState: TrxListUiState.MonthlyState,
    onTrxClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.trxRecordsByDate.isEmpty() && uiState.loadStatus.isCompleted) {
        MyNoData(
            message = "No transactions yet",
            contentDescription = "No transactions",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            for ((index, entry) in uiState.trxRecordsByDate.entries.withIndex()) {
                val (date, record) = entry
                item {
                    Column {
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                date.format(
                                    LocalDate.Format {
                                        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                                        chars(", ")
                                        day(padding = Padding.NONE)
                                        chars(" ")
                                        monthName(MonthNames.ENGLISH_ABBREVIATED)
                                    }
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    top = if (index > 0) 12.dp else 0.dp,
                                    bottom = 8.dp
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (record.totalIncome.absoluteValue > 0) Text(
                                record.totalIncome.absoluteValue.toRupiah(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .padding(
                                        top = if (index > 0) 12.dp else 0.dp,
                                        bottom = 8.dp
                                    )
                            )
                            if (record.totalExpense.absoluteValue > 0) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    record.totalExpense.absoluteValue.toRupiah(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .padding(
                                            top = if (index > 0) 12.dp else 0.dp,
                                            bottom = 8.dp
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                    }
                }
                items(record.trxs) { trx ->
                    TrxCard(trx = trx, onClick = onTrxClick)
                }
            }
        }
    }
}

@Composable
fun TrxCard(trx: Trx, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(trx.id) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MyDefaultShape
                )
                .wrapContentSize()
        ) {
            Text(
                when (trx) {
                    is Trx.Transfer -> "T"
                    else -> trx.category?.name?.split(' ')?.take(2)?.joinToString("") {
                        it.firstOrNull()?.toString() ?: ""
                    } ?: ""
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            val accountInfo = when (trx) {
                is Trx.Transfer -> "${trx.sourceAccount.name} →\t ${trx.targetAccount.name}"
                else -> trx.sourceAccount.name
            }
            val primaryText = trx.description.ifBlank { accountInfo }
            val secondaryText = if (trx.description.isBlank()) null else accountInfo
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleSmall
            )
            secondaryText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                trx.amount.toRupiah(),
                style = MaterialTheme.typography.titleSmall,
                color = when (trx) {
                    is Trx.Income -> MaterialTheme.colorScheme.primary
                    is Trx.Expense -> MaterialTheme.colorScheme.error
                    is Trx.Transfer -> MaterialTheme.colorScheme.onBackground
                }
            )
            Text(
                trx.transactionAt.format(LocalDateTime.Format {
                    hour()
                    chars(":")
                    minute()
                }),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
