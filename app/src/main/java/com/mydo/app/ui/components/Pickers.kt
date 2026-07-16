package com.mydo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Project
import com.mydo.app.ui.theme.LocalPriorityColors
import com.mydo.app.ui.theme.MydoSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
fun priorityColor(priority: Priority): Color = LocalPriorityColors.current.colorFor(priority)

fun priorityLabel(priority: Priority): String = when (priority) {
    Priority.P1 -> "Priority 1 (urgent)"
    Priority.P2 -> "Priority 2 (high)"
    Priority.P3 -> "Priority 3 (medium)"
    Priority.P4 -> "Priority 4 (none)"
}

/** Quick-pick + full calendar due date picker (used by Task Detail, Upcoming reschedule, and bulk due date). */
@Composable
fun DueDatePickerDialog(
    initialDateUtcMillis: Long?,
    zoneId: ZoneId = ZoneId.systemDefault(),
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    var showCalendar by remember { mutableStateOf(false) }
    val today = LocalDate.now(zoneId)

    fun atNoon(date: LocalDate): Long = date.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

    if (showCalendar) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateUtcMillis ?: atNoon(today),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        onConfirm(atNoon(date))
                    } else onConfirm(null)
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Due date") },
        text = {
            Column {
                QuickDateRow("Today", MaterialTheme.colorScheme.onSurface) { onConfirm(atNoon(today)) }
                QuickDateRow("Tomorrow", MaterialTheme.colorScheme.onSurface) { onConfirm(atNoon(today.plusDays(1))) }
                QuickDateRow("Next week", MaterialTheme.colorScheme.onSurface) { onConfirm(atNoon(today.plusWeeks(1))) }
                QuickDateRow("Pick a date\u2026", MaterialTheme.colorScheme.primary) { showCalendar = true }
                if (initialDateUtcMillis != null) {
                    QuickDateRow("No date", MaterialTheme.colorScheme.error) { onConfirm(null) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QuickDateRow(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MydoSpacing.small),
    )
}

@Composable
fun PriorityPickerDialog(current: Priority, onDismiss: () -> Unit, onSelect: (Priority) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Priority") },
        text = {
            Column {
                Priority.entries.forEach { priority ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(priority) }
                            .padding(vertical = MydoSpacing.small),
                    ) {
                        RadioButton(selected = priority == current, onClick = { onSelect(priority) })
                        Box(
                            modifier = Modifier
                                .padding(horizontal = MydoSpacing.small)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (priority == Priority.P4) MaterialTheme.colorScheme.outline else priorityColor(priority)),
                        )
                        Text(priorityLabel(priority))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** Simple project picker (no section drill-down) used for task creation and bulk "Move to". */
@Composable
fun ProjectPickerDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSelectInbox: () -> Unit,
    onSelectProject: (Project) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to") },
        text = {
            LazyColumn {
                item {
                    Text(
                        "Inbox",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSelectInbox)
                            .padding(vertical = MydoSpacing.small),
                    )
                    Divider()
                }
                items(projects, key = { it.id }) { project ->
                    Text(
                        project.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProject(project) }
                            .padding(vertical = MydoSpacing.small),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

val LABEL_COLOR_PALETTE = listOf(
    "#E53935" to "Red", "#FB8C00" to "Orange", "#FDD835" to "Yellow", "#43A047" to "Green",
    "#00897B" to "Teal", "#1E88E5" to "Blue", "#5E35B1" to "Indigo", "#8E24AA" to "Purple",
    "#D81B60" to "Pink", "#757575" to "Gray",
)

@Composable
fun ColorPickerRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MydoSpacing.small)) {
        LABEL_COLOR_PALETTE.forEach { (hex, _) ->
            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected.equals(hex, ignoreCase = true)) {
                    Text("\u2713", color = Color.White)
                }
            }
        }
    }
}

fun dayOfWeekShort(dayValue: Int): String =
    java.time.DayOfWeek.of(dayValue).getDisplayName(TextStyle.SHORT, Locale.getDefault())
