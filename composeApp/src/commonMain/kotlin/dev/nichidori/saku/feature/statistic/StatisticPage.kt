package dev.nichidori.saku.feature.statistic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyNoData
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import kotlinx.datetime.until

@Composable
fun StatisticPage(
    initialMonth: YearMonth,
    viewModel: StatisticViewModel,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    val base = YearMonth(1970, 1)
    val pagerState = rememberPagerState(
        initialPage = base.until(initialMonth, unit = DateTimeUnit.MONTH).toInt(),
        pageCount = { Int.MAX_VALUE }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val month = base.plus(page, unit = DateTimeUnit.MONTH)
            viewModel.load(month = month)
            onMonthChange(month)
        }
    }

    StatisticPageContent(
        uiState = uiState,
        pagerState = pagerState,
        modifier = modifier
    )
}


@Composable
fun StatisticPageContent(
    uiState: StatisticUiState,
    pagerState: PagerState,
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
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.weight(1f)
            ) {
                when (uiState.loadStatus) {
                    is Success<*>, is Failure<*> -> {
                        val (trxsOfCategory, maxAmount) = if (selectedType == TrxType.Income) {
                            Pair(uiState.incomesOfCategory, uiState.totalIncome)
                        } else {
                            Pair(uiState.expensesOfCategory, uiState.totalExpense)
                        }
                        if (trxsOfCategory.isNotEmpty()) {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(trxsOfCategory.entries.toList()) { (category, amount) ->
                                    CategoryItem(
                                        category = category,
                                        amount = amount,
                                        maxAmount = maxAmount
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

@Composable
fun CategoryItem(
    category: Category,
    amount: Long,
    maxAmount: Long,
    modifier: Modifier = Modifier
) {
    var fraction by remember { mutableFloatStateOf(0f) }
    val target = if (maxAmount > 0) amount / maxAmount.toFloat() else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 500)
    )

    LaunchedEffect(target) {
        delay(200)
        fraction = target
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
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
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MyDefaultShape
                    )
            )
        }
        Column(
            modifier = Modifier.padding(top = 16.dp).padding(horizontal = 16.dp)
        ) {
            Text(category.name, style = MaterialTheme.typography.labelSmall)
            Text(amount.toRupiah(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}