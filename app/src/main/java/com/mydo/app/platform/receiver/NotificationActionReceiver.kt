package com.mydo.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.mydo.app.platform.AlarmScheduler
import com.mydo.app.platform.AndroidAlarmScheduler
import com.mydo.app.platform.NotificationBuilder
import com.mydo.app.platform.NotificationPermissionHelper
import java.util.UUID

/**
 * Handles notification action buttons: Open, Complete, Snooze.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_OPEN = "com.mydo.app.action.OPEN_TASK"
        const val ACTION_COMPLETE = "com.mydo.app.action.COMPLETE_TASK"
        const val ACTION_SNOOZE = "com.mydo.app.action.SNOOZE"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderIdStr = intent.getStringExtra(EXTRA_REMINDER_ID)
        val taskIdStr = intent.getStringExtra(EXTRA_TASK_ID)
        if (reminderIdStr == null || taskIdStr == null) return

        val reminderId = UUID.fromString(reminderIdStr)
        val taskId = UUID.fromString(taskIdStr)
        val notificationManager = NotificationManagerCompat.from(context)

        when (action) {
            ACTION_OPEN -> {
                notificationManager.cancel(reminderId.hashCode())
                val openIntent = Intent(context, com.mydo.app.ui.taskdetail.TaskDetailActivity::class.java).apply {
                    putExtra("task_id", taskIdStr)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(openIntent)
            }

            ACTION_COMPLETE -> {
                // Complete task via domain use case (requires DI context)
                // For now, use a simplified approach - send to a WorkManager or use
                // a service that has DI access. Here we'll just dismiss and show toast.
                // Full implementation in a DI-enabled worker/service would be better.
                notificationManager.cancel(reminderId.hashCode())
                
                // Schedule a work to complete the task
                CompleteTaskWorker.enqueue(context, taskIdStr)
                
                Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT).show()
            }

            ACTION_SNOOZE -> {
                val snoozeMinutes = NotificationPermissionHelper.getSnoozeMinutes(context)
                val newTrigger = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
                
                // Update the alarm
                val alarmScheduler = AndroidAlarmScheduler(context)
                val reminder = com.mydo.app.domain.model.Reminder(
                    id = reminderId,
                    taskId = taskId,
                    triggerAtUtcMillis = newTrigger,
                    type = com.mydo.app.domain.model.ReminderType.ABSOLUTE,
                    enabled = true
                )
                alarmScheduler.schedule(reminder)
                
                // Update notification to show snoozed state
                NotificationBuilder.updateForSnooze(context, reminderIdStr, newTrigger)
                
                Toast.makeText(context, "Snoozed $snoozeMinutes min", Toast.LENGTH_SHORT).show()
            }
        }
    }
}