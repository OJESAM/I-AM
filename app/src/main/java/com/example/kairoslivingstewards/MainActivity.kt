package com.example.kairoslivingstewards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kairoslivingstewards.ui.navigation.Screen
import com.example.kairoslivingstewards.ui.navigation.bottomNavItems
import com.example.kairoslivingstewards.ui.screens.AdminDashboard
import com.example.kairoslivingstewards.ui.screens.DevotionalsScreen
import com.example.kairoslivingstewards.ui.screens.FellowshipScreen
import com.example.kairoslivingstewards.ui.screens.LivestreamScreen
import com.example.kairoslivingstewards.ui.theme.KairosLivingStewardsTheme
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.screens.AuthScreen
import com.example.kairoslivingstewards.ui.viewmodel.AuthState
import com.example.kairoslivingstewards.ui.viewmodel.AuthViewModel
import com.example.kairoslivingstewards.ui.viewmodel.DevotionalViewModel
import com.example.kairoslivingstewards.ui.viewmodel.FellowshipViewModel
import com.example.kairoslivingstewards.ui.viewmodel.LivestreamViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.kairoslivingstewards.ui.viewmodel.ViewModelFactory
import androidx.compose.runtime.collectAsState
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import com.example.kairoslivingstewards.ui.screens.DirectMessageScreen
import com.example.kairoslivingstewards.ui.viewmodel.DirectMessageViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KairosLivingStewardsTheme {
                val context = LocalContext.current
                val factory = ViewModelFactory(context.applicationContext as KairosApplication)
                val authViewModel: AuthViewModel = viewModel(factory = factory)
                val authState by authViewModel.authState.collectAsState()

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    // Handle permission result
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                if (authState is AuthState.Authenticated) {
                    val user = (authState as AuthState.Authenticated).user
                    MainScreen(factory, authViewModel, user)
                } else {
                    AuthScreen(authViewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(factory: ViewModelFactory, authViewModel: AuthViewModel, currentUser: UserEntity) {
    val navController = rememberNavController()
    
    val devotionalViewModel: DevotionalViewModel = viewModel(factory = factory)
    val fellowshipViewModel: FellowshipViewModel = viewModel(factory = factory)
    val livestreamViewModel: LivestreamViewModel = viewModel(factory = factory)
    val dmViewModel: DirectMessageViewModel = viewModel(factory = factory)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Devotionals.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Devotionals.route) { 
                DevotionalsScreen(devotionalViewModel, currentUser)
            }
            composable(Screen.Fellowship.route) { 
                FellowshipScreen(fellowshipViewModel, currentUser) 
            }
            composable(Screen.Messages.route) {
                DirectMessageScreen(dmViewModel, currentUser)
            }
            composable(Screen.Livestream.route) { 
                LivestreamScreen(livestreamViewModel) 
            }
            composable(Screen.Settings.route) {
                AdminDashboard(
                    devotionalViewModel = devotionalViewModel,
                    fellowshipViewModel = fellowshipViewModel,
                    livestreamViewModel = livestreamViewModel
                )
            }
        }
    }
}
