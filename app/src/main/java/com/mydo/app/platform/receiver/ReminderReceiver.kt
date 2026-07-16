package com.mydo.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mydo.app.platform.NotificationBuilder
import com.mydo.app.platform.NotificationPermissionHelper

/**
 * Fired by AlarmManager when a reminder is due. Checks permission again
 * (user may have revoked since scheduling) and builds the notification.
 */
class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderIdStr = intent.getStringExtra(EXTRA_REMINDER_ID)
        val taskIdStr = intent.getStringExtra(EXTRA_TASK_ID)
        if (reminderIdStr == null || taskIdStr == null) return

        // Double-check permission at fire time (user could have revoked)
        if (!NotificationPermissionHelper.isGranted(context)) {
            // Record missed reminder for later display
            MissedReminderRepository.recordMissed(context, reminderIdStr)
            return
        }

        NotificationBuilder.buildAndShow(context, reminderIdStr, taskIdStr)
    }
}