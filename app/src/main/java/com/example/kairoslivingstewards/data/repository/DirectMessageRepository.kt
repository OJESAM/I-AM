package com.example.kairoslivingstewards.data.repository

import com.example.kairoslivingstewards.data.local.dao.DirectMessageDao
import com.example.kairoslivingstewards.data.local.entities.DirectMessageEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DirectMessageRepository(private val directMessageDao: DirectMessageDao) {
    private val db = FirebaseFirestore.getInstance()

    fun getMessages(userId: String, otherUserId: String): Flow<List<DirectMessageEntity>> = callbackFlow {
        val subscription = db.collection("direct_messages")
            .whereIn("senderId", listOf(userId, otherUserId))
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.toObjects(DirectMessageEntity::class.java)
                        .filter { 
                            (it.senderId == userId && it.receiverId == otherUserId) || 
                            (it.senderId == otherUserId && it.receiverId == userId) 
                        }
                        .sortedBy { it.timestamp }
                    trySend(messages)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(senderId: String, receiverId: String, content: String) {
        val message = DirectMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        db.collection("direct_messages").document(message.id).set(message).await()
    }

    suspend fun getAllUsers(): List<UserEntity> {
        return db.collection("users").get().await().toObjects(UserEntity::class.java)
    }
}
