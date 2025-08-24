package dev.nichidori.saku.core.platform

expect fun showToast(message: String, duration: ToastDuration)

enum class ToastDuration { Short, Long }