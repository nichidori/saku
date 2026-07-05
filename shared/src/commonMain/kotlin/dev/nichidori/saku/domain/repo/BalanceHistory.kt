package dev.nichidori.saku.domain.repo

data class BalanceHistory(
    val netWorthPerMonth: List<Long>,
    val balancePerAccountPerMonth: Map<String, List<Long>>,
)
