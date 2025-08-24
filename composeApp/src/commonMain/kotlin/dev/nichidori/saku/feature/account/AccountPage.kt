package dev.nichidori.saku.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.nichidori.saku.core.composable.AccountTypeSelector
import dev.nichidori.saku.core.composable.MyAppBar
import dev.nichidori.saku.core.composable.MyButton
import dev.nichidori.saku.core.composable.MyTextField
import dev.nichidori.saku.core.composable.NumberKeyboard
import dev.nichidori.saku.core.composable.label
import dev.nichidori.saku.core.model.Status
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.platform.ToastDuration
import dev.nichidori.saku.core.platform.showToast
import dev.nichidori.saku.domain.model.AccountType
import org.jetbrains.compose.ui.tooling.preview.Preview

// TODO: Fix l10n
@Composable
fun AccountPage(
    viewModel: AccountViewModel,
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

    AccountPageContent(
        uiState = uiState,
        typeOptions = viewModel.typeOptions,
        onUp = onUp,
        onNameChange = viewModel::onNameChange,
        onBalanceChange = viewModel::onBalanceChange,
        onTypeChange = viewModel::onTypeChange,
        onSaveClick = viewModel::saveAccount,
        modifier = modifier
    )
}

@Composable
fun AccountPageContent(
    uiState: AccountUiState,
    typeOptions: List<AccountType>,
    onUp: () -> Unit,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBalanceInput by remember { mutableStateOf(false) }
    var showTypeInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            MyAppBar(title = "Account", onUp = onUp)
        },
        bottomBar = {
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding()
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
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(bottom = bottomPadding)
                    )
                }
                showTypeInput -> {
                    AccountTypeSelector(
                        types = typeOptions,
                        onSelected = {
                            onTypeChange(it)
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
                        modifier = modifier
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
            MyTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = "Name",
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            MyTextField(
                value = uiState.balanceFormatted,
                onValueChange = { },
                label = "Balance",
                readOnly = true,
                modifier = Modifier.onFocusChanged { focusState ->
                    showBalanceInput = focusState.isFocused
                }
            )

            MyTextField(
                value = uiState.type?.label().orEmpty(),
                onValueChange = {},
                label = "Type",
                readOnly = true,
                modifier = Modifier.onFocusChanged { focusState ->
                    showTypeInput = focusState.isFocused
                }
            )
        }
    }
}

@Preview
@Composable
fun AccountPageContentPreview() {
    AccountPageContent(
        uiState = AccountUiState(
            name = "My Bank Account",
            balance = 1000000L,
            type = AccountType.Bank
        ),
        typeOptions = AccountType.entries,
        onUp = {},
        onNameChange = {},
        onBalanceChange = {},
        onTypeChange = {},
        onSaveClick = {}
    )
}