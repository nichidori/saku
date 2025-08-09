package org.arraflydori.fin.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomePage(
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("Home")
        Button(onClick = { onAccountClick("1") }) {
            Text("Go to Account")
        }
    }
}