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
import kotlin.collections.emptyList

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
                    
                    // Mark messages as read when they are received by the other user
                    messages.filter { it.receiverId == userId && !it.isRead }.forEach { msg ->
                        markMessageAsRead(msg.id)
                    }
                    
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
            timestamp = System.currentTimeMillis(),
            isDelivered = false,
            isRead = false
        )
        db.collection("direct_messages").document(message.id).set(message).await()
        // Mark as delivered immediately after successful Firestore write (simplified)
        db.collection("direct_messages").document(message.id).update("isDelivered", true).await()
    }

    private fun markMessageAsRead(messageId: String) {
        db.collection("direct_messages").document(messageId).update("isRead", true)
    }

    fun getUserStatus(userId: String): Flow<UserEntity?> = callbackFlow {
        val subscription = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val user = try {
                        snapshot.toObject(UserEntity::class.java)
                    } catch (e: Exception) {
                        val data = snapshot.data ?: emptyMap()
                        UserEntity(
                            id = snapshot.id,
                            username = data["username"] as? String ?: "",
                            contact = data["contact"] as? String ?: "",
                            profileImageUrl = data["profileImageUrl"] as? String,
                            isVerified = data["isVerified"] as? Boolean ?: false,
                            role = data["role"] as? String ?: "USER",
                            isOnline = data["isOnline"] as? Boolean ?: false,
                            lastSeen = (data["lastSeen"] as? Long) ?: System.currentTimeMillis(),
                            typingTo = data["typingTo"] as? String,
                            fcmToken = data["fcmToken"] as? String
                        )
                    }
                    trySend(user)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun setUserOnlineStatus(userId: String, isOnline: Boolean) {
        db.collection("users").document(userId).update(
            "isOnline", isOnline,
            "lastSeen", System.currentTimeMillis()
        ).await()
    }

    suspend fun setUserTypingStatus(userId: String, typingTo: String?) {
        db.collection("users").document(userId).update("typingTo", typingTo).await()
    }

    suspend fun getAllUsers(): List<UserEntity> {
        return db.collection("users").get().await().toObjects(UserEntity::class.java)
    }

    suspend fun getRecentChatUsers(currentUserId: String): List<UserEntity> {
        return try {
            val sentMessages = db.collection("direct_messages")
                .whereEqualTo("senderId", currentUserId)
                .get().await().toObjects(DirectMessageEntity::class.java)
            
            val receivedMessages = db.collection("direct_messages")
                .whereEqualTo("receiverId", currentUserId)
                .get().await().toObjects(DirectMessageEntity::class.java)

            val otherUserIds = (sentMessages.map { it.receiverId } + receivedMessages.map { it.senderId })
                .distinct()
                .filter { it != currentUserId }

            if (otherUserIds.isEmpty()) return emptyList()

            // Firestore 'whereIn' is limited to 10 items.
            // For a larger set, you would need to chunk this or use a different strategy.
            val limitedIds = otherUserIds.take(10)

            db.collection("users")
                .whereIn("id", limitedIds)
                .get().await().toObjects(UserEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
