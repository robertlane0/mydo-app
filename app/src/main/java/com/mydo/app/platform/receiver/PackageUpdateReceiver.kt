package com.mydo.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Handles MY_PACKAGE_REPLACED (app update). Triggers reminder rescheduling
 * via WorkManager so DB migrations run first if needed.
 */
class PackageUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
            )
        }
    }
}