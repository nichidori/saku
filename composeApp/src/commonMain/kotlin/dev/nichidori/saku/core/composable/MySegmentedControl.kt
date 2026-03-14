package dev.nichidori.saku.core.composable

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MySegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelection: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        items.forEachIndexed { i, item ->
            SegmentedButton(
                shape = when (items.size) {
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
                },
                selected = item == selectedItem,
                onClick = { onItemSelection(item) },
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                )
            ) {
                itemContent(item)
            }
        }
    }
}
