package dev.nichidori.saku.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import dev.nichidori.saku.core.composable.MyBox
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyIconButton
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
    appVersion: () -> String?,
    onCategoryClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    onBudgetClick: (String) -> Unit,
    onNewBudgetClick: () -> Unit,
    darkTheme: Boolean,
    onThemeToggle: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycleIfAvailable()

    val menuWidth = 240.dp
    val menuOffsetPx = with(LocalDensity.current) { menuWidth.toPx() }
    var showMenu by remember { mutableStateOf(false) }
    var themeToggleOffset by remember { mutableStateOf(Offset.Zero) }

    val menuTranslation by animateFloatAsState(
        targetValue = if (showMenu) 0f else menuOffsetPx,
        animationSpec = tween(durationMillis = 300),
        label = "menuTranslation"
    )

    val contentTranslation by animateFloatAsState(
        targetValue = if (showMenu) -menuOffsetPx else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "contentTranslation"
    )

    LaunchedEffect(Unit) {
        val month = Clock.System.now().toYearMonth()
        viewModel.load(month = month)
    }

    Box {
        HomePageContent(
            uiState = uiState,
            onMenuClick = { showMenu = !showMenu },
            onAccountClick = onAccountClick,
            onNewAccountClick = onNewAccountClick,
            onBudgetClick = onBudgetClick,
            onNewBudgetClick = onNewBudgetClick,
            onBalanceToggle = viewModel::onBalanceToggle,
            modifier = modifier.graphicsLayer { translationX = contentTranslation }
        )

        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable { showMenu = false }
                    .graphicsLayer { translationX = contentTranslation }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(menuWidth)
                .graphicsLayer { translationX = menuTranslation }
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick() }
                        .padding(12.dp)
                ) {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeToggle(themeToggleOffset) }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Dark Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (darkTheme) Lucide.Sun else Lucide.Moon,
                            contentDescription = "Toggle theme",
                            modifier = Modifier
                                .size(20.dp)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val size = coords.size
                                    themeToggleOffset = Offset(
                                        x = pos.x + size.width / 2f,
                                        y = pos.y + size.height / 2f,
                                    )
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                appVersion()?.let {
                    Text(
                        text = "v$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageContent(
    uiState: HomeUiState,
    onMenuClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNewAccountClick: () -> Unit,
    onBudgetClick: (String) -> Unit,
    onNewBudgetClick: () -> Unit,
    onBalanceToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Saku",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                expandedHeight = 48.dp,
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Lucide.Menu, contentDescription = "Open menu")
                    }
                }
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            modifier = Modifier.fillMaxSize().padding(contentPadding)
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
            if (uiState.loadStatus.isCompleted
                || uiState.accounts.isNotEmpty()
                || uiState.budgets.isNotEmpty()
            ) item {
                AccountSection(
                    accounts = uiState.accounts,
                    showBalance = uiState.showBalance,
                    onAccountClick = onAccountClick,
                    onNewAccountClick = onNewAccountClick,
                )
                Spacer(modifier = Modifier.height(16.dp))
                BudgetSection(
                    month = uiState.month,
                    budgets = uiState.budgets,
                    onBudgetClick = onBudgetClick,
                    onNewBudgetClick = onNewBudgetClick,
                )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Text(
                "Budget",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            month?.let {
                val date = LocalDate(
                    year = it.year,
                    month = it.month,
                    day = 1
                )

                val monthName = date.format(LocalDate.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) })
                val year = (date.year % 100).toString().padStart(2, '0')

                Text(
                    "  •  $monthName $year",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.weight(1f))
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
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

            var progress by rememberSaveable { mutableFloatStateOf(0f) }
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 500)
            )

            LaunchedEffect(budget.baseAmount, budget.spentAmount) {
                progress = if (budget.baseAmount > 0) {
                    (budget.spentAmount.toFloat() / budget.baseAmount.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MyDefaultShape),
                strokeCap = StrokeCap.Square,
                color = MaterialTheme.colorScheme.primary,
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
        loadStatus = Success(Unit),
        month = YearMonth(2026, 3),
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
                month = YearMonth(2026, 3),
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
        onMenuClick = {},
        onAccountClick = {},
        onNewAccountClick = {},
        onBudgetClick = {},
        onNewBudgetClick = {},
        onBalanceToggle = {},
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
