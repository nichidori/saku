package dev.nichidori.saku.core.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MyFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    MyBox(
        modifier = modifier
            .clip(MyDefaultShape)
            .clickable { onClick() }
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.secondary)
                else Modifier
            )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val contentColor = if (selected) MaterialTheme.colorScheme.onSecondary
            else MaterialTheme.colorScheme.onSurface
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) {
                    label()
                }
            }
        }
    }
}
