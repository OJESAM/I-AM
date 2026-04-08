package com.example.kairoslivingstewards.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kairoslivingstewards.data.local.entities.FellowshipEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipMemberEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipPostEntity
import com.example.kairoslivingstewards.data.repository.FellowshipRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FellowshipViewModel(
    application: Application,
    private val repository: FellowshipRepository
) : AndroidViewModel(application) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val allFellowships: StateFlow<List<FellowshipEntity>> = repository.getAllFellowships()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPosts: StateFlow<List<FellowshipPostEntity>> = repository.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentFellowshipPosts = MutableStateFlow<List<FellowshipPostEntity>>(emptyList())
    val currentFellowshipPosts: StateFlow<List<FellowshipPostEntity>> = _currentFellowshipPosts.asStateFlow()

    private val _currentFellowshipMembers = MutableStateFlow<List<Pair<FellowshipMemberEntity, String>>>(emptyList())
    val currentFellowshipMembers: StateFlow<List<Pair<FellowshipMemberEntity, String>>> = _currentFellowshipMembers.asStateFlow()

    fun loadPosts(fellowshipId: String) {
        viewModelScope.launch {
            repository.getPosts(fellowshipId).collect {
                _currentFellowshipPosts.value = it
            }
        }
    }

    fun loadMembers(fellowshipId: String) {
        viewModelScope.launch {
            repository.getMembers(fellowshipId).collect { members ->
                val userIds = members.map { it.userId }.distinct()
                val usernames = repository.getMemberUserNames(userIds)
                _currentFellowshipMembers.value = members.map { it to (usernames[it.userId] ?: "Unknown User") }
            }
        }
    }

    fun removeMember(fellowshipId: String, userId: String) {
        viewModelScope.launch {
            repository.removeMember(fellowshipId, userId)
        }
    }

    fun createFellowship(name: String, description: String, userId: String) {
        viewModelScope.launch {
            repository.createFellowship(name, description, userId)
        }
    }

    fun joinFellowship(userId: String, inviteCode: String) {
        viewModelScope.launch {
            repository.joinByInviteCode(userId, inviteCode)
        }
    }

    fun postToFellowship(fellowshipId: String, userId: String, userName: String, content: String, mediaUrl: String? = null) {
        viewModelScope.launch {
            repository.createPost(fellowshipId, userId, userName, content, mediaUrl)
        }
    }

    fun deletePost(post: FellowshipPostEntity) {
        viewModelScope.launch {
            repository.deletePost(post)
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation() {
        _isLoading.value = true
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                _userLocation.value = location
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
