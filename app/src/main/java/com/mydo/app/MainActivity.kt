package com.mydo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mydo.app.domain.usecase.CreateTaskUseCase
import com.mydo.app.ui.app.MydoApp
import com.mydo.app.ui.components.TaskComposerViewModel
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.theme.MydoTheme

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels {
        val container = (application as MydoApplication).container
        HomeViewModel.Factory(container.observeInboxTasks)
    }

    private val taskComposerViewModel: TaskComposerViewModel by viewModels {
        val container = (application as MydoApplication).container
        val createTaskUseCase = CreateTaskUseCase(container.taskRepository, container.timeProvider)
        TaskComposerViewModel.Factory(createTaskUseCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MydoTheme {
                MydoApp(homeViewModel = homeViewModel, taskComposerViewModel = taskComposerViewModel)
            }
        }
    }
}
