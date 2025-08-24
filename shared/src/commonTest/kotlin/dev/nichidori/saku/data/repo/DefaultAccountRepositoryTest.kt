package dev.nichidori.saku.data.repo

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.data.getRoomDatabase
import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultAccountRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultAccountRepository

    private val account = Account(
        id = "acc-1",
        name = "Cash",
        initialAmount = 10_000L,
        currentAmount = 10_000L,
        type = AccountType.Cash,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        repository = DefaultAccountRepository(db)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addAccount_shouldInsertAccountWithGeneratedIdAndCreatedAt() = runTest {
        repository.addAccount(account.name, account.initialAmount, account.type)
        val accounts = db.accountDao().getAll().map { it.toDomain() }
        assertEquals(1, accounts.size)
        assertNotEquals("acc-1", accounts.first().id)
        assertTrue(accounts.first().createdAt > Instant.fromEpochMilliseconds(0))
    }

    @Test
    fun getAccountById_shouldReturnCorrectAccount() = runTest {
        db.accountDao().insert(account.toEntity())
        val result = repository.getAccountById(account.id)
        assertEquals(account.name, result?.name)
    }

    @Test
    fun getAccountById_shouldReturnNullIfNotFound() = runTest {
        val result = repository.getAccountById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getAllAccounts_shouldReturnAllAccounts() = runTest {
        db.accountDao().insert(account.toEntity())
        val result = repository.getAllAccounts()
        assertEquals(1, result.size)
        assertEquals(account.name, result.first().name)
    }

    @Test
    fun updateAccount_shouldUpdateExistingAccount() = runTest {
        db.accountDao().insert(account.toEntity())
        repository.updateAccount(account.id, "Updated Cash", account.initialAmount, account.type)
        val result = db.accountDao().getById(account.id)!!.toDomain()
        assertEquals("Updated Cash", result.name)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun deleteAccount_shouldDeleteAccountById() = runTest {
        db.accountDao().insert(account.toEntity())
        repository.deleteAccount(account.id)
        val result = db.accountDao().getById(account.id)
        assertNull(result)
    }

    @Test
    fun deleteAccount_shouldThrowIfAccountNotFound() = runTest {
        val exception = assertFailsWith<NoSuchElementException> {
            repository.deleteAccount("non-existent-id")
        }
        assertEquals("Account not found", exception.message)
    }

    @Test
    fun getTotalBalance_shouldReturnSumOfAllCurrentAmounts() = runTest {
        db.accountDao().insert(account.toEntity())
        db.accountDao().insert(account.copy(id = "acc-2", currentAmount = 5_000L).toEntity())
        val total = repository.getTotalBalance()
        assertEquals(15_000L, total)
    }

    @Test
    fun getTotalBalance_shouldReturnZeroIfNoAccount() = runTest {
        val emptyDb = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        val emptyRepo = DefaultAccountRepository(emptyDb)
        val balance = emptyRepo.getTotalBalance()
        assertEquals(0, balance)
    }
}