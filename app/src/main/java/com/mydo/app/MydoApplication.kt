package com.mydo.app

import android.app.Application
import com.mydo.app.di.AppContainer
import com.mydo.app.platform.NotificationChannels
import kotlinx.coroutines.launch

class MydoApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        // Exact alarms can be silently cleared by the OS outside of the boot/update cases
        // BootCompletedReceiver already covers (e.g. some OEM battery optimizers), so a
        // cheap best-effort re-sync on every cold start is a worthwhile extra safety net.
        container.applicationScope.launch {
            container.reminderAlarmCoordinator.rescheduleAll()
        }
    }
}
