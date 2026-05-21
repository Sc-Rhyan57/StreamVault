package com.streamvault.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.streamvault.data.models.ConnectionType
import com.streamvault.data.models.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "streamvault_prefs")

class AppPreferences(context: Context) {

    private val store = context.dataStore

    companion object {
        val KEY_BASE_URL        = stringPreferencesKey("base_url")
        val KEY_WS_URL          = stringPreferencesKey("ws_url")
        val KEY_API_KEY         = stringPreferencesKey("api_key")
        val KEY_AUTH_TOKEN      = stringPreferencesKey("auth_token")
        val KEY_REFRESH_TOKEN   = stringPreferencesKey("refresh_token")
        val KEY_TOKEN_EXPIRES   = longPreferencesKey("token_expires")
        val KEY_CONN_TYPE       = stringPreferencesKey("conn_type")
        val KEY_DRM_URL         = stringPreferencesKey("drm_url")
        val KEY_PROFILE_ID      = stringPreferencesKey("profile_id")
        val KEY_PROFILE_NAME    = stringPreferencesKey("profile_name")
        val KEY_PROFILE_AVATAR  = stringPreferencesKey("profile_avatar")
        val KEY_SCREENSHOT_PROT = booleanPreferencesKey("screenshot_protection")
        val KEY_CONFIGURED      = booleanPreferencesKey("configured")
    }

    val isConfigured: Flow<Boolean> = store.data.map { it[KEY_CONFIGURED] ?: false }
    val authToken: Flow<String?>    = store.data.map { it[KEY_AUTH_TOKEN] }
    val profileId: Flow<String?>    = store.data.map { it[KEY_PROFILE_ID] }
    val profileName: Flow<String?>  = store.data.map { it[KEY_PROFILE_NAME] }
    val profileAvatar: Flow<String?> = store.data.map { it[KEY_PROFILE_AVATAR] }
    val screenshotProtection: Flow<Boolean> = store.data.map { it[KEY_SCREENSHOT_PROT] ?: true }

    val serverConfig: Flow<ServerConfig?> = store.data.map { prefs ->
        val url = prefs[KEY_BASE_URL] ?: return@map null
        ServerConfig(
            baseUrl         = url,
            connectionType  = runCatching { ConnectionType.valueOf(prefs[KEY_CONN_TYPE] ?: "") }.getOrElse { ConnectionType.REST },
            websocketUrl    = prefs[KEY_WS_URL],
            apiKey          = prefs[KEY_API_KEY],
            authHeader      = prefs[KEY_AUTH_TOKEN]?.let { "Bearer $it" },
            drmLicenseUrl   = prefs[KEY_DRM_URL],
            drmHeaders      = emptyMap()
        )
    }

    suspend fun saveServerConfig(config: ServerConfig) {
        store.edit { prefs ->
            prefs[KEY_BASE_URL]    = config.baseUrl
            prefs[KEY_CONN_TYPE]   = config.connectionType.name
            config.websocketUrl?.let { prefs[KEY_WS_URL] = it }
            config.apiKey?.let      { prefs[KEY_API_KEY] = it }
            config.drmLicenseUrl?.let { prefs[KEY_DRM_URL] = it }
            prefs[KEY_CONFIGURED]  = true
        }
    }

    suspend fun saveAuthToken(token: String, refresh: String?, expiresAt: Long?) {
        store.edit { prefs ->
            prefs[KEY_AUTH_TOKEN]   = token
            refresh?.let { prefs[KEY_REFRESH_TOKEN] = it }
            expiresAt?.let { prefs[KEY_TOKEN_EXPIRES] = it }
        }
    }

    suspend fun saveProfile(id: String, name: String, avatar: String?) {
        store.edit { prefs ->
            prefs[KEY_PROFILE_ID]     = id
            prefs[KEY_PROFILE_NAME]   = name
            avatar?.let { prefs[KEY_PROFILE_AVATAR] = it }
        }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        store.edit { it[KEY_SCREENSHOT_PROT] = enabled }
    }

    suspend fun clearSession() {
        store.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_TOKEN_EXPIRES)
            prefs.remove(KEY_PROFILE_ID)
            prefs.remove(KEY_PROFILE_NAME)
            prefs.remove(KEY_PROFILE_AVATAR)
        }
    }

    suspend fun clearAll() {
        store.edit { it.clear() }
    }
}
