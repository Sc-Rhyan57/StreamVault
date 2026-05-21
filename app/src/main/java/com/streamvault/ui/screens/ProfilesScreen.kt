package com.streamvault.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.streamvault.data.local.AppPreferences
import com.streamvault.data.models.UserProfile
import com.streamvault.data.repository.Result
import com.streamvault.data.repository.StreamRepository
import com.streamvault.ui.theme.StreamColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repository: StreamRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _loading.value = true
            when (val r = repository.getProfiles()) {
                is Result.Success -> _profiles.value = r.data
                else -> _profiles.value = listOf(
                    UserProfile("default", "Usuário", null, com.streamvault.data.models.ProfileType.ADULT, null, 18)
                )
            }
            _loading.value = false
        }
    }

    fun selectProfile(profile: UserProfile, onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.saveProfile(profile.id, profile.name, profile.avatarUrl)
            onDone()
        }
    }
}

@Composable
fun ProfilesScreen(onProfileSelected: () -> Unit, vm: ProfilesViewModel = hiltViewModel()) {
    val profiles by vm.profiles.collectAsState()
    val loading  by vm.loading.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(StreamColors.Background),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = StreamColors.Primary)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(60.dp))
                Text("Quem está assistindo?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(40.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (profiles.size <= 2) profiles.size else 3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(profiles) { profile ->
                        ProfileItem(profile = profile, onClick = { vm.selectProfile(profile, onProfileSelected) })
                    }
                }
                Spacer(Modifier.height(32.dp))
                OutlinedButton(
                    onClick = {},
                    border = BorderStroke(1.dp, StreamColors.TextMuted),
                    shape  = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = StreamColors.TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gerenciar perfis", color = StreamColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ProfileItem(profile: UserProfile, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale.value)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StreamColors.Primary)
        ) {
            if (profile.avatarUrl != null) {
                AsyncImage(model = profile.avatarUrl, contentDescription = profile.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "U", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                }
            }
            if (profile.type == com.streamvault.data.models.ProfileType.KID) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(StreamColors.Background, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Infantil", fontSize = 8.sp, color = StreamColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(profile.name, color = StreamColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
