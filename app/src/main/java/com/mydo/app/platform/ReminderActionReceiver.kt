package com.mydo.app.platform

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.mydo.app.MydoApplication
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Reminder
import com.mydo.app.domain.model.ReminderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Handles the Complete and Snooze actions on a reminder notification (specs09-notifications.md,
 * "Reminder Notifications": "actions such as complete, snooze, or open"). Open is a plain
 * content `PendingIntent` posted directly by [ReminderAlarmReceiver], so it isn't routed
 * through here — only the two actions that need to run app logic before the notification
 * can be dismissed are.
 */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)?.let(UUID::fromString) ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)?.let(UUID::fromString) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, taskId.hashCode())

        val pendingResult = goAsync()
        val container = (context.applicationContext as MydoApplication).container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        container.completeTaskUseCase(taskId)
                    }
                    ACTION_SNOOZE -> {
                        val task = (container.taskRepository.getById(taskId) as? AppResult.Success)?.value
                        if (task != null && !task.completed) {
                            val snoozedUntil = container.timeProvider.nowUtcMillis() + SNOOZE_DURATION_MILLIS
                            val existing = (container.reminderRepository.getByTask(taskId) as? AppResult.Success)
                                ?.value?.firstOrNull { it.id == reminderId }
                            val snoozed = (existing ?: Reminder(
                                id = reminderId,
                                taskId = taskId,
                                triggerAtUtcMillis = snoozedUntil,
                                type = ReminderType.ABSOLUTE,
                                enabled = true,
                            )).copy(triggerAtUtcMillis = snoozedUntil, enabled = true)
                            if (existing != null) {
                                container.updateReminderUseCase(snoozed)
                            } else {
                                // Original reminder row is gone (e.g. deleted mid-flight) —
                                // still honor the snooze tap by arming a fresh one-off alarm.
                                container.notificationScheduler.schedule(snoozed, task)
                            }
                        }
                    }
                }
            } finally {
                NotificationManagerCompat.from(context).cancel(notificationId)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.mydo.app.action.REMINDER_COMPLETE"
        const val ACTION_SNOOZE = "com.mydo.app.action.REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val SNOOZE_DURATION_MILLIS = 10 * 60_000L

        fun completePendingIntent(context: Context, reminderId: UUID, taskId: UUID, notificationId: Int): PendingIntent =
            actionPendingIntent(context, ACTION_COMPLETE, reminderId, taskId, notificationId)

        fun snoozePendingIntent(context: Context, reminderId: UUID, taskId: UUID, notificationId: Int): PendingIntent =
            actionPendingIntent(context, ACTION_SNOOZE, reminderId, taskId, notificationId)

        private fun actionPendingIntent(context: Context, action: String, reminderId: UUID, taskId: UUID, notificationId: Int): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_REMINDER_ID, reminderId.toString())
                putExtra(EXTRA_TASK_ID, taskId.toString())
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            // request code mixes the action into the hash so Complete and Snooze for the
            // same reminder don't collide and overwrite each other's PendingIntent.
            val requestCode = (action to reminderId).hashCode()
            return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
