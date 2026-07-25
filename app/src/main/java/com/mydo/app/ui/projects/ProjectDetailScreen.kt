package com.mydo.app.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.model.Section
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.ui.components.BulkActionBar
import com.mydo.app.ui.components.DueDatePickerDialog
import com.mydo.app.ui.components.LabelMultiSelectDialog
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.components.PriorityPickerDialog
import com.mydo.app.ui.components.ProjectPickerDialog
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * A single project's detail view (specs06-projects.md, "Sections"): unsectioned tasks
 * first, then each named section in order. Reordering tasks within a section uses
 * up/down controls rather than drag — with multiple independently-reorderable groups
 * sharing one scrollable list, per-group drag math is easy to get subtly wrong, so this
 * favors a mechanism that's simple to verify correct over one that "feels" more native.
 * (Inbox, Today, and this project's own Unsectioned bucket still support full drag reorder.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    viewModel: ProjectDetailViewModel,
    navController: NavController,
    onBack: () -> Unit,
    availableProjects: List<Project> = emptyList(),
    availableLabels: List<Label> = emptyList(),
    onAddTask: (sectionId: UUID?) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDueDateDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var showEditProjectDialog by remember { mutableStateOf(false) }
    var showDeleteProjectDialog by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var sectionMenuTarget by remember { mutableStateOf<Section?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectDetailEvent.ProjectDeleted -> onBack()
                else -> {
                    val message = when (event) {
                        is ProjectDetailEvent.TaskCompleted -> "Task completed"
                        is ProjectDetailEvent.BulkActionDone -> event.label
                        is ProjectDetailEvent.BulkLabelsAdded -> "Labels added"
                        is ProjectDetailEvent.SectionCreated -> "Section \u201c${event.name}\u201d added"
                        ProjectDetailEvent.SectionDeleted -> "Section deleted"
                        ProjectDetailEvent.ProjectDeleted -> return@collect
                    }
                    val undoable = event is ProjectDetailEvent.TaskCompleted ||
                        event is ProjectDetailEvent.BulkActionDone ||
                        event is ProjectDetailEvent.BulkLabelsAdded
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message,
                            actionLabel = if (undoable) "Undo" else null,
                            withDismissAction = true,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            when (event) {
                                is ProjectDetailEvent.TaskCompleted -> viewModel.undoComplete(event.outcome)
                                is ProjectDetailEvent.BulkActionDone -> viewModel.undoBulk(event.outcome)
                                is ProjectDetailEvent.BulkLabelsAdded -> viewModel.undoBulkLabels(event.outcome)
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            val projectName = (uiState as? ProjectDetailUiState.Ready)?.project?.name ?: ""
            TopAppBar(
                title = { Text(projectName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Projects")
                    }
                },
                actions = {
                    if (uiState is ProjectDetailUiState.Ready) {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Project options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Add section") }, onClick = { showMenu = false; showAddSectionDialog = true })
                            DropdownMenuItem(text = { Text("Edit project") }, onClick = { showMenu = false; showEditProjectDialog = true })
                            DropdownMenuItem(
                                text = { Text("Delete project", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; showDeleteProjectDialog = true },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val ready = uiState as? ProjectDetailUiState.Ready
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
            ProjectDetailUiState.Loading -> MydoLoadingState(message = "Loading project\u2026", modifier = Modifier.fillMaxSize().padding(padding))
            ProjectDetailUiState.NotFound -> MydoErrorState(
                title = "Project not found",
                message = "This project may have been deleted.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            is ProjectDetailUiState.Error -> MydoErrorState(
                title = "Unable to load project",
                message = state.message,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            is ProjectDetailUiState.Ready -> {
                if (state.isEmpty) {
                    MydoEmptyState(
                        title = "Nothing here yet",
                        message = "Add a task or a section to get this project going.",
                        actionLabel = "Add a task",
                        onAction = { onAddTask(null) },
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(vertical = MydoSpacing.small),
                    ) {
                        if (state.unsectionedTasks.isNotEmpty() || state.orderedSections.isEmpty()) {
                            item {
                                AddTaskRow(onClick = { onAddTask(null) })
                            }
                            items(state.orderedUnsectioned, key = { "u-${it.id}" }) { task ->
                                ProjectTaskRow(
                                    task = task,
                                    selectionMode = state.selectionMode,
                                    selected = state.selectedIds.contains(task.id),
                                    onClick = {
                                        if (state.selectionMode) viewModel.toggleSelected(task.id) else navController.navigate("taskDetail/${task.id}")
                                    },
                                    onSelectToggle = { viewModel.toggleSelected(task.id) },
                                    onLongClick = {
                                        if (!state.selectionMode) { viewModel.toggleSelectionMode(); viewModel.toggleSelected(task.id) }
                                    },
                                    onCompletionToggle = { viewModel.completeTask(task.id) },
                                    onMoveUp = null,
                                    onMoveDown = null,
                                )
                            }
                        }
                        state.orderedSections.forEach { section ->
                            item {
                                SectionHeaderRow(
                                    section = section,
                                    onAddTask = { onAddTask(section.id) },
                                    onMenu = { sectionMenuTarget = section },
                                )
                            }
                            val tasks = state.tasksFor(section.id)
                            items(tasks, key = { "s-${section.id}-${it.id}" }) { task ->
                                val index = tasks.indexOf(task)
                                ProjectTaskRow(
                                    task = task,
                                    selectionMode = state.selectionMode,
                                    selected = state.selectedIds.contains(task.id),
                                    onClick = {
                                        if (state.selectionMode) viewModel.toggleSelected(task.id) else navController.navigate("taskDetail/${task.id}")
                                    },
                                    onSelectToggle = { viewModel.toggleSelected(task.id) },
                                    onLongClick = {
                                        if (!state.selectionMode) { viewModel.toggleSelectionMode(); viewModel.toggleSelected(task.id) }
                                    },
                                    onCompletionToggle = { viewModel.completeTask(task.id) },
                                    onMoveUp = if (index > 0) {
                                        {
                                            val reordered = tasks.toMutableList().apply { add(index - 1, removeAt(index)) }
                                            viewModel.reorderWithin(reordered.map { it.id })
                                        }
                                    } else null,
                                    onMoveDown = if (index < tasks.size - 1) {
                                        {
                                            val reordered = tasks.toMutableList().apply { add(index + 1, removeAt(index)) }
                                            viewModel.reorderWithin(reordered.map { it.id })
                                        }
                                    } else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val readyState = uiState as? ProjectDetailUiState.Ready
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
            onSelectInbox = { viewModel.bulkMoveToProject(null); showMoveDialog = false },
            onSelectProject = { viewModel.bulkMoveToProject(it.id); showMoveDialog = false },
        )
    }
    if (showLabelDialog) {
        LabelMultiSelectDialog(
            labels = availableLabels,
            onDismiss = { showLabelDialog = false },
            onConfirm = { viewModel.bulkAddLabels(it); showLabelDialog = false },
        )
    }
    if (showAddSectionDialog) {
        AddSectionDialog(onDismiss = { showAddSectionDialog = false }, onSave = { viewModel.createSection(it); showAddSectionDialog = false })
    }
    if (showEditProjectDialog && readyState != null) {
        ProjectRenameDialog(
            initialName = readyState.project.name,
            onDismiss = { showEditProjectDialog = false },
            onSave = { name -> viewModel.renameProject(readyState.project, name, readyState.project.color); showEditProjectDialog = false },
        )
    }
    if (showDeleteProjectDialog && readyState != null) {
        AlertDialog(
            onDismissRequest = { showDeleteProjectDialog = false },
            title = { Text("Delete \u201c${readyState.project.name}\u201d?") },
            text = { Text("This can't be undone. Tasks in this project will move to the Inbox rather than being deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProject(); showDeleteProjectDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteProjectDialog = false }) { Text("Cancel") } },
        )
    }
    sectionMenuTarget?.let { section ->
        AlertDialog(
            onDismissRequest = { sectionMenuTarget = null },
            title = { Text(section.name) },
            text = { Text("Delete this section? Its tasks will remain in the project, unsectioned.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSection(section.id); sectionMenuTarget = null }) {
                    Text("Delete section", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { sectionMenuTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddTaskRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Add task", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = MydoSpacing.small))
    }
}

@Composable
private fun SectionHeaderRow(section: Section, onAddTask: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(section.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add task to ${section.name}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAddTask).padding(end = MydoSpacing.medium),
            )
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "${section.name} options",
                modifier = Modifier.clickable(onClick = onMenu),
            )
        }
    }
}

@Composable
private fun ProjectTaskRow(
    task: TaskSummary,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onLongClick: () -> Unit,
    onCompletionToggle: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    MydoTaskRow(
        title = task.title,
        priority = task.priority,
        completed = task.completed,
        selectionMode = selectionMode,
        selected = selected,
        onClick = onClick,
        onSelectToggle = onSelectToggle,
        onLongClick = onLongClick,
        onCompletionToggle = onCompletionToggle,
        trailing = if (onMoveUp != null || onMoveDown != null) {
            {
                Row {
                    if (onMoveUp != null) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.clickable(onClick = onMoveUp))
                    }
                    if (onMoveDown != null) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.clickable(onClick = onMoveDown))
                    }
                }
            }
        } else {
            null
        },
        modifier = Modifier.padding(horizontal = MydoSpacing.screenMargin),
    )
}

@Composable
private fun AddSectionDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New section") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Section name") }) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name) }, enabled = name.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProjectRenameDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit project") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Project name") }) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
