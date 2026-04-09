package com.example.kairoslivingstewards.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Devotionals : Screen("devotionals", "Devotionals", Icons.Rounded.Book)
    object Fellowship : Screen("fellowship", "Fellowship", Icons.Rounded.Groups)
    object Messages : Screen("messages", "Messages", Icons.AutoMirrored.Rounded.Chat)
    object Livestream : Screen("livestream", "Livestream", Icons.Rounded.LiveTv)
    object Admin : Screen("admin", "Admin", Icons.Rounded.Shield)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
}
    val bottomNavItems = listOf(
    Screen.Devotionals,
    Screen.Fellowship,
    Screen.Messages,
    Screen.Livestream,
    Screen.Settings
)
