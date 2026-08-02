package dev.nichidori.saku.domain.model

import kotlinx.datetime.YearMonth
import kotlin.time.Instant

data class MonthlyNetWorth(
    val month: YearMonth,
    val netWorth: Long,
    val createdAt: Instant,
    val updatedAt: Instant?,
)
