package com.mydo.app.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mydo.app.MydoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android clears every exact alarm on reboot, and can also clear them across an app update
 * (AGENTS.md step 5: "reschedule reminders after task edits, recurrence completion, reboot,
 * and app update as required by Android"). This re-arms every enabled, future reminder from
 * the local database so nothing needs to be re-opened by the user for reminders to keep working.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as MydoApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.reminderAlarmCoordinator.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
