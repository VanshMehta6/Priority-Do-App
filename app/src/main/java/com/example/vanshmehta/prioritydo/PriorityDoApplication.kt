package com.example.vanshmehta.prioritydo

import android.app.Application
import android.util.Log
import com.example.vanshmehta.prioritydo.util.TimerNotifier
import com.google.firebase.database.FirebaseDatabase

class PriorityDoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must run before any other Realtime Database call so cached data works offline.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("PriorityDoApplication", "Persistence already configured: ${e.message}")
        }
        TimerNotifier.createChannel(this)
    }
}
