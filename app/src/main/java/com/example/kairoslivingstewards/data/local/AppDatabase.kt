package com.example.kairoslivingstewards.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kairoslivingstewards.data.local.dao.*
import com.example.kairoslivingstewards.data.local.entities.*

@Database(
    entities = [
        DevotionalEntity::class,
        LeaderEntity::class,
        LivestreamSettingsEntity::class,
        CommentEntity::class,
        UserEntity::class,
        FellowshipEntity::class,
        FellowshipMemberEntity::class,
        FellowshipPostEntity::class,
        NoteEntity::class,
        DirectMessageEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun devotionalDao(): DevotionalDao
    abstract fun leaderDao(): LeaderDao
    abstract fun livestreamSettingsDao(): LivestreamSettingsDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun fellowshipDao(): FellowshipDao
    abstract fun noteDao(): NoteDao
    abstract fun directMessageDao(): DirectMessageDao
}
