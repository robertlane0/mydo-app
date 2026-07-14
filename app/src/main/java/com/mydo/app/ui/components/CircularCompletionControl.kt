package com.mydo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mydo.app.domain.model.Priority
import com.mydo.app.ui.theme.LocalPriorityColors
import com.mydo.app.ui.theme.MydoSpacing

@Composable
fun CircularCompletionControl(
    completed: Boolean,
    priority: Priority,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val priorityColor = LocalPriorityColors.current.colorFor(priority)
    val fillColor = if (completed) priorityColor else Color.Transparent
    val checkColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(MydoSpacing.minimumTouchTarget)
            .semantics {
                role = Role.Checkbox
                contentDescription = if (completed) "Mark task incomplete" else "Complete task"
            }
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(fillColor)
                .border(width = 2.dp, color = priorityColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (completed) {
                Text(text = "✓", color = checkColor, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
