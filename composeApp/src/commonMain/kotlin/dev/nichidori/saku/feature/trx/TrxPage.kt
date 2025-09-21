package dev.nichidori.saku.feature.trx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import dev.nichidori.saku.core.composable.AccountSelector
import dev.nichidori.saku.core.composable.CategorySelector
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyButton
import dev.nichidori.saku.core.composable.MyDateTimePicker
import dev.nichidori.saku.core.composable.MyTextField
import dev.nichidori.saku.core.composable.NumberKeyboard
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.format
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun TrxPage(
    viewModel: TrxViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
    onDeleteSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    uiState.saveStatus.let { status ->
        LaunchedEffect(status) {
            when (status) {
                is Success<*> -> onSaveSuccess()
                is Status.Failure<*> -> showToast(
                    status.error.toString(),
                    duration = ToastDuration.Long
                )
                else -> {}
            }
        }
    }

    uiState.deleteStatus.let { status ->
        LaunchedEffect(status) {
            when (status) {
                is Success<*> -> onDeleteSuccess()
                is Status.Failure<*> -> showToast(
                    status.error.toString(),
                    duration = ToastDuration.Long
                )
                else -> {}
            }
        }
    }

    TrxPageContent(
        uiState = uiState,
        types = viewModel.types,
        onUp = onUp,
        onTypeChange = viewModel::onTypeChange,
        onTimeChange = viewModel::onTimeChange,
        onAmountChange = viewModel::onAmountChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSourceAccountChange = viewModel::onSourceAccountChange,
        onTargetAccountChange = viewModel::onTargetAccountChange,
        onCategoryChange = viewModel::onCategoryChange,
        onNoteChange = viewModel::onNoteChange,
        onSaveClick = viewModel::saveTrx,
        onDeleteClick = viewModel::deleteTrx,
        modifier = modifier
    )
}

@Composable
fun TrxPageContent(
    uiState: TrxUiState,
    types: List<TrxType>,
    onUp: () -> Unit,
    onTypeChange: (TrxType) -> Unit,
    onTimeChange: (Instant) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSourceAccountChange: (Account) -> Unit,
    onTargetAccountChange: (Account) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimeInput by remember { mutableStateOf(false) }
    var showAmountInput by remember { mutableStateOf(false) }
    var showSourceAccountInput by remember { mutableStateOf(false) }
    var showTargetAccountInput by remember { mutableStateOf(false) }
    var showCategoryInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            MyAppBar(
                title = "Transaction",
                onUp = onUp,
                action = {
                    if (uiState.canDelete) {
                        IconButton(
                            content = {
                                Icon(
                                    imageVector = Lucide.Trash,
                                    contentDescription = "Delete account",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = onDeleteClick,
                        )
                    }
                }
            )
        },
        bottomBar = {
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding()
            when {
                showTimeInput -> {
                    MyDateTimePicker(
                        startDateTime = (uiState.time ?: Clock.System.now())
                            .toLocalDateTime(TimeZone.currentSystemDefault()),
                        onDateTimePicked = {
                            onTimeChange(it.toInstant(TimeZone.currentSystemDefault()))
                        },
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding)
                    )
                }
                showAmountInput -> {
                    NumberKeyboard(
                        actionLabel = "Next",
                        onValueClick = {
                            onAmountChange(
                                uiState.amount?.toString().orEmpty() + it.toString()
                            )
                        },
                        onDeleteClick = {
                            onAmountChange(
                                uiState.amount?.toString().orEmpty().dropLast(1)
                            )
                        },
                        onActionClick = {
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding)
                    )
                }
                showSourceAccountInput -> {
                    AccountSelector(
                        accounts = uiState.accountOptions,
                        onSelected = {
                            onSourceAccountChange(it)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding)
                    )
                }
                showTargetAccountInput -> {
                    AccountSelector(
                        accounts = uiState.accountOptions,
                        onSelected = {
                            onTargetAccountChange(it)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding)
                    )
                }
                showCategoryInput -> {
                    CategorySelector(
                        categories = uiState.categoryOptions,
                        onSelected = {
                            onCategoryChange(it)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding)
                    )
                }
                else -> {
                    MyButton(
                        text = "Save",
                        enabled = uiState.canSave,
                        onClick = onSaveClick,
                        modifier = modifier
                            .background(color = MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                            .padding(bottom = bottomPadding)
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            SingleChoiceSegmentedButtonRow {
                types.forEachIndexed { i, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = types.size
                        ),
                        selected = type == uiState.type,
                        onClick = {
                            onTypeChange(type)
                            showTargetAccountInput = false
                        },
                    ) {
                        Text(
                            when (type) {
                                TrxType.Income -> "Income"
                                TrxType.Expense -> "Expense"
                                TrxType.Transfer -> "Transfer"
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            MyTextField(
                value = uiState.time?.format(
                    LocalDateTime.Format {
                        day()
                        chars(" ")
                        monthName(MonthNames.ENGLISH_ABBREVIATED)
                        chars(" ")
                        year()
                        chars(" ")
                        hour()
                        chars(":")
                        minute()
                    }
                ).orEmpty(),
                onValueChange = onDescriptionChange,
                label = "Time",
                readOnly = true,
                trailingIcon = {
                    TextButton(
                        onClick = { onTimeChange(Clock.System.now()) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Now")
                    }
                },
                modifier = Modifier.onFocusChanged { focusState ->
                    showTimeInput = focusState.isFocused
                }
            )

            MyTextField(
                value = uiState.amountFormatted,
                onValueChange = { },
                label = "Amount",
                readOnly = true,
                modifier = Modifier.onFocusChanged { focusState ->
                    showAmountInput = focusState.isFocused
                }
            )

            MyTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = "Description",
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            MyTextField(
                value = uiState.sourceAccount?.name.orEmpty(),
                onValueChange = { },
                label = "Source Account",
                enabled = uiState.accountOptions.isNotEmpty(),
                readOnly = true,
                modifier = Modifier.onFocusChanged { focusState ->
                    showSourceAccountInput = focusState.isFocused
                }
            )

            AnimatedVisibility(visible = uiState.type == TrxType.Transfer) {
                MyTextField(
                    value = uiState.targetAccount?.name.orEmpty(),
                    onValueChange = { },
                    label = "Target Account",
                    enabled = uiState.accountOptions.isNotEmpty(),
                    readOnly = true,
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            showTargetAccountInput = focusState.isFocused
                        }
                )
            }

            MyTextField(
                value = uiState.category?.name.orEmpty(),
                onValueChange = { },
                label = "Category",
                enabled = uiState.categoryOptions.isNotEmpty(),
                readOnly = true,
                modifier = Modifier.onFocusChanged { focusState ->
                    showCategoryInput = focusState.isFocused
                }
            )

            MyTextField(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = "Note",
            )
        }
    }
}

@Preview
@Composable
fun TrxPageContentPreview() {
    val uiState = TrxUiState(
        description = "Dinner",
        amount = 25000,
        category = Category(
            id = "1",
            name = "Food",
            type = TrxType.Expense,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
        accountOptions = listOf(
            Account(
                id = "1",
                name = "Cash",
                type = AccountType.Cash,
                initialAmount = 12000,
                currentAmount = 30000,
                createdAt = Clock.System.now(),
                updatedAt = null
            )
        ),
        categoryMap = mapOf(
            TrxType.Expense to listOf(
                Category(
                    id = "1",
                    name = "Food",
                    type = TrxType.Expense,
                    createdAt = Clock.System.now(),
                    updatedAt = null
                ),
            )
        )
    )
    TrxPageContent(
        uiState = uiState,
        types = TrxType.entries,
        onUp = {},
        onTypeChange = {},
        onTimeChange = {},
        onAmountChange = {},
        onDescriptionChange = {},
        onSourceAccountChange = {},
        onTargetAccountChange = {},
        onCategoryChange = {},
        onNoteChange = {},
        onSaveClick = {},
        onDeleteClick = {}
    )
}
