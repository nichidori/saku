package dev.nichidori.saku.feature.trxTemplate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxAccount
import dev.nichidori.saku.domain.model.TrxTemplate
import dev.nichidori.saku.domain.model.TrxType

@Composable
fun TrxTemplatePage(
    viewModel: TrxTemplateViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
    onDeleteSuccess: (TrxTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    uiState.saveStatus.let { status ->
        LaunchedEffect(status) {
            when (status) {
                is Success -> onSaveSuccess()
                is Failure -> showToast(
                    status.error.toString(),
                    duration = ToastDuration.Long
                )

                else -> {}
            }
        }
    }

    uiState.deleteStatus.let { status ->
        LaunchedEffect(status) {
            when (val status = status) {
                is Success -> onDeleteSuccess(status.data)
                is Failure -> showToast(
                    status.error.toString(),
                    duration = ToastDuration.Long
                )

                else -> {}
            }
        }
    }

    if (!uiState.isLoading) {
        TrxTemplatePageContent(
            uiState = uiState,
            types = viewModel.types,
            onUp = onUp,
            onTypeChange = viewModel::onTypeChange,
            onNameChange = viewModel::onNameChange,
            onAmountChange = viewModel::onAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSourceAccountChange = viewModel::onSourceAccountChange,
            onTargetAccountChange = viewModel::onTargetAccountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onSaveClick = viewModel::saveTemplate,
            onDeleteClick = viewModel::deleteTemplate,
            modifier = modifier
        )
    }
}

@Composable
fun TrxTemplatePageContent(
    uiState: TrxTemplateUiState,
    types: List<TrxType>,
    onUp: () -> Unit,
    onTypeChange: (TrxType) -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: ((String) -> String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSourceAccountChange: (TrxAccount) -> Unit,
    onTargetAccountChange: (TrxAccount) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAmountInput by remember { mutableStateOf(false) }
    var showSourceAccountInput by remember { mutableStateOf(false) }
    var showTargetAccountInput by remember { mutableStateOf(false) }
    var showCategoryInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            MyAppBar(
                title = "Template",
                onUp = onUp,
                action = {
                    if (uiState.canDelete) {
                        MyIconButton(
                            content = {
                                Icon(
                                    imageVector = Lucide.Trash,
                                    contentDescription = "Delete template",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = onDeleteClick,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            )
        },
        bottomBar = {
            when {
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
                    )
                }

                showCategoryInput -> {
                    val hasNestedCategories = remember(uiState.categoriesByParent) {
                        uiState.categoriesByParent.values.any { it.isNotEmpty() }
                    }
                    var selectedParent by remember(uiState.category, uiState.type, hasNestedCategories) {
                        mutableStateOf(
                            if (hasNestedCategories) {
                                uiState.categoriesByParent.keys.firstOrNull {
                                    it.id == uiState.category?.id || it.id == uiState.category?.parent?.id
                                } ?: uiState.categoriesByParent.keys.firstOrNull()
                            } else null
                        )
                    }
                    val categories by remember(selectedParent, hasNestedCategories) {
                        derivedStateOf {
                            if (hasNestedCategories) {
                                uiState.categoriesByParent[selectedParent] ?: emptyList()
                            } else {
                                uiState.categoriesByParent.keys.toList()
                            }
                        }
                    }

                    CategorySelector(
                        categories = categories,
                        onSelected = {
                            onCategoryChange(it)
                            focusManager.clearFocus()
                        },
                        selectedWhen = { it.id == uiState.category?.id },
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
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                MySegmentedControl(
                    items = types,
                    selectedItem = uiState.type,
                    onItemSelection = { type ->
                        onTypeChange(type)
                        showTargetAccountInput = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { type ->
                    Text(
                        text = when (type) {
                            TrxType.Income -> "Income"
                            TrxType.Expense -> "Expense"
                            TrxType.Transfer -> "Transfer"
                        },
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                MyLargeTextField(
                    value = uiState.amountFormatted,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            showAmountInput = focusState.isFocused
                        }
                )
                Spacer(modifier = Modifier.height(24.dp))

                MyTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = "Name",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                MyTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = "Description",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    MyTextField(
                        value = uiState.sourceAccount?.name.orEmpty(),
                        onValueChange = { },
                        label = if (uiState.type == TrxType.Transfer) "From" else "Account",
                        enabled = uiState.accountOptions.isNotEmpty(),
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                showSourceAccountInput = focusState.isFocused
                            }
                    )
                    Spacer(modifier = Modifier.width(24.dp))

                    if (uiState.type == TrxType.Transfer) {
                        MyTextField(
                            value = uiState.targetAccount?.name.orEmpty(),
                            onValueChange = { },
                            label = "To",
                            enabled = uiState.accountOptions.isNotEmpty(),
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    showTargetAccountInput = focusState.isFocused
                                }
                        )
                    } else {
                        MyTextField(
                            value = uiState.category?.name.orEmpty(),
                            onValueChange = { },
                            label = "Category",
                            enabled = uiState.categoriesByParent.isNotEmpty(),
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    showCategoryInput = focusState.isFocused
                                }
                        )
                    }
                }
            }
        }
    }
}
