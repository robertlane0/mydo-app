package com.mydo.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.model.SortMode
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.ui.components.BulkActionBar
import com.mydo.app.ui.components.DragHandle
import com.mydo.app.ui.components.DueDatePickerDialog
import com.mydo.app.ui.components.LabelMultiSelectDialog
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.components.PriorityPickerDialog
import com.mydo.app.ui.components.ProjectPickerDialog
import com.mydo.app.ui.components.dragReorderOffset
import com.mydo.app.ui.components.rememberDragDropListState
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.launch

/**
 * The Today destination (specs00-overview.md, specs03-home-screen.md) and the app's
 * default landing screen. Splits tasks into "Overdue" and "Today" sections; only the
 * "Today" section supports manual drag reorder (overdue tasks are better served by
 * rescheduling or completing than reordering).
 */
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    navController: NavController,
    availableProjects: List<Project> = emptyList(),
    availableLabels: List<Label> = emptyList(),
    onAddTask: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDueDateDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is TodayEvent.TaskCompleted -> "Task completed"
                is TodayEvent.BulkActionDone -> event.label
                is TodayEvent.BulkLabelsAdded -> "Labels added"
            }
            scope.launch {
                val result = snackbarHostState.showSnackbar(message, actionLabel = "Undo", withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) {
                    when (event) {
                        is TodayEvent.TaskCompleted -> viewModel.undoComplete(event.outcome)
                        is TodayEvent.BulkActionDone -> viewModel.undoBulk(event.outcome)
                        is TodayEvent.BulkLabelsAdded -> viewModel.undoBulkLabels(event.outcome)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val ready = uiState as? TodayUiState.Ready
            if (ready != null && ready.selectionMode) {
                BulkActionBar(
                    count = ready.selectedIds.size,
                    onComplete = { viewModel.bulkComplete() },
                    onSetPriority = { showPriorityDialog = true },
                    onSetDueDate = { showDueDateDialog = true },
                    onMove = { showMoveDialog = true },
                    onAddLabels = { showLabelDialog = true },
                    onDelete = { viewModel.bulkDelete() },
                    onCancel = { viewModel.clearSelection() },
                )
            }
        },
    ) { padding ->
        when (val state = uiState) {
            TodayUiState.Loading -> MydoLoadingState(
                message = "Loading today\u2026",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            is TodayUiState.Error -> MydoErrorState(
                title = "Unable to load Today",
                message = state.message,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            is TodayUiState.Ready -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    SortModeRow(state.sortMode, onChange = viewModel::setSortMode)

                    if (state.isEmpty) {
                        MydoEmptyState(
                            title = "You're all caught up",
                            message = "Enjoy the rest of your day.",
                            actionLabel = "Add a task",
                            onAction = onAddTask,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val orderedOverdue = state.orderedOverdue
                        val orderedToday = state.orderedDueToday
                        val hasOverdueSection = orderedOverdue.isNotEmpty()
                        val hasTodaySection = orderedToday.isNotEmpty()
                        val todayItemsStartIndex = (if (hasOverdueSection) 1 + orderedOverdue.size else 0) + (if (hasTodaySection) 1 else 0)

                        val lazyListState = rememberLazyListState()
                        var localToday by remember(orderedToday) { mutableStateOf(orderedToday) }
                        val dragState = rememberDragDropListState(lazyListState) { from, to ->
                            val localFrom = from - todayItemsStartIndex
                            val localTo = (to - todayItemsStartIndex).coerceIn(0, (localToday.size - 1).coerceAtLeast(0))
                            if (localFrom in localToday.indices) {
                                localToday = localToday.toMutableList().apply { add(localTo, removeAt(localFrom)) }
                            }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = MydoSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                        ) {
                            if (hasOverdueSection) {
                                item { SectionHeader(title = "Overdue", isOverdue = true) }
                                itemsIndexed(orderedOverdue, key = { _, t -> "overdue-${t.id}" }) { _, task ->
                                    TodayTaskRow(
                                        task = task,
                                        selectionMode = state.selectionMode,
                                        selected = state.selectedIds.contains(task.id),
                                        onSelectToggle = { viewModel.toggleSelected(task.id) },
                                        onClick = {
                                            if (state.selectionMode) viewModel.toggleSelected(task.id)
                                            else navController.navigate("taskDetail/${task.id}")
                                        },
                                        onLongClick = {
                                            if (!state.selectionMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.toggleSelectionMode()
                                                viewModel.toggleSelected(task.id)
                                            }
                                        },
                                        onCompletionToggle = { viewModel.completeTask(task.id) },
                                    )
                                }
                            }
                            if (hasTodaySection) {
                                item { SectionHeader(title = "Today", isOverdue = false) }
                                itemsIndexed(localToday, key = { _, t -> "today-${t.id}" }) { localIndex, task ->
                                    val globalIndex = todayItemsStartIndex + localIndex
                                    TodayTaskRow(
                                        task = task,
                                        selectionMode = state.selectionMode,
                                        selected = state.selectedIds.contains(task.id),
                                        onSelectToggle = { viewModel.toggleSelected(task.id) },
                                        onClick = {
                                            if (state.selectionMode) viewModel.toggleSelected(task.id)
                                            else navController.navigate("taskDetail/${task.id}")
                                        },
                                        onLongClick = {
                                            if (!state.selectionMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.toggleSelectionMode()
                                                viewModel.toggleSelected(task.id)
                                            }
                                        },
                                        onCompletionToggle = { viewModel.completeTask(task.id) },
                                        trailing = if (state.sortMode == SortMode.MANUAL && !state.selectionMode) {
                                            {
                                                DragHandle(state = dragState, index = globalIndex, onDragEnd = {
                                                    viewModel.reorder(localToday.map { it.id })
                                                })
                                            }
                                        } else null,
                                        modifier = Modifier.dragReorderOffset(dragState, globalIndex),
                                    )
                                }
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
            onSelect = { viewModel.bulkSetPriority(it); showPriorityDialog = false },
        )
    }
    if (showDueDateDialog) {
        DueDatePickerDialog(
            initialDateUtcMillis = null,
            onDismiss = { showDueDateDialog = false },
            onConfirm = { viewModel.bulkSetDueDate(it); showDueDateDialog = false },
        )
    }
    if (showMoveDialog) {
        ProjectPickerDialog(
            projects = availableProjects,
            onDismiss = { showMoveDialog = false },
            onSelectInbox = { viewModel.bulkMove(null, null); showMoveDialog = false },
            onSelectProject = { viewModel.bulkMove(it.id, null); showMoveDialog = false },
        )
    }
    if (showLabelDialog) {
        LabelMultiSelectDialog(
            labels = availableLabels,
            onDismiss = { showLabelDialog = false },
            onConfirm = { labelIds -> viewModel.bulkAddLabels(labelIds); showLabelDialog = false },
        )
    }
}

@Composable
private fun TodayTaskRow(
    task: TaskSummary,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCompletionToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    MydoTaskRow(
        title = task.title,
        metadata = task.projectPath,
        priority = task.priority,
        completed = task.completed,
        selectionMode = selectionMode,
        selected = selected,
        onSelectToggle = onSelectToggle,
        onClick = onClick,
        onLongClick = onLongClick,
        onCompletionToggle = onCompletionToggle,
        trailing = trailing,
        modifier = modifier.padding(horizontal = MydoSpacing.screenMargin),
    )
}

@Composable
private fun SectionHeader(title: String, isOverdue: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
