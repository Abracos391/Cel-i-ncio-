package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.CelViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Painel", Icons.Default.Dashboard)
    object Logs : Screen("logs", "Bloqueios", Icons.Default.History)
    object Lists : Screen("lists", "Listas", Icons.Default.FormatListBulleted)
    object Config : Screen("config", "Ajustes", Icons.Default.Settings)
    object About : Screen("about", "Sobre", Icons.Default.Info)
}

@Composable
fun MainNavigationContainer(viewModel: CelViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        Screen.Dashboard,
        Screen.Logs,
        Screen.Lists,
        Screen.Config,
        Screen.About
    )

    // Hide bottom navigation bar on Splash and Permissions screens
    val shouldShowBottomBar = currentRoute in navigationItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    navigationItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(navController = navController)
            }
            
            composable("permissions") {
                PermissionsScreen(navController = navController)
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                    onNavigateToLists = { navController.navigate(Screen.Lists.route) }
                )
            }

            composable(Screen.Logs.route) {
                BlockedLogsScreen(viewModel = viewModel)
            }

            composable(Screen.Lists.route) {
                ListsScreen(viewModel = viewModel)
            }

            composable(Screen.Config.route) {
                ConfigScreen(viewModel = viewModel)
            }

            composable(Screen.About.route) {
                AboutScreen()
            }
        }
    }
}
