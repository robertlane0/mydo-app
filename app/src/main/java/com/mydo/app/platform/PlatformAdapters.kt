package com.mydo.app.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.mydo.app.domain.model.Reminder
import com.mydo.app.domain.model.Task
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Bridges Android's alarm and notification system for task reminders
 * (specs09-notifications.md, "Reminder Notifications"). MyDo never schedules a reminder
 * unless local notification permission is currently granted — there'd be nothing to show
 * when the alarm fired, and posting without permission throws on API 33+ — so every
 * implementation treats "no permission" as a silent no-op rather than an error the caller
 * has to handle. [com.mydo.app.domain.usecase.ReminderAlarmCoordinator] is what keeps this
 * in sync with the `reminders` table; this interface only ever does exactly what it's told.
 */
interface NotificationScheduler {
    /** Arms an OS alarm for [reminder] that, when due, notifies about [task]. No-op if
     *  notification permission isn't currently granted. */
    fun schedule(reminder: Reminder, task: Task)

    /** Cancels a previously armed alarm for [reminderId], if any. Always safe to call,
     *  including for a reminder that was never scheduled. */
    fun cancel(reminderId: UUID)

    /** True if MyDo currently has permission to post local notifications. */
    fun canPostNotifications(): Boolean

    /** True if exact-time alarms are currently available; when false, [schedule] still
     *  arms a reminder but Android may deliver it a little late rather than dropping it. */
    fun canScheduleExactAlarms(): Boolean
}

class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminder: Reminder, task: Task) {
        if (!canPostNotifications()) return
        val pendingIntent = reminderPendingIntent(reminder.id, task.id, task.title)
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtUtcMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtUtcMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Exact-alarm scheduling was revoked between the check above and this call
            // (the user can flip it off anytime in system settings) — degrade to inexact
            // delivery instead of losing the reminder entirely.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtUtcMillis, pendingIntent)
        }
    }

    override fun cancel(reminderId: UUID) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply { action = ReminderAlarmReceiver.ACTION_REMINDER_DUE }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun canPostNotifications(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun reminderPendingIntent(reminderId: UUID, taskId: UUID, taskTitle: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER_DUE
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId.toString())
            putExtra(ReminderAlarmReceiver.EXTRA_TASK_ID, taskId.toString())
            putExtra(ReminderAlarmReceiver.EXTRA_TASK_TITLE, taskTitle)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/**
 * Stable per-reminder [PendingIntent] request code. A UUID hash collision would only ever
 * make one reminder's alarm silently replace another's — never a crash — and is astronomically
 * unlikely for a single device's local reminder set, so this is a deliberate simplification
 * rather than a full collision-free ID scheme.
 */
internal fun reminderRequestCode(reminderId: UUID): Int = reminderId.hashCode()

/**
 * Bridges Android's system save/share chooser for manual backup export and import
 * (specs10-settings.md, "Data"). The picker launch itself happens in Compose with
 * `rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"))`
 * for export and `ActivityResultContracts.OpenDocument()` for import (a picker launch can
 * only happen from an Activity-scoped launcher); this port handles everything after a
 * `content://` URI comes back — MyDo never uploads a backup anywhere on its own.
 */
interface ShareGateway {
    /** Writes [content] to an already-chosen destination [uri] (from `CreateDocument`).
     *  Returns false on any write failure, leaving the destination's prior content, if any,
     *  as the only guarantee — callers must not assume a partial write didn't happen. */
    fun writeText(uri: Uri, content: String): Boolean

    /** Reads the full text of an already-chosen source [uri] (from `OpenDocument`), or null
     *  if it can't be opened or read. */
    fun readText(uri: Uri): String?

    /** Suggested filename for a fresh export, e.g. "mydo-backup-2026-07-16.json". */
    fun suggestedBackupFilename(exportedAtUtcMillis: Long): String
}

class AndroidShareGateway(private val context: Context) : ShareGateway {
    override fun writeText(uri: Uri, content: String): Boolean = try {
        val wrote = context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        }
        wrote != null
    } catch (_: Exception) {
        false
    }

    override fun readText(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (_: Exception) {
        null
    }

    override fun suggestedBackupFilename(exportedAtUtcMillis: Long): String {
        val date = Instant.ofEpochMilli(exportedAtUtcMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return "mydo-backup-$date.json"
    }
}
