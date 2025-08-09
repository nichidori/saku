package org.arraflydori.fin.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.arraflydori.fin.core.composable.MyButton
import org.arraflydori.fin.core.composable.MyTextField
import org.arraflydori.fin.domain.model.AccountType

// TODO: Fix l10n
@Composable
fun AccountPage(
    id: String?,
    viewModel: AccountViewModel = viewModel { AccountViewModel() },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        )

        // TODO: Customize keyboard actions to Next
        MyTextField(
            value = uiState.name,
            onValueChange = { viewModel.onNameChange(it) },
            label = "Name",
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )

        // TODO: Use custom number keyboard
        MyTextField(
            value = uiState.balance,
            onValueChange = { viewModel.onBalanceChange(it) },
            label = "Balance"
        )

        // TODO: Show AccountType as input
        MyTextField(
            value = when (uiState.type) {
                AccountType.Cash -> "Cash"
                AccountType.Bank -> "Bank"
                AccountType.Credit -> "Credit"
                AccountType.Ewallet -> "Ewallet"
                AccountType.Emoney -> "Emoney"
                null -> ""
            },
            onValueChange = { viewModel.onTypeChange(AccountType.Cash) },
            label = "Type"
        )

        Spacer(modifier = Modifier.weight(1f))

        MyButton(
            text = "Save",
            onClick = { viewModel.saveAccount() }
        )
    }
}
