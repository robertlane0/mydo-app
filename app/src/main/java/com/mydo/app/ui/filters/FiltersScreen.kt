package com.mydo.app.ui.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.Filter
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.navigation.Screen
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun FiltersScreen(viewModel: FiltersViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingFilter by remember { mutableStateOf<Filter?>(null) }

    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { showCreateDialog = true }) { Text("+") } },
    ) { padding ->
        when (val state = uiState) {
            FiltersUiState.Loading -> MydoLoadingState(message = "Loading filters\u2026", modifier = Modifier.fillMaxSize().padding(padding))
            is FiltersUiState.Error -> MydoErrorState(title = "Unable to load filters", message = state.message, modifier = Modifier.fillMaxSize().padding(padding))
            is FiltersUiState.Ready -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(MydoSpacing.screenMargin)) {
                    items(state.filters, key = { it.id }) { filter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(Screen.FilterResults.createRoute(filter.id.toString())) }
                                .padding(vertical = MydoSpacing.small),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text((if (filter.favorite) "\u2605 " else "") + filter.name, style = MaterialTheme.typography.bodyLarge)
                                Text(filter.query, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                Text(
                                    text = if (filter.favorite) "Unstar" else "Star",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { viewModel.toggleFavorite(filter) }.padding(end = MydoSpacing.small),
                                )
                                Text("Edit", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { editingFilter = filter })
                            }
                        }
                    }
                    if (state.filters.isEmpty()) {
                        item { Text("No saved filters yet. Tap + to build one.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        FilterEditDialog(
            initialName = "",
            initialQuery = "",
            validate = viewModel.validateFilterQueryUseCase::invoke,
            onDismiss = { showCreateDialog = false },
            onSave = { name, query -> viewModel.createFilter(name, query); showCreateDialog = false },
        )
    }
    editingFilter?.let { filter ->
        FilterEditDialog(
            initialName = filter.name,
            initialQuery = filter.query,
            validate = viewModel.validateFilterQueryUseCase::invoke,
            onDismiss = { editingFilter = null },
            onSave = { name, query -> viewModel.updateFilter(filter.copy(name = name, query = query)); editingFilter = null },
            onDelete = { viewModel.deleteFilter(filter.id); editingFilter = null },
        )
    }
}

@Composable
private fun FilterEditDialog(
    initialName: String,
    initialQuery: String,
    validate: (String) -> String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var query by remember { mutableStateOf(initialQuery) }
    val queryError = remember(query) { if (query.isBlank()) null else validate(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (onDelete == null) "New filter" else "Edit filter") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Filter name") })
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("e.g. priority:1 due:overdue") },
                    isError = queryError != null,
                    supportingText = { queryError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete filter", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && queryError == null) onSave(name, query) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
