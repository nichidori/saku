package org.arraflydori.fin

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.arraflydori.fin.feature.account.AccountPage
import org.arraflydori.fin.feature.home.HomePage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Serializable
data object Home

@Serializable
data class Account(val id: String)

@Composable
@Preview
fun App() {
    val focusManager = LocalFocusManager.current
    val navController = rememberNavController()

    MaterialTheme {
        Surface(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                },
        ) {
            NavHost(navController, startDestination = Home) {
                composable<Home> {
                    HomePage(
                        onAccountClick = { id -> navController.navigate(Account(id)) }
                    )
                }
                composable<Account> { backStackEntry ->
                    val account = backStackEntry.toRoute<Account>()
                    AccountPage(id = account.id)
                }
            }
        }
    }
}