package com.mydo.app.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.home.HomeUiState
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun InboxScreen(homeViewModel: HomeViewModel, navController: NavController) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        HomeUiState.Loading -> MydoLoadingState(
            message = "Opening local database…",
            modifier = Modifier.fillMaxSize()
        )

        is HomeUiState.Error -> MydoErrorState(
            title = "Unable to open local data.",
            message = state.message,
            modifier = Modifier.fillMaxSize()
        )

        is HomeUiState.Ready -> {
            if (state.tasks.isEmpty()) {
                MydoEmptyState(
                    title = "No tasks yet",
                    message = "Add a task to start capturing local work.",
                    actionLabel = "Add a task",
                    onAction = { },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MydoSpacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        MydoTaskRow(
                            title = task.title,
                            metadata = task.projectPath,
                            priority = task.priority,
                            completed = task.completed,
                            onClick = { navController.navigate("taskDetail/${task.id}") },
                            onCompletionToggle = { },
                        )
                    }
                }
            }
        }
    }
}
