package com.mydo.app.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class UpcomingViewModel(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val uiState: Flow<UpcomingUiState> = taskRepository.observeAllScheduledTasks()
        .map { result ->
            when (result) {
                is AppResult.Failure -> UpcomingUiState.Error(result.error.userMessage)
                is AppResult.Success -> {
                    val now = timeProvider.nowUtcMillis()
                    val groups = groupTasksByDate(result.value, now)
                    UpcomingUiState.Ready(groups)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UpcomingUiState.Loading,
        )

    class Factory(
        private val taskRepository: TaskRepository,
        private val timeProvider: TimeProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UpcomingViewModel::class.java)) {
                return UpcomingViewModel(taskRepository, timeProvider) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private fun groupTasksByDate(tasks: List<TaskSummary>, nowUtcMillis: Long): List<UpcomingDateGroupModel> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        
        val groups = mutableMapOf<String, MutableList<UpcomingTaskItem>>()
        
        for (task in tasks) {
            val due = task.dueAtUtcMillis ?: continue
            
            calendar.timeInMillis = due
            val dateKey = dateFormat.format(calendar.time)
            val isOverdue = due < nowUtcMillis && !task.completed
            
            val groupKey = if (isOverdue) "overdue" else dateKey
            
            val displayTitle = if (isOverdue) {
                "Overdue"
            } else {
                val today = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                today.timeInMillis = nowUtcMillis
                val todayKey = dateFormat.format(today.time)
                
                val tomorrow = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                tomorrow.timeInMillis = nowUtcMillis + 24 * 60 * 60 * 1000
                val tomorrowKey = dateFormat.format(tomorrow.time)
                
                when (dateKey) {
                    todayKey -> "Today"
                    tomorrowKey -> "Tomorrow"
                    else -> displayFormat.format(calendar.time)
                }
            }
            
            val item = UpcomingTaskItem(
                id = task.id.toString(),
                title = task.title,
                projectPath = task.projectPath,
                priority = task.priority,
                completed = task.completed,
                dueAtUtcMillis = due,
            )
            
            groups.getOrPut(groupKey) { mutableListOf() }.add(item)
        }
        
        // Sort groups: overdue first, then by date
        val sortedKeys = groups.keys.toMutableList().apply {
            sortWith(compareByDescending<String> { it == "overdue" }.thenBy { it })
        }
        
        return sortedKeys.map { key ->
            val tasks = groups[key]!!
            val headerTitle = if (key == "overdue") "Overdue" else {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = tasks.first().dueAtUtcMillis!!
                displayFormat.format(calendar.time)
            }
            UpcomingDateGroupModel(
                dateKey = key,
                headerTitle = headerTitle,
                tasks = tasks,
            )
        }
    }
}

sealed interface UpcomingUiState {
    data object Loading : UpcomingUiState
    data class Ready(val groups: List<UpcomingDateGroupModel>) : UpcomingUiState
    data class Error(val message: String) : UpcomingUiState
}

data class UpcomingDateGroupModel(
    val dateKey: String,
    val headerTitle: String,
    val tasks: List<UpcomingTaskItem>,
)

data class UpcomingTaskItem(
    val id: String,
    val title: String,
    val projectPath: String?,
    val priority: com.mydo.app.domain.model.Priority,
    val completed: Boolean,
    val dueAtUtcMillis: Long?,
)