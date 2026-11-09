package com.studyit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StuditApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Studit activity",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Answers, replies, upvotes and accepted answers"
        }
        manager?.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "studit_default"
    }
}
