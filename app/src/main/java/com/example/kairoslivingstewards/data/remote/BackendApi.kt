package com.example.kairoslivingstewards.data.remote

import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class CreateFellowshipRequest(
    val name: String,
    val description: String,
    val leaderId: String
)

data class BackendResponse(
    val success: Boolean,
    val id: String? = null,
    val message: String? = null
)

interface BackendApi {
    @POST("createFellowship")
    suspend fun createFellowship(
        @Header("Authorization") token: String,
        @Body request: CreateFellowshipRequest
    ): BackendResponse

    @POST("saveDevotional")
    suspend fun saveDevotional(
        @Header("Authorization") token: String,
        @Body devotional: DevotionalEntity
    ): BackendResponse
}
