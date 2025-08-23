package org.arraflydori.fin.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.arraflydori.fin.core.composable.MyAppBar
import org.arraflydori.fin.core.composable.MyButton
import org.arraflydori.fin.core.composable.MyDefaultShape
import org.arraflydori.fin.core.composable.MyTextField
import org.arraflydori.fin.core.model.Status
import org.arraflydori.fin.core.model.Status.Success
import org.arraflydori.fin.core.platform.ToastDuration
import org.arraflydori.fin.core.platform.showToast
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.TrxType
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Instant

@Composable
fun CategoryPage(
    viewModel: CategoryViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
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

    CategoryPageContent(
        uiState = uiState,
        types = viewModel.types,
        onUp = onUp,
        onTypeChange = viewModel::onTypeChange,
        onNameChange = viewModel::onNameChange,
        onParentChange = viewModel::onParentChange,
        onSaveClick = viewModel::saveCategory,
        modifier = modifier
    )
}

@Composable
fun CategoryPageContent(
    uiState: CategoryUiState,
    types: List<TrxType>,
    onUp: () -> Unit,
    onTypeChange: (TrxType) -> Unit,
    onNameChange: (String) -> Unit,
    onParentChange: (Category) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showParentInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            MyAppBar(title = "Category", onUp = onUp)
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
                        modifier = Modifier.padding(16.dp).padding(bottom = bottomPadding)
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(contentPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow {
                types.forEachIndexed { i, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = types.size
                        ),
                        selected = type == uiState.type,
                        onClick = { onTypeChange(type) },
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
        parent = Category(id = "1", name = "Groceries", type = TrxType.Expense, createdAt = Instant.DISTANT_PAST, updatedAt = null),
        parentsMap = mapOf(
            TrxType.Expense to listOf(
                Category(id = "1", name = "Groceries", type = TrxType.Expense, createdAt = Instant.DISTANT_PAST, updatedAt = null),
                Category(id = "2", name = "Food", type = TrxType.Expense, createdAt = Instant.DISTANT_PAST, updatedAt = null)
            )
        )
    )
    val types = listOf(TrxType.Income, TrxType.Expense, TrxType.Transfer)
    CategoryPageContent(
        uiState = uiState,
        types = types,
        onUp = {},
        onTypeChange = {},
        onNameChange = {},
        onParentChange = {},
        onSaveClick = {}
    )
}

@Preview
@Composable
fun CategorySelectorPreview() {
    val categories = listOf(
        Category(id = "1", name = "Groceries", type = TrxType.Expense, createdAt = Instant.DISTANT_PAST, updatedAt = null),
        Category(id = "2", name = "Salary", type = TrxType.Income, createdAt = Instant.DISTANT_PAST, updatedAt = null),
        Category(id = "3", name = "Freelance", type = TrxType.Income, createdAt = Instant.DISTANT_PAST, updatedAt = null)
    )
    CategorySelector(
        categories = categories,
        onSelected = {}
    )
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
        for (category in categories) {
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
    }
}