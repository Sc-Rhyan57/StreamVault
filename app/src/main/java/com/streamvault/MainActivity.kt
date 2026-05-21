package com.streamvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.streamvault.data.local.AppPreferences
import com.streamvault.security.ScreenshotProtectionManager
import com.streamvault.ui.AppNavGraph
import com.streamvault.ui.Route
import com.streamvault.ui.theme.StreamVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferences: AppPreferences
    @Inject lateinit var screenshotManager: ScreenshotProtectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        var startRoute = Route.Setup.path

        lifecycleScope.launch {
            val configured = preferences.isConfigured.first()
            val token      = preferences.authToken.first()
            val profileId  = preferences.profileId.first()
            val screenshotProt = preferences.screenshotProtection.first()

            screenshotManager.setProtection(this@MainActivity, screenshotProt)

            startRoute = when {
                !configured          -> Route.Setup.path
                profileId.isNullOrBlank() -> Route.Profiles.path
                else                 -> Route.Home.path
            }
            keepSplash = false
        }

        setContent {
            StreamVaultTheme {
                val resolvedStart = remember { startRoute }
                AppNavGraph(startDestination = resolvedStart)
            }
        }
    }
}
