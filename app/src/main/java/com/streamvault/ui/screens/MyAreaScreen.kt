package com.streamvault.ui.screens

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.streamvault.data.local.AppPreferences
import com.streamvault.data.models.*
import com.streamvault.data.repository.Result
import com.streamvault.data.repository.StreamRepository
import com.streamvault.ui.components.*
import com.streamvault.ui.theme.StreamColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyAreaState(
    val profileName: String     = "",
    val profileAvatar: String?  = null,
    val watchlist: List<MediaItem> = emptyList(),
    val downloads: List<MediaItem> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val screenshotProtection: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class MyAreaViewModel @Inject constructor(
    private val repository: StreamRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(MyAreaState())
    val state: StateFlow<MyAreaState> = _state.asStateFlow()

    private var profileId = ""

    init {
        viewModelScope.launch {
            preferences.profileId.filterNotNull().collect { profileId = it; loadAll() }
        }
        viewModelScope.launch {
            preferences.profileName.collect { name -> _state.update { it.copy(profileName = name ?: "") } }
        }
        viewModelScope.launch {
            preferences.profileAvatar.collect { url -> _state.update { it.copy(profileAvatar = url) } }
        }
        viewModelScope.launch {
            preferences.screenshotProtection.collect { v -> _state.update { it.copy(screenshotProtection = v) } }
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            repository.getWatchlist(profileId).collect { items ->
                val media = items.mapNotNull { wl ->
                    val r = repository.getContent(wl.contentId)
                    if (r is Result.Success) r.data else null
                }
                _state.update { it.copy(watchlist = media, isLoading = false) }
            }
        }
        viewModelScope.launch {
            when (val r = repository.getNotifications()) {
                is Result.Success -> _state.update { it.copy(notifications = r.data) }
                else -> {}
            }
        }
    }

    fun setScreenshotProtection(v: Boolean) {
        viewModelScope.launch { preferences.setScreenshotProtection(v) }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch { repository.logout(); onDone() }
    }
}

@Composable
fun MyAreaScreen(
    onItemClick: (MediaItem) -> Unit,
    onNotifications: () -> Unit,
    onProfileSwitch: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    vm: MyAreaViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StreamColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.profileAvatar != null) {
                        AsyncImage(model = state.profileAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(state.profileName.firstOrNull()?.uppercaseChar()?.toString() ?: "U", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(state.profileName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Ver perfil", color = StreamColors.TextMuted, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onNotifications) {
                    BadgedBox(badge = {
                        val unread = state.notifications.count { !it.isRead }
                        if (unread > 0) Badge { Text(unread.toString()) }
                    }) {
                        Icon(Icons.Outlined.Notifications, null, tint = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (state.watchlist.isNotEmpty()) {
            Text("Minha lista", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.watchlist) { item ->
                    MediaCard(item = item, onClick = { onItemClick(item) }, modifier = Modifier.width(110.dp).height(165.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        SectionHeader("Downloads")
        MyAreaOption(Icons.Outlined.Download, "Downloads", "Os filmes e séries baixados ficam aqui.") {}

        Spacer(Modifier.height(8.dp))
        SectionHeader("Configurações")

        MyAreaOption(Icons.Outlined.Person, "Trocar perfil", "Selecione outro perfil") { onProfileSwitch() }
        MyAreaOption(Icons.Outlined.Notifications, "Notificações", "${state.notifications.count { !it.isRead }} não lidas") { onNotifications() }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Security, null, tint = StreamColors.TextSecondary, modifier = Modifier.size(22.dp))
                Column {
                    Text("Proteção de tela", color = Color.White, fontSize = 14.sp)
                    Text("Bloqueia captura de tela", color = StreamColors.TextMuted, fontSize = 12.sp)
                }
            }
            Switch(
                checked = state.screenshotProtection,
                onCheckedChange = vm::setScreenshotProtection,
                colors = SwitchDefaults.colors(checkedThumbColor = StreamColors.Primary, checkedTrackColor = StreamColors.Primary.copy(0.3f))
            )
        }

        MyAreaOption(Icons.Outlined.Settings, "Configurações do app", "") { onSettings() }
        MyAreaOption(Icons.Outlined.Help, "Ajuda", "") {}

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick  = { vm.logout(onLogout) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Outlined.Logout, null, tint = StreamColors.TextSecondary)
            Spacer(Modifier.width(8.dp))
            Text("Sair", color = StreamColors.TextSecondary, fontSize = 14.sp)
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color    = StreamColors.TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun MyAreaOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = StreamColors.TextSecondary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = StreamColors.TextMuted, fontSize = 12.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = StreamColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}
