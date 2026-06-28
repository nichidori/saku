package dev.nichidori.saku.domain.model

import kotlinx.datetime.YearMonth

data class MonthlyCreditBalance(
    val yearMonth: YearMonth,
    val creditId: String,
    val balance: Long
)
