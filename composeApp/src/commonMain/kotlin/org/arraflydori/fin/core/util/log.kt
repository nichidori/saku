package org.arraflydori.fin.core.util

fun Any.log(message: Any) {
    println(this::class.simpleName + ": $message")
}