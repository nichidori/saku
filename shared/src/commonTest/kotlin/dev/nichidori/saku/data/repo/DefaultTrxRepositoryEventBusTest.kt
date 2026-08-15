package dev.nichidori.saku.data.repo

import androidx.room.Room
import dev.nichidori.saku.core.event.AppEvent
import dev.nichidori.saku.core.event.AppEventBus
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class DefaultTrxRepositoryEventBusTest {

    private lateinit var db: AppDatabase
    private lateinit var eventBus: AppEventBus
    private lateinit var repository: DefaultTrxRepository

    private val cashAccount = Account(
        id = "acc-1",
        name = "Cash",
        currentAmount = 10_000L,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private val incomeCategory = Category(
        id = "cat-1",
        name = "Salary",
        type = TrxType.Income,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        eventBus = AppEventBus()
        repository = DefaultTrxRepository(db = db, appEventBus = eventBus)
        runBlocking {
            db.accountDao().insert(cashAccount.toEntity())
            db.categoryDao().insert(incomeCategory.toEntity())
        }
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private fun Instant.toYearMonth(): YearMonth {
        return toLocalDateTime(TimeZone.currentSystemDefault())
            .let { YearMonth(it.year, it.month) }
    }

    @Test
    fun trxCrud_shouldEmitTrxChangedEventsInOrder() = runTest {
        val events = async {
            eventBus.events.filterIsInstance<AppEvent.TrxChanged>().take(3).toList()
        }

        val newId = repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 5_000L,
            description = "July Salary",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,
        )

        repository.updateTrx(
            id = newId,
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 6_000L,
            description = "Updated Salary",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,
        )

        repository.deleteTrx(newId)

        assertEquals(3, events.await().size)
        val (created, updated, deleted) = events.await()

        val createdEvent = created as AppEvent.TrxChanged.Created
        assertEquals(newId, createdEvent.trx.id)
        assertEquals(5_000L, createdEvent.trx.amount)
        assertEquals("July Salary", createdEvent.trx.description)

        val updatedEvent = updated as AppEvent.TrxChanged.Updated
        assertEquals(newId, updatedEvent.before.id)
        assertEquals("July Salary", updatedEvent.before.description)
        assertEquals(newId, updatedEvent.after.id)
        assertEquals(6_000L, updatedEvent.after.amount)
        assertEquals("Updated Salary", updatedEvent.after.description)

        val deletedEvent = deleted as AppEvent.TrxChanged.Deleted
        assertEquals(newId, deletedEvent.trx.id)
    }

    @Test
    fun updateTrx_movingDateToAnotherMonth_shouldCarryBeforeAndAfter() = runTest {
        val events = async {
            eventBus.events.filterIsInstance<AppEvent.TrxChanged>().take(2).toList()
        }

        val newId = repository.addTrx(
            type = TrxType.Income,
            transactionAt = Clock.System.now(),
            amount = 5_000L,
            description = "Salary",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,
        )

        repository.updateTrx(
            id = newId,
            type = TrxType.Income,
            transactionAt = Clock.System.now() + 45.days,
            amount = 5_000L,
            description = "Salary",
            sourceAccount = TrxAccount.Regular(cashAccount),
            targetAccount = null,
            category = incomeCategory,
        )

        val updated = events.await()[1] as AppEvent.TrxChanged.Updated
        assertNotEquals(
            updated.before.transactionAt.toYearMonth(),
            updated.after.transactionAt.toYearMonth()
        )
    }
}