package org.arraflydori.fin.feature.account

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import org.arraflydori.fin.core.model.Status.Success
import org.arraflydori.fin.domain.model.AccountType

// TODO: Fix l10n
@Composable
fun AccountPage(
    viewModel: AccountViewModel,
    onUp: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBalanceInput by remember { mutableStateOf(false) }
    var showTypeInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.saveStatus) {
        if (uiState.saveStatus is Success) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                IconButton(onClick = { onUp() }) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account",
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
                    showBalanceInput -> {
                        NumberKeyboard(
                            onValueClick = {
                                viewModel.onBalanceChange(
                                    uiState.balance?.toString().orEmpty() + it.toString()
                                )
                            },
                            onDeleteClick = {
                                viewModel.onBalanceChange(
                                    uiState.balance?.toString().orEmpty().dropLast(1)
                                )
                            },
                            onDoneClick = {
                                focusManager.moveFocus(FocusDirection.Next)
                            },
                        )
                    }

                    showTypeInput -> {
                        AccountTypeSelector(
                            types = viewModel.typeOptions,
                            onSelected = {
                                viewModel.onTypeChange(it)
                                focusManager.moveFocus(FocusDirection.Next)
                            }
                        )
                    }

                    else -> {
                        MyButton(
                            text = "Save",
                            enabled = uiState.canSave,
                            onClick = { viewModel.saveAccount() },
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
            MyTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
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

@Composable
fun NumberKeyboard(
    onValueClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(16.dp).fillMaxWidth()
    ) {
        for (i in 1..3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (j in 1..3) {
                    val value = ((i - 1) * 3 + j)
                    KeyboardKey(
                        label = value.toString(),
                        onClick = { onValueClick(value) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyboardKey(
                label = "Delete",
                onClick = onDeleteClick,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            KeyboardKey(
                label = "0",
                onClick = { onValueClick(0) },
                modifier = Modifier.weight(1f)
            )
            KeyboardKey(
                label = "Done",
                onClick = onDoneClick,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KeyboardKey(
    label: String,
    onClick: () -> Unit,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = color ?: MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .focusProperties { canFocus = false }
            .clickable { onClick() }
            .height(48.dp)
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun AccountTypeSelector(
    types: List<AccountType>,
    onSelected: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures {}
            }
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        for (type in types) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MyDefaultShape
                    )
                    .clip(MyDefaultShape)
                    .focusProperties { canFocus = false }
                    .clickable { onSelected(type) }
                    .height(48.dp)
            ) {
                Text(
                    type.label(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

fun AccountType.label(): String {
    return when (this) {
        AccountType.Cash -> "Cash"
        AccountType.Bank -> "Bank"
        AccountType.Credit -> "Credit"
        AccountType.Ewallet -> "E-wallet"
        AccountType.Emoney -> "E-money"
    }
}