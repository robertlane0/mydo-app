package com.mydo.app.platform

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mydo.app.MainActivity
import com.mydo.app.MydoApplication
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Notification
import com.mydo.app.domain.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Fires when a reminder's [android.app.AlarmManager] alarm comes due
 * (specs09-notifications.md, "Reminder Notifications"). Posts the system notification with
 * Open/Complete/Snooze actions and records a local [Notification] row so the reminder also
 * shows up in the in-app Notifications history, per specs09's "Notification data is included
 * in manual database exports when stored by the app."
 *
 * The task is re-read from the database rather than trusted from the alarm's extras, since
 * the task may have been completed, retitled, or deleted in the time between scheduling and
 * firing — extras only exist as a fallback label if that lookup itself fails.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_DUE) return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)?.let(UUID::fromString) ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)?.let(UUID::fromString) ?: return
        val fallbackTitle = intent.getStringExtra(EXTRA_TASK_TITLE)?.takeIf { it.isNotBlank() } ?: "Task reminder"

        val pendingResult = goAsync()
        val container = (context.applicationContext as MydoApplication).container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = (container.taskRepository.getById(taskId) as? AppResult.Success)?.value
                // Task finished or was removed since the alarm was armed — DeleteTaskUseCase
                // and CompleteTaskUseCase already try to cancel this alarm proactively, but an
                // in-flight alarm can still slip through, so this is the last line of defense.
                if (task == null || task.completed) return@launch

                val title = task.title.ifBlank { fallbackTitle }
                postNotification(context, reminderId, taskId, title)

                container.notificationRepository.create(
                    Notification(
                        id = UUID.randomUUID(),
                        type = NotificationType.REMINDER,
                        taskId = taskId,
                        title = title,
                        read = false,
                        createdAtUtcMillis = container.timeProvider.nowUtcMillis(),
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled() below
    private fun postNotification(context: Context, reminderId: UUID, taskId: UUID, taskTitle: String) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        NotificationChannels.ensureCreated(context)

        val notificationId = taskId.hashCode()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TASK_ID, taskId.toString())
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderRequestCode(reminderId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(taskTitle)
            .setContentText("Reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Complete", ReminderActionReceiver.completePendingIntent(context, reminderId, taskId, notificationId))
            .addAction(0, "Snooze 10m", ReminderActionReceiver.snoozePendingIntent(context, reminderId, taskId, notificationId))
            .build()

        notifications.notify(notificationId, notification)
    }

    companion object {
        const val ACTION_REMINDER_DUE = "com.mydo.app.action.REMINDER_DUE"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"
    }
}
