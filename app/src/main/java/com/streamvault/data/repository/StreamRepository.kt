package com.streamvault.data.repository

import com.streamvault.data.local.AppPreferences
import com.streamvault.data.local.WatchProgressDao
import com.streamvault.data.local.WatchlistDao
import com.streamvault.data.models.*
import com.streamvault.data.remote.*
import com.streamvault.security.TokenManager
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class StreamRepository @Inject constructor(
    private val apiService: StreamApiService,
    private val preferences: AppPreferences,
    private val progressDao: WatchProgressDao,
    private val watchlistDao: WatchlistDao,
    private val wsClient: StreamWebSocketClient,
    private val tokenManager: TokenManager
) {

    private val headers: suspend () -> Map<String, String> = {
        val token = preferences.authToken.first()
        val config = preferences.serverConfig.first()
        buildMap {
            token?.let { put("Authorization", "Bearer $it") }
            config?.apiKey?.let { put("X-API-Key", it) }
            put("Content-Type", "application/json")
        }
    }

    private val baseUrl: suspend () -> String = {
        preferences.serverConfig.first()?.baseUrl ?: ""
    }

    suspend fun authenticate(
        endpoint: String,
        credentials: Map<String, String>,
        method: String = "POST"
    ): Result<AuthResponse> = runCatching {
        val url = "${baseUrl()}$endpoint"
        val resp = apiService.authenticate(url, credentials, emptyMap())
        if (resp.isSuccessful) {
            val body = resp.body() ?: return Result.Error("Empty response")
            val token = body["token"]?.toString() ?: body["access_token"]?.toString() ?: return Result.Error("No token in response")
            val refresh = body["refresh_token"]?.toString()
            val expires = (body["expires_in"] as? Double)?.toLong()?.let { System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(it) }
            val enc = tokenManager.encryptToken(token)
            preferences.saveAuthToken(enc, refresh, expires)
            Result.Success(AuthResponse(token = enc, refreshToken = refresh, expiresAt = expires, profile = null))
        } else {
            Result.Error("Auth failed: HTTP ${resp.code()}")
        }
    }.getOrElse { Result.Error(it.message ?: "Unknown error", it) }

    fun getHomeRows(): Flow<Result<List<HomeRow>>> = flow {
        emit(Result.Loading)
        runCatching {
            val url = "${baseUrl()}/home"
            val resp = apiService.getHome(url, headers())
            if (resp.isSuccessful) {
                emit(Result.Success(resp.body()?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("HTTP ${resp.code()}"))
            }
        }.onFailure { emit(Result.Error(it.message ?: "Unknown", it)) }
    }

    fun connectWebSocket(): Flow<WsEvent> = flow {
        val config = preferences.serverConfig.first() ?: return@flow
        val token = tokenManager.decryptToken(preferences.authToken.first() ?: "")
        val wsUrl = config.websocketUrl ?: return@flow
        emitAll(wsClient.connect(wsUrl, token))
    }

    suspend fun searchContent(query: String): Result<List<MediaItem>> = runCatching {
        val url = "${baseUrl()}/search"
        val resp = apiService.search(url, query, headers())
        if (resp.isSuccessful) {
            Result.Success(resp.body()?.map { it.toDomain() } ?: emptyList<MediaItem>())
        } else {
            Result.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { Result.Error(it.message ?: "Unknown", it) }

    suspend fun getContent(id: String): Result<MediaItem> = runCatching {
        val url = "${baseUrl()}/content/$id"
        val resp = apiService.getContent(url, headers())
        if (resp.isSuccessful) {
            val item = resp.body()?.toDomain() ?: return Result.Error("Not found")
            Result.Success(item)
        } else {
            Result.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { Result.Error(it.message ?: "Unknown", it) }

    suspend fun getProfiles(): Result<List<UserProfile>> = runCatching {
        val url = "${baseUrl()}/profiles"
        val resp = apiService.getProfiles(url, headers())
        if (resp.isSuccessful) {
            Result.Success(resp.body()?.map { it.toDomain() } ?: emptyList<UserProfile>())
        } else {
            Result.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { Result.Error(it.message ?: "Unknown", it) }

    suspend fun getNotifications(): Result<List<NotificationItem>> = runCatching {
        val url = "${baseUrl()}/notifications"
        val resp = apiService.getNotifications(url, headers())
        if (resp.isSuccessful) {
            Result.Success(resp.body()?.map { it.toDomain() } ?: emptyList<NotificationItem>())
        } else {
            Result.Error("HTTP ${resp.code()}")
        }
    }.getOrElse { Result.Error(it.message ?: "Unknown", it) }

    suspend fun saveProgress(progress: WatchProgress) = progressDao.upsert(progress)
    suspend fun getProgress(id: String, profileId: String) = progressDao.get(id, profileId)
    fun getContinueWatching(profileId: String) = progressDao.getRecent(profileId)
    fun getWatchlist(profileId: String) = watchlistDao.getAll(profileId)
    fun isInWatchlist(id: String, profileId: String) = watchlistDao.contains(id, profileId)

    suspend fun addToWatchlist(id: String, profileId: String) {
        watchlistDao.add(WatchlistItem(id, profileId, System.currentTimeMillis()))
    }

    suspend fun removeFromWatchlist(id: String, profileId: String) {
        watchlistDao.remove(id, profileId)
    }

    suspend fun logout() = preferences.clearSession()
}
