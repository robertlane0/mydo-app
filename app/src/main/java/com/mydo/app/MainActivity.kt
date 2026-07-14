package com.mydo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mydo.app.domain.usecase.CreateTaskUseCase
import com.mydo.app.ui.app.MydoApp
import com.mydo.app.ui.components.TaskComposerViewModel
import com.mydo.app.ui.filters.FiltersViewModel
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.labels.LabelsViewModel
import com.mydo.app.ui.search.SearchViewModel
import com.mydo.app.ui.upcoming.UpcomingViewModel
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

    private val upcomingViewModel: UpcomingViewModel by viewModels {
        val container = (application as MydoApplication).container
        UpcomingViewModel.Factory(container.taskRepository, container.timeProvider)
    }

    private val searchViewModel: SearchViewModel by viewModels {
        val container = (application as MydoApplication).container
        SearchViewModel.Factory(container.searchTasks)
    }

    private val labelsViewModel: LabelsViewModel by viewModels {
        val container = (application as MydoApplication).container
        LabelsViewModel.Factory(container.labelRepository)
    }

    private val filtersViewModel: FiltersViewModel by viewModels {
        val container = (application as MydoApplication).container
        FiltersViewModel.Factory(container.filterRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MydoTheme {
                MydoApp(
                    homeViewModel = homeViewModel,
                    taskComposerViewModel = taskComposerViewModel,
                    upcomingViewModel = upcomingViewModel,
                    searchViewModel = searchViewModel,
                    labelsViewModel = labelsViewModel,
                    filtersViewModel = filtersViewModel,
                )
            }
        }
    }
}
