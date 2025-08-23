package org.arraflydori.fin.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import org.arraflydori.fin.core.composable.MyButton
import org.arraflydori.fin.core.composable.MyDefaultShape
import org.arraflydori.fin.core.composable.MyTextField
import org.arraflydori.fin.core.model.Status
import org.arraflydori.fin.core.model.Status.Success
import org.arraflydori.fin.core.platform.ToastDuration
import org.arraflydori.fin.core.platform.showToast
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.TrxType

@Composable
fun CategoryPage(
    viewModel: CategoryViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showParentInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                IconButton(onClick = { onUp() }) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    ),
            ) {
                when {
                    showParentInput -> {
                        CategorySelector(
                            categories = uiState.parentOptions,
                            onSelected = {
                                viewModel.onParentChange(it)
                                focusManager.clearFocus()
                            }
                        )
                    }
                    else -> {
                        MyButton(
                            text = "Save",
                            enabled = uiState.canSave,
                            onClick = { viewModel.saveCategory() },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
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
                viewModel.types.forEachIndexed { i, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = viewModel.types.size
                        ),
                        selected = type == uiState.type,
                        onClick = { viewModel.onTypeChange(type) },
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
                onValueChange = { viewModel.onNameChange(it) },
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

@Composable
fun CategorySelector(
    categories: List<Category>,
    onSelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        for (category in categories) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
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