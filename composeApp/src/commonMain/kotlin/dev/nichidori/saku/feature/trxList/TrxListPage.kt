package dev.nichidori.saku.feature.trxList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ListX
import com.composables.icons.lucide.Lucide
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.format
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Trx
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.plus
import kotlinx.datetime.until

@Composable
fun TrxListPage(
    initialMonth: YearMonth,
    viewModel: TrxListViewModel,
    onMonthChange: (YearMonth) -> Unit,
    onTrxClick: (String) -> Unit,
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

    HorizontalPager(
        state = pagerState
    ) {
        TrxListContent(
            uiState = uiState,
            onTrxClick = onTrxClick,
            modifier = modifier
        )
    }
}

@Composable
fun TrxListContent(
    uiState: TrxListUiState,
    onTrxClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.trxsByDate.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxSize().wrapContentSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                imageVector = Lucide.ListX,
                contentDescription = "No data",
                tint = Color.LightGray,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No transaction yet",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            for ((index, entry) in uiState.trxsByDate.entries.withIndex()) {
                val (date, trxs) = entry
                item {
                    Column {
                        if (index > 0) {
                            HorizontalDivider()
                        }
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
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
                        )
                    }
                }
                items(trxs) { trx ->
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
                Text(text = it, style = MaterialTheme.typography.labelSmall)
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
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
