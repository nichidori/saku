package dev.nichidori.saku.core.util

fun Any.log(message: Any) {
    println(this::class.simpleName + ": $message")
}