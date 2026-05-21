package com.streamvault.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.streamvault.data.models.DrmScheme
import com.streamvault.data.models.PlayerState
import com.streamvault.data.models.VideoFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient

class PlayerEngine(private val context: Context, private val httpClient: OkHttpClient) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val trackSelector = DefaultTrackSelector(context)
    val player: ExoPlayer

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private var progressJob: Job? = null
    var onProgressChanged: ((Long, Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(buildMediaSourceFactory())
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = when (state) {
                    Player.STATE_IDLE    -> PlaybackState.Idle
                    Player.STATE_BUFFERING -> PlaybackState.Buffering
                    Player.STATE_READY   -> if (player.playWhenReady) PlaybackState.Playing else PlaybackState.Paused
                    Player.STATE_ENDED   -> PlaybackState.Ended
                    else -> PlaybackState.Idle
                }
                if (state == Player.STATE_READY) startProgressTracking()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (player.playbackState == Player.STATE_READY) {
                    _playbackState.value = if (isPlaying) PlaybackState.Playing else PlaybackState.Paused
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = PlaybackState.Error(error.message ?: "Playback error")
                onError?.invoke(error.message ?: "Playback error")
            }
        })
    }

    fun load(state: PlayerState) {
        val mediaSource = buildMediaSource(state)
        player.setMediaSource(mediaSource)
        player.prepare()
        if (state.startPositionMs > 0) player.seekTo(state.startPositionMs)
        player.playWhenReady = true
    }

    private fun buildMediaSource(state: PlayerState): MediaSource {
        val uri          = Uri.parse(state.streamUrl)
        val dataSource   = buildDataSourceFactory(state.drmHeaders)
        val drmConfig    = buildDrmConfig(state)
        val mediaItem    = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeTypeFor(state.videoFormat))
            .apply { drmConfig?.let { setDrmConfiguration(it) } }
            .build()

        return when (state.videoFormat) {
            VideoFormat.HLS              -> HlsMediaSource.Factory(dataSource).createMediaSource(mediaItem)
            VideoFormat.DASH             -> DashMediaSource.Factory(dataSource).createMediaSource(mediaItem)
            VideoFormat.SMOOTH_STREAMING -> SsMediaSource.Factory(dataSource).createMediaSource(mediaItem)
            VideoFormat.RTSP             -> RtspMediaSource.Factory().createMediaSource(mediaItem)
            else                         -> ProgressiveMediaSource.Factory(dataSource).createMediaSource(mediaItem)
        }
    }

    private fun buildDrmConfig(state: PlayerState): MediaItem.DrmConfiguration? {
        if (state.drmScheme == DrmScheme.NONE) return null
        val licenseUrl = state.drmLicenseUrl ?: return null
        return MediaItem.DrmConfiguration.Builder(
            when (state.drmScheme) {
                DrmScheme.WIDEVINE  -> C.WIDEVINE_UUID
                DrmScheme.PLAYREADY -> C.PLAYREADY_UUID
                DrmScheme.CLEARKEY  -> C.CLEARKEY_UUID
                else                -> C.WIDEVINE_UUID
            }
        )
            .setLicenseUri(licenseUrl)
            .setLicenseRequestHeaders(state.drmHeaders)
            .build()
    }

    private fun buildDataSourceFactory(extraHeaders: Map<String, String>) =
        OkHttpDataSource.Factory(httpClient).apply {
            if (extraHeaders.isNotEmpty()) setDefaultRequestProperties(extraHeaders)
        }

    private fun buildMediaSourceFactory(): DefaultMediaSourceFactory {
        val factory = OkHttpDataSource.Factory(httpClient)
        return DefaultMediaSourceFactory(context).setDataSourceFactory(factory)
    }

    private fun mimeTypeFor(format: VideoFormat): String = when (format) {
        VideoFormat.HLS              -> MimeTypes.APPLICATION_M3U8
        VideoFormat.DASH             -> MimeTypes.APPLICATION_MPD
        VideoFormat.SMOOTH_STREAMING -> MimeTypes.APPLICATION_SS
        VideoFormat.RTSP             -> MimeTypes.APPLICATION_RTSP
        VideoFormat.MKV              -> MimeTypes.VIDEO_MATROSKA
        else                         -> MimeTypes.VIDEO_MP4
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val pos = player.currentPosition
                val dur = player.duration.takeIf { it > 0 } ?: 0L
                _position.value = pos
                onProgressChanged?.invoke(pos, dur)
                delay(1000)
            }
        }
    }

    fun seekTo(ms: Long) = player.seekTo(ms)
    fun seekForward(ms: Long = 10_000) = player.seekTo((player.currentPosition + ms).coerceAtMost(player.duration))
    fun seekBackward(ms: Long = 10_000) = player.seekTo((player.currentPosition - ms).coerceAtLeast(0))
    fun togglePlay() { if (player.isPlaying) player.pause() else player.play() }
    fun pause() = player.pause()
    fun play() = player.play()
    fun setVolume(v: Float) { player.volume = v }

    fun setAudioTrack(trackIndex: Int) {
        val tracks = player.currentTracks
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (trackIndex < audioGroups.size) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setOverrideForType(TrackSelectionOverride(audioGroups[trackIndex].mediaTrackGroup, 0))
            )
        }
    }

    fun setSubtitleTrack(trackIndex: Int) {
        val tracks = player.currentTracks
        val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        if (trackIndex < 0) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters().setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            )
        } else if (trackIndex < textGroups.size) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setOverrideForType(TrackSelectionOverride(textGroups[trackIndex].mediaTrackGroup, 0))
            )
        }
    }

    fun release() {
        progressJob?.cancel()
        scope.cancel()
        player.release()
    }
}

sealed class PlaybackState {
    object Idle      : PlaybackState()
    object Buffering : PlaybackState()
    object Playing   : PlaybackState()
    object Paused    : PlaybackState()
    object Ended     : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
