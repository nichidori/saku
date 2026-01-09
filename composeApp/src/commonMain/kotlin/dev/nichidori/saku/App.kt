package dev.nichidori.saku

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
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
import dev.nichidori.saku.core.composable.MyDefaultShape
import dev.nichidori.saku.core.composable.MyNavBar
import dev.nichidori.saku.core.composable.NavBarDestination
import dev.nichidori.saku.core.theme.MyTheme
import dev.nichidori.saku.core.util.toYearMonth
import dev.nichidori.saku.domain.model.*
import dev.nichidori.saku.domain.repo.AccountRepository
import dev.nichidori.saku.domain.repo.CategoryRepository
import dev.nichidori.saku.domain.repo.TrxRepository
import dev.nichidori.saku.feature.account.AccountPage
import dev.nichidori.saku.feature.account.AccountViewModel
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
}

@Composable
fun App(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    trxRepository: TrxRepository
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
                        trxRepository = trxRepository
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
) {
    val innerNavController = rememberNavController()
    val sheetState = rememberModalBottomSheetState()
    var showInputOption by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(Clock.System.now().toYearMonth()) }
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
                        HomeViewModel(accountRepository, trxRepository)
                    },
                    onCategoryClick = {
                        rootNavController.navigate(Route.CategoryList)
                    },
                    onAccountClick = { id ->
                        rootNavController.navigate(Route.Account(id))
                    },
                    onNewAccountClick = {
                        rootNavController.navigate(Route.Account(id = null))
                    }
                )
            }
            composable<Route.TrxList> {
                TrxListPage(
                    initialMonth = selectedMonth,
                    viewModel = viewModel {
                        TrxListViewModel(accountRepository, categoryRepository, trxRepository)
                    },
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
                    onMonthChange = { month ->
                        selectedMonth = month
                    }
                )
            }
        }

        if (showInputOption) {
            ModalBottomSheet(
                dragHandle = null,
                onDismissRequest = { showInputOption = false },
                sheetState = sheetState,
                shape = MyDefaultShape,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 24.dp
                    )
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(16.dp)
    ) {
        InputOption(label = "Add Account", onClick = onAccountClick)
        InputOption(label = "Add Category", onClick = onCategoryClick)
        InputOption(label = "Add Transaction", onClick = onTrxClick)
    }
}

@Composable
fun InputOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = MyDefaultShape
            )
            .clip(MyDefaultShape)
            .focusProperties { canFocus = false }
            .clickable { onClick() }
            .height(48.dp)
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
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
        override suspend fun addCategory(name: String, type: TrxType, parent: Category?) {}
        override suspend fun getCategoryById(id: String): Category? = null
        override suspend fun getAllCategories(): List<Category> = emptyList()
        override suspend fun getRootCategories(): List<Category> = emptyList()
        override suspend fun getSubcategories(parentId: String): List<Category> = emptyList()
        override suspend fun updateCategory(
            id: String,
            name: String,
            type: TrxType,
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
    App(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        trxRepository = trxRepository
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