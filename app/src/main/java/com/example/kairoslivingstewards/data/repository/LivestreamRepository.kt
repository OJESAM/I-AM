package com.example.kairoslivingstewards.data.repository

import com.example.kairoslivingstewards.data.local.dao.CommentDao
import com.example.kairoslivingstewards.data.local.dao.LivestreamSettingsDao
import com.example.kairoslivingstewards.data.local.dao.NoteDao
import com.example.kairoslivingstewards.data.local.entities.CommentEntity
import com.example.kairoslivingstewards.data.local.entities.LivestreamSettingsEntity
import com.example.kairoslivingstewards.data.local.entities.NoteEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LivestreamRepository(
    private val settingsDao: LivestreamSettingsDao,
    private val commentDao: CommentDao,
    private val noteDao: NoteDao
) {
    private val db = FirebaseFirestore.getInstance()

    fun getSettings(): Flow<LivestreamSettingsEntity?> = callbackFlow {
        val subscription = db.collection("settings").document("livestream")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val settings = snapshot.toObject(LivestreamSettingsEntity::class.java)
                    trySend(settings)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateSettings(settings: LivestreamSettingsEntity) {
        db.collection("settings").document("livestream").set(settings).await()
    }

    fun getComments(): Flow<List<CommentEntity>> = commentDao.getCommentsForTarget("livestream")

    suspend fun addComment(comment: CommentEntity) {
        commentDao.insertComment(comment)
    }

    fun getNotes(): Flow<List<NoteEntity>> = noteDao.getNotesForTarget("livestream")

    suspend fun addNote(content: String) {
        val note = NoteEntity(
            id = java.util.UUID.randomUUID().toString(),
            targetId = "livestream",
            content = content
        )
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }
}
