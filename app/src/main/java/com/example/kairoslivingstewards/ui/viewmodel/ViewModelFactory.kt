package com.example.kairoslivingstewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kairoslivingstewards.KairosApplication
import com.example.kairoslivingstewards.data.repository.AuthRepository
import com.example.kairoslivingstewards.data.repository.DevotionalRepository
import com.example.kairoslivingstewards.data.repository.FellowshipRepository
import com.example.kairoslivingstewards.data.repository.LivestreamRepository

class ViewModelFactory(private val application: KairosApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = application.database
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(AuthRepository(db.userDao())) as T
            }
            modelClass.isAssignableFrom(DevotionalViewModel::class.java) -> {
                DevotionalViewModel(DevotionalRepository(db.devotionalDao(), db.commentDao())) as T
            }
            modelClass.isAssignableFrom(FellowshipViewModel::class.java) -> {
                FellowshipViewModel(application, FellowshipRepository(db.fellowshipDao())) as T
            }
            modelClass.isAssignableFrom(LivestreamViewModel::class.java) -> {
                LivestreamViewModel(LivestreamRepository(db.livestreamSettingsDao(), db.commentDao(), db.noteDao())) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
