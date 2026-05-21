package com.streamvault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.local.AppPreferences
import com.streamvault.data.models.*
import com.streamvault.data.repository.Result
import com.streamvault.data.repository.StreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val rows: List<HomeRow> = emptyList(),
    val featured: MediaItem? = null,
    val continueWatching: List<Pair<MediaItem, WatchProgress>> = emptyList(),
    val myList: List<MediaItem> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val profileName: String? = null,
    val profileAvatar: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StreamRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val profileId: String get() = _currentProfileId

    private var _currentProfileId = ""

    init {
        viewModelScope.launch {
            preferences.profileId.filterNotNull().collect { id ->
                _currentProfileId = id
                loadAll()
            }
        }
        viewModelScope.launch {
            preferences.profileName.collect { name ->
                _state.update { it.copy(profileName = name) }
            }
        }
        viewModelScope.launch {
            preferences.profileAvatar.collect { url ->
                _state.update { it.copy(profileAvatar = url) }
            }
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.getHomeRows()
                .collect { result ->
                    when (result) {
                        is Result.Loading -> _state.update { it.copy(isLoading = true) }
                        is Result.Success -> {
                            val rows = result.data
                            val featured = rows.flatMap { it.items }.firstOrNull { it.isFeatured }
                                ?: rows.firstOrNull()?.items?.firstOrNull()
                            _state.update { it.copy(rows = rows, featured = featured, isLoading = false) }
                        }
                        is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
        }
        viewModelScope.launch {
            loadContinueWatching()
        }
        viewModelScope.launch {
            loadNotifications()
        }
    }

    private suspend fun loadContinueWatching() {
        if (profileId.isBlank()) return
        repository.getContinueWatching(profileId).collect { progressList ->
            val pairs = progressList.mapNotNull { progress ->
                val result = repository.getContent(progress.contentId)
                if (result is Result.Success) Pair(result.data, progress) else null
            }
            _state.update { it.copy(continueWatching = pairs) }
        }
    }

    private suspend fun loadNotifications() {
        when (val r = repository.getNotifications()) {
            is Result.Success -> _state.update { it.copy(notifications = r.data) }
            else -> {}
        }
    }

    fun refresh() = loadAll()
}
