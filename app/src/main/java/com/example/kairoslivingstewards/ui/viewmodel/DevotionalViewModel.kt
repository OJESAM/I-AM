package com.example.kairoslivingstewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kairoslivingstewards.data.local.entities.CommentEntity
import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import com.example.kairoslivingstewards.data.repository.DevotionalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class DevotionalViewModel(
    private val repository: DevotionalRepository
) : ViewModel() {

    private val _devotionals = MutableStateFlow<List<DevotionalEntity>>(emptyList())
    val devotionals: StateFlow<List<DevotionalEntity>> = _devotionals.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        loadDevotionals()
    }

    private fun loadDevotionals() {
        viewModelScope.launch {
            repository.getDevotionals(_selectedCategory.value).collect {
                _devotionals.value = it
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        loadDevotionals()
    }

    fun getDevotional(id: String): DevotionalEntity? {
        return _devotionals.value.find { it.id == id }
    }

    fun addDevotional(title: String, content: String, scripture: String, category: String, ownerId: String, imageUrl: String? = null) {
        viewModelScope.launch {
            val devotional = DevotionalEntity(
                id = UUID.randomUUID().toString(),
                ownerId = ownerId,
                title = title,
                content = content,
                scripture = scripture,
                category = category,
                imageUrl = imageUrl,
                date = "Today",
                timestamp = System.currentTimeMillis()
            )
            repository.saveDevotional(devotional)
        }
    }

    fun deleteDevotional(devotional: DevotionalEntity) {
        viewModelScope.launch {
            repository.deleteDevotional(devotional)
        }
    }

    fun getCommentsForDevotional(devotionalId: String): Flow<List<CommentEntity>> {
        return repository.getComments(devotionalId)
    }

    fun addComment(devotionalId: String, userName: String, text: String) {
        viewModelScope.launch {
            val comment = CommentEntity(
                targetId = devotionalId,
                userName = userName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            repository.addComment(comment)
        }
    }

    fun updateLikes(devotionalId: String, newCount: Int) {
        viewModelScope.launch {
            repository.updateLikes(devotionalId, newCount)
        }
    }
}
