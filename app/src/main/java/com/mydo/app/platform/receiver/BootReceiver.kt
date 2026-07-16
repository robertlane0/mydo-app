package com.mydo.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Handles BOOT_COMPLETED, QUICKBOOT_POWERON, TIMEZONE_CHANGED, LOCALE_CHANGED.
 * Delegates the actual rescheduling to [RescheduleRemindersWorker] via WorkManager
 * so we have DB access and don't block the broadcast thread.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_QUICKBOOT_POWERON -> {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<RescheduleRemindersWorker>()
                        .setInitialDelay(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                )
            }
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
                )
            }
        }
    }
}