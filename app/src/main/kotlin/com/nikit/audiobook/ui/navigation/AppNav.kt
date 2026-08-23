package com.nikit.audiobook.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nikit.audiobook.ui.book.BookDetailScreen
import com.nikit.audiobook.ui.library.LibraryScreen
import com.nikit.audiobook.ui.player.PlayerBar
import com.nikit.audiobook.ui.player.PlayerScreen
import com.nikit.audiobook.ui.player.PlayerViewModel
import com.nikit.audiobook.ui.settings.SettingsScreen

object Routes {
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val BOOK = "book/{bookId}"
    const val PLAYER = "player"

    fun book(id: String) = "book/$id"
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs =
    listOf(
        Tab(Routes.LIBRARY, "Библиотека", Icons.Default.LibraryBooks),
        Tab(Routes.SETTINGS, "Настройки", Icons.Default.Settings),
    )

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val playerVm: PlayerViewModel = hiltViewModel()

    val showBar = current?.route in setOf(Routes.LIBRARY, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBar) {
                Column {
                    PlayerBar(onExpand = { navController.navigate(Routes.PLAYER) }, vm = playerVm)
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = current?.hierarchy?.any { it.route == tab.route } == true,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onBookClick = { id -> navController.navigate(Routes.book(id)) },
                    onRescanClick = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(
                Routes.BOOK,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) {
                BookDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPlay = { id, chapter, position ->
                        playerVm.playBook(id, chapter, position)
                        navController.navigate(Routes.PLAYER)
                    },
                )
            }
            composable(Routes.PLAYER) {
                PlayerScreen(onBack = { navController.popBackStack() }, vm = playerVm)
            }
        }
    }
}
