package com.mydo.app.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mydo.app.domain.model.Reminder
import com.mydo.app.platform.receiver.ReminderReceiver
import java.util.UUID

interface AlarmScheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: UUID)
    fun cancelAll()
}

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminder: Reminder) {
        val triggerMillis = reminder.triggerAtUtcMillis
        val pendingIntent = createPendingIntent(reminder.id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(reminderId: UUID) {
        val pendingIntent = createPendingIntent(reminderId)
        alarmManager.cancel(pendingIntent)
    }

    override fun cancelAll() {
        // We'd need to track all pending intents to cancel them all
        // This is a simplified implementation
    }

    private fun createPendingIntent(reminderId: UUID): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId.toString())
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, reminderId.hashCode(), intent, flags)
    }
}