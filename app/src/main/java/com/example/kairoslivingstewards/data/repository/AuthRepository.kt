package com.example.kairoslivingstewards.data.repository

import android.net.Uri
import com.example.kairoslivingstewards.data.local.dao.UserDao
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val userDao: UserDao) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    val loggedInUser: Flow<UserEntity?> = userDao.getLoggedInUser()

    suspend fun register(username: String, email: String, password: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // Send verification email
                firebaseUser.sendEmailVerification().await()
                
                val user = UserEntity(
                    id = firebaseUser.uid,
                    username = username,
                    contact = email,
                    isVerified = false // Match firestore rules (requires false on create)
                )
                db.collection("users").document(user.id).set(user).await()
                userDao.insertUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val doc = db.collection("users").document(firebaseUser.uid).get().await()
                val isEmailVerified = firebaseUser.isEmailVerified
                
                val user = if (doc.exists()) {
                    val firestoreUser = try {
                        doc.toObject(UserEntity::class.java)!!
                    } catch (e: Exception) {
                        // Fallback if toObject fails due to mapping issues
                        val data = doc.data ?: emptyMap()
                        UserEntity(
                            id = doc.id,
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
                    // Update verification status if it changed
                    if (firestoreUser.isVerified != isEmailVerified) {
                        val updatedUser = firestoreUser.copy(isVerified = isEmailVerified)
                        db.collection("users").document(firebaseUser.uid)
                            .update("isVerified", isEmailVerified).await()
                        updatedUser
                    } else {
                        firestoreUser
                    }
                } else {
                    UserEntity(
                        id = firebaseUser.uid,
                        username = firebaseUser.displayName ?: email.substringBefore("@"),
                        contact = email,
                        isVerified = isEmailVerified
                    )
                }
                userDao.insertUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun logout() {
        auth.signOut()
        userDao.clearUser()
    }

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateProfile(username: String, profileImageUrl: String): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            val updates = mapOf(
                "username" to username,
                "profileImageUrl" to profileImageUrl
            )
            db.collection("users").document(uid).update(updates).await()
            val user = userDao.getUserById(uid)
            if (user != null) {
                userDao.insertUser(user.copy(username = username, profileImageUrl = profileImageUrl))
            } else {
                // If user not in local DB, fetch and save
                val doc = db.collection("users").document(uid).get().await()
                val data = doc.data ?: emptyMap()
                val fetchedUser = UserEntity(
                    id = doc.id,
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
                userDao.insertUser(fetchedUser)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateFcmToken(token: String): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            db.collection("users").document(uid).update("fcmToken", token).await()
            val user = userDao.getUserById(uid)
            if (user != null) {
                userDao.insertUser(user.copy(fcmToken = token))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadProfileImage(uri: Uri): String? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val ref = storage.reference.child("profile_images/$uid.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
