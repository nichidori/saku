package dev.nichidori.saku.data.repo

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import dev.nichidori.saku.data.AppDatabase
import dev.nichidori.saku.data.entity.toDomain
import dev.nichidori.saku.data.entity.toEntity
import dev.nichidori.saku.domain.model.Credit
import dev.nichidori.saku.domain.repo.CreditRepository
import java.util.*
import kotlin.time.Clock

class DefaultCreditRepository(
    private val db: AppDatabase,
) : CreditRepository {

    override suspend fun addCredit(name: String, limit: Long, initialAmount: Long) {
        val currentTime = Clock.System.now()
        val credit = Credit(
            id = UUID.randomUUID().toString(),
            name = name,
            limit = limit,
            currentAmount = initialAmount,
            createdAt = currentTime,
            updatedAt = null
        )
        db.useWriterConnection {
            db.creditDao().insert(credit.toEntity())
        }
    }

    override suspend fun getCreditById(id: String): Credit? {
        return db.creditDao().getById(id)?.toDomain()
    }

    override suspend fun getAllCredits(): List<Credit> {
        return db.useReaderConnection {
            db.creditDao().getAll().map { it.toDomain() }
        }
    }

    override suspend fun updateCredit(id: String, name: String, limit: Long) {
        val currentTime = Clock.System.now()
        db.useWriterConnection {
            it.immediateTransaction {
                val updated = db.creditDao().getById(id)?.toDomain()
                    ?.copy(
                        name = name,
                        limit = limit,
                        updatedAt = currentTime
                    )
                    ?: throw NoSuchElementException("Credit not found")
                db.creditDao().update(updated.toEntity())
            }
        }
    }

    override suspend fun deleteCredit(id: String) {
        db.useWriterConnection {
            it.immediateTransaction {
                db.creditDao().getById(id) ?: throw NoSuchElementException("Credit not found")
                db.creditDao().deleteById(id)
            }
        }
    }

    override suspend fun getTotalBalance(): Long {
        return db.useReaderConnection {
            db.creditDao().getTotalBalance() ?: 0
        }
    }

    override suspend fun getTotalLimit(): Long {
        return db.useReaderConnection {
            db.creditDao().getTotalLimit() ?: 0
        }
    }

}
