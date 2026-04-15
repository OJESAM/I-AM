package com.example.kairoslivingstewards.data.remote

import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import com.example.kairoslivingstewards.data.local.entities.LivestreamSettingsEntity
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

    @POST("updateLivestreamSettings")
    suspend fun updateLivestreamSettings(
        @Header("Authorization") token: String,
        @Body settings: LivestreamSettingsEntity
    ): BackendResponse

    @POST("deleteDevotional")
    suspend fun deleteDevotional(
        @Header("Authorization") token: String,
        @Body request: DeleteRequest
    ): BackendResponse

    @POST("updateUserRole")
    suspend fun updateUserRole(
        @Header("Authorization") token: String,
        @Body request: UpdateRoleRequest
    ): BackendResponse
}

data class DeleteRequest(val id: String)
data class UpdateRoleRequest(val userId: String, val role: String)
