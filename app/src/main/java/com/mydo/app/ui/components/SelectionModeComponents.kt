package com.mydo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydo.app.ui.theme.MydoSpacing
import java.util.UUID

@Composable
fun BulkActionToolbar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onMove: () -> Unit,
    onAddLabels: () -> Unit,
    onSetPriority: () -> Unit,
    onSetDueDate: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MydoSpacing.small),
            verticalArrangement = Arrangement.spacedBy(MydoSpacing.extraSmall),
        ) {
            // Count and clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Text(
                    text = "$selectedCount selected",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                )
                androidx.compose.material3.Text(
                    text = "Clear",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = MydoSpacing.medium)
                        .then(androidx.compose.foundation.layout.clickable(onClick = onClearSelection))
                )
            }

            // Primary actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MydoSpacing.small),
            ) {
                BulkActionButton(text = "Move", onClick = onMove)
                BulkActionButton(text = "Labels", onClick = onAddLabels)
                BulkActionButton(text = "Priority", onClick = onSetPriority)
                BulkActionButton(text = "Due Date", onClick = onSetDueDate)
            }

            // Secondary actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MydoSpacing.small),
            ) {
                BulkActionButton(text = "Complete", onClick = onComplete, isDestructive = false)
                BulkActionButton(text = "Delete", onClick = onDelete, isDestructive = true)
                androidx.compose.material3.IconButton(onClick = onMore) {
                    Icon(androidx.compose.material.icons.filled.MoreVert, contentDescription = "More actions")
                }
            }
        }
    }
}

@Composable
private fun BulkActionButton(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val colors = if (isDestructive) {
        androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
fun SelectionModeWrapper(
    isInSelectionMode: Boolean,
    selectedItems: Set<java.util.UUID>,
    onItemClick: (java.util.UUID) -> Unit,
    onItemLongClick: (java.util.UUID) -> Unit,
    onSelectionChange: (Set<java.util.UUID>) -> Unit,
    onClearSelection: () -> Unit,
    onMove: () -> Unit,
    onAddLabels: () -> Unit,
    onSetPriority: () -> Unit,
    onSetDueDate: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    content: @Composable () -> Unit,
) {
    var localSelectedItems by remember { mutableStateOf(selectedItems) }
    
    if (isInSelectionMode) {
        Column {
            BulkActionToolbar(
                selectedCount = localSelectedItems.size,
                onClearSelection = {
                    onClearSelection()
                    localSelectedItems = emptySet()
                },
                onMove = onMove,
                onAddLabels = onAddLabels,
                onSetPriority = onSetPriority,
                onSetDueDate = onSetDueDate,
                onComplete = onComplete,
                onDelete = onDelete,
                onMore = onMore,
            )
            content()
        }
    } else {
        content()
    }
}