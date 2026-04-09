package com.example.kairoslivingstewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserEntity) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    val loggedInUser: StateFlow<UserEntity?> = repository.loggedInUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        viewModelScope.launch {
            repository.loggedInUser.collectLatest { user ->
                _authState.value = if (user == null) {
                    AuthState.Unauthenticated
                } else {
                    AuthState.Authenticated(user)
                }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val success = repository.register(username, email, password)
            if (!success) {
                _authState.value = AuthState.Error("Registration failed")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val success = repository.login(email, password)
            if (!success) {
                _authState.value = AuthState.Error("Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val success = repository.resetPassword(email)
            if (success) {
                _authState.value = AuthState.Idle // Or a specific success state
            } else {
                _authState.value = AuthState.Error("Password reset failed")
            }
        }
    }

    fun updateProfile(username: String, profileImageUrl: String) {
        viewModelScope.launch {
            repository.updateProfile(username, profileImageUrl)
        }
    }
}
