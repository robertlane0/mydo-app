package com.mydo.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MydoSpacing.minimumTouchTarget),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(vertical = MydoSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularCompletionControl(
                completed = completed,
                priority = priority,
                onToggle = onCompletionToggle,
            )
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
                if (metadata != null) {
                    Spacer(modifier = Modifier.padding(top = MydoSpacing.extraSmall))
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
