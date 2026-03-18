package dev.nichidori.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TrxType {
    Income,
    Expense,
    Transfer
}