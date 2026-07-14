package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.repository.TaskRepository
import java.util.UUID

class CompleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(taskId: UUID): AppResult<Task> {
        val taskResult = taskRepository.getById(taskId)
        return when (taskResult) {
            is AppResult.Failure -> taskResult
            is AppResult.Success -> {
                val task = taskResult.value
                if (task == null) {
                    AppResult.Failure(ValidationError("not_found", "Task not found"))
                } else if (task.completed) {
                    AppResult.Failure(ValidationError("already_completed", "Task already completed"))
                } else {
                    completeTask(task)
                }
            }
        }
    }

    private suspend fun completeTask(task: Task): AppResult<Task> {
        val now = timeProvider.nowUtcMillis()
        val completedTask = task.copy(
            completed = true,
            completedAtUtcMillis = now,
            updatedAtUtcMillis = now,
        )

        // Mark the task as completed
        val updateResult = taskRepository.updateCompletion(
            completedTask.id,
            true,
            now,
            now,
        )

        if (updateResult is AppResult.Failure) {
            return updateResult
        }

        // Check if this is a recurring task
        if (task.recurringRule != null) {
            // Generate next occurrence
            val nextDue = calculateNextDueDate(task.recurringRule!!, task.dueAtUtcMillis!!)
            
            if (nextDue != null) {
                // Check if we've reached the end of recurrence
                if (hasReachedRecurrenceEnd(task, nextDue)) {
                    return AppResult.Success(completedTask)
                }

                // Create next occurrence
                val nextTask = generateNextRecurringTask(completedTask, nextDue)
                val insertResult = taskRepository.create(nextTask)
                
                if (insertResult is AppResult.Failure) {
                    return insertResult
                }
            }
        }

        return AppResult.Success(completedTask)
    }

    private fun calculateNextDueDate(rrule: String, currentDue: Long): Long? {
        val parts = rrule.split(";").associate { 
            val (k, v) = it.split("=")
            k to v
        }
        
        val freq = parts["FREQ"] ?: return null
        val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
        val currentMs = currentDue
        val dayMs = 24L * 60 * 60 * 1000
        
        return when (freq) {
            "DAILY" -> currentMs + interval * dayMs
            "WEEKLY" -> currentMs + interval * 7 * dayMs
            "MONTHLY" -> addMonths(currentMs, interval)
            "YEARLY" -> addYears(currentMs, interval)
            else -> null
        }
    }

    private fun addMonths(timeMillis: Long, months: Int): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = timeMillis
        calendar.add(java.util.Calendar.MONTH, months)
        return calendar.timeInMillis
    }

    private fun addYears(timeMillis: Long, years: Int): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = timeMillis
        calendar.add(java.util.Calendar.YEAR, years)
        return calendar.timeInMillis
    }

    private fun hasReachedRecurrenceEnd(task: Task, nextDue: Long): Boolean {
        val rrule = task.recurringRule!!
        val parts = rrule.split(";").associate { 
            val (k, v) = it.split("=")
            k to v
        }
        
        // Check UNTIL
        parts["UNTIL"]?.let { untilStr ->
            try {
                val year = untilStr.substring(0, 4).toInt()
                val month = untilStr.substring(4, 6).toInt() - 1
                val day = untilStr.substring(6, 8).toInt()
                val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                calendar.set(year, month, day, 23, 59, 59)
                calendar.set(java.util.Calendar.MILLISECOND, 999)
                
                if (nextDue > calendar.timeInMillis) {
                    return true
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        
        return false
    }

    private fun generateNextRecurringTask(completedTask: Task, nextDue: Long): Task {
        val sortOrder = if (completedTask.sectionId != null) {
            // In a real implementation, get the next sort order for the section
            0
        } else if (completedTask.projectId != null) {
            // In a real implementation, get the next sort order for the project
            0
        } else {
            // In a real implementation, get the next sort order for inbox
            0
        }
        
        return Task(
            id = java.util.UUID.randomUUID(),
            projectId = completedTask.projectId,
            sectionId = completedTask.sectionId,
            parentTaskId = null,
            title = completedTask.title,
            description = completedTask.description,
            completed = false,
            priority = completedTask.priority,
            dueAtUtcMillis = nextDue,
            recurringRule = completedTask.recurringRule,
            sortOrder = sortOrder,
            createdAtUtcMillis = timeProvider.nowUtcMillis(),
            updatedAtUtcMillis = timeProvider.nowUtcMillis(),
            completedAtUtcMillis = null,
            labels = completedTask.labels,
            subtaskCount = 0,
            completedSubtaskCount = 0,
        )
    }
}