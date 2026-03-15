package dev.nichidori.saku.core.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun <T> MySegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelection: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-2).dp)
    ) {
        items.forEachIndexed { i, item ->
            val isSelected = item == selectedItem
            val shape = when (items.size) {
                1 -> MyDefaultShape
                else -> when (i) {
                    0 -> MyDefaultShape.copy(
                        bottomEnd = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp)
                    )
                    items.lastIndex -> MyDefaultShape.copy(
                        bottomStart = CornerSize(0.dp),
                        topStart = CornerSize(0.dp)
                    )
                    else -> RectangleShape
                }
            }

            MyBox(
                shape = shape,
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .clickable { onItemSelection(item) }
                    .then(
                        if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) {
                            itemContent(item)
                        }
                    }
                }
            }
        }
    }
}
