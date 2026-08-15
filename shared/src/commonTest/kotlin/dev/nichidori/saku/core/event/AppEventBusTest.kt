package dev.nichidori.saku.core.event

import dev.nichidori.saku.domain.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class AppEventBusTest {

    private val account = Account(
        id = "acc-1",
        name = "Cash",
        currentAmount = 0L,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null,
    )

    private val category = Category(
        id = "cat-1",
        name = "Salary",
        type = TrxType.Income,
        createdAt = Clock.System.now(),
        updatedAt = null,
    )

    private fun trx(id: String) = Trx.Income(
        id = id,
        transactionAt = Clock.System.now(),
        amount = 1_000L,
        description = "Test",
        sourceAccount = TrxAccount.Regular(account),
        category = category,
        createdAt = Clock.System.now(),
        updatedAt = null,
    )

    private fun created(id: String) = AppEvent.TrxChanged.Created(trx(id))
    private fun updated(id: String) = AppEvent.TrxChanged.Updated(trx(id), trx(id))
    private fun deleted(id: String) = AppEvent.TrxChanged.Deleted(trx(id))

    @Test
    fun event_emittedAfterSubscribe_isDelivered() = runTest {
        val bus = AppEventBus()
        val received = mutableListOf<AppEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.events.collect { received += it }
        }

        val event = created("a")
        bus.emit(event)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(event), received.toList())
    }

    @Test
    fun event_emittedBeforeSubscribe_isNotReplayed() = runTest {
        val bus = AppEventBus()
        bus.emit(created("a"))

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

        val events = listOf(created("a"), updated("a"), deleted("a"))
        events.forEach { bus.emit(it) }
        testScheduler.advanceUntilIdle()

        assertEquals(events, received.toList())
    }
}