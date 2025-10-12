package dev.nichidori.saku.core.util

import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

@androidx.compose.runtime.Composable
actual fun <T> StateFlow<T>.collectAsStateWithLifecycleIfAvailable(): State<T> {
    return collectAsState()
}