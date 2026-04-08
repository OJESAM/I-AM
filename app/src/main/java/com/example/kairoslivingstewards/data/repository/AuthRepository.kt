package com.example.kairoslivingstewards.data.repository

import com.example.kairoslivingstewards.data.local.dao.UserDao
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class AuthRepository(private val userDao: UserDao) {
    private val auth = FirebaseAuth.getInstance()
    val loggedInUser: Flow<UserEntity?> = userDao.getLoggedInUser()

    suspend fun register(username: String, email: String, password: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = UserEntity(
                    id = firebaseUser.uid,
                    username = username,
                    contact = email,
                    isVerified = true // Firebase handles verification if needed via email
                )
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
                // In a real app, you would fetch user details from Firestore here
                val user = UserEntity(
                    id = firebaseUser.uid,
                    username = firebaseUser.displayName ?: email.substringBefore("@"),
                    contact = email,
                    isVerified = true
                )
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
}
