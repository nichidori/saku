package dev.nichidori.saku.core.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.*
import kotlinx.datetime.format.MonthNames

@Composable
fun MyMonthChipRow(
    selectedMonth: YearMonth,
    earliestMonth: YearMonth,
    latestMonth: YearMonth,
    onMonthSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val months = remember(earliestMonth, latestMonth) {
        val list = mutableListOf<YearMonth>()
        var temp = latestMonth
        while (temp >= earliestMonth) {
            list.add(temp)
            temp = temp.minusMonth()
        }
        list
    }

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMonth) {
        val index = months.indexOf(selectedMonth)
        if (index == -1) return@LaunchedEffect

        // Bring the item into the layout info scope so we can measure it
        if (!isInitialized) {
            listState.scrollToItem(index, 0)
        }

        // Wait for layout to finish measuring the item
        snapshotFlow { listState.layoutInfo }
            .mapNotNull { info ->
                val item = info.visibleItemsInfo.find { it.index == index }
                if (item != null) info to item else null
            }
            .first()
            .also { (layoutInfo, visibleItem) ->
                // Calculate offset to center item
                val viewportWidth = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset
                val targetOffset = (viewportWidth / 2) - (visibleItem.size / 2)

                if (!isInitialized) {
                    listState.scrollToItem(index, -targetOffset)
                    isInitialized = true
                } else {
                    listState.animateScrollToItem(index, -targetOffset)
                }
            }
    }

    LazyRow(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(months) { month ->
            FilterChip(
                selected = month == selectedMonth,
                onClick = {
                    if (month != selectedMonth) onMonthSelect(month)
                },
                label = {
                    Text(
                        LocalDate(
                            year = month.year,
                            month = month.month.ordinal + 1,
                            day = 1
                        ).format(
                            LocalDate.Format {
                                monthName(MonthNames.ENGLISH_ABBREVIATED)
                                if (month == latestMonth ||
                                    month.month == Month.JANUARY ||
                                    month.month == Month.DECEMBER
                                ) {
                                    chars(" ")
                                    year()
                                }
                            }
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}