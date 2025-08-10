package org.arraflydori.fin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.arraflydori.fin.core.composable.MyNavBar
import org.arraflydori.fin.feature.account.AccountPage
import org.arraflydori.fin.feature.home.HomePage
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
fun App() {
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
                    AnimatedVisibility(
                        visible = showNavBar,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight }
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight }
                        ),
                        modifier = Modifier.safeContentPadding()
                    ) {
                        MyNavBar(
                            onHomeClick = { navController.navigate(Home) },
                            onAddClick = { navController.navigate(Account(id = null)) },
                            onStatisticClick = { navController.navigate(Statistic) }
                        )
                    }
                }
            ) {
                NavHost(navController, startDestination = Home) {
                    composable<Home> {
                        HomePage(
                            onAccountClick = { id -> navController.navigate(Account(id)) }
                        )
                    }
                    composable<Account> { backStackEntry ->
                        val account = backStackEntry.toRoute<Account>()
                        AccountPage(
                            id = account.id,
                            onUp = { navController.popBackStack() }
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