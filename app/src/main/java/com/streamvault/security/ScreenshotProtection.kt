package com.streamvault.security

import android.app.Activity
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotProtectionManager @Inject constructor() {

    fun enable(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun disable(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun setProtection(activity: Activity, enabled: Boolean) {
        if (enabled) enable(activity) else disable(activity)
    }
}
