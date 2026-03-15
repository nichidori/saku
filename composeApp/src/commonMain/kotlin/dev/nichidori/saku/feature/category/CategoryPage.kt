package dev.nichidori.saku.feature.category

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.IconPickerCategories
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.model.toPickerIcon
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
            onIconChange = viewModel::onIconChange,
            onNameChange = viewModel::onNameChange,
            onParentChange = viewModel::onParentChange,
            onSaveClick = viewModel::saveCategory,
            onDeleteClick = viewModel::deleteCategory,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPageContent(
    uiState: CategoryUiState,
    onUp: () -> Unit,
    onTypeChange: (TrxType) -> Unit,
    onIconChange: (String?) -> Unit,
    onNameChange: (String) -> Unit,
    onParentChange: (Category?) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showParentInput by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    if (showIconPicker) {
        ModalBottomSheet(
            onDismissRequest = { showIconPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = MyDefaultShape.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(
                top = WindowInsets.statusBars.asPaddingValues()
                    .calculateTopPadding()
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                Text(
                    "Select Icon",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "None",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        val selected = uiState.icon == null
                        Box(
                            modifier = Modifier
                                .requiredSize(48.dp)
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.secondary
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .clickable {
                                    onIconChange(null)
                                    showIconPicker = false
                                }
                                .wrapContentSize()
                        ) {
                            Text(
                                uiState.name.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconPickerCategories.forEach { category ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(category.icons) { pickerIcon ->
                            val selected = uiState.icon == pickerIcon.label
                            Box(
                                modifier = Modifier
                                    .requiredSize(48.dp)
                                    .background(
                                        color = if (selected) MaterialTheme.colorScheme.secondary
                                        else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface
                                        else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable {
                                        onIconChange(pickerIcon.label)
                                        showIconPicker = false
                                    }
                                    .wrapContentSize()
                            ) {
                                Icon(
                                    imageVector = pickerIcon.icon,
                                    contentDescription = pickerIcon.label,
                                    tint = if (selected) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            MyAppBar(
                title = "Category",
                onUp = onUp,
                action = {
                    if (uiState.canDelete) {
                        MyIconButton(
                            content = {
                                Icon(
                                    imageVector = Lucide.Trash,
                                    contentDescription = "Delete category",
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
                        selectedWhen = { it.id == uiState.parent?.id },
                        modifier = Modifier
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
                MySegmentedControl(
                    items = listOf(TrxType.Income, TrxType.Expense),
                    selectedItem = uiState.type,
                    onItemSelection = onTypeChange,
                    modifier = Modifier.fillMaxWidth()
                ) { type ->
                    Text(text = if (type == TrxType.Income) "Income" else "Expense",)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { showIconPicker = true }
                    .wrapContentSize()
            ) {
                val icon = uiState.icon.toPickerIcon()?.icon
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Category icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Text(
                        uiState.name.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
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
                trailingIcon = if (uiState.parent != null) {
                    {
                        MyTextButton(
                            text = "Remove",
                            onClick = { onParentChange(null) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
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
        onDeleteClick = {},
        onIconChange = {},
    )
}