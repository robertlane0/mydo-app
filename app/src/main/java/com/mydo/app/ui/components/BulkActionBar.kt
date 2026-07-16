package com.mydo.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mydo.app.ui.theme.MydoSpacing

/**
 * The bottom action bar shown while a task list is in selection mode
 * (specs17-bulk-operations.md, "Selection Toolbar"). [count] disables every action at 0
 * and shows the plain "Select tasks" prompt.
 */
@Composable
fun BulkActionBar(
    count: Int,
    onComplete: () -> Unit,
    onSetPriority: () -> Unit,
    onSetDueDate: () -> Unit,
    onMove: () -> Unit,
    onAddLabels: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MydoSpacing.medium, vertical = MydoSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (count == 0) "Select tasks" else "$count selected",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            BulkActionButton("Cancel", onCancel)
            if (count > 0) {
                BulkActionButton("Done", onComplete)
                BulkActionButton("Priority", onSetPriority)
                BulkActionButton("Date", onSetDueDate)
                BulkActionButton("Move", onMove)
                BulkActionButton("Labels", onAddLabels)
                BulkActionButton("Delete", onDelete, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun BulkActionButton(label: String, onClick: () -> Unit, color: Color? = null) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color ?: MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .wrapContentWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MydoSpacing.small, vertical = MydoSpacing.small),
    )
}
