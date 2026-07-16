package com.mydo.app.platform

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Reminder
import com.mydo.app.platform.receiver.NotificationActionReceiver
import com.mydo.app.platform.receiver.ReminderReceiver
import java.util.UUID

object NotificationBuilder {

    private const val CHANNEL_REMINDERS = "reminders"
    private const val CHANNEL_SYSTEM = "system"

    fun buildAndShow(context: Context, reminderId: String, taskId: String) {
        // We need task details to build a rich notification
        // In a real implementation, we'd fetch from DB. For now, build basic.
        val manager = NotificationManagerCompat.from(context)

        val openIntent = Intent(context, com.mydo.app.ui.taskdetail.TaskDetailActivity::class.java).apply {
            putExtra("task_id", taskId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val completeIntent = Intent(NotificationActionReceiver.ACTION_COMPLETE).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            setPackage(context.packageName)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(NotificationActionReceiver.ACTION_SNOOZE).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            setPackage(context.packageName)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setContentTitle("Task Reminder")
            .setContentText("Tap to view task")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_view,
                    "Open",
                    openPendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Complete",
                    completePendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_lock_silent_mode,
                    "Snooze",
                    snoozePendingIntent
                ).build()
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        manager.notify(reminderId.hashCode(), notification)
    }

    fun buildRich(
        context: Context,
        reminderId: String,
        taskTitle: String,
        dueAtUtcMillis: Long?,
        projectName: String?,
        priority: Priority,
    ) {
        val manager = NotificationManagerCompat.from(context)

        val openIntent = Intent(context, com.mydo.app.ui.taskdetail.TaskDetailActivity::class.java).apply {
            putExtra("task_id", UUID.fromString(reminderId).toString()) // TODO: pass actual taskId
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val completeIntent = Intent(NotificationActionReceiver.ACTION_COMPLETE).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, UUID.fromString(reminderId).toString()) // TODO: pass actual taskId
            setPackage(context.packageName)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(NotificationActionReceiver.ACTION_SNOOZE).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, UUID.fromString(reminderId).toString()) // TODO: pass actual taskId
            setPackage(context.packageName)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val color = when (priority) {
            Priority.P1 -> Color.parseColor("#D1453B") // Red
            Priority.P2 -> Color.parseColor("#EB8909") // Orange
            Priority.P3 -> Color.parseColor("#246FE0") // Blue
            Priority.P4 -> Color.GRAY
        }

        val dueText = dueAtUtcMillis?.let { formatDueTime(context, it) } ?: "No due time"
        val projectText = projectName?.let { " · $it" } ?: ""

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setContentTitle(taskTitle)
            .setContentText("$dueText$projectText")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setColor(color)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_lock_silent_mode, "Snooze", snoozePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        manager.notify(reminderId.hashCode(), notification)
    }

    fun updateForSnooze(context: Context, reminderId: String, newTriggerMillis: Long) {
        // Re-build notification with "Snoozed until..." text
        // For simplicity, we'll just update the notification with a new text
        val manager = NotificationManagerCompat.from(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setContentTitle("Snoozed")
            .setContentText("Will remind again at ${formatDueTime(context, newTriggerMillis)}")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(reminderId.hashCode(), notification)
    }

    private fun formatDueTime(context: Context, utcMillis: Long): String {
        return android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(utcMillis))
    }

    fun buildSystemNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val manager = NotificationManagerCompat.from(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }
}