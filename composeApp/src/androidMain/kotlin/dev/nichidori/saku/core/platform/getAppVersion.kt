package dev.nichidori.saku.core.platform

import dev.nichidori.saku.composeApp.BuildConfig

actual fun getAppVersion(): String? = BuildConfig.VERSION_NAME
