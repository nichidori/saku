package dev.nichidori.saku.data.dao

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.AccountEntity
import dev.nichidori.saku.data.entity.AccountTypeEntity
import dev.nichidori.saku.data.getRoomDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val cashAccount = AccountEntity(
    id = "acc-cash",
    name = "Cash Wallet",
    initialAmount = 1_000_000,
    currentAmount = 1_500_000,
    type = AccountTypeEntity.Cash,
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

private val bankAccount = AccountEntity(
    id = "acc-bank",
    name = "Bank Account",
    initialAmount = 5_000_000,
    currentAmount = 4_800_000,
    type = AccountTypeEntity.Bank,
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

private val creditCardAccount = AccountEntity(
    id = "acc-credit",
    name = "Credit Card",
    initialAmount = 0,
    currentAmount = -200_000,
    type = AccountTypeEntity.Credit,
    createdAt = System.currentTimeMillis(),
    updatedAt = null
)

class AccountDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var accountDao: AccountDao

    @BeforeTest
    fun setup() {
        db = getRoomDatabase(builder = Room.inMemoryDatabaseBuilder<AppDatabase>())
        accountDao = db.accountDao()
    }

    @AfterTest
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetById_shouldReturnMatchingAccount() = runTest {
        accountDao.insert(cashAccount)

        val result = accountDao.getById(cashAccount.id)

        assertNotNull(result)
        assertEquals(cashAccount.id, result.id)
        assertEquals(cashAccount.name, result.name)
        assertEquals(cashAccount.initialAmount, result.initialAmount)
        assertEquals(cashAccount.currentAmount, result.currentAmount)
        assertEquals(cashAccount.type, result.type)
    }

    @Test
    fun getById_withNonExistentId_shouldReturnNull() = runTest {
        val result = accountDao.getById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun insert_withOnConflictReplace_shouldReplaceExistingAccount() = runTest {
        accountDao.insert(cashAccount)

        val updatedAccount = cashAccount.copy(
            name = "Updated Cash Wallet",
            currentAmount = 2_000_000,
            updatedAt = System.currentTimeMillis()
        )
        accountDao.insert(updatedAccount)

        val result = accountDao.getById(cashAccount.id)
        assertNotNull(result)
        assertEquals("Updated Cash Wallet", result.name)
        assertEquals(2_000_000, result.currentAmount)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun update_shouldModifyExistingAccount() = runTest {
        accountDao.insert(cashAccount)

        val updatedAccount = cashAccount.copy(
            name = "Modified Cash",
            currentAmount = 3_000_000,
            updatedAt = System.currentTimeMillis()
        )
        accountDao.update(updatedAccount)

        val result = accountDao.getById(cashAccount.id)
        assertNotNull(result)
        assertEquals("Modified Cash", result.name)
        assertEquals(3_000_000, result.currentAmount)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun deleteById_shouldRemoveAccount() = runTest {
        accountDao.insert(cashAccount)

        accountDao.deleteById(cashAccount.id)

        val result = accountDao.getById(cashAccount.id)
        assertNull(result)
    }

    @Test
    fun deleteById_withNonExistentId_shouldNotThrowException() = runTest {
        accountDao.deleteById("non-existent-id")
    }

    @Test
    fun getAll_withMultipleAccounts_shouldReturnAllOrderedByName() = runTest {
        accountDao.insert(cashAccount)
        accountDao.insert(bankAccount)
        accountDao.insert(creditCardAccount)

        val results = accountDao.getAll()

        assertEquals(3, results.size)
        assertEquals("Bank Account", results[0].name)
        assertEquals("Cash Wallet", results[1].name)
        assertEquals("Credit Card", results[2].name)
    }

    @Test
    fun getAll_withEmptyDatabase_shouldReturnEmptyList() = runTest {
        val results = accountDao.getAll()
        assertTrue(results.isEmpty())
    }

    @Test
    fun getAll_withSingleAccount_shouldReturnSingleItemList() = runTest {
        accountDao.insert(cashAccount)

        val results = accountDao.getAll()

        assertEquals(1, results.size)
        assertEquals(cashAccount.id, results[0].id)
    }

    @Test
    fun getTotalBalance_withMultipleAccounts_shouldReturnSumOfCurrentAmounts() = runTest {
        accountDao.insert(cashAccount)
        accountDao.insert(bankAccount)
        accountDao.insert(creditCardAccount)

        val totalBalance = accountDao.getTotalBalance()

        assertNotNull(totalBalance)
        assertEquals(6_100_000, totalBalance)
    }

    @Test
    fun getTotalBalance_withEmptyDatabase_shouldReturnNull() = runTest {
        val totalBalance = accountDao.getTotalBalance()
        assertNull(totalBalance)
    }

    @Test
    fun getTotalBalance_withSingleAccount_shouldReturnThatAccountBalance() = runTest {
        accountDao.insert(cashAccount)

        val totalBalance = accountDao.getTotalBalance()

        assertNotNull(totalBalance)
        assertEquals(cashAccount.currentAmount, totalBalance)
    }

    @Test
    fun getTotalBalance_withNegativeBalances_shouldCalculateCorrectly() = runTest {
        val debtAccount1 = creditCardAccount.copy(
            id = "debt-1",
            currentAmount = -500_000
        )
        val debtAccount2 = creditCardAccount.copy(
            id = "debt-2",
            currentAmount = -300_000
        )

        accountDao.insert(debtAccount1)
        accountDao.insert(debtAccount2)

        val totalBalance = accountDao.getTotalBalance()

        assertNotNull(totalBalance)
        assertEquals(-800_000, totalBalance)
    }

    @Test
    fun getTotalBalance_withZeroBalances_shouldReturnZero() = runTest {
        val zeroAccount1 = cashAccount.copy(
            id = "zero-1",
            currentAmount = 0
        )
        val zeroAccount2 = bankAccount.copy(
            id = "zero-2",
            currentAmount = 0
        )

        accountDao.insert(zeroAccount1)
        accountDao.insert(zeroAccount2)

        val totalBalance = accountDao.getTotalBalance()

        assertNotNull(totalBalance)
        assertEquals(0, totalBalance)
    }

    @Test
    fun insert_withDifferentAccountTypes_shouldAllBeRetrievable() = runTest {
        accountDao.insert(cashAccount)
        accountDao.insert(bankAccount)
        accountDao.insert(creditCardAccount)

        val cashResult = accountDao.getById(cashAccount.id)
        val bankResult = accountDao.getById(bankAccount.id)
        val creditResult = accountDao.getById(creditCardAccount.id)

        assertNotNull(cashResult)
        assertNotNull(bankResult)
        assertNotNull(creditResult)

        assertEquals(AccountTypeEntity.Cash, cashResult.type)
        assertEquals(AccountTypeEntity.Bank, bankResult.type)
        assertEquals(AccountTypeEntity.Credit, creditResult.type)
    }

    @Test
    fun getAll_withSameFirstLetter_shouldOrderAlphabetically() = runTest {
        val account1 = cashAccount.copy(id = "acc-1", name = "Apple Bank")
        val account2 = cashAccount.copy(id = "acc-2", name = "Amazon Card")
        val account3 = cashAccount.copy(id = "acc-3", name = "Atlantic Savings")

        accountDao.insert(account3)
        accountDao.insert(account1)
        accountDao.insert(account2)

        val results = accountDao.getAll()

        assertEquals(3, results.size)
        assertEquals("Amazon Card", results[0].name)
        assertEquals("Apple Bank", results[1].name)
        assertEquals("Atlantic Savings", results[2].name)
    }

    @Test
    fun getAll_withCaseInsensitiveOrdering_shouldOrderCorrectly() = runTest {
        val account1 = cashAccount.copy(id = "acc-1", name = "bank account")
        val account2 = cashAccount.copy(id = "acc-2", name = "Bank Card")
        val account3 = cashAccount.copy(id = "acc-3", name = "BANK SAVINGS")

        accountDao.insert(account2)
        accountDao.insert(account3)
        accountDao.insert(account1)

        val results = accountDao.getAll()

        assertEquals(3, results.size)
        assertTrue(results.all { it.name.lowercase().startsWith("bank") })
    }

    @Test
    fun update_withSameId_shouldPreserveId() = runTest {
        accountDao.insert(cashAccount)

        val updatedAccount = cashAccount.copy(
            name = "Completely Different Name",
            currentAmount = 999_999,
            type = AccountTypeEntity.Bank
        )
        accountDao.update(updatedAccount)

        val result = accountDao.getById(cashAccount.id)
        assertNotNull(result)
        assertEquals(cashAccount.id, result.id)
        assertEquals("Completely Different Name", result.name)
        assertEquals(999_999, result.currentAmount)
        assertEquals(AccountTypeEntity.Bank, result.type)
    }

    @Test
    fun insert_withDuplicateNames_shouldAllowMultipleAccountsWithSameName() = runTest {
        val account1 = cashAccount.copy(id = "acc-1", name = "My Account")
        val account2 = cashAccount.copy(id = "acc-2", name = "My Account")

        accountDao.insert(account1)
        accountDao.insert(account2)

        val results = accountDao.getAll()
        assertEquals(2, results.size)
        assertTrue(results.all { it.name == "My Account" })
        assertTrue(results.map { it.id }.containsAll(listOf("acc-1", "acc-2")))
    }

    @Test
    fun getTotalBalance_withLargeAmounts_shouldCalculateCorrectly() = runTest {
        val largeAccount = cashAccount.copy(
            id = "large-acc",
            currentAmount = Long.MAX_VALUE / 2
        )
        val anotherLargeAccount = bankAccount.copy(
            id = "another-large",
            currentAmount = Long.MAX_VALUE / 2
        )

        accountDao.insert(largeAccount)
        accountDao.insert(anotherLargeAccount)

        val totalBalance = accountDao.getTotalBalance()

        assertNotNull(totalBalance)
        assertEquals(Long.MAX_VALUE - 1, totalBalance)
    }

    @Test
    fun insert_withTimestamps_shouldPreserveTimestamps() = runTest {
        val specificTime = 1234567890L
        val accountWithTimestamp = cashAccount.copy(
            createdAt = specificTime,
            updatedAt = specificTime + 1000
        )

        accountDao.insert(accountWithTimestamp)

        val result = accountDao.getById(accountWithTimestamp.id)
        assertNotNull(result)
        assertEquals(specificTime, result.createdAt)
        assertEquals(specificTime + 1000, result.updatedAt)
    }

    @Test
    fun insert_withMinimumLongValue_shouldWork() = runTest {
        val extremeAccount = cashAccount.copy(
            id = "extreme-negative",
            currentAmount = Long.MIN_VALUE
        )

        accountDao.insert(extremeAccount)

        val result = accountDao.getById(extremeAccount.id)
        assertNotNull(result)
        assertEquals(Long.MIN_VALUE, result.currentAmount)
    }

    @Test
    fun insert_withMaximumLongValue_shouldWork() = runTest {
        val extremeAccount = cashAccount.copy(
            id = "extreme-positive",
            currentAmount = Long.MAX_VALUE
        )

        accountDao.insert(extremeAccount)

        val result = accountDao.getById(extremeAccount.id)
        assertNotNull(result)
        assertEquals(Long.MAX_VALUE, result.currentAmount)
    }
}