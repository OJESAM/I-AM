package com.example.kairoslivingstewards.data.repository

import com.example.kairoslivingstewards.data.local.dao.FellowshipDao
import com.example.kairoslivingstewards.data.local.entities.FellowshipEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipMemberEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipPostEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.data.model.FellowshipCell
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FellowshipRepository(
    private val fellowshipDao: FellowshipDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    fun getAllFellowships(): Flow<List<FellowshipEntity>> = callbackFlow {
        val subscription = db.collection("fellowships").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val fellowships = snapshot.toObjects(FellowshipEntity::class.java)
                trySend(fellowships)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun createFellowship(name: String, description: String, leaderId: String) {
        val data = hashMapOf(
            "name" to name,
            "description" to description,
            "leaderId" to leaderId
        )
        functions
            .getHttpsCallable("createFellowship")
            .call(data)
            .await()
    }

    suspend fun joinByInviteCode(userId: String, inviteCode: String): Boolean {
        val data = hashMapOf(
            "userId" to userId,
            "inviteCode" to inviteCode
        )
        val result = functions
            .getHttpsCallable("joinFellowship")
            .call(data)
            .await()
        
        // The Cloud Function should return { success: true/false } or similar
        val resultData = result.data as? Map<*, *>
        return resultData?.get("success") as? Boolean ?: false
    }

    fun getPosts(fellowshipId: String): Flow<List<FellowshipPostEntity>> = callbackFlow {
        val subscription = db.collection("fellowship_posts")
            .whereEqualTo("fellowshipId", fellowshipId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val posts = snapshot.toObjects(FellowshipPostEntity::class.java)
                        .sortedByDescending { it.timestamp }
                    trySend(posts)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getAllPosts(): Flow<List<FellowshipPostEntity>> = callbackFlow {
        val subscription = db.collection("fellowship_posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val posts = snapshot.toObjects(FellowshipPostEntity::class.java)
                    trySend(posts)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getMembers(fellowshipId: String): Flow<List<FellowshipMemberEntity>> = callbackFlow {
        val subscription = db.collection("fellowship_members")
            .whereEqualTo("fellowshipId", fellowshipId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val members = snapshot.toObjects(FellowshipMemberEntity::class.java)
                    trySend(members)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getMemberUserNames(userIds: List<String>): Map<String, String> {
        if (userIds.isEmpty()) return emptyMap()
        val users = db.collection("users").whereIn("id", userIds).get().await()
        return users.documents.associate { 
            it.getString("id")!! to (it.getString("username") ?: "Unknown User")
        }
    }

    suspend fun removeMember(fellowshipId: String, userId: String) {
        val data = hashMapOf(
            "fellowshipId" to fellowshipId,
            "userId" to userId
        )
        functions
            .getHttpsCallable("removeMember")
            .call(data)
            .await()
    }

    suspend fun createPost(
        fellowshipId: String, 
        userId: String, 
        userName: String, 
        content: String, 
        mediaUri: String? = null
    ) {
        var mediaUrl: String? = null
        var mediaType: String? = null

        if (mediaUri != null) {
            val fileRef = storage.reference.child("posts/${UUID.randomUUID()}")
            fileRef.putFile(android.net.Uri.parse(mediaUri)).await()
            mediaUrl = fileRef.downloadUrl.await().toString()
            mediaType = if (mediaUri.contains("video")) "video" else "image"
        }

        val post = FellowshipPostEntity(
            id = UUID.randomUUID().toString(),
            fellowshipId = fellowshipId,
            userId = userId,
            userName = userName,
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis()
        )
        db.collection("fellowship_posts").document(post.id).set(post).await()
    }

    suspend fun deletePost(post: FellowshipPostEntity) {
        db.collection("fellowship_posts").document(post.id).delete().await()
    }

    fun getAllUsers(): Flow<List<UserEntity>> = callbackFlow {
        val subscription = db.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val users = snapshot.toObjects(UserEntity::class.java)
                trySend(users)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun updateUserRole(userId: String, role: String) {
        db.collection("users").document(userId).update("role", role).await()
    }

    fun saveLeader(cell: FellowshipCell) {}
}
