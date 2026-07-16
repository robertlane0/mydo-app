package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.recurrence.RecurrenceCalculator
import com.mydo.app.domain.recurrence.RecurrenceRuleParser
import com.mydo.app.domain.repository.TaskRepository
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Applies a recurrence rule to a task (specs16-recurring-tasks.md, "Recurrence Editor").
 * A recurring task needs an anchor date to compute future occurrences from; if the task
 * has no due date yet, one is defaulted to today so recurrence has somewhere to start —
 * the spec doesn't cover this case explicitly, so this is this codebase's convention.
 */
class SetRecurrenceUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend operator fun invoke(taskId: UUID, ruleString: String): AppResult<Unit> {
        RecurrenceRuleParser.validate(ruleString)?.let { message ->
            return AppResult.Failure(ValidationError("invalid_recurrence_rule", message))
        }
        val task = when (val result = taskRepository.getById(taskId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(ValidationError("not_found", "Task not found"))
        }
        val now = timeProvider.nowUtcMillis()
        val dueAtUtcMillis = task.dueAtUtcMillis ?: run {
            val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
            today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        }

        return taskRepository.update(
            task.copy(
                recurringRule = ruleString,
                recurrenceAnchorUtcMillis = dueAtUtcMillis,
                dueAtUtcMillis = dueAtUtcMillis,
                updatedAtUtcMillis = now,
            )
        )
    }
}

/** Clears recurrence from a task, leaving it as a normal one-off task (specs16, "Remove Recurrence"). */
class RemoveRecurrenceUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(taskId: UUID): AppResult<Unit> =
        taskRepository.updateRecurrence(taskId, null, timeProvider.nowUtcMillis())
}

/**
 * Advances a recurring task to its next scheduled date without completing it or
 * generating a separate row (specs16, "Skip Occurrence"). Occurrence count still
 * advances so COUNT-limited series terminate on schedule.
 */
class SkipNextOccurrenceUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend operator fun invoke(taskId: UUID): AppResult<Unit> {
        val task = when (val result = taskRepository.getById(taskId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(ValidationError("not_found", "Task not found"))
        }
        val rawRule = task.recurringRule ?: return AppResult.Failure(ValidationError("not_recurring", "Task is not recurring"))
        val rule = try {
            RecurrenceRuleParser.parse(rawRule)
        } catch (e: com.mydo.app.domain.recurrence.RecurrenceRuleException) {
            return AppResult.Failure(ValidationError("invalid_recurrence_rule", e.message ?: "Invalid recurrence rule"))
        }

        val anchorMillis = task.recurrenceAnchorUtcMillis ?: task.dueAtUtcMillis
            ?: return AppResult.Failure(ValidationError("no_due_date", "Task has no due date to skip from"))
        val anchorDate = Instant.ofEpochMilli(anchorMillis).atZone(zoneId).toLocalDate()
        val nextDate = RecurrenceCalculator.nextOccurrence(rule, anchorDate)
        if (!RecurrenceCalculator.canGenerateNext(rule, task.occurrenceNumber, nextDate)) {
            return AppResult.Failure(ValidationError("series_ended", "This recurring series has ended"))
        }
        val timeOfDay = Instant.ofEpochMilli(task.dueAtUtcMillis ?: anchorMillis).atZone(zoneId).toLocalTime()
        val nextDueMillis = nextDate.atTime(timeOfDay).atZone(zoneId).toInstant().toEpochMilli()
        val now = timeProvider.nowUtcMillis()

        return taskRepository.update(
            task.copy(
                dueAtUtcMillis = nextDueMillis,
                recurrenceAnchorUtcMillis = nextDueMillis,
                occurrenceNumber = task.occurrenceNumber + 1,
                updatedAtUtcMillis = now,
            )
        )
    }
}

/**
 * Reschedules the *current* occurrence's due date only — drag/date-picker on a recurring
 * task must not shift the anchor, so future occurrences still land on the original
 * schedule (specs16, "Reschedule Current").
 */
class RescheduleTaskUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(taskId: UUID, dueAtUtcMillis: Long?): AppResult<Unit> =
        taskRepository.updateDueDate(taskId, dueAtUtcMillis, timeProvider.nowUtcMillis())
}
