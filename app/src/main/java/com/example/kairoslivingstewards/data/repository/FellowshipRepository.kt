package com.example.kairoslivingstewards.data.repository

import com.example.kairoslivingstewards.data.local.dao.FellowshipDao
import com.example.kairoslivingstewards.data.local.entities.FellowshipEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipMemberEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipPostEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.data.model.FellowshipCell
import com.example.kairoslivingstewards.data.remote.CreateFellowshipRequest
import com.example.kairoslivingstewards.data.remote.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private val auth = FirebaseAuth.getInstance()

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
        try {
            val token = auth.currentUser?.getIdToken(true)?.await()?.token ?: throw Exception("Not authenticated")
            val request = CreateFellowshipRequest(name, description, leaderId)
            
            val response = RetrofitClient.instance.createFellowship("Bearer $token", request)
            
            if (!response.success) {
                throw Exception(response.message ?: "Failed to create fellowship via backend")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    suspend fun joinByInviteCode(userId: String, inviteCode: String): Boolean {
        return try {
            val snapshot = db.collection("fellowships")
                .whereEqualTo("inviteCode", inviteCode.uppercase())
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return false

            val fellowshipId = snapshot.documents[0].id

            // Check if already a member
            val memberSnapshot = db.collection("fellowship_members")
                .whereEqualTo("fellowshipId", fellowshipId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            if (!memberSnapshot.isEmpty) return true

            val member = FellowshipMemberEntity(
                id = UUID.randomUUID().toString(),
                fellowshipId = fellowshipId,
                userId = userId,
                role = "USER",
                joinedAt = System.currentTimeMillis()
            )
            db.collection("fellowship_members").document(member.id).set(member).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
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
        val snapshot = db.collection("fellowship_members")
            .whereEqualTo("fellowshipId", fellowshipId)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
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
            try {
                val fileRef = storage.reference.child("posts/${UUID.randomUUID()}")
                fileRef.putFile(android.net.Uri.parse(mediaUri)).await()
                mediaUrl = fileRef.downloadUrl.await().toString()
                mediaType = if (mediaUri.contains("video")) "video" else "image"
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            }
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
        fellowshipDao.insertPost(post)
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
