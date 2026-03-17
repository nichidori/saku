package dev.nichidori.saku.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.domain.model.Category

@Composable
fun DefaultBudgetPage(
    viewModel: DefaultBudgetViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
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

    DefaultBudgetPageContent(
        uiState = uiState,
        onUp = onUp,
        onSave = viewModel::save,
        onCategoryChange = viewModel::onCategoryChange,
        onAmountChange = viewModel::onAmountChange,
        modifier = modifier
    )
}

@Composable
fun DefaultBudgetPageContent(
    uiState: DefaultBudgetUiState,
    onUp: () -> Unit,
    onSave: () -> Unit,
    onCategoryChange: (Category) -> Unit,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var showCategoryInput by remember { mutableStateOf(false) }
    var showAmountInput by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MyAppBar(
                title = when (uiState.newBudget) {
                    true -> "Budget"
                    false -> "Default Budget"
                    else -> ""
                },
                onUp = onUp
            )
        },
        bottomBar = {
            when {
                showCategoryInput -> {
                    val hasNestedCategories = remember(uiState.categoriesByParent) {
                        uiState.categoriesByParent.values.any { it.isNotEmpty() }
                    }
                    var selectedParent by remember(uiState.category, hasNestedCategories) {
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
                        header = {
                            if (hasNestedCategories) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
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
                                                color = if (parent == selectedParent) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onBackground
                                                },
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        },
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
                    )
                }

                else -> {
                    MyButton(
                        text = "Save",
                        onClick = onSave,
                        enabled = uiState.canSave,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            when (uiState.newBudget) {
                true -> {
                    MyTextField(
                        value = uiState.category?.name ?: "",
                        onValueChange = {},
                        label = "Category",
                        readOnly = true,
                        modifier = Modifier.onFocusChanged { showCategoryInput = it.isFocused }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                false -> {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        uiState.category?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                else -> Unit
            }

            if (uiState.loadStatus.isCompleted) {
                MyTextField(
                    value = uiState.amountFormatted,
                    onValueChange = {},
                    label = "Amount",
                    readOnly = true,
                    modifier = Modifier.onFocusChanged { showAmountInput = it.isFocused }
                )
            }
        }
    }
}
