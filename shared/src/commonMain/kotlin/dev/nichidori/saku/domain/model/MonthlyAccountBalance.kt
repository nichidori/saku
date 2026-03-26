package dev.nichidori.saku.domain.model

import kotlinx.datetime.YearMonth

data class MonthlyAccountBalance(
    val yearMonth: YearMonth,
    val accountId: String,
    val balance: Long
)
