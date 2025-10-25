package dev.nichidori.saku.core.composable

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChartPie
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.ReceiptText

enum class NavBarDestination { Home, Trx, Statistic }

@Composable
fun MyNavBar(
    selectedDestination: NavBarDestination?,
    onHomeClick: () -> Unit,
    onTrxClick: () -> Unit,
    onStatisticClick: () -> Unit,
    onAddClick: () -> Unit,
    onAddLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var hoverInteraction: HoverInteraction.Enter? by remember { mutableStateOf(null) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 32.dp, vertical = 12.dp)
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
    ) {
        Icon(
            imageVector = Lucide.House,
            contentDescription = "Home",
            tint = if (selectedDestination == NavBarDestination.Home) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier
                .size(48.dp)
                .clip(MyDefaultShape)
                .clickable { onHomeClick() }
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Icon(
            imageVector = Lucide.ReceiptText,
            contentDescription = "Transaction",
            tint = if (selectedDestination == NavBarDestination.Trx) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier
                .size(48.dp)
                .clip(MyDefaultShape)
                .clickable { onTrxClick() }
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Icon(
            imageVector = Lucide.ChartPie,
            contentDescription = "Statistic",
            tint = if (selectedDestination == NavBarDestination.Statistic) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier
                .size(48.dp)
                .clip(MyDefaultShape)
                .clickable { onStatisticClick() }
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MyDefaultShape
                )
                .clip(MyDefaultShape)
                .indication(interactionSource, LocalIndication.current)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            tryAwaitRelease()
                            interactionSource.emit(PressInteraction.Release(press))
                        },
                        onTap = { onAddClick() },
                        onLongPress = { onAddLongPress() }
                    )
                }
                .pointerInput(interactionSource) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            when (event.type) {
                                PointerEventType.Enter -> {
                                    if (hoverInteraction == null) {
                                        val enter = HoverInteraction.Enter()
                                        hoverInteraction = enter
                                        interactionSource.tryEmit(enter)
                                    }
                                }

                                PointerEventType.Exit -> {
                                    hoverInteraction?.let {
                                        interactionSource.tryEmit(HoverInteraction.Exit(it))
                                        hoverInteraction = null
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Lucide.Pencil, contentDescription = "Add")
        }
    }
}