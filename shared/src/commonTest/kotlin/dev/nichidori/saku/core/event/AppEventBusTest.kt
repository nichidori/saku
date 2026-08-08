package dev.nichidori.saku.core.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppEventBusTest {

    @Test
    fun event_emittedAfterSubscribe_isDelivered() = runTest {
        val bus = AppEventBus()
        val received = mutableListOf<AppEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.events.collect { received += it }
        }

        bus.emit(AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Created))
        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf(AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Created)),
            received.toList()
        )
    }

    @Test
    fun event_emittedBeforeSubscribe_isNotReplayed() = runTest {
        val bus = AppEventBus()
        bus.emit(AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Created))

        val received = mutableListOf<AppEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.events.collect { received += it }
        }
        testScheduler.advanceUntilIdle()

        assertTrue(received.isEmpty())
    }

    @Test
    fun multipleEvents_areDeliveredInOrder() = runTest {
        val bus = AppEventBus()
        val received = mutableListOf<AppEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.events.collect { received += it }
        }

        bus.emit(AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Created))
        bus.emit(AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Updated))
        bus.emit(AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Deleted))
        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Created),
                AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Updated),
                AppEvent.TrxChanged(id = "a", action = AppEvent.WriteAction.Deleted),
            ),
            received.toList()
        )
    }
}