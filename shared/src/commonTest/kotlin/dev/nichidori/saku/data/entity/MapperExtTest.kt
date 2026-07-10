package dev.nichidori.saku.data.entity

import dev.nichidori.saku.domain.model.Account
import dev.nichidori.saku.domain.model.AccountType
import dev.nichidori.saku.domain.model.Category
import dev.nichidori.saku.domain.model.Trx
import dev.nichidori.saku.domain.model.TrxAccount
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
            updatedAt = null,
            icon = "ic_food"
        )

        val parent = Category(
            id = "cat-parent",
            name = "Essentials",
            type = TrxType.Expense,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null,
            icon = "ic_essentials"
        )

        val domain = entity.toDomain(parent)
        val roundTrip = domain.toEntity()

        assertEquals(entity.copy(parentId = domain.parent?.id), roundTrip)
    }

    @Test
    fun toDomainAndBack_withAccountType_shouldMatch() {
        AccountType.entries.filter { it != AccountType.Credit }.forEach {
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
            sourceCreditId = null,
            targetAccountId = null,
            targetCreditId = null,
            transactionAt = 1_000_000L,
            createdAt = 1_000_001L,
            updatedAt = null,
            type = TrxTypeEntity.Income
        )

        val domain = entity.toDomain(category, TrxAccount.Regular(account))
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
            sourceCreditId = null,
            targetAccountId = null,
            targetCreditId = null,
            transactionAt = 1_111_111L,
            createdAt = 1_111_112L,
            updatedAt = 1_111_113L,
            type = TrxTypeEntity.Expense
        )

        val domain = entity.toDomain(category, TrxAccount.Regular(account))
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
            sourceCreditId = null,
            targetAccountId = target.id,
            targetCreditId = null,
            transactionAt = 1_000_000L,
            createdAt = 1_000_100L,
            updatedAt = null,
            type = TrxTypeEntity.Transfer
        )

        val domain = entity.toDomain(null, TrxAccount.Regular(source), TrxAccount.Regular(target))
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
            sourceCreditId = null,
            targetAccountId = "acc2",
            targetCreditId = null,
            transactionAt = 1_000_000L,
            createdAt = 1_000_100L,
            updatedAt = null,
            type = TrxTypeEntity.Transfer
        )

        assertFailsWith<IllegalArgumentException> {
            entity.toDomain(category, TrxAccount.Regular(source), null)
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
            sourceCreditId = null,
            targetAccountId = null,
            targetCreditId = null,
            transactionAt = 1_650_000_000L,
            createdAt = 1_650_000_100L,
            updatedAt = 1_650_000_200L,
            type = TrxTypeEntity.Income
        )

        val trxWithDetails = TrxWithDetailsEntity(
            trx = trxEntity,
            categoryWithParent = categoryWithParent,
            sourceAccount = sourceAccount,
            targetAccount = null,
            sourceCredit = null,
            targetCredit = null
        )

        val domain = trxWithDetails.toDomain()

        assertTrue(domain is Trx.Income)
        assertEquals(trxEntity.id, domain.id)
        assertEquals(trxEntity.description, domain.description)
        assertEquals(trxEntity.amount, domain.amount)
        assertEquals(trxEntity.transactionAt, domain.transactionAt.toEpochMilliseconds())
        assertEquals(trxEntity.createdAt, domain.createdAt.toEpochMilliseconds())
        assertEquals(trxEntity.updatedAt, domain.updatedAt?.toEpochMilliseconds())
        assertEquals(TrxAccount.Regular(sourceAccount.toDomain()), domain.sourceAccount)
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
                sourceCreditId = null,
                targetAccountId = null,
                targetCreditId = null,
                transactionAt = 1_000_000L,
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
            targetAccount = null,
            sourceCredit = null,
            targetCredit = null
        )

        val domain = entity.toDomain()

        assertTrue(domain is Trx.Expense)
        assertEquals("trx-expense", domain.id)
        assertEquals("Dinner", domain.description)
        assertEquals(50000L, domain.amount)
        assertEquals("Food & Dining", domain.category?.name)
        assertEquals("Wallet", domain.sourceAccount.name)
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
                sourceCreditId = null,
                targetAccountId = "acc-bank",
                targetCreditId = null,
                transactionAt = 1_000_100L,
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
            ),
            sourceCredit = null,
            targetCredit = null
        )

        val domain = entity.toDomain()

        assertTrue(domain is Trx.Transfer)
        assertEquals("trx-transfer", domain.id)
        assertEquals("Transfer to Bank", domain.description)
        assertEquals(100_000L, domain.amount)
        assertEquals("Wallet", domain.sourceAccount.name)
        assertEquals("Bank", domain.targetAccount.name)
        assertEquals(1_000_101L, domain.createdAt.toEpochMilliseconds())
        assertNull(domain.updatedAt)
    }

    @Test
    fun toDomainAndBack_withBudgetEntity_shouldPreserveData() {
        val category = Category(
            id = "cat-1",
            name = "Food",
            type = TrxType.Expense,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val entity = BudgetEntity(
            id = "budget-1",
            templateId = "tmpl-1",
            categoryId = category.id,
            month = 3,
            year = 2026,
            baseAmount = 5000L,
            spentAmount = 1000L,
            createdAt = 1_000_000L,
            updatedAt = 2_000_000L
        )

        val domain = entity.toDomain(category)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun toDomainAndBack_withBudgetTemplateEntity_shouldPreserveData() {
        val category = Category(
            id = "cat-1",
            name = "Food",
            type = TrxType.Expense,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val entity = BudgetTemplateEntity(
            id = "tmpl-1",
            categoryId = category.id,
            defaultAmount = 5000L,
            createdAt = 1_000_000L,
            updatedAt = 2_000_000L
        )

        val domain = entity.toDomain(category)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun toDomain_withBudgetWithCategoryEntity_shouldMapCorrectly() {
        val catEntity = CategoryEntity(
            id = "cat-1",
            name = "Food",
            type = TrxTypeEntity.Expense,
            parentId = null,
            createdAt = 1_000L,
            updatedAt = null
        )
        val budgetEntity = BudgetEntity(
            id = "budget-1",
            templateId = "tmpl-1",
            categoryId = catEntity.id,
            month = 3,
            year = 2026,
            baseAmount = 5000L,
            spentAmount = 1000L,
            createdAt = 1_000_000L,
            updatedAt = 2_000_000L
        )
        val withCategory = BudgetWithCategoryEntity(
            budget = budgetEntity,
            category = catEntity
        )

        val domain = withCategory.toDomain()

        assertEquals(budgetEntity.id, domain.id)
        assertEquals(catEntity.name, domain.category.name)
    }

    @Test
    fun toDomain_withBudgetTemplateWithCategoryEntity_shouldMapCorrectly() {
        val catEntity = CategoryEntity(
            id = "cat-1",
            name = "Food",
            type = TrxTypeEntity.Expense,
            parentId = null,
            createdAt = 1_000L,
            updatedAt = null
        )
        val templateEntity = BudgetTemplateEntity(
            id = "tmpl-1",
            categoryId = catEntity.id,
            defaultAmount = 5000L,
            createdAt = 1_000_000L,
            updatedAt = 2_000_000L
        )
        val withCategory = BudgetTemplateWithCategoryEntity(
            budgetTemplate = templateEntity,
            category = catEntity
        )

        val domain = withCategory.toDomain()

        assertEquals(templateEntity.id, domain.id)
        assertEquals(catEntity.name, domain.category.name)
    }

    @Test
    fun toDomainAndBack_withTrxTemplateEntity_shouldPreserveData() {
        val category = Category(
            id = "cat-1",
            name = "Food",
            type = TrxType.Expense,
            parent = null,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        val account = Account(
            id = "acc-1",
            name = "Cash",
            initialAmount = 10_000L,
            currentAmount = 10_000L,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        val entity = TrxTemplateEntity(
            id = "tmpl-1",
            name = "Groceries",
            type = TrxTypeEntity.Expense,
            description = "Weekly groceries",
            amount = 500_000L,
            categoryId = category.id,
            sourceAccountId = account.id,
            sourceCreditId = null,
            targetAccountId = null,
            targetCreditId = null,
            createdAt = 1_000_000L,
            updatedAt = 2_000_000L
        )

        val domain = entity.toDomain(category, TrxAccount.Regular(account), null)
        val roundTrip = domain.toEntity()

        assertEquals(entity, roundTrip)
    }

    @Test
    fun toDomain_withTrxTemplateEntity_nullCategory() {
        val account = Account(
            id = "acc-1",
            name = "Cash",
            initialAmount = 10_000L,
            currentAmount = 10_000L,
            type = AccountType.Cash,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        val entity = TrxTemplateEntity(
            id = "tmpl-1",
            name = "Transfer",
            type = TrxTypeEntity.Transfer,
            description = "Move money",
            amount = 1_000_000L,
            categoryId = null,
            sourceAccountId = account.id,
            sourceCreditId = null,
            targetAccountId = "acc-2",
            targetCreditId = null,
            createdAt = 1_000_000L,
            updatedAt = null
        )

        val domain = entity.toDomain(null, TrxAccount.Regular(account), null)

        assertNull(domain.category)
        assertEquals("tmpl-1", domain.id)
        assertEquals("Transfer", domain.name)
    }

    @Test
    fun toDomain_withTrxTemplateWithDetailsEntity_income() {
        val entity = TrxTemplateWithDetailsEntity(
            trxTemplate = TrxTemplateEntity(
                id = "tmpl-income",
                name = "Salary",
                type = TrxTypeEntity.Income,
                description = "Monthly salary",
                amount = 5_000_000L,
                categoryId = "cat-1",
                sourceAccountId = "acc-1",
                sourceCreditId = null,
                targetAccountId = null,
                targetCreditId = null,
                createdAt = 1_000_000L,
                updatedAt = null
            ),
            categoryWithParent = CategoryWithParentEntity(
                category = CategoryEntity(
                    id = "cat-1",
                    name = "Salary",
                    type = TrxTypeEntity.Income,
                    parentId = null,
                    createdAt = 900_000L,
                    updatedAt = null
                ),
                parent = null
            ),
            sourceAccount = AccountEntity(
                id = "acc-1",
                name = "Bank",
                initialAmount = 0L,
                currentAmount = 5_000_000L,
                type = AccountTypeEntity.Bank,
                createdAt = 800_000L,
                updatedAt = null
            ),
            sourceCredit = null,
            targetAccount = null,
            targetCredit = null
        )

        val domain = entity.toDomain()

        assertEquals("tmpl-income", domain.id)
        assertEquals("Salary", domain.name)
        assertEquals(TrxType.Income, domain.type)
        assertEquals("Monthly salary", domain.description)
        assertEquals(5_000_000L, domain.amount)
        assertEquals("Salary", domain.category?.name)
        assertEquals("Bank", domain.sourceAccount.name)
        assertNull(domain.targetAccount)
    }

    @Test
    fun toDomain_withTrxTemplateWithDetailsEntity_expense() {
        val entity = TrxTemplateWithDetailsEntity(
            trxTemplate = TrxTemplateEntity(
                id = "tmpl-expense",
                name = "Groceries",
                type = TrxTypeEntity.Expense,
                description = "Weekly groceries",
                amount = 500_000L,
                categoryId = "cat-1",
                sourceAccountId = "acc-1",
                sourceCreditId = null,
                targetAccountId = null,
                targetCreditId = null,
                createdAt = 1_000_000L,
                updatedAt = 2_000_000L
            ),
            categoryWithParent = CategoryWithParentEntity(
                category = CategoryEntity(
                    id = "cat-1",
                    name = "Food",
                    type = TrxTypeEntity.Expense,
                    parentId = null,
                    createdAt = 900_000L,
                    updatedAt = null
                ),
                parent = null
            ),
            sourceAccount = AccountEntity(
                id = "acc-1",
                name = "Cash",
                initialAmount = 10_000L,
                currentAmount = 10_000L,
                type = AccountTypeEntity.Cash,
                createdAt = 800_000L,
                updatedAt = null
            ),
            sourceCredit = null,
            targetAccount = null,
            targetCredit = null
        )

        val domain = entity.toDomain()

        assertEquals("tmpl-expense", domain.id)
        assertEquals("Groceries", domain.name)
        assertEquals(TrxType.Expense, domain.type)
        assertEquals(500_000L, domain.amount)
        assertEquals("Food", domain.category?.name)
        assertEquals("Cash", domain.sourceAccount.name)
        assertNull(domain.targetAccount)
        assertEquals(2_000_000L, domain.updatedAt?.toEpochMilliseconds())
    }

    @Test
    fun toDomain_withTrxTemplateWithDetailsEntity_transfer() {
        val entity = TrxTemplateWithDetailsEntity(
            trxTemplate = TrxTemplateEntity(
                id = "tmpl-transfer",
                name = "Deposit",
                type = TrxTypeEntity.Transfer,
                description = "Transfer to bank",
                amount = 1_000_000L,
                categoryId = null,
                sourceAccountId = "acc-1",
                sourceCreditId = null,
                targetAccountId = "acc-2",
                targetCreditId = null,
                createdAt = 1_000_000L,
                updatedAt = null
            ),
            categoryWithParent = null,
            sourceAccount = AccountEntity(
                id = "acc-1",
                name = "Cash",
                initialAmount = 5_000_000L,
                currentAmount = 4_000_000L,
                type = AccountTypeEntity.Cash,
                createdAt = 800_000L,
                updatedAt = null
            ),
            sourceCredit = null,
            targetAccount = AccountEntity(
                id = "acc-2",
                name = "Bank",
                initialAmount = 1_000_000L,
                currentAmount = 2_000_000L,
                type = AccountTypeEntity.Bank,
                createdAt = 850_000L,
                updatedAt = null
            ),
            targetCredit = null
        )

        val domain = entity.toDomain()

        assertEquals("tmpl-transfer", domain.id)
        assertEquals("Deposit", domain.name)
        assertEquals(TrxType.Transfer, domain.type)
        assertEquals(1_000_000L, domain.amount)
        assertNull(domain.category)
        assertEquals("Cash", domain.sourceAccount.name)
        assertEquals("Bank", domain.targetAccount?.name)
    }
}