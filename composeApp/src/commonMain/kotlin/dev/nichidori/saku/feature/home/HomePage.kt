package dev.nichidori.saku.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyNoData
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import kotlinx.datetime.until
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
fun HomePage(
    initialMonth: YearMonth,
    viewModel: HomeViewModel,
    onMonthChange: (YearMonth) -> Unit,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    val base = YearMonth(1970, 1)
    val pagerState = rememberPagerState(
        initialPage = base.until(initialMonth, unit = DateTimeUnit.MONTH).toInt(),
        pageCount = { Int.MAX_VALUE }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val month = base.plus(page, unit = DateTimeUnit.MONTH)
            viewModel.load(month = month)
            onMonthChange(month)
        }
    }

    HorizontalPager(
        state = pagerState,
    ) {
        HomePageContent(
            uiState = uiState,
            onAccountClick = onAccountClick,
            onNewAccountClick = onNewAccountClick,
            modifier = modifier
        )
    }
}

@Composable
fun HomePageContent(
    uiState: HomeUiState,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.consumeWindowInsets(contentPadding)
        ) {
            item {
                TrendCard(title = "Net Worth", value = uiState.netWorthFormatted)
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                AccountSection(
                    accounts = uiState.accounts,
                    onAccountClick = onAccountClick,
                    onNewAccountClick = onNewAccountClick,
                )
            }
        }
    }
}

@Composable
fun TrendCard(title: String, value: String, modifier: Modifier = Modifier) {
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
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium)

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
        isLoading = false,
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
    HomePageContent(uiState = uiState, onAccountClick = {}, onNewAccountClick = {})
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
    AccountSection(accounts = accounts, onAccountClick = {}, onNewAccountClick = {})
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
    AccountSection(accounts = accounts, onAccountClick = {}, onNewAccountClick = {})
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
    AccountSection(accounts = accounts, onAccountClick = {}, onNewAccountClick = {})
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
