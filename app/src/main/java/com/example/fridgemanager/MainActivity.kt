package com.example.fridgemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.fridgemanager.ui.navigation.Screen
import com.example.fridgemanager.ui.navigation.bottomNavItems
import com.example.fridgemanager.ui.screens.additem.AddItemScreen
import com.example.fridgemanager.ui.screens.camera.CameraRecognitionScreen
import com.example.fridgemanager.ui.screens.camera.CameraViewModel
import com.example.fridgemanager.ui.screens.camera.MultiItemConfirmScreen
import com.example.fridgemanager.ui.screens.home.HomeScreen
import com.example.fridgemanager.ui.screens.settings.SettingsScreen
import com.example.fridgemanager.ui.screens.stats.StatsScreen
import com.example.fridgemanager.ui.theme.FridgeManagerTheme
import com.example.fridgemanager.ui.viewmodel.FoodViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FridgeManagerTheme {
                FridgeManagerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeManagerApp() {
    val navController = rememberNavController()
    val foodViewModel: FoodViewModel = hiltViewModel()
    val cameraViewModel: CameraViewModel = hiltViewModel()

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStack?.destination

    // 判断是否显示底部导航栏
    val showBottomBar = bottomNavItems.any {
        currentDestination?.hierarchy?.any { dest -> dest.route == it.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy
                                ?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = foodViewModel,
                    onAddItem = { navController.navigate(Screen.AddItem.createRoute()) },
                    onEditItem = { id -> navController.navigate(Screen.AddItem.createRoute(id)) },
                    onOpenCamera = { navController.navigate("camera") }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(viewModel = foodViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = foodViewModel)
            }

            composable(
                route = Screen.AddItem.route,
                arguments = listOf(navArgument("itemId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
                AddItemScreen(
                    viewModel = foodViewModel,
                    itemId = itemId,
                    prefillItem = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("camera") {
                CameraRecognitionScreen(
                    cameraViewModel = cameraViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToConfirm = {
                        navController.navigate("confirm") {
                            popUpTo("camera") { inclusive = false }
                        }
                    }
                )
            }

            composable("confirm") {
                MultiItemConfirmScreen(
                    cameraViewModel = cameraViewModel,
                    foodViewModel = foodViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
