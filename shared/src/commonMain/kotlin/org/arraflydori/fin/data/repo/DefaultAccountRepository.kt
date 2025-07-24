package org.arraflydori.fin.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import org.arraflydori.fin.data.AppDatabase
import org.arraflydori.fin.data.entity.toDomain
import org.arraflydori.fin.data.entity.toEntity
import org.arraflydori.fin.domain.model.Account
import org.arraflydori.fin.domain.repo.AccountRepository
import java.util.UUID

class DefaultAccountRepository(
    private val db: AppDatabase,
) : AccountRepository {
    override suspend fun addAccount(account: Account) {
        val accountWithId = account.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis()
        )
        db.useWriterConnection {
            db.accountDao().insert(accountWithId.toEntity())
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

    override suspend fun updateAccount(account: Account) {
        db.useWriterConnection {
            val updatedAccount = account.copy(updatedAt = System.currentTimeMillis())
            db.accountDao().update(updatedAccount.toEntity())
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