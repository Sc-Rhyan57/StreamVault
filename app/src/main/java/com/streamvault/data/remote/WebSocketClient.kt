package com.streamvault.data.remote

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed class WsEvent {
    data class Message(val payload: JSONObject) : WsEvent()
    data class Connected(val url: String) : WsEvent()
    data class Disconnected(val code: Int, val reason: String) : WsEvent()
    data class Error(val cause: Throwable) : WsEvent()
}

@Singleton
class StreamWebSocketClient @Inject constructor(private val httpClient: OkHttpClient) {

    private var webSocket: WebSocket? = null

    fun connect(url: String, token: String?): Flow<WsEvent> = callbackFlow {
        val request = Request.Builder()
            .url(url)
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                webSocket = ws
                trySend(WsEvent.Connected(url))
            }

            override fun onMessage(ws: WebSocket, text: String) {
                runCatching { trySend(WsEvent.Message(JSONObject(text))) }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                trySend(WsEvent.Disconnected(code, reason))
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WsClient", "Failure", t)
                trySend(WsEvent.Error(t))
            }
        }

        webSocket = httpClient.newWebSocket(request, listener)

        awaitClose { disconnect() }
    }

    fun send(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closed")
        webSocket = null
    }
}
