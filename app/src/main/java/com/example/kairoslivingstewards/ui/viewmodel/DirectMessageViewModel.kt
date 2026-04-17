package com.example.kairoslivingstewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kairoslivingstewards.data.local.entities.DirectMessageEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.data.repository.DirectMessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DirectMessageViewModel(private val repository: DirectMessageRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<DirectMessageEntity>>(emptyList())
    val messages: StateFlow<List<DirectMessageEntity>> = _messages.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val allUsers: StateFlow<List<UserEntity>> = _allUsers.asStateFlow()

    private val _recentUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val recentUsers: StateFlow<List<UserEntity>> = _recentUsers.asStateFlow()

    private val _recipientStatus = MutableStateFlow<UserEntity?>(null)
    val recipientStatus: StateFlow<UserEntity?> = _recipientStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun setOnlineStatus(userId: String, isOnline: Boolean) {
        viewModelScope.launch {
            try {
                repository.setUserOnlineStatus(userId, isOnline)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setTypingStatus(userId: String, typingTo: String?) {
        viewModelScope.launch {
            try {
                repository.setUserTypingStatus(userId, typingTo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun observeRecipientStatus(recipientId: String) {
        viewModelScope.launch {
            try {
                repository.getUserStatus(recipientId).collect {
                    _recipientStatus.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _allUsers.value = repository.getAllUsers()
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load users"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadRecentChats(currentUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _recentUsers.value = repository.getRecentChatUsers(currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load recent chats"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMessages(userId: String, otherUserId: String) {
        viewModelScope.launch {
            try {
                repository.getMessages(userId, otherUserId).collect {
                    _messages.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load messages"
            }
        }
    }

    fun sendMessage(senderId: String, receiverId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                repository.sendMessage(senderId, receiverId, content)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to send message"
            }
        }
    }
}
