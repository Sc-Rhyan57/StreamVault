package com.streamvault.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import com.streamvault.data.models.PlayerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri

@Singleton
class CastManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val _castState = MutableStateFlow<CastState>(CastState.NotConnected)
    val castState: StateFlow<CastState> = _castState

    private var castSession: CastSession? = null

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            _castState.value = CastState.Connected
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            _castState.value = CastState.NotConnected
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            _castState.value = CastState.Connected
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _castState.value = CastState.Error("Connection failed: $error")
        }

        override fun onSessionStarting(session: CastSession) {
            _castState.value = CastState.Connecting
        }

        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _castState.value = CastState.Error("Resume failed: $error")
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    fun init() {
        runCatching {
            val castContext = CastContext.getSharedInstance(context)
            castContext.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
        }
    }

    fun castMedia(state: PlayerState, title: String, posterUrl: String?, positionMs: Long) {
        val session = castSession ?: return
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            posterUrl?.let { addImage(WebImage(Uri.parse(it))) }
        }
        val contentType = when {
            state.streamUrl.contains(".m3u8") -> "application/x-mpegurl"
            state.streamUrl.contains(".mpd")  -> "application/dash+xml"
            else                               -> "video/mp4"
        }
        val mediaInfo = MediaInfo.Builder(state.streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setMetadata(metadata)
            .build()
        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setCurrentTime(positionMs)
            .setAutoplay(true)
            .build()
        session.remoteMediaClient?.load(loadRequest)
    }

    fun release() {
        runCatching {
            CastContext.getSharedInstance(context)
                .sessionManager
                .removeSessionManagerListener(sessionListener, CastSession::class.java)
        }
    }
}

sealed class CastState {
    object NotConnected : CastState()
    object Connecting   : CastState()
    object Connected    : CastState()
    data class Error(val message: String) : CastState()
}
