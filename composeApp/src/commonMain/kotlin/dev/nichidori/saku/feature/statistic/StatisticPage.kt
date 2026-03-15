package dev.nichidori.saku.feature.statistic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SlidersHorizontal
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.model.toPickerIcon
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import kotlinx.datetime.until
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

    if (showGroupByOptions) {
        ModalBottomSheet(
            onDismissRequest = { showGroupByOptions = false },
            shape = MyDefaultShape.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
            sheetState = sheetState
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
                                color = if (selected) MaterialTheme.colorScheme.secondary
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
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary
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

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .height(60.dp)
            ) {
                Text(
                    "Statistic",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                MyIconButton(onClick = { showGroupByOptions = true }) {
                    Icon(
                        imageVector = Lucide.SlidersHorizontal,
                        contentDescription = "Group By",
                        modifier = Modifier.size(20.dp),
                    )
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
                onMonthSelect = onMonthChange,
                listState = monthChipsListState
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatisticPageContent(
                uiState = uiState,
                pagerState = pagerState,
                selectedMonth = initialMonth,
                earliestMonth = earliestMonth,
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (type == TrxType.Income) "Income" else "Expense",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(text = if (type == TrxType.Income) uiState.totalIncome.toRupiah() else uiState.totalExpense.toRupiah())
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 0,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageMonth = earliestMonth.plus(page, unit = DateTimeUnit.MONTH)
                if (pageMonth == selectedMonth) {
                    when (uiState.loadStatus) {
                        is Success<*>, is Failure<*> -> {
                            val animatedCategories = remember(uiState.loadStatus, uiState.groupBy, selectedType) {
                                mutableStateMapOf<String, Boolean>()
                            }

                            val (items, maxAmount) = remember(uiState, selectedType) {
                                when (uiState.groupBy) {
                                    StatisticGroupBy.Category -> if (selectedType == TrxType.Income) {
                                        Pair(
                                            uiState.incomesOfCategory.entries.toList(),
                                            uiState.totalIncome
                                        )
                                    } else {
                                        Pair(
                                            uiState.expensesOfCategory.entries.toList(),
                                            uiState.totalExpense
                                        )
                                    }

                                    StatisticGroupBy.Account -> if (selectedType == TrxType.Income) {
                                        Pair(
                                            uiState.incomesOfAccount.entries.toList(),
                                            uiState.totalIncome
                                        )
                                    } else {
                                        Pair(
                                            uiState.expensesOfAccount.entries.toList(),
                                            uiState.totalExpense
                                        )
                                    }

                                    StatisticGroupBy.AccountType -> if (selectedType == TrxType.Income) {
                                        Pair(
                                            uiState.incomesOfAccountType.entries.toList(),
                                            uiState.totalIncome
                                        )
                                    } else {
                                        Pair(
                                            uiState.expensesOfAccountType.entries.toList(),
                                            uiState.totalExpense
                                        )
                                    }
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
                                    items(items, key = { it.key.toString() }) { (item, amount) ->
                                        val (name, icon) = when (item) {
                                            is Category -> item.name to item.icon.toPickerIcon()?.icon
                                            is Account -> item.name to null
                                            is AccountType -> item.label() to null
                                            else -> "" to null
                                        }

                                        StatisticItem(
                                            name = name,
                                            icon = icon,
                                            amount = amount,
                                            maxAmount = maxAmount,
                                            hasAnimated = animatedCategories[item.toString()] == true,
                                            onAnimationComplete = {
                                                animatedCategories[item.toString()] = true
                                            }
                                        )
                                    }
                                }
                            } else {
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
}

@Composable
fun StatisticItem(
    name: String,
    icon: ImageVector?,
    amount: Long,
    maxAmount: Long,
    hasAnimated: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val target = if (maxAmount > 0) amount / maxAmount.toFloat() else 0f
    var animationTarget by remember { mutableFloatStateOf(if (hasAnimated) target else 0f) }

    val animatedFraction by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 500)
    )

    LaunchedEffect(target, hasAnimated) {
        if (!hasAnimated) {
            animationTarget = target
            onAnimationComplete()
        } else {
            animationTarget = target
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
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
                    name.split(' ').take(2).joinToString("") {
                        it.firstOrNull()?.toString() ?: ""
                    },
                    style = MaterialTheme.typography.titleSmall,
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
}