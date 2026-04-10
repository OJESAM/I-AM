package com.example.kairoslivingstewards.data.repository

import com.example.kairoslivingstewards.data.local.dao.CommentDao
import com.example.kairoslivingstewards.data.local.dao.DevotionalDao
import com.example.kairoslivingstewards.data.local.entities.CommentEntity
import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DevotionalRepository(
    private val devotionalDao: DevotionalDao,
    private val commentDao: CommentDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

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
        val data = hashMapOf(
            "id" to devotional.id,
            "ownerId" to devotional.ownerId,
            "title" to devotional.title,
            "date" to devotional.date,
            "content" to devotional.content,
            "scripture" to devotional.scripture,
            "category" to devotional.category,
            "imageUrl" to devotional.imageUrl,
            "likesCount" to devotional.likesCount,
            "commentsEnabled" to devotional.commentsEnabled,
            "timestamp" to devotional.timestamp
        )
        functions
            .getHttpsCallable("saveDevotional")
            .call(data)
            .await()
    }

    suspend fun deleteDevotional(devotional: DevotionalEntity) {
        val data = hashMapOf(
            "id" to devotional.id
        )
        functions
            .getHttpsCallable("deleteDevotional")
            .call(data)
            .await()
    }

    fun getComments(devotionalId: String): Flow<List<CommentEntity>> {
        return commentDao.getCommentsForTarget(devotionalId)
    }

    suspend fun addComment(comment: CommentEntity) {
        commentDao.insertComment(comment)
        // Also sync comment to Firestore in a real app
    }
}
