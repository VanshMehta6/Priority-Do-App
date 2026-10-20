package com.example.vanshmehta.prioritydo.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.vanshmehta.prioritydo.MainActivity
import com.example.vanshmehta.prioritydo.R

/**
 * "Focus timer finished" notification. Posted only when a focus session the user started
 * reaches zero while the app process is alive. It is not a task reminder: nothing is scheduled
 * and no task deadline ever creates a notification.
 */
object TimerNotifier {
    private const val CHANNEL = "focus_timer"
    private const val NOTIFICATION_ID = 7001

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Focus timer", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Tells you when a focus session you started has finished" }
        )
    }

    @SuppressLint("MissingPermission")
    fun showDone(ctx: Context, taskTitle: String?) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        val open = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(ctx, 7002, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Focus session finished")
            .setContentText(taskTitle?.let { "Time is up for: $it" } ?: "Your focus session has finished")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, n)
    }
}
