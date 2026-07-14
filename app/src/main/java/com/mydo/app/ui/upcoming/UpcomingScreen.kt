package com.mydo.app.ui.upcoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun UpcomingScreen(
    viewModel: UpcomingViewModel,
    navController: androidx.navigation.NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(
        initialValue = UpcomingUiState.Loading,
    )

    when (val state = uiState) {
        is UpcomingUiState.Loading -> MydoLoadingState(
            message = "Loading schedule…",
            modifier = Modifier.fillMaxSize()
        )

        is UpcomingUiState.Error -> MydoErrorState(
            title = "Unable to load schedule",
            message = state.message,
            modifier = Modifier.fillMaxSize()
        )

        is UpcomingUiState.Ready -> {
            if (state.groups.isEmpty()) {
                MydoEmptyState(
                    title = "Nothing scheduled",
                    message = "Tasks with due dates will appear here.",
                    actionLabel = "Add a task",
                    onAction = { },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MydoSpacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(MydoSpacing.medium),
                ) {
                    items<UpcomingDateGroupModel>(state.groups) { group ->
                        UpcomingDateGroup(
                            group = group,
                            onTaskClick = { task ->
                                navController.navigate("taskDetail/${task.id}")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingDateGroup(
    group: UpcomingDateGroupModel,
    onTaskClick: (UpcomingTaskItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Date header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MydoSpacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = group.headerTitle,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${group.tasks.size} task${if (group.tasks.size != 1) "s" else ""}",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        
        Divider(
            modifier = Modifier.padding(bottom = MydoSpacing.extraSmall),
            color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
        )

        // Tasks
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(group.tasks, key = { it.id }) { task ->
                MydoTaskRow(
                    title = task.title,
                    metadata = task.projectPath,
                    priority = task.priority,
                    completed = task.completed,
                    onClick = { onTaskClick(task) },
                    onCompletionToggle = { },
                )
            }
        }
    }
}