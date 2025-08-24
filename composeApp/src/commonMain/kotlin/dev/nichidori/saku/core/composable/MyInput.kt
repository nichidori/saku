package dev.nichidori.saku.core.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.format.TimeFormat
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter
import dev.darkokoa.datetimewheelpicker.core.format.timeFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.Instant

@Suppress("ModifierParameter")
@Composable
fun NumberKeyboard(
    onValueClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onActionClick: () -> Unit,
    actionLabel: String = "Done",
    spacing: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
            .pointerInput(Unit) { detectTapGestures {} }
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        for (i in 1..3) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (j in 1..3) {
                    val value = ((i - 1) * 3 + j)
                    KeyboardKey(
                        label = value.toString(),
                        onClick = { onValueClick(value) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            KeyboardKey(
                label = "Delete",
                onClick = onDeleteClick,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            KeyboardKey(
                label = "0",
                onClick = { onValueClick(0) },
                modifier = Modifier.weight(1f)
            )
            KeyboardKey(
                label = actionLabel,
                onClick = onActionClick,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
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
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = color ?: MaterialTheme.colorScheme.surface,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .focusProperties { canFocus = false }
            .clickable { onClick() }
            .height(56.dp)
            .padding(16.dp)
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge
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
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .pointerInput(Unit) { detectTapGestures {} }
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        for (type in types) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
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
                    style = MaterialTheme.typography.labelLarge
                )
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
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        accounts.chunked(2).forEach { rowAccounts ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowAccounts.forEach { account ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MyDefaultShape
                            )
                            .clip(MyDefaultShape)
                            .focusProperties { canFocus = false }
                            .clickable { onSelected(account) }
                            .height(48.dp)
                    ) {
                        Text(
                            account.name,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
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
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        categories.chunked(2).forEach { rowCategories ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowCategories.forEach { category ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MyDefaultShape
                            )
                            .clip(MyDefaultShape)
                            .focusProperties { canFocus = false }
                            .clickable { onSelected(category) }
                            .height(48.dp)
                    ) {
                        Text(
                            category.name,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
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
    modifier: Modifier = Modifier
) {
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
        modifier = modifier
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(16.dp)
            .fillMaxWidth()
    )
}

@Preview
@Composable
fun MyDateTimePickerPreview() {
    MyDateTimePicker(
        startDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0),
        onDateTimePicked = {}
    )
}