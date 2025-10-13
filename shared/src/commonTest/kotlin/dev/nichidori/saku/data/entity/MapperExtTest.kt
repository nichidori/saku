package dev.nichidori.saku.data.entity

import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class MapperExtTest {

    @Test
    fun toDomainAndBack_withAccountEntity_shouldPreserveData() {
        val entity = AccountEntity(
            id = "acc-1",
            name = "Cash Wallet",
            initialAmount = 1000L,
            currentAmount = 800L,
            type = AccountTypeEntity.Cash,
            createdAt = 1_000_000L,
            updatedAt = 2_000_000L
        )

        val domain = entity.toDomain()
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun toDomainAndBack_withCategoryEntity_shouldPreserveData() {
        val entity = CategoryEntity(
            id = "cat-1",
            name = "Food",
            type = TrxTypeEntity.Expense,
            parentId = "cat-parent",
            createdAt = 1_000L,
            updatedAt = null
        )

        val parent = Category(
            id = "cat-parent",
            name = "Essentials",
            type = TrxType.Expense,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        val domain = entity.toDomain(parent)
        val roundTrip = domain.toEntity()

        assertEquals(entity.copy(parentId = domain.parent?.id), roundTrip)
    }

    @Test
    fun toDomainAndBack_withAccountType_shouldMatch() {
        AccountType.entries.forEach {
            val roundTrip = it.toEntity().toDomain()
            assertEquals(it, roundTrip)
        }
    }

    @Test
    fun toDomainAndBack_withTrxType_shouldMatch() {
        TrxType.entries.forEach {
            val roundTrip = it.toEntity().toDomain()
            assertEquals(it, roundTrip)
        }
    }

    @Test
    fun toDomainAndBack_withIncomeTransaction_shouldPreserveData() {
        val category = Category(
            id = "cat",
            name = "Salary",
            type = TrxType.Income,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val account = Account(
            id = "acc",
            name = "Bank",
            initialAmount = 0L,
            currentAmount = 1000L,
            type = AccountType.Bank,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        val entity = TrxEntity(
            id = "trx1",
            description = "July Salary",
            amount = 1_000_000L,
            categoryId = category.id,
            sourceAccountId = account.id,
            targetAccountId = null,
            transactionAt = 1_000_000L,
            note = "Monthly salary",
            createdAt = 1_000_001L,
            updatedAt = null,
            type = TrxTypeEntity.Income
        )

        val domain = entity.toDomain(category, account)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun toDomainAndBack_withExpenseTransaction_shouldPreserveData() {
        val category = Category(
            id = "cat",
            name = "Groceries",
            type = TrxType.Expense,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val account = Account(
            id = "acc",
            name = "Cash Wallet",
            initialAmount = 50000L,
            currentAmount = 30000L,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        val entity = TrxEntity(
            id = "trx3",
            description = "Buy food",
            amount = 20000L,
            categoryId = category.id,
            sourceAccountId = account.id,
            targetAccountId = null,
            transactionAt = 1_111_111L,
            note = "Weekly groceries",
            createdAt = 1_111_112L,
            updatedAt = 1_111_113L,
            type = TrxTypeEntity.Expense
        )

        val domain = entity.toDomain(category, account)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun toDomainAndBack_withTransferTransaction_shouldPreserveData() {
        val source = Account(
            id = "acc1",
            name = "Wallet",
            initialAmount = 5000L,
            currentAmount = 3000L,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val target = Account(
            id = "acc2",
            name = "Bank",
            initialAmount = 10000L,
            currentAmount = 12000L,
            type = AccountType.Bank,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        val entity = TrxEntity(
            id = "trx2",
            description = "Move to Bank",
            amount = 2000L,
            categoryId = null,
            sourceAccountId = source.id,
            targetAccountId = target.id,
            transactionAt = 1_000_000L,
            note = null,
            createdAt = 1_000_100L,
            updatedAt = null,
            type = TrxTypeEntity.Transfer
        )

        val domain = entity.toDomain(null, source, target)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun toDomain_withTransferTransactionMissingTargetAccount_shouldThrowException() {
        val category = Category(
            id = "cat",
            name = "Internal Transfer",
            type = TrxType.Transfer,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val source = Account(
            id = "acc1",
            name = "Wallet",
            initialAmount = 5000L,
            currentAmount = 3000L,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        val entity = TrxEntity(
            id = "trx2",
            description = "Move to Bank",
            amount = 2000L,
            categoryId = category.id,
            sourceAccountId = source.id,
            targetAccountId = "acc2",
            transactionAt = 1_000_000L,
            note = null,
            createdAt = 1_000_100L,
            updatedAt = null,
            type = TrxTypeEntity.Transfer
        )

        assertFailsWith<IllegalArgumentException> {
            entity.toDomain(category, source, null)
        }
    }

    @Test
    fun toDomain_withIncomeTrxWithDetailsEntity_shouldReturnCorrectDomainModel() {
        val categoryWithParent = CategoryWithParentEntity(
            category = CategoryEntity(
                id = "cat-1",
                name = "Salary",
                type = TrxTypeEntity.Income,
                parentId = null,
                createdAt = 1_000L,
                updatedAt = 2_000L
            ),
            parent = null
        )

        val sourceAccount = AccountEntity(
            id = "acc-1",
            name = "Bank",
            initialAmount = 0L,
            currentAmount = 5_000_000L,
            type = AccountTypeEntity.Bank,
            createdAt = 500L,
            updatedAt = 1000L
        )

        val trxEntity = TrxEntity(
            id = "trx-1",
            description = "Monthly Salary",
            amount = 10_000_000L,
            categoryId = categoryWithParent.category.id,
            sourceAccountId = sourceAccount.id,
            targetAccountId = null,
            transactionAt = 1_650_000_000L,
            note = "July salary",
            createdAt = 1_650_000_100L,
            updatedAt = 1_650_000_200L,
            type = TrxTypeEntity.Income
        )

        val trxWithDetails = TrxWithDetailsEntity(
            trx = trxEntity,
            categoryWithParent = categoryWithParent,
            sourceAccount = sourceAccount,
            targetAccount = null
        )

        val domain = trxWithDetails.toDomain()

        assertTrue(domain is Trx.Income)
        assertEquals(trxEntity.id, domain.id)
        assertEquals(trxEntity.description, domain.description)
        assertEquals(trxEntity.amount, domain.amount)
        assertEquals(trxEntity.transactionAt, domain.transactionAt.toEpochMilliseconds())
        assertEquals(trxEntity.note, domain.note)
        assertEquals(trxEntity.createdAt, domain.createdAt.toEpochMilliseconds())
        assertEquals(trxEntity.updatedAt, domain.updatedAt?.toEpochMilliseconds())
        assertEquals(sourceAccount.toDomain(), domain.sourceAccount)
        assertEquals(categoryWithParent.category.toDomain(), domain.category)
    }

    @Test
    fun toDomain_withExpenseTrxWithDetailsEntity_shouldReturnCorrectDomainModel() {
        val entity = TrxWithDetailsEntity(
            trx = TrxEntity(
                id = "trx-expense",
                description = "Dinner",
                amount = 50000L,
                categoryId = "cat-expense",
                sourceAccountId = "acc-wallet",
                targetAccountId = null,
                transactionAt = 1_000_000L,
                note = "Friday night dinner",
                createdAt = 1_000_001L,
                updatedAt = 1_000_002L,
                type = TrxTypeEntity.Expense
            ),
            categoryWithParent = CategoryWithParentEntity(
                category = CategoryEntity(
                    id = "cat-expense",
                    name = "Food & Dining",
                    type = TrxTypeEntity.Expense,
                    parentId = null,
                    createdAt = 900_000L,
                    updatedAt = 900_001L
                ),
                parent = null,
            ),
            sourceAccount = AccountEntity(
                id = "acc-wallet",
                name = "Wallet",
                initialAmount = 200_000L,
                currentAmount = 150_000L,
                type = AccountTypeEntity.Cash,
                createdAt = 800_000L,
                updatedAt = 800_001L
            ),
            targetAccount = null
        )

        val domain = entity.toDomain()

        assertTrue(domain is Trx.Expense)
        assertEquals("trx-expense", domain.id)
        assertEquals("Dinner", domain.description)
        assertEquals(50000L, domain.amount)
        assertEquals("Food & Dining", domain.category?.name)
        assertEquals("Wallet", domain.sourceAccount.name)
        assertEquals("Friday night dinner", domain.note)
        assertEquals(1_000_001L, domain.createdAt.toEpochMilliseconds())
        assertEquals(1_000_002L, domain.updatedAt?.toEpochMilliseconds())
    }

    @Test
    fun toDomain_withTransferTrxWithDetailsEntity_shouldReturnCorrectDomainModel() {
        val entity = TrxWithDetailsEntity(
            trx = TrxEntity(
                id = "trx-transfer",
                description = "Transfer to Bank",
                amount = 100_000L,
                categoryId = "cat-transfer",
                sourceAccountId = "acc-wallet",
                targetAccountId = "acc-bank",
                transactionAt = 1_000_100L,
                note = "Monthly transfer",
                createdAt = 1_000_101L,
                updatedAt = null,
                type = TrxTypeEntity.Transfer
            ),
            categoryWithParent = null,
            sourceAccount = AccountEntity(
                id = "acc-wallet",
                name = "Wallet",
                initialAmount = 500_000L,
                currentAmount = 400_000L,
                type = AccountTypeEntity.Cash,
                createdAt = 800_000L,
                updatedAt = null
            ),
            targetAccount = AccountEntity(
                id = "acc-bank",
                name = "Bank",
                initialAmount = 1_000_000L,
                currentAmount = 1_100_000L,
                type = AccountTypeEntity.Bank,
                createdAt = 850_000L,
                updatedAt = null
            )
        )

        val domain = entity.toDomain()

        assertTrue(domain is Trx.Transfer)
        assertEquals("trx-transfer", domain.id)
        assertEquals("Transfer to Bank", domain.description)
        assertEquals(100_000L, domain.amount)
        assertEquals("Wallet", domain.sourceAccount.name)
        assertEquals("Bank", domain.targetAccount.name)
        assertEquals("Monthly transfer", domain.note)
        assertEquals(1_000_101L, domain.createdAt.toEpochMilliseconds())
        assertNull(domain.updatedAt)
    }
}