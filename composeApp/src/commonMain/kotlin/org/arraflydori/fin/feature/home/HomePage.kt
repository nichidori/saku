package org.arraflydori.fin.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.arraflydori.fin.core.composable.MyDefaultShape
import org.arraflydori.fin.core.util.format
import org.arraflydori.fin.core.util.toRupiah
import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.Trx
import kotlin.time.Clock

@Composable
fun HomePage(
    viewModel: HomeViewModel,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(month = Clock.System.now().toYearMonth())
    }

    Scaffold(
        modifier = modifier
    ) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Text("August", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                TrendCard(title = "Net Worth", value = uiState.netWorth)
                Spacer(modifier = Modifier.height(16.dp))
                AccountSection(accounts = uiState.accounts, onAccountClick = onAccountClick)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recent Activities", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(uiState.trxs) { trx ->
                TransactionCard(trx = trx)
            }
        }
    }
}

@Composable
fun TrendCard(title: String, value: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            )
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value.toRupiah(), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MyDefaultShape
                    )
            ) {
                // TODO: Draw line chart here
            }
        }
    }
}

@Composable
fun AccountSection(
    accounts: List<Account>,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = accounts.chunked(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { account ->
                    AccountCard(
                        account = account,
                        onClick = onAccountClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (row.size < 3) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AccountCard(account: Account, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .clickable { onClick(account.id) },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(account.name, style = MaterialTheme.typography.labelSmall)
            Text(account.currentAmount.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TransactionCard(trx: Trx, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            ),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MyDefaultShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(trx.name, style = MaterialTheme.typography.titleMedium)
                Text(trx.sourceAccount.name, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(trx.amount.toRupiah(), style = MaterialTheme.typography.titleMedium)
                Text(trx.transactionAt.format(LocalDateTime.Format {
                    hour()
                    minute()
                }), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}