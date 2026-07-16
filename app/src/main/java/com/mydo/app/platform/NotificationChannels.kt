package com.mydo.app.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object NotificationChannels {
    const val REMINDERS = "reminders"
    const val SYSTEM = "system"
    const val DAILY_SUMMARY = "daily_summary"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminders = NotificationChannel(
                REMINDERS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Reminders for tasks with due dates"
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }

            val system = NotificationChannel(
                SYSTEM,
                "System Notifications",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Important app messages (import errors, etc.)"
                setShowBadge(true)
            }

            val daily = NotificationChannel(
                DAILY_SUMMARY,
                "Daily Summary",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Optional daily task summary"
                setShowBadge(false)
            }

            manager.createNotificationChannels(listOf(reminders, system, daily))
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}