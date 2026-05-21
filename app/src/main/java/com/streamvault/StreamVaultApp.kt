package com.streamvault

import android.app.Application
import com.streamvault.cast.CastManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StreamVaultApp : Application() {

    @Inject lateinit var castManager: CastManager

    override fun onCreate() {
        super.onCreate()
        castManager.init()
    }

    override fun onTerminate() {
        castManager.release()
        super.onTerminate()
    }
}
