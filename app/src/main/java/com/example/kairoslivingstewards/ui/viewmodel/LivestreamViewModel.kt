package com.example.kairoslivingstewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kairoslivingstewards.data.local.entities.CommentEntity
import com.example.kairoslivingstewards.data.local.entities.LivestreamSettingsEntity
import com.example.kairoslivingstewards.data.local.entities.NoteEntity
import com.example.kairoslivingstewards.data.repository.LivestreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LivestreamViewModel(
    private val repository: LivestreamRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<LivestreamSettingsEntity?>(null)
    val settings: StateFlow<LivestreamSettingsEntity?> = _settings.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentEntity>>(emptyList())
    val comments: StateFlow<List<CommentEntity>> = _comments.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> = repository.getNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getSettings().collectLatest {
                _settings.value = it
            }
        }
        viewModelScope.launch {
            repository.getComments().collectLatest {
                _comments.value = it
            }
        }
    }

    fun addComment(userId: String, userName: String, text: String) {
        viewModelScope.launch {
            val comment = CommentEntity(
                targetId = "livestream",
                userId = userId,
                userName = userName,
                text = text
            )
            repository.addComment(comment)
        }
    }

    fun updateSettings(videoId: String, commentsEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(
                LivestreamSettingsEntity(
                    youtubeVideoId = videoId,
                    commentsEnabled = commentsEnabled
                )
            )
        }
    }

    fun addNote(content: String) {
        viewModelScope.launch {
            repository.addNote(content)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun deleteComment(comment: CommentEntity) {
        viewModelScope.launch {
            repository.deleteComment(comment)
        }
    }
}
