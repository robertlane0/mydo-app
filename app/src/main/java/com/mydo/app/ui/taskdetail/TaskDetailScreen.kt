package com.mydo.app.ui.taskdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.recurrence.RecurrenceSummaryFormatter
import com.mydo.app.ui.components.CircularCompletionControl
import com.mydo.app.ui.components.DueDatePickerDialog
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.PriorityPickerDialog
import com.mydo.app.ui.components.ProjectPickerDialog
import com.mydo.app.ui.components.RecurrenceEditorSheet
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun TaskDetailScreen(taskViewModel: TaskDetailViewModel, onBack: () -> Unit) {
    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDueDateDialog by remember { mutableStateOf(false) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var showRecurrenceSheet by remember { mutableStateOf(false) }
    var showLabelsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) taskViewModel.addAttachments(uris)
    }

    LaunchedEffect(taskViewModel) {
        taskViewModel.events.collect { event ->
            when (event) {
                is TaskDetailEvent.Completed -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar("Task completed", actionLabel = "Undo", withDismissAction = true)
                        if (result == SnackbarResult.ActionPerformed) taskViewModel.undoComplete(event.outcome)
                    }
                }
                is TaskDetailEvent.Message -> scope.launch { snackbarHostState.showSnackbar(event.text) }
                TaskDetailEvent.Deleted -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    Text("\u2190", modifier = Modifier.padding(16.dp).clickable(onClick = onBack))
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val state = uiState) {
            TaskDetailUiState.Loading -> MydoLoadingState(message = "Loading task\u2026", modifier = Modifier.fillMaxSize().padding(padding))
            TaskDetailUiState.NotFound -> MydoErrorState(title = "Task not found", message = "It may have been deleted.", modifier = Modifier.fillMaxSize().padding(padding))
            is TaskDetailUiState.Error -> MydoErrorState(title = "Something went wrong", message = state.message, modifier = Modifier.fillMaxSize().padding(padding))
            is TaskDetailUiState.Ready -> {
                val task = state.task
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(MydoSpacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(MydoSpacing.medium),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.Top) {
                            CircularCompletionControl(
                                completed = task.completed,
                                priority = task.priority,
                                onToggle = { taskViewModel.toggleComplete() },
                            )
                            var titleText by remember(task.id) { mutableStateOf(task.title) }
                            OutlinedTextField(
                                value = titleText,
                                onValueChange = { titleText = it },
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                                ),
                                modifier = Modifier.weight(1f).padding(start = MydoSpacing.small),
                            )
                            LaunchedEffect(titleText) {
                                if (titleText != task.title) {
                                    delay(500)
                                    if (titleText.isNotBlank()) taskViewModel.updateTitle(titleText)
                                }
                            }
                        }
                    }

                    item {
                        var descriptionText by remember(task.id) { mutableStateOf(task.description) }
                        OutlinedTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            placeholder = { Text("Add a description") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LaunchedEffect(descriptionText) {
                            if (descriptionText != task.description) {
                                delay(500)
                                taskViewModel.updateDescription(descriptionText)
                            }
                        }
                    }

                    item {
                        DetailRow(label = "Priority", value = priorityLabelShort(task.priority)) { showPriorityDialog = true }
                    }
                    item {
                        DetailRow(label = "Due date", value = task.dueAtUtcMillis?.let(::formatDate) ?: "None") { showDueDateDialog = true }
                    }
                    item {
                        DetailRow(
                            label = "Project",
                            value = state.allProjects.firstOrNull { it.id == task.projectId }?.name ?: "Inbox",
                        ) { showProjectDialog = true }
                    }
                    item {
                        DetailRow(
                            label = "Repeat",
                            value = task.recurringRule?.let { RecurrenceSummaryFormatter.summarize(it) } ?: "Doesn't repeat",
                        ) { showRecurrenceSheet = true }
                    }
                    if (task.recurringRule != null) {
                        item {
                            TextButton(onClick = { taskViewModel.skipNextOccurrence() }) { Text("Skip next occurrence") }
                        }
                    }

                    item { Divider() }

                    item {
                        SectionHeader("Labels") { showLabelsDialog = true }
                        Row {
                            task.labels.forEach { label -> LabelChip(label.name, label.color) }
                            if (task.labels.isEmpty()) Text("No labels", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    item { Divider() }

                    item {
                        SectionHeader("Reminders") { showReminderDialog = true }
                        state.reminders.forEach { reminder ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatDateTime(reminder.triggerAtUtcMillis))
                                Text("\u2715", modifier = Modifier.clickable { taskViewModel.deleteReminder(reminder.id) })
                            }
                        }
                        if (state.reminders.isEmpty()) Text("No reminders", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    item { Divider() }

                    item {
                        SectionHeader("Attachments") { documentPickerLauncher.launch(arrayOf("*/*")) }
                        state.attachments.forEach { attachment ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(attachment.filename, modifier = Modifier.weight(1f))
                                Text("\u2715", modifier = Modifier.clickable { taskViewModel.removeAttachment(attachment) })
                            }
                        }
                        if (state.attachments.isEmpty()) Text("No attachments", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    item { Divider() }

                    item {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("Delete task", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (showPriorityDialog) {
                    PriorityPickerDialog(
                        current = task.priority,
                        onDismiss = { showPriorityDialog = false },
                        onSelect = { taskViewModel.updatePriority(it); showPriorityDialog = false },
                    )
                }
                if (showDueDateDialog) {
                    DueDatePickerDialog(
                        initialDateUtcMillis = task.dueAtUtcMillis,
                        onDismiss = { showDueDateDialog = false },
                        onConfirm = { taskViewModel.updateDueDate(it); showDueDateDialog = false },
                    )
                }
                if (showProjectDialog) {
                    ProjectPickerDialog(
                        projects = state.allProjects,
                        onDismiss = { showProjectDialog = false },
                        onSelectInbox = { taskViewModel.moveToProject(null); showProjectDialog = false },
                        onSelectProject = { taskViewModel.moveToProject(it.id); showProjectDialog = false },
                    )
                }
                if (showRecurrenceSheet) {
                    val zoneId = ZoneId.systemDefault()
                    val dueDate = task.dueAtUtcMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() } ?: LocalDate.now(zoneId)
                    RecurrenceEditorSheet(
                        initialRuleString = task.recurringRule,
                        dueDayOfWeek = dueDate.dayOfWeek,
                        dueDayOfMonth = dueDate.dayOfMonth,
                        onDismiss = { showRecurrenceSheet = false },
                        onSave = { taskViewModel.setRecurrence(it); showRecurrenceSheet = false },
                        onRemove = { taskViewModel.removeRecurrence(); showRecurrenceSheet = false },
                    )
                }
                if (showLabelsDialog) {
                    LabelPickerDialog(
                        allLabels = state.allLabels,
                        appliedLabelIds = task.labels.map { it.id }.toSet(),
                        onDismiss = { showLabelsDialog = false },
                        onToggle = { labelId, applied -> taskViewModel.toggleLabel(labelId, applied) },
                    )
                }
                if (showReminderDialog) {
                    ReminderPickerDialog(
                        onDismiss = { showReminderDialog = false },
                        onSelect = { minutes -> taskViewModel.addRelativeReminder(minutes); showReminderDialog = false },
                    )
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Delete this task?") },
                        text = { Text("This can't be undone.") },
                        confirmButton = {
                            TextButton(onClick = { taskViewModel.deleteTask(); showDeleteConfirm = false }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text("+ Add", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAdd))
    }
}

@Composable
private fun LabelChip(name: String, colorHex: String) {
    val color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = MydoSpacing.small),
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(name, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun LabelPickerDialog(
    allLabels: List<Label>,
    appliedLabelIds: Set<UUID>,
    onDismiss: () -> Unit,
    onToggle: (UUID, Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Labels") },
        text = {
            LazyColumn {
                items(allLabels, key = { it.id }) { label ->
                    val applied = appliedLabelIds.contains(label.id)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(label.id, applied) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = applied, onCheckedChange = { onToggle(label.id, applied) })
                        Text(label.name)
                    }
                }
                if (allLabels.isEmpty()) {
                    item { Text("No labels yet. Create one from the Labels screen.") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ReminderPickerDialog(onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remind me") },
        text = {
            Column {
                listOf(
                    "At time of due date" to 0L,
                    "15 minutes before" to 15L,
                    "30 minutes before" to 30L,
                    "1 hour before" to 60L,
                    "1 day before" to 24 * 60L,
                ).forEach { (label, minutes) ->
                    Text(label, modifier = Modifier.fillMaxWidth().clickable { onSelect(minutes) }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun priorityLabelShort(priority: Priority): String = when (priority) {
    Priority.P1 -> "P1 \u00b7 Urgent"
    Priority.P2 -> "P2 \u00b7 High"
    Priority.P3 -> "P3 \u00b7 Medium"
    Priority.P4 -> "None"
}

private fun formatDate(utcMillis: Long): String =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

private fun formatDateTime(utcMillis: Long): String =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy \u00b7 h:mm a"))
