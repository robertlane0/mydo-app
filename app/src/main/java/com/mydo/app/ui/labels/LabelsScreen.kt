package com.mydo.app.ui.labels

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.Label
import com.mydo.app.ui.components.ColorPickerRow
import com.mydo.app.ui.components.LABEL_COLOR_PALETTE
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.navigation.Screen
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun LabelsScreen(viewModel: LabelsViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingLabel by remember { mutableStateOf<Label?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) { Text("+") }
        },
    ) { padding ->
        when (val state = uiState) {
            LabelsUiState.Loading -> MydoLoadingState(message = "Loading labels\u2026", modifier = Modifier.fillMaxSize().padding(padding))
            is LabelsUiState.Error -> MydoErrorState(title = "Unable to load labels", message = state.message, modifier = Modifier.fillMaxSize().padding(padding))
            is LabelsUiState.Ready -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(MydoSpacing.screenMargin)) {
                    items(state.labels, key = { it.id }) { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(Screen.LabelDetail.createRoute(label.id.toString())) }
                                .padding(vertical = MydoSpacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val color = runCatching { Color(android.graphics.Color.parseColor(label.color)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
                                Text(label.name, modifier = Modifier.padding(start = MydoSpacing.small))
                            }
                            Text("Edit", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { editingLabel = label })
                        }
                    }
                    if (state.labels.isEmpty()) {
                        item { Text("No labels yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        LabelEditDialog(
            initialName = "",
            initialColor = LABEL_COLOR_PALETTE.first().first,
            onDismiss = { showCreateDialog = false },
            onSave = { name, color -> viewModel.createLabel(name, color); showCreateDialog = false },
        )
    }
    editingLabel?.let { label ->
        LabelEditDialog(
            initialName = label.name,
            initialColor = label.color,
            onDismiss = { editingLabel = null },
            onSave = { name, color -> viewModel.updateLabel(label.copy(name = name, color = color)); editingLabel = null },
            onDelete = { viewModel.deleteLabel(label.id); editingLabel = null },
        )
    }
}

@Composable
private fun LabelEditDialog(
    initialName: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (onDelete == null) "New label" else "Edit label") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Label name") })
                ColorPickerRow(selected = color, onSelect = { color = it })
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete label", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name, color) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
