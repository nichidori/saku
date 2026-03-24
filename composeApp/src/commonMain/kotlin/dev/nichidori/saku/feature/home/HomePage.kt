package dev.nichidori.saku.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun HomePage(
    viewModel: HomeViewModel,
    onMenuClick: () -> Unit,
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
        onMenuClick = onMenuClick,
        onAccountClick = onAccountClick,
        onNewAccountClick = onNewAccountClick,
        onBudgetClick = onBudgetClick,
        onNewBudgetClick = onNewBudgetClick,
        onBalanceToggle = viewModel::onBalanceToggle,
        modifier = modifier
    )
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
    budgets: List<ActiveBudget>,
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
                budgets.forEach { activeBudget ->
                    BudgetItem(
                        activeBudget = activeBudget,
                        onClick = { onBudgetClick(activeBudget.budget.templateId) }
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
    activeBudget: ActiveBudget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = activeBudget.budget
    val dailyAllowance = activeBudget.dailyAllowance

    MyBox(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                budget.category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            BudgetProgressBar(
                spentAmount = budget.spentAmount,
                baseAmount = budget.baseAmount,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    budget.remainingAmount.toRupiah() + (if (budget.remainingAmount >= 0) " left" else " overspent"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (budget.remainingAmount < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                if (dailyAllowance > 0) {
                    Text(
                        "${dailyAllowance.toRupiah()} / day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

        }
    }
}

@Composable
fun BudgetProgressBar(
    spentAmount: Long,
    baseAmount: Long,
    modifier: Modifier = Modifier
) {
    val spentRatio = if (baseAmount > 0) {
        (spentAmount.toFloat() / baseAmount.toFloat()).coerceAtLeast(0f)
    } else {
        0f
    }

    var animatedSpentRatio by rememberSaveable { mutableFloatStateOf(0f) }
    val animatedSpent by animateFloatAsState(
        targetValue = animatedSpentRatio,
        animationSpec = tween(durationMillis = 500)
    )

    LaunchedEffect(spentRatio) {
        animatedSpentRatio = spentRatio
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val indicatorColor = MaterialTheme.colorScheme.background

    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val currentMonth = YearMonth(currentDate.year, currentDate.month)
    val dayOfMonth = currentDate.day
    val daysInCurrentMonth = currentMonth.days.size
    val dateIndicatorRatio = dayOfMonth.toFloat() / daysInCurrentMonth.toFloat()

    Canvas(modifier = modifier.fillMaxWidth().height(8.dp).clip(MyDefaultShape)) {
        val width = size.width
        val height = size.height
        val shapeRadius = MyDefaultShape.topStart.toPx(Size(width, height), this)
        val cornerRadius = CornerRadius(shapeRadius, shapeRadius)

        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = cornerRadius
        )

        val clampedSpent = animatedSpent.coerceAtMost(1f)
        val overspend = (animatedSpent - 1f).coerceAtLeast(0f)

        if (clampedSpent > 0f) {
            drawRoundRect(
                color = primaryColor,
                size = Size(width * clampedSpent, height),
                cornerRadius = cornerRadius
            )
        }

        if (overspend > 0f) {
            drawRoundRect(
                color = errorColor,
                size = Size(width * overspend, height),
                cornerRadius = cornerRadius
            )
        }

        val indicatorX = width * dateIndicatorRatio
        drawLine(
            color = indicatorColor,
            start = Offset(indicatorX, 0f),
            end = Offset(indicatorX, height),
            strokeWidth = 4.dp.toPx()
        )
    }
}


