package com.mydo.app.ui.projects

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.Project
import com.mydo.app.ui.components.ColorPickerRow
import com.mydo.app.ui.components.LABEL_COLOR_PALETTE
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.navigation.Screen
import com.mydo.app.ui.theme.MydoSpacing
import kotlinx.coroutines.launch

/**
 * The Projects list (specs06-projects.md): favorites pinned above the rest of the active
 * projects, with an archived-projects toggle at the bottom. Previously this screen was an
 * unimplemented placeholder ("Text(\"Projects\")").
 */
@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var deletingProject by remember { mutableStateOf<Project?>(null) }
    var deleteTaskCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is ProjectsEvent.ProjectCreated -> "Project \u201c${event.project.name}\u201d created"
                is ProjectsEvent.ProjectDeleted -> if (event.movedTaskCount > 0) {
                    "Deleted \u201c${event.name}\u201d \u2014 ${event.movedTaskCount} task${if (event.movedTaskCount == 1) "" else "s"} moved to Inbox"
                } else {
                    "Deleted \u201c${event.name}\u201d"
                }
            }
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val state = uiState) {
            ProjectsUiState.Loading -> MydoLoadingState(
                message = "Loading projects\u2026",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            is ProjectsUiState.Error -> MydoErrorState(
                title = "Unable to load projects",
                message = state.message,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            is ProjectsUiState.Ready -> {
                if (state.isEmpty) {
                    MydoEmptyState(
                        title = "No projects yet",
                        message = "Create a project to organize related tasks.",
                        actionLabel = "Create project",
                        onAction = { showCreateDialog = true },
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(vertical = MydoSpacing.small),
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCreateDialog = true }
                                    .padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Add project",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = MydoSpacing.small),
                                )
                            }
                        }
                        if (state.favorites.isNotEmpty()) {
                            item { SectionLabel("Favorites") }
                            items(state.favorites, key = { it.id }) { project ->
                                ProjectRow(
                                    project = project,
                                    onClick = { navController.navigate(Screen.ProjectDetail.createRoute(project.id.toString())) },
                                    onEdit = { editingProject = project },
                                    onToggleFavorite = { viewModel.toggleFavorite(project) },
                                    onArchive = { viewModel.setArchived(project, true) },
                                    onDelete = {
                                        deletingProject = project
                                        scope.launch { deleteTaskCount = viewModel.countActiveTasks(project.id) }
                                    },
                                )
                            }
                        }
                        if (state.others.isNotEmpty()) {
                            item { SectionLabel("Projects") }
                            items(state.others, key = { it.id }) { project ->
                                ProjectRow(
                                    project = project,
                                    onClick = { navController.navigate(Screen.ProjectDetail.createRoute(project.id.toString())) },
                                    onEdit = { editingProject = project },
                                    onToggleFavorite = { viewModel.toggleFavorite(project) },
                                    onArchive = { viewModel.setArchived(project, true) },
                                    onDelete = {
                                        deletingProject = project
                                        scope.launch { deleteTaskCount = viewModel.countActiveTasks(project.id) }
                                    },
                                )
                            }
                        }
                        item {
                            Text(
                                text = if (state.showingArchived) "Hide archived" else "Show archived",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleShowArchived() }
                                    .padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.medium),
                            )
                        }
                        if (state.showingArchived) {
                            items(state.archived, key = { it.id }) { project ->
                                ProjectRow(
                                    project = project,
                                    archived = true,
                                    onClick = { navController.navigate(Screen.ProjectDetail.createRoute(project.id.toString())) },
                                    onEdit = { editingProject = project },
                                    onToggleFavorite = { viewModel.toggleFavorite(project) },
                                    onArchive = { viewModel.setArchived(project, false) },
                                    onDelete = {
                                        deletingProject = project
                                        scope.launch { deleteTaskCount = viewModel.countActiveTasks(project.id) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ProjectEditDialog(
            initialName = "",
            initialColor = LABEL_COLOR_PALETTE.first().first,
            onDismiss = { showCreateDialog = false },
            onSave = { name, color -> viewModel.createProject(name, color); showCreateDialog = false },
        )
    }
    editingProject?.let { project ->
        ProjectEditDialog(
            initialName = project.name,
            initialColor = project.color,
            onDismiss = { editingProject = null },
            onSave = { name, color -> viewModel.renameProject(project, name, color); editingProject = null },
        )
    }
    deletingProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            title = { Text("Delete \u201c${project.name}\u201d?") },
            text = {
                Text(
                    if (deleteTaskCount > 0) {
                        "This can't be undone. $deleteTaskCount active task${if (deleteTaskCount == 1) "" else "s"} in this project will move to the Inbox rather than being deleted."
                    } else {
                        "This can't be undone. This project has no active tasks."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProject(project); deletingProject = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deletingProject = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.extraSmall),
    )
}

@Composable
private fun ProjectRow(
    project: Project,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    archived: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            val color = runCatching { Color(android.graphics.Color.parseColor(project.color)) }.getOrDefault(MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
            Text(
                project.name,
                modifier = Modifier.padding(start = MydoSpacing.small),
                color = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            imageVector = if (project.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (project.favorite) "Unfavorite" else "Favorite",
            tint = if (project.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(onClick = onToggleFavorite).padding(end = MydoSpacing.small),
        )
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options for ${project.name}")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
                DropdownMenuItem(
                    text = { Text(if (archived) "Unarchive" else "Archive") },
                    onClick = { showMenu = false; onArchive() },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun ProjectEditDialog(
    initialName: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "New project" else "Edit project") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Project name") })
                ColorPickerRow(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, color) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
