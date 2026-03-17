package dev.nichidori.saku.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.*
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.domain.model.BudgetStatus
import dev.nichidori.saku.domain.model.status
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames

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
                title = "Monthly Budget",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            "Category",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            budget.category.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp).weight(1f))
                    MyBox(
                        modifier = Modifier.background(
                            color = if (budget.status.isActive) MaterialTheme.colorScheme.secondary
                            else Color.Transparent
                        )
                    ) {
                        val date = LocalDate(
                            year = budget.year,
                            month = budget.month,
                            day = 1
                        )
                        val month = date.format(LocalDate.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) })
                        val year = (date.year % 100).toString().padStart(2, '0')

                        Text(
                            "$month $year",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val pastBudget = budget.status == BudgetStatus.Past
                MyTextField(
                    value = uiState.amountFormatted,
                    onValueChange = {},
                    label = "Amount",
                    enabled = !pastBudget,
                    readOnly = true,
                    trailingIcon = if (!pastBudget) {
                        {
                            MyTextButton(
                                text = "Default",
                                onClick = {
                                    onAmountChange(uiState.defaultAmount?.toString() ?: "")
                                },
                                enabled = uiState.canDefault,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier.onFocusChanged { showAmountInput = it.isFocused }
                )
                if (pastBudget) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Past budget cannot be changed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
