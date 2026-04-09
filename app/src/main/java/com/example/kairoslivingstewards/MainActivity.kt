package com.example.kairoslivingstewards

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.navigation.Screen
import com.example.kairoslivingstewards.ui.navigation.bottomNavItems
import com.example.kairoslivingstewards.ui.screens.AdminDashboard
import com.example.kairoslivingstewards.ui.screens.AuthScreen
import com.example.kairoslivingstewards.ui.screens.DevotionalsScreen
import com.example.kairoslivingstewards.ui.screens.DirectMessageScreen
import com.example.kairoslivingstewards.ui.screens.FellowshipScreen
import com.example.kairoslivingstewards.ui.screens.LivestreamScreen
import com.example.kairoslivingstewards.ui.screens.SettingsScreen
import com.example.kairoslivingstewards.ui.theme.KairosLivingStewardsTheme
import com.example.kairoslivingstewards.ui.viewmodel.AuthState
import com.example.kairoslivingstewards.ui.viewmodel.AuthViewModel
import com.example.kairoslivingstewards.ui.viewmodel.DevotionalViewModel
import com.example.kairoslivingstewards.ui.viewmodel.DirectMessageViewModel
import com.example.kairoslivingstewards.ui.viewmodel.FellowshipViewModel
import com.example.kairoslivingstewards.ui.viewmodel.LivestreamViewModel
import com.example.kairoslivingstewards.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
                    if (user.isVerified) {
                        MainScreen(factory, authViewModel, user)
                    } else {
                        EmailVerificationScreen(authViewModel)
                    }
                } else {
                    AuthScreen(authViewModel)
                }
            }
        }
    }
}

@Composable
fun EmailVerificationScreen(viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify your Email",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "A verification link has been sent to your email. Please verify it and login again.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.logout() }) {
            Text("Back to Login")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(factory: ViewModelFactory, authViewModel: AuthViewModel, currentUser: UserEntity) {
    val navController = rememberNavController()
    
    val devotionalViewModel: DevotionalViewModel = viewModel(factory = factory)
    val fellowshipViewModel: FellowshipViewModel = viewModel(factory = factory)
    val livestreamViewModel: LivestreamViewModel = viewModel(factory = factory)
    val dmViewModel: DirectMessageViewModel = viewModel(factory = factory)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("I AM") },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val visibleTabs = remember(currentUser.role) {
                    bottomNavItems.filter { screen ->
                        if (screen == Screen.Admin) {
                            currentUser.role == "ADMIN"
                        } else {
                            true
                        }
                    }
                }

                visibleTabs.forEach { screen ->
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
    ) { innerPadding: PaddingValues ->
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
                SettingsScreen(
                    currentUser = currentUser,
                    authViewModel = authViewModel,
                    onNavigateToAdmin = { navController.navigate(Screen.Admin.route) }
                )
            }
            composable(Screen.Admin.route) {
                AdminDashboard(
                    devotionalViewModel = devotionalViewModel,
                    fellowshipViewModel = fellowshipViewModel,
                    livestreamViewModel = livestreamViewModel,
                    currentUser = currentUser
                )
            }
        }
    }
}
