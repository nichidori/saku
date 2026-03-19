package dev.nichidori.saku.feature.trxList

import androidx.compose.foundation.background
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeftRight
import com.composables.icons.lucide.ListFilter
import com.composables.icons.lucide.Lucide
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.toPickerIcon
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.format
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxType
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
    monthChipsListState: LazyListState = rememberLazyListState(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
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
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(
                top = WindowInsets.statusBars.asPaddingValues()
                    .calculateTopPadding()
            )
        ) {
            var selectedAccountIds by remember { mutableStateOf(uiState.filterAccountIds) }
            var selectedAccountTypes by remember { mutableStateOf(uiState.filterAccountTypes) }
            var selectedCategoryIds by remember { mutableStateOf(uiState.filterCategoryIds) }
            var selectedTrxTypes by remember { mutableStateOf(uiState.filterTrxTypes) }

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
                    MyTextButton(
                        text = "Reset",
                        onClick = {
                            selectedAccountIds = emptySet()
                            selectedAccountTypes = emptySet()
                            selectedCategoryIds = emptySet()
                            selectedTrxTypes = emptySet()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    FilterSection(title = "Account") {
                        uiState.accounts.forEach {
                            val selected = selectedAccountIds.contains(it.id)
                            MyFilterChip(
                                selected = selected,
                                onClick = {
                                    selectedAccountIds = if (selected) {
                                        selectedAccountIds - it.id
                                    } else {
                                        selectedAccountIds + it.id
                                    }
                                },
                                label = {
                                    Text(it.name)
                                },
                            )
                        }
                    }

                    FilterSection(title = "Account Type") {
                        uiState.accountTypes.forEach {
                            val selected = selectedAccountTypes.contains(it)
                            MyFilterChip(
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

                    FilterSection(title = "Transaction Type") {
                        uiState.trxTypes.forEach {
                            val selected = selectedTrxTypes.contains(it)
                            MyFilterChip(
                                selected = selected,
                                onClick = {
                                    selectedTrxTypes = if (selected) {
                                        selectedTrxTypes - it
                                    } else {
                                        selectedTrxTypes + it
                                    }
                                },
                                label = {
                                    Text(it.label())
                                },
                            )
                        }
                    }

                    if (uiState.categories.isNotEmpty()) {
                        FilterSection(title = "Category") {
                            uiState.categories.forEach { category ->
                                val selected = selectedCategoryIds.contains(category.id)
                                MyFilterChip(
                                    selected = selected,
                                    onClick = {
                                        val childrenIds = uiState.categories
                                            .filter { it.parent?.id == category.id }
                                            .map { it.id }
                                        val parentId = category.parent?.id

                                        selectedCategoryIds = if (selected) {
                                            var nextSet = selectedCategoryIds - category.id
                                            if (childrenIds.isNotEmpty()) {
                                                nextSet = nextSet - childrenIds.toSet()
                                            }
                                            if (parentId != null) {
                                                nextSet = nextSet - parentId
                                            }
                                            nextSet
                                        } else {
                                            var nextSet = selectedCategoryIds + category.id
                                            if (childrenIds.isNotEmpty()) {
                                                nextSet = nextSet + childrenIds.toSet()
                                            }
                                            nextSet
                                        }
                                    },
                                    label = {
                                        Text(category.name)
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                MyButton(
                    text = "Save",
                    onClick = {
                        viewModel.applyFilters(
                            accountIds = selectedAccountIds,
                            accountTypes = selectedAccountTypes,
                            categoryIds = selectedCategoryIds,
                            trxTypes = selectedTrxTypes
                        )
                        showFilterOption = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                expandedHeight = 48.dp,
                actions = {
                    Box {
                        IconButton(
                            onClick = {
                                showFilterOption = true
                                viewModel.loadAccounts()
                                viewModel.loadCategories()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.ListFilter,
                                contentDescription = "Filter transactions"
                            )
                        }

                        if (uiState.hasFilter) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .padding(2.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            )
                        }
                    }
                }
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Spacer(modifier = Modifier.height(8.dp))
            MyMonthChipRow(
                selectedMonth = initialMonth,
                earliestMonth = earliestMonth,
                latestMonth = currentMonth,
                onMonthSelect = onMonthChange,
                listState = monthChipsListState,
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
                                color = MaterialTheme.colorScheme.onBackground,
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
                .wrapContentSize()
        ) {
            val icon = when (trx) {
                is Trx.Transfer -> Lucide.ArrowLeftRight
                else -> trx.category?.icon.toPickerIcon()?.icon
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (trx is Trx.Transfer) "Transfer" else trx.category?.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = trx.category?.name?.firstOrNull()?.toString() ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
            val secondaryText = if (trx.description.isBlank()) trx.category?.name
            else accountInfo + (trx.category?.name?.let { "  •  $it" } ?: "")
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            secondaryText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
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
                    is Trx.Transfer -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Bold,
            )
            Text(
                trx.transactionAt.format(LocalDateTime.Format {
                    hour()
                    chars(":")
                    minute()
                }),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

fun TrxType.label(): String {
    return when (this) {
        TrxType.Income -> "Income"
        TrxType.Expense -> "Expense"
        TrxType.Transfer -> "Transfer"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
