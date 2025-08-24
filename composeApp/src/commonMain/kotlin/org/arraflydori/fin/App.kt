package org.arraflydori.fin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.arraflydori.fin.core.composable.MyNavBar
import org.arraflydori.fin.domain.repo.AccountRepository
import org.arraflydori.fin.domain.repo.CategoryRepository
import org.arraflydori.fin.domain.repo.TrxRepository
import org.arraflydori.fin.feature.account.AccountPage
import org.arraflydori.fin.feature.account.AccountViewModel
import org.arraflydori.fin.feature.category.CategoryPage
import org.arraflydori.fin.feature.category.CategoryViewModel
import org.arraflydori.fin.feature.home.HomePage
import org.arraflydori.fin.feature.home.HomeViewModel
import org.arraflydori.fin.feature.statistic.StatisticPage
import org.arraflydori.fin.feature.statistic.StatisticViewModel
import org.arraflydori.fin.feature.trx.TrxPage
import org.arraflydori.fin.feature.trx.TrxViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Serializable sealed interface Route {
    @Serializable data object Main : Route
    @Serializable data object Home : Route
    @Serializable data object Statistic : Route
    @Serializable data class Account(val id: String?) : Route
    @Serializable data class Category(val id: String?) : Route
    @Serializable data class Trx(val id: String?) : Route
}

@Composable
@Preview
fun App(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    trxRepository: TrxRepository
) {
    val focusManager = LocalFocusManager.current
    val rootNavController = rememberNavController()

    MaterialTheme {
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
                composable<Route.Account> { backStackEntry ->
                    val account = backStackEntry.toRoute<Route.Account>()
                    AccountPage(
                        viewModel = viewModel {
                            AccountViewModel(accountRepository, account.id)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onSaveSuccess = { rootNavController.popBackStack() }
                    )
                }
                composable<Route.Category> { backStackEntry ->
                    val category = backStackEntry.toRoute<Route.Category>()
                    CategoryPage(
                        viewModel = viewModel {
                            CategoryViewModel(categoryRepository, category.id)
                        },
                        onUp = { rootNavController.popBackStack() },
                        onSaveSuccess = { rootNavController.popBackStack() }
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
                        onSaveSuccess = { rootNavController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun MainContainer(
    rootNavController: NavHostController,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    trxRepository: TrxRepository,
) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            MyNavBar(
                onHomeClick = {
                    innerNavController.popBackStack(Route.Home, inclusive = false)
                },
                onAddClick = {
                    rootNavController.navigate(Route.Trx(id = null))
                },
                onAddLongPress = {
                    // TODO: Add option to create
                    // Trx, Account, or Category
                    rootNavController.navigate(Route.Category(id = null))
                },
                onStatisticClick = {
                    innerNavController.navigate(Route.Statistic) {
                        popUpTo(Route.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { contentPadding ->
        NavHost(
            innerNavController,
            startDestination = Route.Home,
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
            },
            modifier = Modifier.padding(contentPadding)
        ) {
            composable<Route.Home> {
                HomePage(
                    viewModel = viewModel {
                        HomeViewModel(accountRepository, trxRepository)
                    },
                    onAccountClick = { id ->
                        rootNavController.navigate(Route.Account(id))
                    }
                )
            }
            composable<Route.Statistic> {
                StatisticPage(
                    viewModel = viewModel {
                        StatisticViewModel(categoryRepository)
                    }
                )
            }
        }
    }
}