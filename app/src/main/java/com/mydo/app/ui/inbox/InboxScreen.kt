package com.mydo.app.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.model.SortMode
import com.mydo.app.ui.components.BulkActionBar
import com.mydo.app.ui.components.DragHandle
import com.mydo.app.ui.components.DueDatePickerDialog
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.components.PriorityPickerDialog
import com.mydo.app.ui.components.ProjectPickerDialog
import com.mydo.app.ui.components.dragReorderOffset
import com.mydo.app.ui.components.rememberDragDropListState
import com.mydo.app.ui.home.HomeEvent
import com.mydo.app.ui.home.HomeUiState
import com.mydo.app.ui.home.HomeViewModel
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.launch

@Composable
fun InboxScreen(
    homeViewModel: HomeViewModel,
    navController: NavController,
    availableProjects: List<Project> = emptyList(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDueDateDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(homeViewModel) {
        homeViewModel.events.collect { event ->
            val message = when (event) {
                is HomeEvent.TaskCompleted -> "Task completed"
                is HomeEvent.BulkActionDone -> event.label
                is HomeEvent.BulkLabelsAdded -> "Labels added"
            }
            scope.launch {
                val result = snackbarHostState.showSnackbar(message, actionLabel = "Undo", withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) {
                    when (event) {
                        is HomeEvent.TaskCompleted -> homeViewModel.undoComplete(event.outcome)
                        is HomeEvent.BulkActionDone -> homeViewModel.undoBulk(event.outcome)
                        is HomeEvent.BulkLabelsAdded -> homeViewModel.undoBulkLabels(event.outcome)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val ready = uiState as? HomeUiState.Ready
            if (ready != null && ready.selectionMode) {
                BulkActionBar(
                    count = ready.selectedIds.size,
                    onComplete = { homeViewModel.bulkComplete() },
                    onSetPriority = { showPriorityDialog = true },
                    onSetDueDate = { showDueDateDialog = true },
                    onMove = { showMoveDialog = true },
                    onAddLabels = { /* labels are managed from Task Detail; bulk-label picker omitted here for now */ },
                    onDelete = { homeViewModel.bulkDelete() },
                    onCancel = { homeViewModel.clearSelection() },
                )
            }
        },
    ) { padding ->
        when (val state = uiState) {
            HomeUiState.Loading -> MydoLoadingState(
                message = "Opening local database\u2026",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            is HomeUiState.Error -> MydoErrorState(
                title = "Unable to open local data.",
                message = state.message,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            is HomeUiState.Ready -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    SortModeRow(state.sortMode, onChange = homeViewModel::setSortMode)

                    if (state.tasks.isEmpty()) {
                        MydoEmptyState(
                            title = "No tasks yet",
                            message = "Add a task to start capturing local work.",
                            actionLabel = "Add a task",
                            onAction = { },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val ordered = state.orderedTasks
                        val lazyListState = rememberLazyListState()
                        var localOrder by remember(ordered) { mutableStateOf(ordered) }
                        val dragState = rememberDragDropListState(lazyListState) { from, to ->
                            localOrder = localOrder.toMutableList().apply { add(to, removeAt(from)) }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(MydoSpacing.screenMargin),
                            verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                        ) {
                            itemsIndexed(localOrder, key = { _, task -> task.id }) { index, task ->
                                MydoTaskRow(
                                    title = task.title,
                                    metadata = task.projectPath,
                                    recurringSummary = null,
                                    priority = task.priority,
                                    completed = task.completed,
                                    selectionMode = state.selectionMode,
                                    selected = state.selectedIds.contains(task.id),
                                    onSelectToggle = { homeViewModel.toggleSelected(task.id) },
                                    onClick = {
                                        if (state.selectionMode) homeViewModel.toggleSelected(task.id)
                                        else navController.navigate("taskDetail/${task.id}")
                                    },
                                    onLongClick = {
                                        if (!state.selectionMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            homeViewModel.toggleSelectionMode()
                                            homeViewModel.toggleSelected(task.id)
                                        }
                                    },
                                    onCompletionToggle = { homeViewModel.completeTask(task.id) },
                                    trailing = if (state.sortMode == SortMode.MANUAL && !state.selectionMode) {
                                        {
                                            DragHandle(state = dragState, index = index, onDragEnd = {
                                                homeViewModel.reorder(localOrder.map { it.id })
                                            })
                                        }
                                    } else null,
                                    modifier = Modifier.dragReorderOffset(dragState, index),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPriorityDialog) {
        PriorityPickerDialog(
            current = Priority.P4,
            onDismiss = { showPriorityDialog = false },
            onSelect = { homeViewModel.bulkSetPriority(it); showPriorityDialog = false },
        )
    }
    if (showDueDateDialog) {
        DueDatePickerDialog(
            initialDateUtcMillis = null,
            onDismiss = { showDueDateDialog = false },
            onConfirm = { homeViewModel.bulkSetDueDate(it); showDueDateDialog = false },
        )
    }
    if (showMoveDialog) {
        ProjectPickerDialog(
            projects = availableProjects,
            onDismiss = { showMoveDialog = false },
            onSelectInbox = { homeViewModel.bulkMove(null, null); showMoveDialog = false },
            onSelectProject = { homeViewModel.bulkMove(it.id, null); showMoveDialog = false },
        )
    }
}

@Composable
private fun SortModeRow(current: SortMode, onChange: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small)) {
        Text(
            text = "Sort: ${sortModeLabel(current)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(sortModeLabel(mode)) },
                    onClick = { onChange(mode); expanded = false },
                )
            }
        }
    }
}

private fun sortModeLabel(mode: SortMode): String = when (mode) {
    SortMode.MANUAL -> "Manual"
    SortMode.DUE_DATE -> "Due date"
    SortMode.PRIORITY -> "Priority"
    SortMode.NAME -> "Name"
    SortMode.CREATED -> "Date created"
}
