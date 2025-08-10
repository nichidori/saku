package org.arraflydori.fin.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.arraflydori.fin.core.composable.MyDefaultShape
import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.AccountType

@Composable
fun HomePage(
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("August", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            TrendCard(
                title = "Net Worth",
                value = "Rp 10.000.000"
            )
            Spacer(modifier = Modifier.height(16.dp))
            AccountSection(onAccountClick = onAccountClick)
            Spacer(modifier = Modifier.height(24.dp))
            RecentActivities()
        }
    }
}

@Composable
fun TrendCard(title: String, value: String, modifier: Modifier = Modifier) {
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
            Text(value, style = MaterialTheme.typography.titleMedium)
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
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = List(8) { 900_000L }
    val rows = items.chunked(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { amount ->
                    AccountCard(
                        account = Account(
                            id = "",
                            name = "Bank BCA",
                            initialAmount = amount,
                            currentAmount = amount,
                            type = AccountType.Bank,
                            createdAt = 0,
                            updatedAt = null,
                        ),
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
fun RecentActivities() {
    Column {
        Text(
            "Recent Activities",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        TransactionCard(
            title = "Dinner",
            subtitle = "BCA · Needs",
            amount = "Rp 30.000",
            time = "19.42"
        )
        Spacer(modifier = Modifier.height(8.dp))
        TransactionCard(
            title = "Dinner",
            subtitle = "BCA · Needs",
            amount = "Rp 30.000",
            time = "19.42"
        )
        Spacer(modifier = Modifier.height(8.dp))
        TransactionCard(
            title = "Dinner",
            subtitle = "BCA · Needs",
            amount = "Rp 30.000",
            time = "19.42"
        )
    }
}


@Composable
fun TransactionCard(
    title: String,
    subtitle: String,
    amount: String,
    time: String,
    modifier: Modifier = Modifier
) {
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
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, style = MaterialTheme.typography.titleMedium)
                Text(time, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}