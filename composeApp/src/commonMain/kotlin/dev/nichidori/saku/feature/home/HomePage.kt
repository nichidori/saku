package dev.nichidori.saku.feature.home

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import dev.nichidori.saku.core.composable.MyBox
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyIconButton
import dev.nichidori.saku.core.model.Status.Failure
import dev.nichidori.saku.core.model.Status.Success
import dev.nichidori.saku.core.util.collectAsStateWithLifecycleIfAvailable
import dev.nichidori.saku.core.util.toRupiah
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
fun HomePage(
    viewModel: HomeViewModel,
    onCategoryClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    onBudgetClick: (String) -> Unit,
    onNewBudgetClick: () -> Unit,
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
        onBudgetClick = onBudgetClick,
        onNewBudgetClick = onNewBudgetClick,
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
    onBudgetClick: (String) -> Unit,
    onNewBudgetClick: () -> Unit,
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
                MyIconButton(
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
                        MyIconButton(onClick = onBalanceToggle) {
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
                    is Success<*>, is Failure<*> -> {
                        AccountSection(
                            accounts = uiState.accounts,
                            showBalance = uiState.showBalance,
                            onAccountClick = onAccountClick,
                            onNewAccountClick = onNewAccountClick,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BudgetSection(
                            month = when (val status = uiState.loadStatus) {
                                is Success -> status.data
                                else -> null
                            },
                            budgets = uiState.budgets,
                            onBudgetClick = onBudgetClick,
                            onNewBudgetClick = onNewBudgetClick,
                        )
                    }

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
    MyBox(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelSmall)
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
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
            MyIconButton(onClick = onNewAccountClick) {
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
            Text(
                "No accounts yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
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
    MyBox(
        modifier = modifier
            .clip(MyDefaultShape)
            .clickable { onClick(account.id) },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(account.name, style = MaterialTheme.typography.labelSmall)
            Text(
                account.balanceFormatted(show = showBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun BudgetSection(
    month: YearMonth?,
    budgets: List<Budget>,
    onBudgetClick: (String) -> Unit,
    onNewBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp)
                .fillMaxWidth()
        ) {
            val monthSuffix = month?.let {
                val date = LocalDate(
                    year = it.year,
                    month = it.month,
                    day = 1
                )

                val monthName = date.format(LocalDate.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) })
                val year = (date.year % 100).toString().padStart(2, '0')

                "  •  $monthName $year"
            } ?: ""

            Text(
                "Budget$monthSuffix",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            MyIconButton(onClick = onNewBudgetClick) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "New Budget"
                )
            }
        }
        if (budgets.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                budgets.forEach { budget ->
                    BudgetItem(
                        budget = budget,
                        onClick = { onBudgetClick(budget.templateId) }
                    )
                }
            }
        } else {
            Text(
                "No budgets yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun BudgetItem(
    budget: Budget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MyBox(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    budget.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    budget.remainingAmount.toRupiah(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (budget.remainingAmount < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val progress = if (budget.baseAmount > 0) {
                (budget.spentAmount.toFloat() / budget.baseAmount.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MyDefaultShape),
                strokeCap = StrokeCap.Square,
                color = if (progress >= 1f) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(modifier = Modifier.height(4.dp))
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
        budgets = listOf(
            Budget(
                id = "b1",
                templateId = "t1",
                category = Category(
                    id = "c1",
                    name = "Food",
                    type = TrxType.Expense,
                    createdAt = Clock.System.now(),
                    updatedAt = null
                ),
                month = 3,
                year = 2026,
                baseAmount = 1000000,
                spentAmount = 300000,
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
        onBudgetClick = {},
        onNewBudgetClick = {},
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
