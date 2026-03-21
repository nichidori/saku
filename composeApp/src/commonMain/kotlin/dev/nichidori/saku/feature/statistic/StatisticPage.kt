package dev.nichidori.saku.feature.statistic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SlidersHorizontal
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.Status.*
import dev.nichidori.saku.core.model.toPickerIcon
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.format
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.datetime.*
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.Padding
import kotlin.time.Clock

fun StatisticGroupBy.label(): String {
    return when (this) {
        StatisticGroupBy.Category -> "Category"
        StatisticGroupBy.Account -> "Account"
        StatisticGroupBy.AccountType -> "Account Type"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticPage(
    initialMonth: YearMonth,
    viewModel: StatisticViewModel,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    monthChipsListState: LazyListState = rememberLazyListState(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()
    var showGroupByOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    if (showGroupByOptions) {
        ModalBottomSheet(
            onDismissRequest = { showGroupByOptions = false },
            shape = MyDefaultShape.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Text(
                    "Group by",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                )
                StatisticGroupBy.entries.forEach {
                    val selected = it == uiState.groupBy
                    MyBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = MyDefaultShape
                            )
                            .clip(MyDefaultShape)
                            .clickable {
                                viewModel.setGroupBy(it)
                                showGroupByOptions = false
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                it.label(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    val earliestMonth = YearMonth(2025, 1)
    val currentMonth = Clock.System.now().toYearMonth()
    val totalPageCount = remember(earliestMonth, currentMonth) {
        earliestMonth.until(currentMonth, unit = DateTimeUnit.MONTH).toInt() + 1
    }

    val pagerState = rememberPagerState(
        initialPage = earliestMonth.until(initialMonth, unit = DateTimeUnit.MONTH).toInt(),
        pageCount = { totalPageCount }
    )

    LaunchedEffect(initialMonth) {
        val page = earliestMonth.until(initialMonth, unit = DateTimeUnit.MONTH).toInt()
        if (pagerState.currentPage != page) {
            pagerState.scrollToPage(page)
        } else {
            viewModel.load(month = initialMonth)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val month = earliestMonth.plus(page, unit = DateTimeUnit.MONTH)
            viewModel.load(month = month)
            onMonthChange(month)
        }
    }

    LaunchedEffect(uiState.groupBy) {
        viewModel.onItemCollapse(initialMonth)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Statistic",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = { showGroupByOptions = true }) {
                        Icon(
                            imageVector = Lucide.SlidersHorizontal,
                            contentDescription = "Group By",
                            modifier = Modifier.size(20.dp),
                        )
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
                listState = monthChipsListState
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatisticPageContent(
                uiState = uiState,
                pagerState = pagerState,
                selectedMonth = initialMonth,
                earliestMonth = earliestMonth,
                onItemExpand = viewModel::onItemExpand,
                onItemCollapse = viewModel::onItemCollapse,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatisticPageContent(
    uiState: StatisticUiState,
    pagerState: PagerState,
    selectedMonth: YearMonth,
    earliestMonth: YearMonth,
    onItemExpand: (YearMonth, StatisticItemKey) -> Unit,
    onItemCollapse: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
    ) { contentPadding ->
        var selectedType by remember { mutableStateOf(TrxType.Expense) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(contentPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            MySegmentedControl(
                items = listOf(TrxType.Income, TrxType.Expense),
                selectedItem = selectedType,
                onItemSelection = { selectedType = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { type ->
                val currentMonthlyState = uiState.stateByMonth[selectedMonth] ?: StatisticUiState.MonthlyState()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (type == TrxType.Income) "Income" else "Expense",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(text = if (type == TrxType.Income) currentMonthlyState.totalIncome.toRupiah() else currentMonthlyState.totalExpense.toRupiah())
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val categoryFractions = remember(uiState.groupBy, selectedType) {
                mutableStateMapOf<Int, Float>()
            }
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 0,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageMonth = earliestMonth.plus(page, unit = DateTimeUnit.MONTH)
                val monthlyState = uiState.stateByMonth[pageMonth] ?: StatisticUiState.MonthlyState()

                when (monthlyState.loadStatus) {
                    Loading, is Success<*>, is Failure<*> -> {
                        val (items, maxAmount) = remember(monthlyState, uiState.groupBy, selectedType) {
                            when (uiState.groupBy) {
                                StatisticGroupBy.Category -> if (selectedType == TrxType.Income) {
                                    Pair(
                                        monthlyState.incomesOfCategory.entries.map {
                                            StatisticItemKey.ByCategory(it.key) to it.value
                                        },
                                        monthlyState.totalIncome
                                    )
                                } else {
                                    Pair(
                                        monthlyState.expensesOfCategory.entries.map {
                                            StatisticItemKey.ByCategory(it.key) to it.value
                                        },
                                        monthlyState.totalExpense
                                    )
                                }

                                StatisticGroupBy.Account -> if (selectedType == TrxType.Income) {
                                    Pair(
                                        monthlyState.incomesOfAccount.entries.map {
                                            StatisticItemKey.ByAccount(it.key) to it.value
                                        },
                                        monthlyState.totalIncome
                                    )
                                } else {
                                    Pair(
                                        monthlyState.expensesOfAccount.entries.map {
                                            StatisticItemKey.ByAccount(it.key) to it.value
                                        },
                                        monthlyState.totalExpense
                                    )
                                }

                                StatisticGroupBy.AccountType -> if (selectedType == TrxType.Income) {
                                    Pair(
                                        monthlyState.incomesOfAccountType.entries.map {
                                            StatisticItemKey.ByAccountType(it.key) to it.value
                                        },
                                        monthlyState.totalIncome
                                    )
                                } else {
                                    Pair(
                                        monthlyState.expensesOfAccountType.entries.map {
                                            StatisticItemKey.ByAccountType(it.key) to it.value
                                        },
                                        monthlyState.totalExpense
                                    )
                                }
                            }
                        }

                        LaunchedEffect(items.size) {
                            categoryFractions.keys.filter { it >= items.size }.forEach {
                                categoryFractions.remove(it)
                            }
                        }

                        if (items.isNotEmpty()) {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items,
                                    key = { index, _ -> index }
                                ) { index, (itemKey, amount) ->
                                    val (name, icon) = when (itemKey) {
                                        is StatisticItemKey.ByCategory -> itemKey.category.name to itemKey.category.icon.toPickerIcon()?.icon
                                        is StatisticItemKey.ByAccount -> itemKey.account.name to null
                                        is StatisticItemKey.ByAccountType -> itemKey.type.label() to null
                                    }

                                    val isExpanded = monthlyState.expandedItemKey == itemKey
                                    val trxsStatus = monthlyState.trxsStatusByItemKey[itemKey]

                                    StatisticItem(
                                        name = name,
                                        icon = icon,
                                        amount = amount,
                                        maxAmount = maxAmount,
                                        previousTarget = categoryFractions[index] ?: 0f,
                                        onTargetChange = {
                                            categoryFractions[index] = it
                                        },
                                        expanded = isExpanded,
                                        onExpandToggle = {
                                            if (isExpanded) {
                                                onItemCollapse(pageMonth)
                                            } else {
                                                onItemExpand(pageMonth, itemKey)
                                            }
                                        },
                                        expandedContent = {
                                            when (trxsStatus) {
                                                is Failure -> Text(
                                                    text = "Failed to load transactions",
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                )

                                                is Success -> {
                                                    val transactions = trxsStatus.data
                                                    Column {
                                                        transactions.forEach { trx ->
                                                            StatisticTrxItem(trx = trx)
                                                        }
                                                    }
                                                }

                                                else -> Unit
                                            }
                                        }
                                    )
                                }
                            }
                        } else if (monthlyState.loadStatus !is Loading) {
                            MyNoData(
                                message = "No transactions yet",
                                contentDescription = "No transactions",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun StatisticItem(
    name: String,
    icon: ImageVector?,
    amount: Long,
    maxAmount: Long,
    previousTarget: Float,
    onTargetChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    expandedContent: @Composable ColumnScope.() -> Unit = {},
) {
    val target = if (maxAmount > 0) amount / maxAmount.toFloat() else 0f
    var animationTarget by remember { mutableFloatStateOf(previousTarget) }

    val animatedFraction by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 500)
    )

    LaunchedEffect(target) {
        animationTarget = target
        onTargetChange(target)
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .wrapContentSize()
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        name.firstOrNull()?.toString() ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MyDefaultShape
                    )
                    .clip(shape = MyDefaultShape)
                    .clickable { onExpandToggle() }
            ) {
                if (animatedFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFraction)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = MyDefaultShape
                            )
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            amount.toRupiah(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "${(target * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            expandedContent()
        }
    }
}

@Composable
private fun StatisticTrxItem(trx: Trx, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 56.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val accountInfo = when (trx) {
            is Trx.Transfer -> "${trx.sourceAccount.name} →\t ${trx.targetAccount.name}"
            else -> trx.sourceAccount.name
        }
        val primaryText = trx.description.ifBlank { accountInfo }

        Text(
            text = trx.transactionAt.format(
                LocalDateTime.Format {
                    dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                    chars(" ")
                    day(padding = Padding.NONE)
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = primaryText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = trx.amount.toRupiah(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = when (trx) {
                is Trx.Income -> MaterialTheme.colorScheme.primary
                is Trx.Expense -> MaterialTheme.colorScheme.error
                is Trx.Transfer -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}