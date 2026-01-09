package dev.nichidori.saku.feature.trx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.format
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

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

    if (!uiState.isLoading) {
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
}

@Composable
fun TrxPageContent(
    uiState: TrxUiState,
    types: List<TrxType>,
    onUp: () -> Unit,
    onTypeChange: (TrxType) -> Unit,
    onTimeChange: (Instant) -> Unit,
    onAmountChange: ((String) -> String) -> Unit,
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
                            onAmountChange { current ->
                                current + it
                            }
                        },
                        onDeleteClick = {
                            onAmountChange { current ->
                                current.dropLast(1)
                            }
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
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                        selectedWhen = { it == uiState.sourceAccount },
                        enabledWhen = { it != uiState.targetAccount },
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
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                        selectedWhen = { it == uiState.targetAccount },
                        enabledWhen = { it != uiState.sourceAccount },
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding)
                    )
                }

                showCategoryInput -> {
                    var selectedParent by remember(uiState.category) {
                        mutableStateOf(uiState.categoriesByParent.keys.firstOrNull {
                            it.id == uiState.category?.id || it.id == uiState.category?.parent?.id
                        })
                    }
                    val categories by remember(selectedParent) {
                        derivedStateOf { uiState.categoriesByParent[selectedParent] ?: emptyList() }
                    }

                    Column {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .pointerInput(Unit) { detectTapGestures {} }
                                .background(color = MaterialTheme.colorScheme.surfaceContainer)
                                .fillMaxWidth()
                                .padding(16.dp, 16.dp, 16.dp, 0.dp)
                        ) {
                            items(uiState.categoriesByParent.keys.toList()) { parent ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .background(
                                            color = if (parent == selectedParent) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.background
                                            },
                                            shape = MyDefaultShape
                                        )
                                        .clip(MyDefaultShape)
                                        .focusProperties { canFocus = false }
                                        .clickable {
                                            if (uiState.categoriesByParent[parent]?.isNotEmpty() != true) {
                                                selectedParent = parent
                                                onCategoryChange(parent)
                                                focusManager.clearFocus()
                                            } else {
                                                selectedParent = parent
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        parent.name,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        color =  if (parent == selectedParent) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        }
                                    )
                                }
                            }
                        }
                        CategorySelector(
                            categories = categories,
                            onSelected = {
                                onCategoryChange(it)
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.surfaceContainer)
                                .padding(bottom = bottomPadding)
                        )
                    }
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                types.forEachIndexed { i, type ->
                    SegmentedButton(
                        shape = when (i) {
                            0 -> MyDefaultShape.copy(
                                bottomEnd = CornerSize(0.dp),
                                topEnd = CornerSize(0.dp)
                            )

                            types.lastIndex -> MyDefaultShape.copy(
                                bottomStart = CornerSize(0.dp),
                                topStart = CornerSize(0.dp)
                            )

                            else -> RectangleShape
                        },
                        selected = type == uiState.type,
                        onClick = {
                            onTypeChange(type)
                            showTargetAccountInput = false
                        },
                        icon = {}
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

            AnimatedVisibility(visible = uiState.type != TrxType.Transfer) {
                MyTextField(
                    value = uiState.category?.name.orEmpty(),
                    onValueChange = { },
                    label = "Category",
                    enabled = uiState.categoriesByParent.isNotEmpty(),
                    readOnly = true,
                    modifier = Modifier.onFocusChanged { focusState ->
                        showCategoryInput = focusState.isFocused
                    }
                )
            }

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
