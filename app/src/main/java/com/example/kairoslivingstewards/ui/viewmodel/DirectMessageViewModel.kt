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

    fun loadUsers() {
        viewModelScope.launch {
            _allUsers.value = repository.getAllUsers()
        }
    }

    fun loadRecentChats(currentUserId: String) {
        viewModelScope.launch {
            _recentUsers.value = repository.getRecentChatUsers(currentUserId)
        }
    }

    fun loadMessages(userId: String, otherUserId: String) {
        viewModelScope.launch {
            repository.getMessages(userId, otherUserId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(senderId: String, receiverId: String, content: String) {
        viewModelScope.launch {
            repository.sendMessage(senderId, receiverId, content)
        }
    }
}
