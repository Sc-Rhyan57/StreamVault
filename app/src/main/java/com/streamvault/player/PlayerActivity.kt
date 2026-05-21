package com.streamvault.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.streamvault.data.models.PlayerState
import com.streamvault.data.models.VideoFormat
import com.streamvault.data.models.DrmScheme
import com.streamvault.security.ScreenshotProtectionManager
import com.streamvault.ui.theme.StreamVaultTheme
import com.streamvault.ui.screens.PlayerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.gson.Gson

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    @Inject lateinit var screenshotManager: ScreenshotProtectionManager

    companion object {
        const val EXTRA_PLAYER_STATE = "player_state"

        fun start(context: Context, state: PlayerState) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java).apply {
                    putExtra(EXTRA_PLAYER_STATE, Gson().toJson(state))
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stateJson = intent.getStringExtra(EXTRA_PLAYER_STATE)
        val playerState = runCatching {
            Gson().fromJson(stateJson, PlayerState::class.java)
        }.getOrNull() ?: run {
            finish()
            return
        }

        if (playerState.screenshotProtection) {
            screenshotManager.enable(this)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            StreamVaultTheme {
                PlayerScreen(
                    playerState = playerState,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
