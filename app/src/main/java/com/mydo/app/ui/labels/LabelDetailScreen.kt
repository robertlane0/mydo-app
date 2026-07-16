package com.mydo.app.ui.labels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun LabelDetailScreen(viewModel: LabelDetailViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        LabelDetailUiState.Loading -> MydoLoadingState(message = "Loading tasks\u2026", modifier = Modifier.fillMaxSize())
        is LabelDetailUiState.Error -> MydoErrorState(title = "Unable to load", message = state.message, modifier = Modifier.fillMaxSize())
        is LabelDetailUiState.Ready -> {
            if (state.tasks.isEmpty()) {
                MydoEmptyState(title = "No tasks with this label", message = "Apply this label to a task to see it here.", modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MydoSpacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        MydoTaskRow(
                            title = task.title,
                            completed = task.completed,
                            priority = task.priority,
                            metadata = task.projectPath,
                            onClick = { navController.navigate("taskDetail/${task.id}") },
                            onCompletionToggle = { },
                        )
                    }
                }
            }
        }
    }
}
