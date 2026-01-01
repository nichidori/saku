package dev.nichidori.saku.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyNoData
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
fun HomePage(
    viewModel: HomeViewModel,
    onCategoryClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    LaunchedEffect(Unit) {
        val month = Clock.System.now().toYearMonth()
        viewModel.load(month = month)
    }

    HomePageContent(
        uiState = uiState,
        onCategoryClick = onCategoryClick,
        onAccountClick = onAccountClick,
        onNewAccountClick = onNewAccountClick,
        onBalanceToggle = viewModel::onBalanceToggle,
        modifier = modifier
    )
}

@Composable
fun HomePageContent(
    uiState: HomeUiState,
    onCategoryClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    onBalanceToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .height(60.dp)
            ) {
                Text(
                    "Saku",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = onCategoryClick
                ) {
                    Icon(imageVector = Lucide.Menu, contentDescription = "Open category list")
                }
            }
        },
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            modifier = Modifier.padding(contentPadding)
        ) {
            item {
                TrendCard(
                    title = "Net Worth",
                    value = uiState.netWorthFormatted,
                    action = {
                        IconButton(onClick = onBalanceToggle) {
                            Icon(
                                imageVector = if (uiState.showBalance) Lucide.EyeOff else Lucide.Eye,
                                contentDescription = "Toggle balance visibility"
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                when (uiState.loadStatus) {
                    is Success<*>, is Failure<*> -> AccountSection(
                        accounts = uiState.accounts,
                        showBalance = uiState.showBalance,
                        onAccountClick = onAccountClick,
                        onNewAccountClick = onNewAccountClick,
                    )

                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun TrendCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MyDefaultShape
            )
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelSmall)
                    Text(value, style = MaterialTheme.typography.titleMedium)
                }
                action()
            }

            // TODO: Draw line chart here
//            Spacer(modifier = Modifier.height(16.dp))
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(80.dp)
//                    .background(
//                        color = MaterialTheme.colorScheme.primaryContainer,
//                        shape = MyDefaultShape
//                    )
//            ) {
//            }
        }
    }
}

@Composable
fun AccountSection(
    accounts: List<Account>,
    showBalance: Boolean,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNewAccountClick) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "New Account"
                )
            }
        }
        if (accounts.isNotEmpty()) {
            accounts.chunked(2).forEachIndexed { i, row ->
                if (i > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    row.forEach { account ->
                        AccountCard(
                            account = account,
                            showBalance = showBalance,
                            onClick = onAccountClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            MyNoData(
                message = "No accounts yet",
                contentDescription = "No accounts",
                modifier = Modifier.height(200.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    showBalance: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text(
                account.balanceFormatted(show = showBalance),
                style = MaterialTheme.typography.titleMedium
            )
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
        description = "Salary",
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
        loadStatus = Success(Clock.System.now().toYearMonth()),
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
                description = "Groceries",
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
    HomePageContent(
        uiState = uiState,
        onCategoryClick = {},
        onAccountClick = {},
        onNewAccountClick = {},
        onBalanceToggle = {}
    )
}

@Preview
@Composable
fun TrendCardPreview() {
    TrendCard(title = "Net Worth", value = "Rp 1.310.000")
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
    AccountSection(
        accounts = accounts,
        showBalance = true,
        onAccountClick = {},
        onNewAccountClick = {})
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
    AccountSection(
        accounts = accounts,
        showBalance = true,
        onAccountClick = {},
        onNewAccountClick = {})
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
    AccountSection(
        accounts = accounts,
        showBalance = true,
        onAccountClick = {},
        onNewAccountClick = {})
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
    AccountCard(account = account, showBalance = true, onClick = {})
}
