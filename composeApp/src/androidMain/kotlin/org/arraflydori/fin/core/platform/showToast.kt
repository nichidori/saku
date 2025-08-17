package org.arraflydori.fin.core.platform

import android.app.Activity
import android.widget.Toast

private var activityProvider: () -> Activity? = {
    null
}

fun setToastActivityProvider(provider: () -> Activity?) {
    activityProvider = provider
}

actual fun showToast(message: String, duration: ToastDuration) {
    val context = activityProvider() ?: return
    val duration = when (duration) {
        ToastDuration.Short -> Toast.LENGTH_SHORT
        ToastDuration.Long -> Toast.LENGTH_LONG
    }
    Toast.makeText(context, message, duration).show()
}