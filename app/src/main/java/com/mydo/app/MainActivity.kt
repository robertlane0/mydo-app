package com.mydo.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.ThemeMode
import com.mydo.app.ui.app.MydoApp
import com.mydo.app.ui.components.TaskComposerViewModel
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.theme.MydoTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as MydoApplication).container }

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(
            observeInboxTasks = container.observeInboxTasks,
            completeTaskUseCase = container.completeTaskUseCase,
            undoCompleteTaskUseCase = container.undoCompleteTaskUseCase,
            reorderTasksUseCase = container.reorderTasksUseCase,
            bulkSetPriorityUseCase = container.bulkSetPriorityUseCase,
            bulkSetDueDateUseCase = container.bulkSetDueDateUseCase,
            bulkMoveTasksUseCase = container.bulkMoveTasksUseCase,
            bulkCompleteTasksUseCase = container.bulkCompleteTasksUseCase,
            bulkDeleteTasksUseCase = container.bulkDeleteTasksUseCase,
            undoBulkTaskOperationUseCase = container.undoBulkTaskOperationUseCase,
            bulkAddLabelsUseCase = container.bulkAddLabelsUseCase,
            undoBulkAddLabelsUseCase = container.undoBulkAddLabelsUseCase,
        )
    }

    private val taskComposerViewModel: TaskComposerViewModel by viewModels {
        TaskComposerViewModel.Factory(container.createTaskUseCase)
    }

    /** Set by [handleIntent] when MyDo is opened from a reminder notification's tap target. */
    private var deepLinkTaskId by mutableStateOf<UUID?>(null)

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            // Permission just changed from denied to granted — any reminder created or
            // edited while it was denied was silently skipped by NotificationScheduler, so
            // re-sync everything now that posting is actually possible.
            lifecycleScope.launch { container.reminderAlarmCoordinator.rescheduleAll() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        requestNotificationPermissionIfNeeded()

        setContent {
            // Wrap the Flow in remember so it is only created once
            val settingsFlow = remember {
                container.observeSettingsUseCase()
                    .map { (it as? AppResult.Success)?.value }
            }
            
            // Collect from the remembered Flow
            val settings by settingsFlow.collectAsState(initial = null)
            
            val themeMode = settings?.themeMode ?: ThemeMode.SYSTEM
            val useDynamicColor = settings?.useDynamicColor ?: true

            MydoTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                },
                dynamicColor = useDynamicColor,
            ) {
                MydoApp(
                    homeViewModel = homeViewModel,
                    taskComposerViewModel = taskComposerViewModel,
                    container = container,
                    deepLinkTaskId = deepLinkTaskId,
                    onDeepLinkConsumed = { deepLinkTaskId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra(EXTRA_OPEN_TASK_ID)?.let { raw ->
            deepLinkTaskId = try { UUID.fromString(raw) } catch (_: IllegalArgumentException) { null }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return // permission didn't exist before Android 13
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        /** Extra key carrying the task id a reminder notification was posted for
         *  (see [com.mydo.app.platform.ReminderAlarmReceiver]). */
        const val EXTRA_OPEN_TASK_ID = "openTaskId"
    }
}
