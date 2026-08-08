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
import kotlin.test.*
import kotlin.time.Clock

class DefaultTrxRepositoryEventBusTest {

    private lateinit var db: AppDatabase
    private lateinit var eventBus: AppEventBus
    private lateinit var repository: DefaultTrxRepository

    private val cashAccount = Account(
        id = "acc-1",
        name = "Cash",
        initialAmount = 10_000L,
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

        assertEquals(
            listOf(
                AppEvent.TrxChanged(id = newId, action = AppEvent.WriteAction.Created),
                AppEvent.TrxChanged(id = newId, action = AppEvent.WriteAction.Updated),
                AppEvent.TrxChanged(id = newId, action = AppEvent.WriteAction.Deleted),
            ),
            events.await()
        )
    }
}