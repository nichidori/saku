package org.arraflydori.fin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.arraflydori.fin.core.composable.MyNavBar
import org.arraflydori.fin.domain.repo.AccountRepository
import org.arraflydori.fin.domain.repo.TrxRepository
import org.arraflydori.fin.feature.account.AccountPage
import org.arraflydori.fin.feature.account.AccountViewModel
import org.arraflydori.fin.feature.home.HomePage
import org.arraflydori.fin.feature.home.HomeViewModel
import org.arraflydori.fin.feature.statistic.StatisticPage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Serializable
sealed interface Route

@Serializable
data object Home : Route

@Serializable
data class Account(val id: String?) : Route

@Serializable
data object Statistic : Route

@Composable
@Preview
fun App(
    accountRepository: AccountRepository,
    trxRepository: TrxRepository
) {
    val focusManager = LocalFocusManager.current
    val navController = rememberNavController()
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry.value?.destination

    val showNavBar = currentDestination?.hierarchy?.any {
        it.hasRoute(Home::class) || it.hasRoute(Statistic::class)
    } == true

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
            Scaffold(
                bottomBar = {
                    Box(modifier = Modifier.animateContentSize()) {
                        if (showNavBar) {
                            MyNavBar(
                                onHomeClick = {
                                    navController.popBackStack(Home, inclusive = false)
                                },
                                onAddClick = {
                                    navController.navigate(Account(id = null))
                                },
                                onStatisticClick = {
                                    navController.navigate(Statistic) {
                                        popUpTo(Home) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { contentPadding ->
                NavHost(
                    navController,
                    startDestination = Home,
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
                    composable<Home> {
                        HomePage(
                            viewModel = viewModel {
                                HomeViewModel(accountRepository, trxRepository)
                            },
                            onAccountClick = { id -> navController.navigate(Account(id)) }
                        )
                    }
                    composable<Account> { backStackEntry ->
                        val account = backStackEntry.toRoute<Account>()
                        AccountPage(
                            viewModel = viewModel {
                                AccountViewModel(accountRepository, account.id)
                            },
                            onUp = { navController.popBackStack() },
                            onSaveSuccess = { navController.popBackStack() }
                        )
                    }
                    composable<Statistic> {
                        StatisticPage()
                    }
                }
            }
        }
    }
}