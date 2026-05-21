package com.streamvault.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.streamvault.data.models.PlayerState
import com.streamvault.player.PlayerActivity
import com.streamvault.ui.screens.*

sealed class Route(val path: String) {
    object Setup         : Route("setup")
    object Profiles      : Route("profiles")
    object Home          : Route("home")
    object Search        : Route("search")
    object MyArea        : Route("my_area")
    object Notifications : Route("notifications")
    object Detail        : Route("detail/{contentId}") {
        fun build(id: String) = "detail/$id"
    }
}

@Composable
fun AppNavGraph(startDestination: String, navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current

    NavHost(
        navController     = navController,
        startDestination  = startDestination,
        enterTransition   = { fadeIn(tween(250)) },
        exitTransition    = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition  = { fadeOut(tween(200)) }
    ) {

        composable(Route.Setup.path) {
            SetupScreen(onComplete = {
                navController.navigate(Route.Profiles.path) {
                    popUpTo(Route.Setup.path) { inclusive = true }
                }
            })
        }

        composable(Route.Profiles.path) {
            ProfilesScreen(onProfileSelected = {
                navController.navigate(Route.Home.path) {
                    popUpTo(Route.Profiles.path) { inclusive = true }
                }
            })
        }

        composable(Route.Home.path) {
            HomeScreen(
                onItemClick = { item ->
                    navController.navigate(Route.Detail.build(item.id))
                },
                onSearch = { navController.navigate(Route.Search.path) },
                onMyArea = { navController.navigate(Route.MyArea.path) },
                onNotifications = { navController.navigate(Route.Notifications.path) },
                onProfileClick = { navController.navigate(Route.Profiles.path) }
            )
        }

        composable(Route.Search.path) {
            SearchScreen(
                onItemClick = { item -> navController.navigate(Route.Detail.build(item.id)) },
                onBack      = { navController.popBackStack() }
            )
        }

        composable(
            route     = Route.Detail.path,
            arguments = listOf(navArgument("contentId") { type = NavType.StringType })
        ) {
            DetailScreen(
                onPlay = { state -> PlayerActivity.start(context, state) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.MyArea.path) {
            val homeEntry = remember(it) { navController.getBackStackEntry(Route.Home.path) }
            val homeVm    = hiltViewModel<HomeViewModel>(homeEntry)
            val state by homeVm.state.collectAsState()
            MyAreaScreen(
                onItemClick      = { item -> navController.navigate(Route.Detail.build(item.id)) },
                onNotifications  = { navController.navigate(Route.Notifications.path) },
                onProfileSwitch  = {
                    navController.navigate(Route.Profiles.path) {
                        popUpTo(Route.Home.path) { inclusive = false }
                    }
                },
                onSettings = {},
                onLogout = {
                    navController.navigate(Route.Setup.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Notifications.path) {
            val homeEntry = remember(it) { navController.getBackStackEntry(Route.Home.path) }
            val homeVm    = hiltViewModel<HomeViewModel>(homeEntry)
            val state by homeVm.state.collectAsState()
            NotificationsScreen(
                notifications = state.notifications,
                onBack        = { navController.popBackStack() }
            )
        }
    }
}
