package com.streamvault.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamvault.data.models.*
import com.streamvault.ui.components.*
import com.streamvault.ui.theme.StreamColors

@Composable
fun HomeScreen(
    onItemClick: (MediaItem) -> Unit,
    onSearch: () -> Unit,
    onMyArea: () -> Unit,
    onNotifications: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var currentTab by remember { mutableStateOf("home") }

    Box(Modifier.fillMaxSize().background(StreamColors.Background)) {
        if (state.isLoading) {
            HomeShimmer()
        } else if (state.error != null) {
            ErrorView(state.error!!, onRetry = { viewModel.refresh() })
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    state.featured?.let { featured ->
                        HeroBanner(
                            item          = featured,
                            onPlay        = { onItemClick(featured) },
                            onInfo        = { onItemClick(featured) },
                            onMyList      = {},
                            isInWatchlist = false
                        )
                    }
                }

                if (state.continueWatching.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Continuar assistindo",
                            color = StreamColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.continueWatching) { (item, progress) ->
                                ContinueWatchingCard(
                                    item     = item,
                                    progress = progress,
                                    onClick  = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }

                items(state.rows) { row ->
                    Spacer(Modifier.height(12.dp))
                    MediaRowSection(row = row, onItemClick = onItemClick)
                }
            }

            Box(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                StreamVaultTopBar(
                    profileAvatarUrl    = state.profileAvatar,
                    profileName         = state.profileName,
                    notificationCount   = state.notifications.count { !it.isRead },
                    currentRoute        = currentTab,
                    onRouteChange       = { currentTab = it },
                    onProfileClick      = onProfileClick,
                    onNotificationsClick = onNotifications
                )
            }
        }

        BottomNavBar(
            currentRoute  = if (currentTab == "search") "search" else if (currentTab == "mynetflix") "mynetflix" else "home",
            onRouteChange = { route ->
                when (route) {
                    "search"    -> onSearch()
                    "mynetflix" -> onMyArea()
                    else        -> currentTab = route
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeShimmer() {
    Column(Modifier.fillMaxSize()) {
        LoadingShimmer(Modifier.fillMaxWidth().height(500.dp))
        Spacer(Modifier.height(16.dp))
        repeat(3) {
            Spacer(Modifier.height(8.dp))
            LoadingShimmer(Modifier.padding(horizontal = 16.dp).width(160.dp).height(18.dp))
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    LoadingShimmer(Modifier.width(110.dp).height(165.dp))
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(currentRoute: String, onRouteChange: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        containerColor = StreamColors.Surface,
        contentColor   = Color.White,
        tonalElevation = 0.dp,
        modifier       = modifier
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick  = { onRouteChange("home") },
            icon     = {
                Icon(
                    if (currentRoute == "home") Icons.Outlined.Home else Icons.Outlined.Home,
                    null
                )
            },
            label  = { Text("Início") },
            colors = navItemColors()
        )
        NavigationBarItem(
            selected = currentRoute == "search",
            onClick  = { onRouteChange("search") },
            icon     = { Icon(Icons.Outlined.Search, null) },
            label    = { Text("Buscar") },
            colors   = navItemColors()
        )
        NavigationBarItem(
            selected = currentRoute == "mynetflix",
            onClick  = { onRouteChange("mynetflix") },
            icon     = { Icon(Icons.Outlined.Person, null) },
            label    = { Text("Minha área") },
            colors   = navItemColors()
        )
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor   = Color.White,
    unselectedIconColor = StreamColors.TextMuted,
    selectedTextColor   = Color.White,
    unselectedTextColor = StreamColors.TextMuted,
    indicatorColor      = StreamColors.SurfaceVar
)
