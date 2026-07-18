package com.mydo.app.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Centralizes MyDo's single local notification channel (specs09-notifications.md covers
 *  only reminder and system notices — there's no separate marketing/social channel to add). */
object NotificationChannels {
    const val REMINDERS_CHANNEL_ID = "reminders"

    /** Idempotent — safe to call on every app start and right before posting a notification. */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(REMINDERS_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            REMINDERS_CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Local reminders for tasks with a due date or a custom reminder time."
        }
        manager.createNotificationChannel(channel)
    }
}
