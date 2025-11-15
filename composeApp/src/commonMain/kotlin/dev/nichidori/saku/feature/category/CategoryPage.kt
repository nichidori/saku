package dev.nichidori.saku.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
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
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.TrxType
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Instant

@Composable
fun CategoryPage(
    viewModel: CategoryViewModel,
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
        CategoryPageContent(
            uiState = uiState,
            onUp = onUp,
            onTypeChange = viewModel::onTypeChange,
            onNameChange = viewModel::onNameChange,
            onParentChange = viewModel::onParentChange,
            onSaveClick = viewModel::saveCategory,
            onDeleteClick = viewModel::deleteCategory,
            modifier = modifier
        )
    }
}

@Composable
fun CategoryPageContent(
    uiState: CategoryUiState,
    onUp: () -> Unit,
    onTypeChange: (TrxType) -> Unit,
    onNameChange: (String) -> Unit,
    onParentChange: (Category?) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showParentInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            MyAppBar(
                title = "Category",
                onUp = onUp,
                action = {
                    if (uiState.canDelete) {
                        IconButton(
                            content = {
                                Icon(
                                    imageVector = Lucide.Trash,
                                    contentDescription = "Delete category",
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
                showParentInput -> {
                    CategorySelector(
                        categories = uiState.parentOptions,
                        onSelected = {
                            onParentChange(it)
                            focusManager.moveFocus(FocusDirection.Next)
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
                        modifier = Modifier
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
            if (uiState.canChooseType) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = MyDefaultShape.copy(
                            topEnd = CornerSize(0.dp),
                            bottomEnd = CornerSize(0.dp)
                        ),
                        selected = uiState.type == TrxType.Income,
                        onClick = { onTypeChange(TrxType.Income) },
                        icon = {},
                    ) {
                        Text("Income", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        shape = MyDefaultShape.copy(
                            topStart = CornerSize(0.dp),
                            bottomStart = CornerSize(0.dp)
                        ),
                        selected = uiState.type == TrxType.Expense,
                        onClick = { onTypeChange(TrxType.Expense) },
                        icon = {},
                    ) {
                        Text("Expense", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            MyTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = "Name",
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            MyTextField(
                value = uiState.parent?.name.orEmpty(),
                onValueChange = { },
                label = "Parent",
                enabled = uiState.parentOptions.isNotEmpty(),
                readOnly = true,
                trailingIcon = if (uiState.parent != null) {
                    {
                        TextButton(
                            onClick = { onParentChange(null) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Remove")
                        }
                    }
                } else null,
                modifier = Modifier.onFocusChanged { focusState ->
                    showParentInput = focusState.isFocused
                }
            )
        }
    }
}

@Preview
@Composable
fun CategoryPageContentPreview() {
    val uiState = CategoryUiState(
        name = "Food",
        type = TrxType.Expense,
        parent = Category(
            id = "1",
            name = "Groceries",
            type = TrxType.Expense,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = null
        ),
        parentsOfType = mapOf(
            TrxType.Expense to listOf(
                Category(
                    id = "1",
                    name = "Groceries",
                    type = TrxType.Expense,
                    createdAt = Instant.DISTANT_PAST,
                    updatedAt = null
                ),
                Category(
                    id = "2",
                    name = "Food",
                    type = TrxType.Expense,
                    createdAt = Instant.DISTANT_PAST,
                    updatedAt = null
                )
            )
        )
    )
    CategoryPageContent(
        uiState = uiState,
        onUp = {},
        onTypeChange = {},
        onNameChange = {},
        onParentChange = {},
        onSaveClick = {},
        onDeleteClick = {}
    )
}