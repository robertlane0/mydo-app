package com.mydo.app.ui.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.ui.components.DueDatePickerDialog
import com.mydo.app.ui.components.MydoEmptyState
import com.mydo.app.ui.components.MydoErrorState
import com.mydo.app.ui.components.MydoLoadingState
import com.mydo.app.ui.components.MydoTaskRow
import com.mydo.app.ui.components.TaskComposerViewModel
import com.mydo.app.ui.theme.MydoSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Composable
fun UpcomingScreen(
    viewModel: UpcomingViewModel,
    composerViewModel: TaskComposerViewModel,
    navController: NavController,
    onRequestAddTask: (presetDueAtUtcMillis: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var rescheduleTaskId by remember { mutableStateOf<UUID?>(null) }

    when (val state = uiState) {
        UpcomingUiState.Loading -> MydoLoadingState(message = "Loading your schedule\u2026", modifier = Modifier.fillMaxSize())
        is UpcomingUiState.Error -> MydoErrorState(title = "Unable to load Upcoming", message = state.message, modifier = Modifier.fillMaxSize())
        is UpcomingUiState.Ready -> {
            if (state.overdue.isEmpty() && state.days.all { it.tasks.isEmpty() }) {
                MydoEmptyState(
                    title = "Nothing scheduled",
                    message = "Tasks with due dates will show up here.",
                    actionLabel = null,
                    onAction = {},
                    modifier = Modifier.fillMaxSize(),
                )
                return
            }

            val listState = rememberLazyListState()
            val nearBottom by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = listState.layoutInfo.totalItemsCount
                    total > 0 && lastVisible >= total - 5
                }
            }
            LaunchedEffect(nearBottom) { if (nearBottom) viewModel.loadMore() }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = MydoSpacing.small),
            ) {
                if (state.overdue.isNotEmpty()) {
                    item { GroupHeader(title = "Overdue", isOverdue = true) }
                    items(state.overdue, key = { "overdue-${it.id}" }) { task ->
                        UpcomingTaskRow(task, navController) { rescheduleTaskId = task.id }
                    }
                }
                state.days.forEach { day ->
                    item(key = "header-${day.date}") {
                        GroupHeader(
                            title = dayLabel(day.date),
                            isOverdue = false,
                            onAdd = { onRequestAddTask(day.date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()) },
                        )
                    }
                    items(day.tasks, key = { "d-${day.date}-${it.id}" }) { task ->
                        UpcomingTaskRow(task, navController) { rescheduleTaskId = task.id }
                    }
                }
            }
        }
    }

    rescheduleTaskId?.let { taskId ->
        DueDatePickerDialog(
            initialDateUtcMillis = null,
            onDismiss = { rescheduleTaskId = null },
            onConfirm = { millis ->
                if (millis != null) {
                    val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    viewModel.reschedule(taskId, date)
                }
                rescheduleTaskId = null
            },
        )
    }
}

@Composable
private fun UpcomingTaskRow(task: TaskSummary, navController: NavController, onLongClick: () -> Unit) {
    MydoTaskRow(
        title = task.title,
        completed = task.completed,
        priority = task.priority,
        metadata = task.projectPath,
        onClick = { navController.navigate("taskDetail/${task.id}") },
        onLongClick = onLongClick,
        onCompletionToggle = { },
        modifier = Modifier.padding(horizontal = MydoSpacing.screenMargin),
    )
}

@Composable
private fun GroupHeader(title: String, isOverdue: Boolean, onAdd: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = MydoSpacing.screenMargin, vertical = MydoSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (onAdd != null) {
            Text("+", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAdd))
        }
    }
}

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today \u00b7 ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
        today.plusDays(1) -> "Tomorrow \u00b7 ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
        else -> "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
    }
}
