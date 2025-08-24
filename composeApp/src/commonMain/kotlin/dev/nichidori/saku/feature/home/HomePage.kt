package dev.nichidori.saku.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.MonthNames
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.util.format
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxType
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
fun HomePage(
    viewModel: HomeViewModel,
    onAccountClick: (String) -> Unit,
    onTrxClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(month = Clock.System.now().toYearMonth())
    }

    HomePageContent(
        uiState = uiState,
        onAccountClick = onAccountClick,
        onTrxClick = onTrxClick,
        modifier = modifier
    )
}

@Composable
fun HomePageContent(
    uiState: HomeUiState,
    onAccountClick: (String) -> Unit,
    onTrxClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.consumeWindowInsets(contentPadding)
        ) {
            item {
                Text("August", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                TrendCard(title = "Net Worth", value = uiState.netWorthFormatted)
                Spacer(modifier = Modifier.height(16.dp))
                AccountSection(accounts = uiState.accounts, onAccountClick = onAccountClick)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recent Activities", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(uiState.trxs) { trx ->
                TransactionCard(trx = trx, onClick = onTrxClick)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview
@Composable
fun HomePageContentPreview() {
    val sampleAccount = Account(
        id = "1",
        name = "Cash",
        initialAmount = 100000,
        currentAmount = 150000,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )
    val sampleTrx = Trx.Income(
        id = "trx1",
        name = "Salary",
        amount = 5000000,
        category = Category(
            id = "cat1",
            name = "Income",
            type = TrxType.Income,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
        sourceAccount = sampleAccount,
        transactionAt = Clock.System.now(),
        note = "Monthly salary",
        createdAt = Clock.System.now(),
        updatedAt = null
    )
    val uiState = HomeUiState(
        isLoading = false,
        currentMonth = YearMonth(2023, Month.AUGUST),
        netWorth = 10000000,
        netWorthTrend = listOf(1f, 1.2f, 1.1f, 1.3f),
        accounts = listOf(
            sampleAccount,
            Account(
                id = "2",
                name = "Bank BCA",
                initialAmount = 5000000,
                currentAmount = 4500000,
                type = AccountType.Bank,
                createdAt = Clock.System.now(),
                updatedAt = null
            )
        ),
        trxs = listOf(
            sampleTrx,
            Trx.Expense(
                id = "trx2",
                name = "Groceries",
                amount = 200000,
                category = Category(
                    id = "cat2",
                    name = "Food",
                    type = TrxType.Expense,
                    createdAt = Clock.System.now(),
                    updatedAt = null
                ),
                sourceAccount = sampleAccount,
                transactionAt = Clock.System.now(),
                note = null,
                createdAt = Clock.System.now(),
                updatedAt = null
            )
        )
    )
    HomePageContent(uiState = uiState, onAccountClick = {}, onTrxClick = {})
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

@Preview
@Composable
fun TrendCardPreview() {
    TrendCard(title = "Net Worth", value = "Rp 1.310.000")
}

@Composable
fun AccountSection(
    accounts: List<Account>,
    onAccountClick: (String) -> Unit,
    spacing: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val rows = accounts.chunked(2)
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier,
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing)
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
            }
        }
    }
}

@Preview
@Composable
fun AccountSectionPreview() {
    val accounts = listOf(
        Account(
            id = "1",
            name = "Cash",
            initialAmount = 1000,
            currentAmount = 1500,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
        Account(
            id = "2",
            name = "Bank Mandiri",
            initialAmount = 5000,
            currentAmount = 4500,
            type = AccountType.Bank,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
        Account(
            id = "3",
            name = "GoPay",
            initialAmount = 200,
            currentAmount = 150,
            type = AccountType.Ewallet,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
        Account(
            id = "4",
            name = "OVO",
            initialAmount = 300,
            currentAmount = 250,
            type = AccountType.Ewallet,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
    )
    AccountSection(accounts = accounts, onAccountClick = {})
}

@Preview
@Composable
fun AccountSectionSinglePreview() {
    val accounts = listOf(
        Account(
            id = "1",
            name = "Cash",
            initialAmount = 1000,
            currentAmount = 1500,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
    )
    AccountSection(accounts = accounts, onAccountClick = {})
}

@Preview
@Composable
fun AccountSectionDoublePreview() {
    val accounts = listOf(
        Account(
            id = "1",
            name = "Cash",
            initialAmount = 1000,
            currentAmount = 1500,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
        Account(
            id = "2",
            name = "Bank Mandiri",
            initialAmount = 5000,
            currentAmount = 4500,
            type = AccountType.Bank,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
    )
    AccountSection(accounts = accounts, onAccountClick = {})
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
            Text(account.balanceFormatted(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview
@Composable
fun AccountCardPreview() {
    val account = Account(
        id = "1",
        name = "Bank BCA",
        initialAmount = 1000000,
        currentAmount = 1200000,
        type = AccountType.Bank,
        createdAt = Clock.System.now(),
        updatedAt = null
    )
    AccountCard(account = account, onClick = {})
}

@Composable
fun TransactionCard(trx: Trx, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .clickable { onClick(trx.id) },
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
                Text(
                    trx.transactionAt.format(LocalDateTime.Format {
                        day()
                        chars(" ")
                        monthName(MonthNames.ENGLISH_ABBREVIATED)
                        chars(" ")
                        year()
                        chars(" • ")
                        hour()
                        chars(":")
                        minute()
                    }),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Preview
@Composable
fun TransactionCardPreview() {
    val sampleAccount = Account(
        id = "1",
        name = "Cash",
        initialAmount = 100000,
        currentAmount = 150000,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )
    val trx = Trx.Expense(
        id = "trx123",
        name = "Lunch at Warteg",
        amount = 25000,
        category = Category(
            id = "cat1", name = "Food", type = TrxType.Expense,
            createdAt = Clock.System.now(),
            updatedAt = null
        ),
        sourceAccount = sampleAccount,
        transactionAt = Clock.System.now(),
        note = "Nasi, ayam, es teh",
        createdAt = Clock.System.now(),
        updatedAt = null
    )
    TransactionCard(trx = trx, onClick = {})
}