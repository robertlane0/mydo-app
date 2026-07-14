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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.components.SelectionModeWrapper
import com.mydo.app.ui.home.HomeUiState
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.theme.MydoSpacing
import java.util.UUID

@Composable
fun InboxScreen(homeViewModel: HomeViewModel, navController: NavController) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    var isInSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(emptySet<UUID>()) }

    val toggleSelection = { taskId: UUID ->
        selectedItems = if (taskId in selectedItems) {
            selectedItems - taskId
        } else {
            selectedItems + taskId
        }
    }

    val onItemLongClick = { taskId: UUID ->
        if (!isInSelectionMode) {
            isInSelectionMode = true
        }
        toggleSelection(taskId)
    }

    val onItemClick = { taskId: UUID ->
        if (isInSelectionMode) {
            toggleSelection(taskId)
        } else {
            navController.navigate("taskDetail/$taskId")
        }
    }

    val clearSelection = {
        isInSelectionMode = false
        selectedItems = emptySet()
    }

    SelectionModeWrapper(
        isInSelectionMode = isInSelectionMode,
        selectedItems = selectedItems,
        onItemClick = onItemClick,
        onItemLongClick = onItemLongClick,
        onSelectionChange = { selectedItems = it },
        onClearSelection = clearSelection,
        onMove = { /* TODO: Implement move */ },
        onAddLabels = { /* TODO: Implement add labels */ },
        onSetPriority = { /* TODO: Implement set priority */ },
        onSetDueDate = { /* TODO: Implement set due date */ },
        onComplete = { /* TODO: Implement complete */ },
        onDelete = { /* TODO: Implement delete */ },
        onMore = { /* TODO: Implement more */ },
    ) {
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
                                isSelected = task.id in selectedItems,
                                onClick = { onItemClick(task.id) },
                                onLongClick = { onItemLongClick(task.id) },
                                onCompletionToggle = { },
                                onSelectionToggle = { toggleSelection(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
