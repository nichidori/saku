package dev.nichidori.saku.core.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.format.TimeFormat
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import dev.darkokoa.datetimewheelpicker.core.format.timeFormatter
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.Instant

val defaultInputHeight = 320.dp

@Composable
private fun CloseRow(
    content: @Composable RowScope.() -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
        MyIconButton(
            onClick = { focusManager.clearFocus() },
            modifier = Modifier.padding(vertical = 4.dp).size(32.dp)
        ) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = "Close",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun NumberKeyboard(
    onValueClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Done",
    spacing: Dp = 12.dp,
    height: Dp = defaultInputHeight
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
            .requiredHeight(height)
            .pointerInput(Unit) { detectTapGestures {} }
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        CloseRow()
        for (i in 1..3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.weight(1f)
            ) {
                for (j in 1..3) {
                    val value = ((i - 1) * 3 + j)
                    KeyboardKey(
                        label = value.toString(),
                        onClick = { onValueClick(value) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.weight(1f)
        ) {
            KeyboardKey(
                label = "Delete",
                onClick = onDeleteClick,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                foregroundColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKey(
                label = "0",
                onClick = { onValueClick(0) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKey(
                label = actionLabel,
                onClick = onActionClick,
                backgroundColor = MaterialTheme.colorScheme.primary,
                foregroundColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Preview
@Composable
fun NumberKeyboardPreview() {
    NumberKeyboard(
        onValueClick = {},
        onDeleteClick = {},
        onActionClick = {}
    )
}

@Composable
fun KeyboardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    foregroundColor: Color? = null,
    initialDelay: Long = 500L,
    repeatDelay: Long = 100L,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed, initialDelay, repeatDelay) {
        if (pressed) {
            onClick()
            delay(initialDelay)
            while (true) {
                onClick()
                delay(repeatDelay)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = backgroundColor ?: MaterialTheme.colorScheme.surface,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .focusProperties { canFocus = false }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = {}
            )
            .padding(16.dp)
    ) {
            Text(
                label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = foregroundColor ?: MaterialTheme.colorScheme.onSurface
            )
    }
}

@Preview
@Composable
fun KeyboardKeyPreview() {
    KeyboardKey(
        label = "1",
        onClick = {}
    )
}

@Composable
fun AccountTypeSelector(
    types: List<AccountType>,
    onSelected: (AccountType) -> Unit,
    selectedWhen: (AccountType) -> Boolean = { false },
    modifier: Modifier = Modifier,
    height: Dp = defaultInputHeight,
) {
    Column(
        modifier = modifier
            .requiredHeight(height)
            .pointerInput(Unit) { detectTapGestures {} }
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        CloseRow()
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(types.chunked(2)) { rowTypes ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowTypes.forEach { type ->
                        val selected = selectedWhen(type)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.background
                                    },
                                    shape = MyDefaultShape
                                )
                                .clip(MyDefaultShape)
                                .focusProperties { canFocus = false }
                                .clickable { onSelected(type) }
                                .height(48.dp)
                        ) {
                            Text(
                                type.label(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onSecondary
                                    else -> MaterialTheme.colorScheme.onBackground
                                }
                            )
                        }
                    }
                    if (rowTypes.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AccountTypeSelectorPreview() {
    AccountTypeSelector(
        types = AccountType.entries,
        onSelected = {}
    )
}

fun AccountType.label(): String {
    return when (this) {
        AccountType.Cash -> "Cash"
        AccountType.Bank -> "Bank"
        AccountType.Credit -> "Credit"
        AccountType.Ewallet -> "E-wallet"
        AccountType.Emoney -> "E-money"
    }
}

@Composable
fun AccountSelector(
    accounts: List<Account>,
    onSelected: (Account) -> Unit,
    modifier: Modifier = Modifier,
    selectedWhen: (Account) -> Boolean = { false },
    enabledWhen: (Account) -> Boolean = { true },
    height: Dp = defaultInputHeight,
    header: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .requiredHeight(height)
            .pointerInput(Unit) { detectTapGestures {} }
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        CloseRow(content = header)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(accounts.chunked(2)) { rowAccounts ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowAccounts.forEach { account ->
                        val selected = remember(selectedWhen, account) { selectedWhen(account) }
                        val enabled = remember(enabledWhen, account) { enabledWhen(account) }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.primaryContainer
                                        enabled -> MaterialTheme.colorScheme.background
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    shape = MyDefaultShape
                                )
                                .clip(MyDefaultShape)
                                .focusProperties { canFocus = false }
                                .clickable(enabled = enabled) { onSelected(account) }
                                .height(48.dp)
                        ) {
                            Text(
                                account.name,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    enabled -> MaterialTheme.colorScheme.onBackground
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    if (rowAccounts.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AccountSelectorPreview() {
    val accounts = listOf(
        Account(
            id = "1",
            name = "Cash",
            initialAmount = 100000,
            currentAmount = 100000,
            type = AccountType.Cash,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        ),
        Account(
            id = "2",
            name = "Bank BCA",
            initialAmount = 1000000,
            currentAmount = 1000000,
            type = AccountType.Bank,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        ),
        Account(
            id = "3",
            name = "Gopay",
            initialAmount = 50000,
            currentAmount = 50000,
            type = AccountType.Ewallet,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        ),
        Account(
            id = "4",
            name = "Flazz",
            initialAmount = 100000,
            currentAmount = 77500,
            type = AccountType.Emoney,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        )
    )
    AccountSelector(accounts = accounts, onSelected = {})
}

@Composable
fun CategorySelector(
    categories: List<Category>,
    onSelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
    selectedWhen: (Category) -> Boolean = { false },
    enabledWhen: (Category) -> Boolean = { true },
    height: Dp = defaultInputHeight,
    header: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .requiredHeight(height)
            .pointerInput(Unit) { detectTapGestures {} }
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        CloseRow(content = header)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories.chunked(2)) { rowCategories ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowCategories.forEach { category ->
                        val selected = remember(selectedWhen, category) { selectedWhen(category) }
                        val enabled = remember(enabledWhen, category) { enabledWhen(category) }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.primaryContainer
                                        enabled -> MaterialTheme.colorScheme.background
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    shape = MyDefaultShape
                                )
                                .clip(MyDefaultShape)
                                .focusProperties { canFocus = false }
                                .clickable(enabled = enabled) { onSelected(category) }
                                .height(48.dp)
                        ) {
                            Text(
                                category.name,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    enabled -> MaterialTheme.colorScheme.onBackground
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    if (rowCategories.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun CategorySelectorPreview() {
    val categories = listOf(
        Category(
            id = "1",
            name = "Groceries",
            type = TrxType.Expense,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        ),
        Category(
            id = "2",
            name = "Salary",
            type = TrxType.Income,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        ),
        Category(
            id = "3",
            name = "Freelance",
            type = TrxType.Income,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        )
    )
    CategorySelector(
        categories = categories,
        onSelected = {}
    )
}

@Composable
fun MyDateTimePicker(
    startDateTime: LocalDateTime,
    onDateTimePicked: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = defaultInputHeight,
) {
    Column(
        modifier = modifier
            .requiredHeight(height)
            .pointerInput(Unit) { detectTapGestures {} }
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        CloseRow()
        Spacer(modifier = Modifier.height(32.dp))
        WheelDateTimePicker(
            startDateTime = startDateTime,
            maxDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let {
                LocalDateTime(
                    year = it.year,
                    month = 12,
                    day = 31,
                    hour = 23,
                    minute = 59,
                    second = 59
                )
            },
            dateFormatter = dateFormatter(),
            timeFormatter = timeFormatter(timeFormat = TimeFormat.HOUR_24),
            onSnappedDateTime = onDateTimePicked,
            selectorProperties = WheelPickerDefaults.selectorProperties(
                shape = MyDefaultShape,
                color = MaterialTheme.colorScheme.secondary,
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
fun MyDateTimePickerPreview() {
    MyDateTimePicker(
        startDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0),
        onDateTimePicked = {}
    )
}
