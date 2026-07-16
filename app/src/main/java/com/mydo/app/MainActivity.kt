package com.mydo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.ThemeMode
import com.mydo.app.ui.app.MydoApp
import com.mydo.app.ui.components.TaskComposerViewModel
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.theme.MydoTheme
import kotlinx.coroutines.flow.map

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by container.observeSettingsUseCase()
                .map { (it as? AppResult.Success)?.value }
                .collectAsState(initial = null)
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
                )
            }
        }
    }
}
