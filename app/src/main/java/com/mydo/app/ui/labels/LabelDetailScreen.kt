package com.mydo.app.ui.labels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.launch

@Composable
fun LabelDetailScreen(viewModel: LabelDetailViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LabelDetailEvent.TaskCompleted -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar("Task completed", actionLabel = "Undo", withDismissAction = true)
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoComplete(event.outcome)
                    }
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val state = uiState) {
            LabelDetailUiState.Loading -> MydoLoadingState(message = "Loading tasks\u2026", modifier = Modifier.fillMaxSize().padding(padding))
            is LabelDetailUiState.Error -> MydoErrorState(title = "Unable to load", message = state.message, modifier = Modifier.fillMaxSize().padding(padding))
            is LabelDetailUiState.Ready -> {
                if (state.tasks.isEmpty()) {
                    MydoEmptyState(
                        title = "No tasks with this label",
                        message = "Apply this label to a task to see it here.",
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
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
                                onCompletionToggle = { viewModel.completeTask(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
