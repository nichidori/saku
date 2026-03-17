package dev.nichidori.saku

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.nichidori.saku.core.composable.MyBox
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyNavBar
import dev.nichidori.saku.core.composable.NavBarDestination
import dev.nichidori.saku.core.theme.MyTheme
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.*
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.BudgetRepository
import dev.nichidori.saku.domain.repo.CategoryRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import dev.nichidori.saku.feature.account.AccountPage
import dev.nichidori.saku.feature.account.AccountViewModel
import dev.nichidori.saku.feature.budget.*
import dev.nichidori.saku.feature.category.CategoryPage
import dev.nichidori.saku.feature.category.CategoryViewModel
import dev.nichidori.saku.feature.categoryList.CategoryListPage
import dev.nichidori.saku.feature.categoryList.CategoryListViewModel
import dev.nichidori.saku.feature.home.HomePage
import dev.nichidori.saku.feature.home.HomeViewModel
import dev.nichidori.saku.feature.statistic.StatisticPage
import dev.nichidori.saku.feature.statistic.StatisticViewModel
import dev.nichidori.saku.feature.trx.TrxPage
import dev.nichidori.saku.feature.trx.TrxViewModel
import dev.nichidori.saku.feature.trxList.TrxListPage
import dev.nichidori.saku.feature.trxList.TrxListViewModel
import kotlinx.datetime.YearMonth
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable sealed interface Route {
    @Serializable data object Main : Route
    @Serializable data object Home : Route
    @Serializable data object Statistic : Route
    @Serializable data object CategoryList : Route
    @Serializable data object TrxList : Route
    @Serializable data class Account(val id: String?) : Route
    @Serializable data class Category(val id: String?) : Route
    @Serializable data class Trx(val id: String?) : Route
    @Serializable data class CategoryBudget(val templateId: String) : Route
    @Serializable data class DefaultBudget(val templateId: String?) : Route
    @Serializable data class MonthBudget(val budgetId: String) : Route
}

@Composable
fun App(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    trxRepository: TrxRepository,
    budgetRepository: BudgetRepository
) {
    val focusManager = LocalFocusManager.current
    val rootNavController = rememberNavController()

    MyTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                },
        ) {
            NavHost(
                rootNavController,
                startDestination = Route.Main,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start)
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End)
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
                }
            ) {
                composable<Route.Main> {
                    MainContainer(
                        rootNavController = rootNavController,
                        accountRepository = accountRepository,
                        categoryRepository = categoryRepository,
                        trxRepository = trxRepository,
                        budgetRepository = budgetRepository
                    )
                }
                composable<Route.CategoryList> {
                    CategoryListPage(
                        viewModel = viewModel {
                            CategoryListViewModel(categoryRepository)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onNewCategoryClick = {
                            rootNavController.navigate(Route.Category(id = null))
                        },
                        onCategoryClick = { id ->
                            rootNavController.navigate(Route.Category(id))
                        }
                    )
                }
                composable<Route.Account> { backStackEntry ->
                    val account = backStackEntry.toRoute<Route.Account>()
                    AccountPage(
                        viewModel = viewModel {
                            AccountViewModel(accountRepository, account.id)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onSaveSuccess = { rootNavController.popBackStack() },
                        onDeleteSuccess = { rootNavController.popBackStack() }
                    )
                }
                composable<Route.Category> { backStackEntry ->
                    val category = backStackEntry.toRoute<Route.Category>()
                    CategoryPage(
                        viewModel = viewModel {
                            CategoryViewModel(categoryRepository, category.id)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onSaveSuccess = { rootNavController.popBackStack() },
                        onDeleteSuccess = { rootNavController.popBackStack() }
                    )
                }
                composable<Route.Trx> { backStackEntry ->
                    val trx = backStackEntry.toRoute<Route.Trx>()
                    TrxPage(
                        viewModel = viewModel {
                            TrxViewModel(
                                accountRepository,
                                categoryRepository,
                                trxRepository,
                                trx.id
                            )
                        },
                        onUp = { rootNavController.popBackStack() },
                        onSaveSuccess = { rootNavController.popBackStack() },
                        onDeleteSuccess = { rootNavController.popBackStack() },
                    )
                }
                composable<Route.CategoryBudget> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.CategoryBudget>()
                    CategoryBudgetPage(
                        viewModel = viewModel {
                            CategoryBudgetViewModel(budgetRepository, route.templateId)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onDefaultBudgetClick = { rootNavController.navigate(Route.DefaultBudget(it)) },
                        onMonthBudgetClick = { rootNavController.navigate(Route.MonthBudget(it)) }
                    )
                }
                composable<Route.DefaultBudget> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.DefaultBudget>()
                    DefaultBudgetPage(
                        viewModel = viewModel {
                            DefaultBudgetViewModel(categoryRepository, budgetRepository, route.templateId)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onSaveSuccess = { rootNavController.popBackStack() }
                    )
                }
                composable<Route.MonthBudget> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.MonthBudget>()
                    MonthBudgetPage(
                        viewModel = viewModel {
                            MonthBudgetViewModel(budgetRepository, route.budgetId)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onSaveSuccess = { rootNavController.popBackStack() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    rootNavController: NavHostController,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    trxRepository: TrxRepository,
    budgetRepository: BudgetRepository,
) {
    val innerNavController = rememberNavController()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInputOption by remember { mutableStateOf(false) }
    var selectedMonth by rememberSaveable { mutableStateOf(Clock.System.now().toYearMonth()) }
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            MyNavBar(
                selectedDestination = when {
                    currentDestination?.hierarchy?.any { it.hasRoute<Route.Home>() } == true -> NavBarDestination.Home
                    currentDestination?.hierarchy?.any { it.hasRoute<Route.TrxList>() } == true -> NavBarDestination.Trx
                    currentDestination?.hierarchy?.any { it.hasRoute<Route.Statistic>() } == true -> NavBarDestination.Statistic
                    else -> null
                },
                onHomeClick = {
                    val currentDestination = innerNavController.currentBackStackEntry?.destination
                    if (currentDestination?.hierarchy?.none { it.hasRoute<Route.Home>() } == true) {
                        innerNavController.navigate(Route.Home) {
                            popUpTo(Route.Home) {
                                inclusive = true
                            }
                        }
                    }
                },
                onTrxClick = {
                    val currentDestination = innerNavController.currentBackStackEntry?.destination
                    if (currentDestination?.hierarchy?.none { it.hasRoute<Route.TrxList>() } == true) {
                        innerNavController.navigate(Route.TrxList) {
                            popUpTo(Route.Home) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onStatisticClick = {
                    val currentDestination = innerNavController.currentBackStackEntry?.destination
                    if (currentDestination?.hierarchy?.none { it.hasRoute<Route.Statistic>() } == true) {
                        innerNavController.navigate(Route.Statistic) {
                            popUpTo(Route.Home) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onAddClick = {
                    rootNavController.navigate(Route.Trx(id = null))
                },
                onAddLongPress = {
                    showInputOption = true
                },
            )
        }
    ) { contentPadding ->
        val monthChipsListState = rememberLazyListState()

        NavHost(
            innerNavController,
            startDestination = Route.Home,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding)
        ) {
            composable<Route.Home> {
                HomePage(
                    viewModel = viewModel {
                        HomeViewModel(accountRepository, trxRepository, budgetRepository)
                    },
                    onCategoryClick = {
                        rootNavController.navigate(Route.CategoryList)
                    },
                    onAccountClick = { id ->
                        rootNavController.navigate(Route.Account(id))
                    },
                    onNewAccountClick = {
                        rootNavController.navigate(Route.Account(id = null))
                    },
                    onBudgetClick = { templateId ->
                        rootNavController.navigate(Route.CategoryBudget(templateId))
                    },
                    onNewBudgetClick = {
                        rootNavController.navigate(Route.DefaultBudget(templateId = null))
                    }
                )
            }
            composable<Route.TrxList> {
                TrxListPage(
                    initialMonth = selectedMonth,
                    viewModel = viewModel {
                        TrxListViewModel(accountRepository, categoryRepository, trxRepository)
                    },
                    monthChipsListState = monthChipsListState,
                    onMonthChange = { month ->
                        selectedMonth = month
                    },
                    onTrxClick = { id ->
                        rootNavController.navigate(Route.Trx(id))
                    }
                )
            }
            composable<Route.Statistic> {
                StatisticPage(
                    initialMonth = selectedMonth,
                    viewModel = viewModel {
                        StatisticViewModel(trxRepository)
                    },
                    monthChipsListState = monthChipsListState,
                    onMonthChange = { month ->
                        selectedMonth = month
                    }
                )
            }
        }

        if (showInputOption) {
            ModalBottomSheet(
                onDismissRequest = { showInputOption = false },
                shape = MyDefaultShape.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                InputOptionSelector(
                    onAccountClick = {
                        rootNavController.navigate(Route.Account(id = null))
                        showInputOption = false
                    },
                    onCategoryClick = {
                        rootNavController.navigate(Route.Category(id = null))
                        showInputOption = false
                    },
                    onTrxClick = {
                        rootNavController.navigate(Route.Trx(id = null))
                        showInputOption = false
                    },
                )
            }
        }
    }
}

@Composable
fun InputOptionSelector(
    onAccountClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onTrxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Add",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
        )
        InputOption(label = "Account", onClick = onAccountClick)
        InputOption(label = "Category", onClick = onCategoryClick)
        InputOption(label = "Transaction", onClick = onTrxClick)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InputOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MyBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    val accountRepository = object : AccountRepository {
        override suspend fun addAccount(
            name: String,
            initialAmount: Long,
            type: AccountType
        ) {
        }

        override suspend fun getAccountById(id: String): Account? = null
        override suspend fun getAllAccounts(): List<Account> = emptyList()
        override suspend fun updateAccount(
            id: String,
            name: String,
            initialAmount: Long,
            type: AccountType
        ) {
        }

        override suspend fun deleteAccount(id: String) {}
        override suspend fun getTotalBalance(): Long = 0
    }
    val categoryRepository = object : CategoryRepository {
        override suspend fun addCategory(name: String, type: TrxType, icon: String?, parent: Category?) {}
        override suspend fun getCategoryById(id: String): Category? = null
        override suspend fun getAllCategories(): List<Category> = emptyList()
        override suspend fun getRootCategories(): List<Category> = emptyList()
        override suspend fun getSubcategories(parentId: String): List<Category> = emptyList()
        override suspend fun updateCategory(
            id: String,
            name: String,
            type: TrxType,
            icon: String?,
            parent: Category?
        ) {
        }

        override suspend fun deleteCategory(id: String) {}
    }
    val trxRepository = object : TrxRepository {
        override suspend fun addTrx(
            type: TrxType,
            transactionAt: Instant,
            amount: Long,
            description: String,
            sourceAccount: Account,
            targetAccount: Account?,
            category: Category?,
            note: String
        ) {
        }

        override suspend fun getTrxById(id: String): Trx? = null
        override suspend fun getFilteredTrxs(filter: TrxFilter): List<Trx> = emptyList()
        override suspend fun updateTrx(
            id: String,
            type: TrxType,
            transactionAt: Instant,
            amount: Long,
            description: String,
            sourceAccount: Account,
            targetAccount: Account?,
            category: Category?,
            note: String
        ) {
        }

        override suspend fun deleteTrx(id: String) {}
    }
    val budgetRepository = object : BudgetRepository {
        override suspend fun addBudgetTemplate(category: Category, defaultAmount: Long) {}
        override suspend fun getBudgetTemplateById(id: String): BudgetTemplate? = null
        override suspend fun getBudgetTemplateByCategoryId(categoryId: String): BudgetTemplate? = null
        override suspend fun getAllBudgetTemplates(): List<BudgetTemplate> = emptyList()
        override suspend fun updateBudgetTemplate(id: String, category: Category, defaultAmount: Long) {}
        override suspend fun deleteBudgetTemplate(id: String) {}
        override suspend fun ensureBudgetsExist(now: YearMonth) {}
        override suspend fun getBudgetById(id: String): Budget? = null
        override suspend fun getBudgetsByYearMonth(month: YearMonth): List<Budget> = emptyList()
        override suspend fun getBudgetsByCategory(categoryId: String): List<Budget> = emptyList()
        override suspend fun updateBudget(id: String, baseAmount: Long, spentAmount: Long) {}
        override suspend fun deleteBudget(id: String) {}
    }
    App(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        trxRepository = trxRepository,
        budgetRepository = budgetRepository
    )
}

@Preview
@Composable
fun InputOptionSelectorPreview() {
    InputOptionSelector(
        onAccountClick = {},
        onCategoryClick = {},
        onTrxClick = {}
    )
}