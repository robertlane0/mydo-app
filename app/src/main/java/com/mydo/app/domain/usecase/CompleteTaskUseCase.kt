package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.Reminder
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.recurrence.RecurrenceCalculator
import com.mydo.app.domain.recurrence.RecurrenceRuleException
import com.mydo.app.domain.recurrence.RecurrenceRuleParser
import com.mydo.app.domain.repository.ReminderRepository
import com.mydo.app.domain.repository.TaskRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Completes a task and, if it recurs, generates the next occurrence in the same local
 * transaction-like sequence (specs16-recurring-tasks.md, "Completion Behavior"). Only
 * active tasks generate a next occurrence, and only while COUNT/UNTIL allow it.
 */
class CompleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val timeProvider: TimeProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    /** [generatedTask] is non-null iff a next occurrence was created; the caller offers Undo either way. */
    data class Outcome(val completedTask: Task, val generatedTask: Task?)

    suspend operator fun invoke(taskId: UUID): AppResult<Outcome> {
        val existing = when (val result = taskRepository.getById(taskId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(ValidationError("not_found", "Task not found"))
        }
        if (existing.completed) return AppResult.Success(Outcome(existing, null))

        val now = timeProvider.nowUtcMillis()
        when (val result = taskRepository.updateCompletion(taskId, true, now, now)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> Unit
        }
        val completedTask = existing.copy(completed = true, completedAtUtcMillis = now, updatedAtUtcMillis = now)

        val rawRule = existing.recurringRule ?: return AppResult.Success(Outcome(completedTask, null))
        val generated = generateNextOccurrence(existing, rawRule, now)
        if (generated != null) {
            when (val createResult = taskRepository.create(generated)) {
                is AppResult.Failure -> return AppResult.Success(Outcome(completedTask, null)) // spec: log + toast, task stays complete
                is AppResult.Success -> Unit
            }
            copyReminders(existing.id, generated.id, generated.dueAtUtcMillis, existing.dueAtUtcMillis)
        }
        return AppResult.Success(Outcome(completedTask, generated))
    }

    private suspend fun generateNextOccurrence(task: Task, rawRule: String, now: Long): Task? {
        val rule = try {
            RecurrenceRuleParser.parse(rawRule)
        } catch (e: RecurrenceRuleException) {
            return null // "Next date calculation fails" -> complete without generating
        }
        val anchorMillis = task.recurrenceAnchorUtcMillis ?: task.dueAtUtcMillis ?: return null
        val anchorDate = Instant.ofEpochMilli(anchorMillis).atZone(zoneId).toLocalDate()
        val nextDate = try {
            RecurrenceCalculator.nextOccurrence(rule, anchorDate)
        } catch (e: Exception) {
            return null
        }
        if (!RecurrenceCalculator.canGenerateNext(rule, task.occurrenceNumber, nextDate)) return null

        val timeOfDay = Instant.ofEpochMilli(task.dueAtUtcMillis ?: anchorMillis).atZone(zoneId).toLocalTime()
        val nextDueMillis = nextDate.atTime(timeOfDay).atZone(zoneId).toInstant().toEpochMilli()

        return task.copy(
            id = UUID.randomUUID(),
            completed = false,
            completedAtUtcMillis = null,
            dueAtUtcMillis = nextDueMillis,
            recurrenceAnchorUtcMillis = nextDueMillis,
            occurrenceNumber = task.occurrenceNumber + 1,
            previousOccurrenceTaskId = task.id,
            createdAtUtcMillis = now,
            updatedAtUtcMillis = now,
            // Attachments are not copied (specs16, "Business Rules"); subtasks are not
            // recurring by default, so subtaskCount is left at 0 for the new occurrence.
            subtaskCount = 0,
            completedSubtaskCount = 0,
        )
    }

    private suspend fun copyReminders(fromTaskId: UUID, toTaskId: UUID, newDueAtUtcMillis: Long?, oldDueAtUtcMillis: Long?) {
        val offsetShift = if (newDueAtUtcMillis != null && oldDueAtUtcMillis != null) newDueAtUtcMillis - oldDueAtUtcMillis else 0L
        val reminders = (reminderRepository.getByTask(fromTaskId) as? AppResult.Success)?.value.orEmpty()
        reminders.forEach { reminder ->
            reminderRepository.create(
                Reminder(
                    id = UUID.randomUUID(),
                    taskId = toTaskId,
                    triggerAtUtcMillis = reminder.triggerAtUtcMillis + offsetShift,
                    type = reminder.type,
                    enabled = reminder.enabled,
                )
            )
        }
    }
}

/**
 * Reverses [CompleteTaskUseCase] within the undo window (specs16, "Undo Completion"):
 * deletes the generated next occurrence, if any, and restores the original to active.
 */
class UndoCompleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(outcome: CompleteTaskUseCase.Outcome): AppResult<Unit> {
        outcome.generatedTask?.let { generated ->
            when (val result = taskRepository.delete(generated.id)) {
                is AppResult.Failure -> return result
                is AppResult.Success -> Unit
            }
        }
        return taskRepository.updateCompletion(
            outcome.completedTask.id,
            completed = false,
            completedAtUtcMillis = null,
            updatedAtUtcMillis = timeProvider.nowUtcMillis(),
        )
    }
}
