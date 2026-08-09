package dev.nichidori.saku.domain.model

import kotlin.time.Instant

data class Installment(
    val id: String,
    val description: String,
    val category: Category,
    val credit: Credit,
    val principal: Long,
    val months: Int,
    val monthlyRatePercent: Double,
    val totalAmount: Long,
    val monthlyPayment: Long,
    val lastPayment: Long,
    val startAt: Instant,
    val dueDay: Int,
    val nextIndex: Int,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    val remainingPayments: Int
        get() = (months - nextIndex).coerceAtLeast(0)

    fun paymentAmountForIndex(index: Int): Long {
        require(index in 0 until months) { "Index out of range: $index" }
        return if (index == months - 1) lastPayment else monthlyPayment
    }
}