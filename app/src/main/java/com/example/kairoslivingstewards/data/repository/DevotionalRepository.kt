package com.example.kairoslivingstewards.data.repository

import android.net.Uri
import com.example.kairoslivingstewards.data.local.dao.CommentDao
import com.example.kairoslivingstewards.data.local.dao.DevotionalDao
import com.example.kairoslivingstewards.data.local.entities.CommentEntity
import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import com.example.kairoslivingstewards.data.remote.DeleteRequest
import com.example.kairoslivingstewards.data.remote.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DevotionalRepository(
    private val devotionalDao: DevotionalDao,
    private val commentDao: CommentDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getDevotionals(category: String? = null): Flow<List<DevotionalEntity>> {
        return (if (category == null || category == "All") {
            devotionalDao.getAllDevotionals()
        } else {
            devotionalDao.getDevotionalsByCategory(category)
        }).onStart {
            syncDevotionals()
        }
    }

    private suspend fun syncDevotionals() {
        try {
            val snapshot = db.collection("devotionals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val devotionals = snapshot.toObjects(DevotionalEntity::class.java)
            devotionalDao.insertDevotionals(devotionals)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            // Handle offline state
        }
    }

    suspend fun getDevotionalById(id: String): DevotionalEntity? {
        return devotionalDao.getDevotionalById(id)
    }

    suspend fun saveDevotional(devotional: DevotionalEntity) {
        try {
            var finalDevotional = devotional
            
            // If imageUrl is a local Uri, upload it first
            if (devotional.imageUrl?.startsWith("content://") == true || devotional.imageUrl?.startsWith("file://") == true) {
                try {
                    val fileRef = storage.reference.child("devotionals/${UUID.randomUUID()}")
                    fileRef.putFile(Uri.parse(devotional.imageUrl)).await()
                    val downloadUrl = fileRef.downloadUrl.await().toString()
                    finalDevotional = devotional.copy(imageUrl = downloadUrl)
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
            
            val token = auth.currentUser?.getIdToken(true)?.await()?.token ?: throw Exception("Not authenticated")
            val response = RetrofitClient.instance.saveDevotional("Bearer $token", finalDevotional)
            
            if (!response.success) {
                throw Exception(response.message ?: "Failed to save devotional via backend")
            }

            devotionalDao.insertDevotionals(listOf(finalDevotional))
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e // Re-throw to handle in ViewModel if needed
        }
    }

    suspend fun deleteDevotional(devotional: DevotionalEntity) {
        try {
            val token = auth.currentUser?.getIdToken(true)?.await()?.token ?: throw Exception("Not authenticated")
            val response = RetrofitClient.instance.deleteDevotional("Bearer $token", DeleteRequest(devotional.id))
            
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete devotional via backend")
            }
            devotionalDao.deleteDevotional(devotional)
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun getComments(devotionalId: String): Flow<List<CommentEntity>> {
        return commentDao.getCommentsForTarget(devotionalId)
    }

    suspend fun addComment(comment: CommentEntity) {
        commentDao.insertComment(comment)
        // Also sync comment to Firestore in a real app
    }

    suspend fun updateLikes(devotionalId: String, newCount: Int) {
        try {
            db.collection("devotionals").document(devotionalId)
                .update("likesCount", newCount).await()
            val devotional = devotionalDao.getDevotionalById(devotionalId)
            if (devotional != null) {
                devotionalDao.insertDevotionals(listOf(devotional.copy(likesCount = newCount)))
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}
