package org.arraflydori.fin.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.entity.toDomain
import org.arraflydori.fin.data.entity.toEntity
import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.model.AccountType
import org.arraflydori.fin.domain.repo.AccountRepository
import java.util.UUID
import kotlin.time.Clock

class DefaultAccountRepository(
    private val db: AppDatabase,
) : AccountRepository {
    override suspend fun addAccount(name: String, initialAmount: Long, type: AccountType) {
        val account = Account(
            id = UUID.randomUUID().toString(),
            name = name,
            initialAmount = initialAmount,
            currentAmount = initialAmount,
            type = type,
            createdAt = Clock.System.now(),
            updatedAt = null
        )
        db.useWriterConnection {
            db.accountDao().insert(account.toEntity())
        }
    }

    override suspend fun getAccountById(id: String): Account? {
        return db.accountDao().getById(id)?.toDomain()
    }

    override suspend fun getAllAccounts(): List<Account> {
        return db.useReaderConnection {
            db.accountDao().getAll().map { it.toDomain() }
        }
    }

    override suspend fun updateAccount(
        id: String, name: String, initialAmount: Long, type: AccountType
    ) {
        db.useWriterConnection {
            it.immediateTransaction {
                val updatedAccount = db.accountDao().getById(id)?.toDomain()
                    ?.copy(
                        name = name,
                        initialAmount = initialAmount,
                        currentAmount = initialAmount,
                        type = type,
                        updatedAt = Clock.System.now()
                    )
                    ?: throw NoSuchElementException("Account not found")
                db.accountDao().update(updatedAccount.toEntity())
            }
        }
    }

    override suspend fun deleteAccount(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.accountDao().getById(id) ?: throw NoSuchElementException("Account not found")
                db.accountDao().deleteById(id)
            }
        }
    }

    override suspend fun getTotalBalance(): Long {
        return db.useReaderConnection {
            db.accountDao().getTotalBalance() ?: 0
        }
    }
}