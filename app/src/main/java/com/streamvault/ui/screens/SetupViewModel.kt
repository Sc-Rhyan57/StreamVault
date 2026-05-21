package com.streamvault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.local.AppPreferences
import com.streamvault.data.models.ConnectionType
import com.streamvault.data.models.ServerConfig
import com.streamvault.data.repository.Result
import com.streamvault.data.repository.StreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupState(
    val step: SetupStep = SetupStep.SERVER,
    val baseUrl: String = "",
    val connectionType: ConnectionType = ConnectionType.REST,
    val wsUrl: String = "",
    val apiKey: String = "",
    val drmUrl: String = "",
    val authEndpoint: String = "/auth/login",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class SetupStep { SERVER, AUTH, PROFILES }

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val repository: StreamRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    fun updateBaseUrl(v: String)  { _state.update { it.copy(baseUrl = v) } }
    fun updateWsUrl(v: String)    { _state.update { it.copy(wsUrl = v) } }
    fun updateApiKey(v: String)   { _state.update { it.copy(apiKey = v) } }
    fun updateDrmUrl(v: String)   { _state.update { it.copy(drmUrl = v) } }
    fun updateConnType(v: ConnectionType) { _state.update { it.copy(connectionType = v) } }
    fun updateUsername(v: String) { _state.update { it.copy(username = v) } }
    fun updatePassword(v: String) { _state.update { it.copy(password = v) } }
    fun updateAuthEndpoint(v: String) { _state.update { it.copy(authEndpoint = v) } }

    fun saveServer() {
        val s = _state.value
        viewModelScope.launch {
            preferences.saveServerConfig(
                ServerConfig(
                    baseUrl        = s.baseUrl.trimEnd('/'),
                    connectionType = s.connectionType,
                    websocketUrl   = s.wsUrl.takeIf { it.isNotBlank() },
                    apiKey         = s.apiKey.takeIf { it.isNotBlank() },
                    authHeader     = null,
                    drmLicenseUrl  = s.drmUrl.takeIf { it.isNotBlank() },
                    drmHeaders     = emptyMap()
                )
            )
            _state.update { it.copy(step = SetupStep.AUTH) }
        }
    }

    fun authenticate(onSuccess: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = repository.authenticate(
                endpoint    = s.authEndpoint,
                credentials = mapOf("username" to s.username, "password" to s.password)
            )
            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, step = SetupStep.PROFILES) }
                    onSuccess()
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun skipAuth(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(step = SetupStep.PROFILES) }
            onSuccess()
        }
    }
}
