package org.arraflydori.fin.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import org.arraflydori.fin.core.composable.MyButton
import org.arraflydori.fin.core.composable.MyTextField
import org.arraflydori.fin.domain.model.AccountType

// TODO: Fix l10n
@Composable
fun AccountPage(
    id: String?,
    onUp: () -> Unit,
    viewModel: AccountViewModel = viewModel { AccountViewModel() },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
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
        }
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
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
}
