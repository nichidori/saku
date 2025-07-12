package org.arraflydori.fin.data.entity

import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.AccountType
import org.arraflydori.fin.domain.model.Category
import org.arraflydori.fin.domain.model.TrxType
import kotlin.test.*
import org.junit.Test

class MapperExtTest {

    @Test
    fun `AccountEntity toDomain and back should preserve data`() {
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
    fun `CategoryEntity toDomain and back should preserve data`() {
        val entity = CategoryEntity(
            id = "cat-1",
            name = "Food",
            type = TrxTypeEntity.Spending,
            parentId = "cat-parent",
            createdAt = 1_000L,
            updatedAt = null
        )

        val parent = Category(
            id = "cat-parent",
            name = "Essentials",
            type = TrxType.Spending,
            parent = null,
            createdAt = 999L,
            updatedAt = null
        )

        val domain = entity.toDomain(parent)
        val roundTrip = domain.toEntity()

        assertEquals(entity.copy(parentId = domain.parent?.id), roundTrip)
    }

    @Test
    fun `AccountType round trip should match`() {
        AccountType.entries.forEach {
            val roundTrip = it.toEntity().toDomain()
            assertEquals(it, roundTrip)
        }
    }

    @Test
    fun `TrxType round trip should match`() {
        TrxType.entries.forEach {
            val roundTrip = it.toEntity().toDomain()
            assertEquals(it, roundTrip)
        }
    }

    @Test
    fun `TrxEntity toDomain and back for Income`() {
        val category = Category("cat", "Salary", TrxType.Income, null, 1000L, null)
        val account = Account("acc", "Bank", 0L, 1000L, AccountType.Bank, 1000L, null)

        val entity = TrxEntity(
            id = "trx1",
            name = "July Salary",
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
    fun `TrxEntity toDomain and back for Spending`() {
        val category = Category("cat", "Groceries", TrxType.Spending, null, 1000L, null)
        val account = Account("acc", "Cash Wallet", 50000L, 30000L, AccountType.Cash, 1000L, null)

        val entity = TrxEntity(
            id = "trx3",
            name = "Buy food",
            amount = 20000L,
            categoryId = category.id,
            sourceAccountId = account.id,
            targetAccountId = null,
            transactionAt = 1_111_111L,
            note = "Weekly groceries",
            createdAt = 1_111_112L,
            updatedAt = 1_111_113L,
            type = TrxTypeEntity.Spending
        )

        val domain = entity.toDomain(category, account)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun `TrxEntity toDomain and back for Transfer`() {
        val category = Category("cat", "Internal Transfer", TrxType.Transfer, null, 1000L, null)
        val source = Account("acc1", "Wallet", 5000L, 3000L, AccountType.Cash, 1000L, null)
        val target = Account("acc2", "Bank", 10000L, 12000L, AccountType.Bank, 1000L, null)

        val entity = TrxEntity(
            id = "trx2",
            name = "Move to Bank",
            amount = 2000L,
            categoryId = category.id,
            sourceAccountId = source.id,
            targetAccountId = target.id,
            transactionAt = 1_000_000L,
            note = null,
            createdAt = 1_000_100L,
            updatedAt = null,
            type = TrxTypeEntity.Transfer
        )

        val domain = entity.toDomain(category, source, target)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun `TrxEntity toDomain throws on missing targetAccount for Transfer`() {
        val category = Category("cat", "Internal Transfer", TrxType.Transfer, null, 1000L, null)
        val source = Account("acc1", "Wallet", 5000L, 3000L, AccountType.Cash, 1000L, null)

        val entity = TrxEntity(
            id = "trx2",
            name = "Move to Bank",
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
}
