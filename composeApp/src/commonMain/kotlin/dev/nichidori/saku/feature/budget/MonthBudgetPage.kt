package dev.nichidori.saku.feature.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyButton
import dev.nichidori.saku.core.composable.MyTextField
import dev.nichidori.saku.core.composable.NumberKeyboard
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import kotlinx.datetime.Month

@Composable
fun MonthBudgetPage(
    viewModel: MonthBudgetViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    LaunchedEffect(uiState.saveStatus) {
        when (val status = uiState.saveStatus) {
            is Success<*> -> onSaveSuccess()
            is Status.Failure<*> -> showToast(
                status.error.toString(),
                duration = ToastDuration.Long
            )
            else -> {}
        }
    }

    MonthBudgetPageContent(
        uiState = uiState,
        onUp = onUp,
        onSave = viewModel::save,
        onAmountChange = viewModel::onAmountChange,
        modifier = modifier
    )
}

@Composable
fun MonthBudgetPageContent(
    uiState: MonthBudgetUiState,
    onUp: () -> Unit,
    onSave: () -> Unit,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var showAmountInput by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MyAppBar(
                title = uiState.budget?.let { "${Month(it.month).name.take(3)} ${it.year}" } ?: "Budget",
                onUp = onUp
            )
        },
        bottomBar = {
            if (showAmountInput) {
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
                    }
                )
            } else {
                MyButton(
                    text = "Save",
                    onClick = onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                )
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
            uiState.budget?.let { budget ->
                Text(
                    budget.category.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Default: ${uiState.defaultAmountFormatted}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                MyTextField(
                    value = uiState.amountFormatted,
                    onValueChange = {},
                    label = "Budget Amount",
                    readOnly = true,
                    modifier = Modifier.onFocusChanged { showAmountInput = it.isFocused }
                )
            }
        }
    }
}
