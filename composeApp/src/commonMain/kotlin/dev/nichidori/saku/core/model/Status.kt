package dev.nichidori.saku.core.model

sealed interface Status<out T, out E> {
    data object Initial : Status<Nothing, Nothing>
    data object Loading : Status<Nothing, Nothing>
    data class Success<T>(val data: T) : Status<T, Nothing>
    data class Failure<E>(val error: E) : Status<Nothing, E>

    val isCompleted: Boolean
        get() = this is Success<*> || this is Failure<*>
}
