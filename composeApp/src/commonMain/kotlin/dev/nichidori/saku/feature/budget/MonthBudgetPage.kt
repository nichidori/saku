package dev.nichidori.saku.feature.budget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyButton
import dev.nichidori.saku.core.composable.MyTextField
import dev.nichidori.saku.core.composable.NumberKeyboard
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import kotlinx.datetime.Month

@Composable
fun MonthBudgetPage(
    viewModel: MonthBudgetViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()
    var amountText by remember { mutableStateOf("") }
    var showNumberKeyboard by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.budget) {
        uiState.budget?.let {
            amountText = it.baseAmount.toString()
        }
    }

    LaunchedEffect(uiState.saveStatus) {
        if (uiState.saveStatus is Success) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            MyAppBar(
                title = uiState.budget?.let { "${Month(it.month).name.take(3)} ${it.year}" } ?: "Budget",
                onUp = onUp
            )
        },
        bottomBar = {
            if (showNumberKeyboard) {
                NumberKeyboard(
                    onValueClick = {
                        amountText += it.toString()
                    },
                    onDeleteClick = {
                        if (amountText.isNotEmpty()) {
                            amountText = amountText.dropLast(1)
                        }
                    },
                    onActionClick = {
                        showNumberKeyboard = false
                    }
                )
            } else {
                MyButton(
                    text = "Save",
                    onClick = {
                        viewModel.save(amountText.toLongOrNull() ?: 0L)
                    },
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
                .padding(contentPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            uiState.budget?.let { budget ->
                Text(
                    budget.category.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.template?.let { template ->
                    Text(
                        "Default: ${template.defaultAmount.toRupiah()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                MyTextField(
                    value = (amountText.toLongOrNull() ?: 0L).toRupiah(),
                    onValueChange = {},
                    label = "Budget Amount",
                    readOnly = true,
                    modifier = Modifier.onFocusChanged { showNumberKeyboard = it.isFocused }
                )
            }
        }
    }
}
