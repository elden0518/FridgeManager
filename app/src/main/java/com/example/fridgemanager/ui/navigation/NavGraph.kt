package com.example.fridgemanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "冰箱",  Icons.Default.Kitchen)
    object Stats    : Screen("stats",    "统计",  Icons.Default.BarChart)
    object Settings : Screen("settings", "设置",  Icons.Default.Settings)

    object AddItem  : Screen("add_item/{itemId}", "录入食材", Icons.Default.Kitchen) {
        fun createRoute(itemId: Long = -1) = "add_item/$itemId"
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Stats, Screen.Settings)
