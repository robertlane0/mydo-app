package com.mydo.app.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.mydo.app.domain.model.Priority
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun MydoTaskRow(
    title: String,
    completed: Boolean,
    priority: Priority,
    onClick: () -> Unit,
    onCompletionToggle: () -> Unit,
    modifier: Modifier = Modifier,
    metadata: String? = null,
    recurringSummary: String? = null,
    /** When set, a selection checkbox replaces the completion control (specs17-bulk-operations.md). */
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null,
    /** Long-press starts selection mode (specs17-bulk-operations.md, "Entering Selection Mode"). */
    onLongClick: (() -> Unit)? = null,
    /** Optional trailing slot — used for a drag handle in Manual sort mode. */
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MydoSpacing.minimumTouchTarget),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = MydoSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onSelectToggle?.invoke() })
            } else {
                CircularCompletionControl(
                    completed = completed,
                    priority = priority,
                    onToggle = onCompletionToggle,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MydoSpacing.medium),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                )
                if (metadata != null || recurringSummary != null) {
                    Spacer(modifier = Modifier.padding(top = MydoSpacing.extraSmall))
                    Row {
                        if (recurringSummary != null) {
                            Text(
                                text = "\u21BB $recurringSummary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = if (metadata != null) Modifier.padding(end = MydoSpacing.small) else Modifier,
                            )
                        }
                        if (metadata != null) {
                            Text(
                                text = metadata,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            trailing?.invoke()
        }
    }
}
