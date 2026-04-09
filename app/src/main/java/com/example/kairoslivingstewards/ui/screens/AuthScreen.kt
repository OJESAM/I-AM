package com.example.kairoslivingstewards.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.kairoslivingstewards.ui.viewmodel.AuthState
import com.example.kairoslivingstewards.ui.viewmodel.AuthViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsState()
    var isLoginMode by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (authState) {
            is AuthState.Unauthenticated, is AuthState.Idle -> {
                if (isLoginMode) {
                    LoginScreen(
                        onLogin = viewModel::login,
                        onSwitchToRegister = { isLoginMode = false },
                        onForgotPassword = { showResetDialog = true }
                    )
                } else {
                    RegistrationScreen(
                        onRegister = viewModel::register,
                        onSwitchToLogin = { isLoginMode = true }
                    )
                }
            }
            is AuthState.Loading -> CircularProgressIndicator()
            is AuthState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${(authState as AuthState.Error).message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.logout() }) { Text("Retry") }
                }
            }
            else -> {} // Authenticated state handled in MainActivity
        }
    }

    if (showResetDialog) {
        ForgotPasswordDialog(
            onDismiss = { showResetDialog = false },
            onReset = { email ->
                viewModel.resetPassword(email)
                showResetDialog = false
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, onReset: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your email to receive a password reset link.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onReset(email) }, enabled = email.isNotBlank()) {
                Text("Send Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onSwitchToRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Login", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.length >= 6
        ) {
            Text("Login")
        }
        
        TextButton(onClick = onForgotPassword) {
            Text("Forgot Password?")
        }

        TextButton(onClick = onSwitchToRegister) {
            Text("Don't have an account? Register")
        }
    }
}

@Composable
fun RegistrationScreen(onRegister: (String, String, String) -> Unit, onSwitchToLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 6 chars)") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Button(
            onClick = { onRegister(username, email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && email.isNotBlank() && password.length >= 6
        ) {
            Text("Register")
        }
        TextButton(onClick = onSwitchToLogin) {
            Text("Already have an account? Login")
        }
    }
}
