package com.streamvault.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.PlayerView
import com.streamvault.data.local.AppPreferences
import com.streamvault.data.models.PlayerState
import com.streamvault.data.models.WatchProgress
import com.streamvault.data.repository.StreamRepository
import com.streamvault.player.PlaybackState
import com.streamvault.player.PlayerEngine
import com.streamvault.ui.theme.StreamColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: StreamRepository,
    private val preferences: AppPreferences,
    private val httpClient: OkHttpClient
) : ViewModel() {

    var engine: PlayerEngine? = null
        private set

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val _position      = MutableStateFlow(0L)
    private val _duration      = MutableStateFlow(0L)
    private val _controlsVisible = MutableStateFlow(true)

    val playbackState: StateFlow<PlaybackState>   = _playbackState.asStateFlow()
    val position: StateFlow<Long>                 = _position.asStateFlow()
    val duration: StateFlow<Long>                 = _duration.asStateFlow()
    val controlsVisible: StateFlow<Boolean>       = _controlsVisible.asStateFlow()

    private var hideJob: Job? = null
    private var profileId = ""

    init {
        viewModelScope.launch {
            preferences.profileId.filterNotNull().collect { profileId = it }
        }
    }

    fun initEngine(context: android.content.Context, playerState: PlayerState) {
        if (engine != null) return
        engine = PlayerEngine(context, httpClient).also { e ->
            e.onProgressChanged = { pos, dur ->
                _position.value  = pos
                _duration.value  = dur
                saveProgressDebounced(playerState.contentId, pos, dur)
            }
            e.load(playerState)
            viewModelScope.launch {
                e.playbackState.collect { _playbackState.value = it }
            }
        }
        scheduleHideControls()
    }

    fun toggleControls() {
        _controlsVisible.value = !_controlsVisible.value
        if (_controlsVisible.value) scheduleHideControls()
    }

    private fun scheduleHideControls() {
        hideJob?.cancel()
        hideJob = viewModelScope.launch {
            delay(4000)
            _controlsVisible.value = false
        }
    }

    fun showControls() {
        _controlsVisible.value = true
        scheduleHideControls()
    }

    private var progressJob: Job? = null

    private fun saveProgressDebounced(id: String, pos: Long, dur: Long) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            delay(5000)
            if (profileId.isNotBlank()) {
                repository.saveProgress(
                    WatchProgress(
                        contentId  = id,
                        profileId  = profileId,
                        positionMs = pos,
                        durationMs = dur,
                        seasonNumber = null,
                        episodeId  = null,
                        updatedAt  = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        engine?.release()
        engine = null
        super.onCleared()
    }
}

@Composable
fun PlayerScreen(playerState: PlayerState, onBack: () -> Unit, vm: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val context = LocalContext.current

    LaunchedEffect(playerState) { vm.initEngine(context, playerState) }

    val pbState      by vm.playbackState.collectAsState()
    val position     by vm.position.collectAsState()
    val duration     by vm.duration.collectAsState()
    val showControls by vm.controlsVisible.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = { vm.toggleControls() }) }
    ) {
        val engine = vm.engine
        if (engine != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = engine.player
                        useController = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (pbState is PlaybackState.Buffering) {
            CircularProgressIndicator(
                color = StreamColors.Primary,
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                strokeWidth = 3.dp
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter   = fadeIn(),
            exit    = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControls(
                title       = playerState.title,
                playbackState = pbState,
                position    = position,
                duration    = duration,
                onBack      = onBack,
                onPlayPause = { vm.showControls(); engine?.togglePlay() },
                onForward   = { vm.showControls(); engine?.seekForward() },
                onRewind    = { vm.showControls(); engine?.seekBackward() },
                onSeek      = { ms -> vm.showControls(); engine?.seekTo(ms) }
            )
        }
    }
}

@Composable
private fun PlayerControls(
    title: String,
    playbackState: PlaybackState,
    position: Long,
    duration: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onRewind: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(0.7f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(0.85f)
                    )
                )
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
        }

        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRewind, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Outlined.Replay10, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Box(
                Modifier
                    .size(72.dp)
                    .background(Color.White.copy(0.15f), CircleShape)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                when (playbackState) {
                    is PlaybackState.Playing  -> Icon(Icons.Filled.Pause,     null, tint = Color.White, modifier = Modifier.size(42.dp))
                    is PlaybackState.Buffering -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                    else                      -> Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
            }
            IconButton(onClick = onForward, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Outlined.Forward10, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatMs(position), color = Color.White, fontSize = 12.sp)
                Text(formatMs(duration), color = StreamColors.TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Slider(
                value         = progress,
                onValueChange = { onSeek((it * duration).toLong()) },
                modifier      = Modifier.fillMaxWidth(),
                colors        = SliderDefaults.colors(
                    thumbColor          = Color.White,
                    activeTrackColor    = StreamColors.Primary,
                    inactiveTrackColor  = Color.White.copy(0.3f)
                )
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.ClosedCaption, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.AudioFile, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Cast, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Fullscreen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s   = ms / 1000
    val h   = s / 3600
    val m   = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
