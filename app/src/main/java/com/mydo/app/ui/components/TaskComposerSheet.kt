package com.mydo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Project
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The globally-available Quick Add bottom sheet (specs12-user-flows.md, "Core Flow: Quick
 * Add Task"; AGENTS.md step 3). A title alone is enough to save; `#Project`, `p1`-`p4`, and
 * a few relative-date words are recognized inline via [parseQuickAdd] and previewed as
 * chips so the user can see what will be applied before submitting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskComposerSheet(
    onDismiss: () -> Unit,
    viewModel: TaskComposerViewModel,
    availableProjects: List<Project> = emptyList(),
) {
    var title by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val parsed = remember(title, availableProjects) { parseQuickAdd(title, availableProjects) }
    val hasPresetProject = viewModel.presetProjectId != null
    val hasPresetDue = viewModel.presetDueAtUtcMillis != null

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ComposerEvent.Created -> {
                    submitting = false
                    val destination = event.projectName ?: "Inbox"
                    MydoSnackbarController.show(
                        message = "Task added to $destination",
                        actionLabel = "Undo",
                        onAction = { viewModel.undoCreate(event.task.id) },
                    )
                }
                is ComposerEvent.Failed -> {
                    submitting = false
                    MydoSnackbarController.show(message = event.message)
                }
            }
        }
    }

    // specs08-search.md-style immediate focus, applied here per specs12: "The text input
    // field immediately receives focus."
    LaunchedEffect(Unit) {
        delay(80)
        focusRequester.requestFocus()
    }

    fun submit() {
        val clean = parsed.title.trim()
        if (clean.isEmpty() || submitting) return
        submitting = true
        keyboardController?.hide()
        viewModel.createTask(
            title = clean,
            projectId = parsed.projectId,
            projectName = parsed.projectName,
            dueAtUtcMillis = parsed.dueAtUtcMillis,
            priority = parsed.priority ?: Priority.P4,
        )
        title = ""
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(MydoSpacing.screenMargin)) {
            Text(text = "New task", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(MydoSpacing.small))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Submit report tomorrow #Work") },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = false,
                    maxLines = 4,
                )
                Spacer(modifier = Modifier.width(MydoSpacing.small))
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.padding(MydoSpacing.small))
                } else {
                    IconButton(
                        onClick = { submit() },
                        enabled = parsed.title.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Add task",
                            tint = if (parsed.title.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                }
            }

            val chips = buildList {
                (parsed.dueLabel ?: presetDueLabel(hasPresetDue))?.let { add("\uD83D\uDCC5 $it") }
                (parsed.projectName ?: presetProjectLabel(hasPresetProject, availableProjects, viewModel.presetProjectId))?.let { add("\uD83D\uDCC1 $it") }
                parsed.priority?.let { add(priorityLabel(it)) }
            }
            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MydoSpacing.small))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MydoSpacing.extraSmall)) {
                    items(chips) { chip ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = MydoSpacing.small, vertical = MydoSpacing.extraSmall),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(MydoSpacing.medium))
        }
    }
}

private fun presetDueLabel(hasPreset: Boolean): String? = if (hasPreset) "Scheduled" else null

private fun presetProjectLabel(hasPreset: Boolean, projects: List<Project>, presetProjectId: UUID?): String? {
    if (!hasPreset) return null
    return projects.firstOrNull { it.id == presetProjectId }?.name
}
