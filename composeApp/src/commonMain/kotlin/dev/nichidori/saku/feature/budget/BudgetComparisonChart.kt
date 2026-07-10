package dev.nichidori.saku.feature.budget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nichidori.saku.domain.model.Budget
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.number

private fun YearMonth.minusMonths(count: Int): YearMonth {
    var y = year
    var m = month.number - count
    while (m < 1) { m += 12; y-- }
    return YearMonth(y, kotlinx.datetime.Month(m))
}

@Composable
fun BudgetComparisonChart(
    budgets: List<Budget>,
    chartMonthCount: Int,
    modifier: Modifier = Modifier,
    columnWidthDp: Dp = 56.dp,
    barWidthDp: Dp = 20.dp,
    chartHeightDp: Dp = 120.dp,
) {
    if (budgets.isEmpty()) return

    val sorted = remember(budgets) { budgets.sortedBy { it.month } }
    val budgetMap = remember(sorted) { sorted.associateBy { it.month } }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val lineColor = MaterialTheme.colorScheme.onBackground
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val density = LocalDensity.current
    val columnWidthPx = with(density) { columnWidthDp.toPx() }
    val barWidthPx = with(density) { barWidthDp.toPx() }

    val textMeasurer = rememberTextMeasurer()

    val maxValue = remember(sorted) {
        sorted.maxOf { maxOf(it.baseAmount, it.spentAmount) }.coerceAtLeast(1L)
    }

    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (hasAnimated) 1f else 0f) }

    LaunchedEffect(hasAnimated) {
        if (!hasAnimated) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            hasAnimated = true
        }
    }

    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
    )

    Canvas(
        modifier = modifier
            .height(chartHeightDp + 32.dp)
    ) {
        val currentProgress = progress.value
        val canvasWidth = size.width
        val canvasHeight = size.height
        val bottomAreaHeight = 28.dp.toPx()
        val chartAreaHeight = canvasHeight - bottomAreaHeight
        val topPadding = 16.dp.toPx()

        // Draw baseline
        drawLine(
            color = trackColor,
            start = Offset(0f, chartAreaHeight),
            end = Offset(canvasWidth, chartAreaHeight),
            strokeWidth = 1.dp.toPx()
        )

        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val halfBarWidth = barWidthPx / 2f

        val lastMonth = sorted.last().month

        val linePoints = mutableListOf<Offset>()

        for (i in 0 until chartMonthCount) {
            val month = lastMonth.minusMonths(chartMonthCount - 1 - i)
            val centerX = columnWidthPx * i + columnWidthPx / 2f

            val date = LocalDate(
                year = month.year,
                month = month.month.number,
                day = 1
            )
            val monthLabel = date.format(LocalDate.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) })
            val yearLabel = (date.year % 100).toString().padStart(2, '0')
            val labelText = "$monthLabel $yearLabel"

            // Draw month label for all columns
            val labelResult = textMeasurer.measure(labelText, labelStyle)
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(
                    centerX - labelResult.size.width / 2f,
                    chartAreaHeight + 4.dp.toPx()
                )
            )

            val budget = budgetMap[month]
            if (budget != null) {
                val barTopY = chartAreaHeight - topPadding

                // Spent bar
                val spentHeight = if (maxValue > 0) {
                    (budget.spentAmount.toFloat() / maxValue.toFloat() * barTopY) * currentProgress
                } else 0f
                val spentColor = if (budget.spentAmount > budget.baseAmount) errorColor else primaryColor

                if (spentHeight > 0f) {
                    drawRoundRect(
                        color = spentColor,
                        topLeft = Offset(centerX - halfBarWidth, chartAreaHeight - spentHeight),
                        size = Size(barWidthPx, spentHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Budget line point
                val budgetY = if (maxValue > 0) {
                    chartAreaHeight - (budget.baseAmount.toFloat() / maxValue.toFloat() * barTopY) * currentProgress
                } else chartAreaHeight
                linePoints.add(Offset(centerX, budgetY))
            }
        }

        // Draw budget line
        if (linePoints.size >= 2 && currentProgress > 0f) {
            val linePath = Path().apply {
                moveTo(linePoints.first().x, linePoints.first().y)
                for (i in 1 until linePoints.size) {
                    lineTo(linePoints[i].x, linePoints[i].y)
                }
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw dots at each point
            linePoints.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }
    }
}
