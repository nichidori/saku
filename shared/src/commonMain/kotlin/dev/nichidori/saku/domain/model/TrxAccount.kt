package dev.nichidori.saku.domain.model

sealed class TrxAccount {
    abstract val id: String
    abstract val name: String
    abstract val currentAmount: Long
    open val isDeleted: Boolean get() = false

    data class Regular(val account: Account) : TrxAccount() {
        override val id get() = account.id
        override val name get() = account.name
        override val currentAmount get() = account.currentAmount
        override val isDeleted get() = account.isDeleted
    }

    data class Credit(val credit: dev.nichidori.saku.domain.model.Credit) : TrxAccount() {
        override val id get() = credit.id
        override val name get() = credit.name
        override val currentAmount get() = credit.currentAmount
    }
}