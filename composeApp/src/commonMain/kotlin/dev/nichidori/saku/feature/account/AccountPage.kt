package dev.nichidori.saku.feature.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.domain.model.AccountType

@Composable
fun AccountPage(
    viewModel: AccountViewModel,
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
        AccountPageContent(
            uiState = uiState,
            typeOptions = viewModel.typeOptions,
            onUp = onUp,
            onNameChange = viewModel::onNameChange,
            onBalanceChange = viewModel::onBalanceChange,
            onLimitChange = viewModel::onLimitChange,
            onTypeChange = viewModel::onTypeChange,
            onSaveClick = viewModel::saveAccount,
            onDeleteClick = viewModel::deleteAccount,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPageContent(
    uiState: AccountUiState,
    typeOptions: List<AccountType>,
    onUp: () -> Unit,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onLimitChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBalanceInput by remember { mutableStateOf(false) }
    var showLimitInput by remember { mutableStateOf(false) }
    var showTypeInput by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    if (showDeleteConfirmation) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteConfirmation = false },
            sheetState = sheetState,
            shape = MyDefaultShape.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Delete Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Are you sure to delete this Account? Deleting would also delete related Transactions. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(32.dp))
                MyButton(
                    text = "Delete",
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Scaffold(
        topBar = {
            MyAppBar(
                title = "Account",
                onUp = onUp,
                action = {
                    if (uiState.isEditing) {
                        MyIconButton(
                            content = {
                                Icon(
                                    imageVector = Lucide.Trash,
                                    contentDescription = "Delete account",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { showDeleteConfirmation = true },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            )
        },
        bottomBar = {
            when {
                showBalanceInput -> {
                    NumberKeyboard(
                        actionLabel = "Next",
                        onValueClick = {
                            onBalanceChange(
                                uiState.balance?.toString().orEmpty() + it.toString()
                            )
                        },
                        onDeleteClick = {
                            onBalanceChange(
                                uiState.balance?.toString().orEmpty().dropLast(1)
                            )
                        },
                        onActionClick = {
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                    )
                }

                showLimitInput -> {
                    NumberKeyboard(
                        actionLabel = "Next",
                        onValueClick = {
                            onLimitChange(
                                uiState.limit?.toString().orEmpty() + it.toString()
                            )
                        },
                        onDeleteClick = {
                            onLimitChange(
                                uiState.limit?.toString().orEmpty().dropLast(1)
                            )
                        },
                        onActionClick = {
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                    )
                }

                showTypeInput -> {
                    AccountTypeSelector(
                        types = typeOptions,
                        onSelected = {
                            onTypeChange(it)
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                        selectedWhen = { it == uiState.type },
                    )
                }

                else -> {
                    MyButton(
                        text = "Save",
                        enabled = uiState.canSave,
                        onClick = onSaveClick,
                        modifier = modifier
                            .background(color = MaterialTheme.colorScheme.background)
                            .navigationBarsPadding()
                            .padding(16.dp)
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
                .padding(20.dp, 0.dp, 20.dp, 20.dp)
        ) {
            MyTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = "Name",
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            MyTextField(
                value = uiState.type?.label().orEmpty(),
                onValueChange = {},
                label = "Type",
                readOnly = true,
                modifier = if (!uiState.isEditing) Modifier.onFocusChanged { focusState ->
                    showTypeInput = focusState.isFocused
                } else Modifier
            )
            Spacer(modifier = Modifier.height(24.dp))

            MyTextField(
                value = uiState.balanceFormatted,
                onValueChange = { },
                label = "Balance",
                enabled = !uiState.isEditing,
                readOnly = true,
                modifier = if (!uiState.isEditing) Modifier.onFocusChanged { focusState ->
                    showBalanceInput = focusState.isFocused
                } else Modifier
            )
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = uiState.showLimitInput) {
                MyTextField(
                    value = uiState.limitFormatted,
                    onValueChange = { },
                    label = "Limit",
                    readOnly = true,
                    modifier = Modifier.onFocusChanged { focusState ->
                        showLimitInput = focusState.isFocused
                    }
                )
            }
        }
    }
}

