package dev.nichidori.saku.core.event

sealed interface AppEvent {
    enum class WriteAction { Created, Updated, Deleted }

    data class TrxChanged(
        val id: String? = null,
        val action: WriteAction,
    ) : AppEvent
}