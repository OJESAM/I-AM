package com.example.kairoslivingstewards

import android.app.Application
import androidx.room.Room
import com.example.kairoslivingstewards.data.local.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class KairosApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = Firebase.appCheck
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        database = Room.databaseBuilder(
                this,
                AppDatabase::class.java,
                "kairos_database"
            ).fallbackToDestructiveMigration(true)
            .build()
    }
}
