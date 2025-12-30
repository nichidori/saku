package dev.nichidori.saku.feature.statistic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyMonthChipRow
import dev.nichidori.saku.core.composable.MyNoData
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import kotlinx.datetime.until
import kotlin.time.Clock

@Composable
fun StatisticPage(
    initialMonth: YearMonth,
    viewModel: StatisticViewModel,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

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

    Column(modifier = modifier) {
        MyMonthChipRow(
            selectedMonth = initialMonth,
            earliestMonth = earliestMonth,
            latestMonth = currentMonth,
            onMonthSelect = onMonthChange
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatisticPageContent(
            uiState = uiState,
            pagerState = pagerState,
            initialMonth = initialMonth,
            earliestMonth = earliestMonth,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatisticPageContent(
    uiState: StatisticUiState,
    pagerState: PagerState,
    initialMonth: YearMonth,
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
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                SegmentedButton(
                    shape = MyDefaultShape.copy(
                        topEnd = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp)
                    ),
                    selected = selectedType == TrxType.Income,
                    onClick = { selectedType = TrxType.Income },
                    icon = {},
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Income", style = MaterialTheme.typography.labelSmall)
                        Text(
                            uiState.totalIncome.toRupiah(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                SegmentedButton(
                    shape = MyDefaultShape.copy(
                        topStart = CornerSize(0.dp),
                        bottomStart = CornerSize(0.dp)
                    ),
                    selected = selectedType == TrxType.Expense,
                    onClick = { selectedType = TrxType.Expense },
                    icon = {},
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Expense", style = MaterialTheme.typography.labelSmall)
                        Text(
                            uiState.totalExpense.toRupiah(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 0,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageMonth = earliestMonth.plus(page, unit = DateTimeUnit.MONTH)
                if (pageMonth == initialMonth) {
                    when (uiState.loadStatus) {
                        is Success<*>, is Failure<*> -> {
                            val animatedCategories = remember(uiState.loadStatus, selectedType) {
                                mutableStateMapOf<String, Boolean>()
                            }

                            val (trxsOfCategory, maxAmount) = if (selectedType == TrxType.Income) {
                                Pair(uiState.incomesOfCategory, uiState.totalIncome)
                            } else {
                                Pair(uiState.expensesOfCategory, uiState.totalExpense)
                            }
                            if (trxsOfCategory.isNotEmpty()) {
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(trxsOfCategory.entries.toList(), key = { it.key.id }) { (category, amount) ->
                                        CategoryItem(
                                            category = category,
                                            amount = amount,
                                            maxAmount = maxAmount,
                                            hasAnimated = animatedCategories[category.id] == true,
                                            onAnimationComplete = {
                                                animatedCategories[category.id] = true
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
fun CategoryItem(
    category: Category,
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

    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MyDefaultShape
                )
                .wrapContentSize()
        ) {
            Text(
                category.name.split(' ').take(2).joinToString("") {
                    it.firstOrNull()?.toString() ?: ""
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MyDefaultShape
                        )
                )
            }
            Column(
                modifier = Modifier.padding(top = 12.dp).padding(horizontal = 12.dp)
            ) {
                Text(category.name, style = MaterialTheme.typography.labelSmall)
                Text(amount.toRupiah(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}