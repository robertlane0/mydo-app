package com.mydo.app.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun LabelsScreen(
    viewModel: LabelsViewModel,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is LabelsUiState.Loading -> MydoLoadingState(
            message = "Loading labels…",
            modifier = Modifier.fillMaxSize()
        )

        is LabelsUiState.Error -> MydoErrorState(
            title = "Unable to load labels",
            message = state.message,
            modifier = Modifier.fillMaxSize()
        )

        is LabelsUiState.Ready -> {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MydoSpacing.screenMargin),
                    contentPadding = PaddingValues(vertical = MydoSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(MydoSpacing.small),
                ) {
                    if (state.labels.isEmpty()) {
                        item {
                            MydoEmptyState(
                                title = "No Labels Yet",
                                message = "Create labels to categorize your tasks across projects.",
                                actionLabel = "Create Label",
                                onAction = { /* TODO: Show create label dialog */ },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
                        items(state.labels, key = { it.id }) { label ->
                            LabelRow(
                                label = label,
                                onClick = { /* TODO: Open label tasks */ },
                                onEdit = { /* TODO: Show edit dialog */ },
                                onDelete = { /* TODO: Show delete confirmation */ },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabelRow(
    label: com.mydo.app.domain.model.Label,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MydoSpacing.extraSmall),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MydoSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(label.color.substring(1).toInt(16) + 0xFF000000)),
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(end = MydoSpacing.medium))
            
            // Label name
            Text(
                text = label.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            // Actions
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit label")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete label")
            }
        }
    }
}