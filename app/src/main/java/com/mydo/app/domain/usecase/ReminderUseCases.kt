package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.domain.model.Reminder
import com.mydo.app.domain.model.ReminderType
import com.mydo.app.domain.repository.ReminderRepository
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Observes a task's reminders, newest trigger first (data only — step 5 schedules the OS alarm). */
class ObserveRemindersUseCase(private val reminderRepository: ReminderRepository) {
    operator fun invoke(taskId: UUID): Flow<AppResult<List<Reminder>>> = reminderRepository.observeByTask(taskId)
}

/** Creates a reminder at an explicit absolute time. */
class CreateAbsoluteReminderUseCase(private val reminderRepository: ReminderRepository) {
    suspend operator fun invoke(taskId: UUID, triggerAtUtcMillis: Long): AppResult<Unit> =
        reminderRepository.create(
            Reminder(id = UUID.randomUUID(), taskId = taskId, triggerAtUtcMillis = triggerAtUtcMillis, type = ReminderType.ABSOLUTE, enabled = true)
        )
}

/** Creates a reminder a fixed offset before the task's current due date. */
class CreateRelativeReminderUseCase(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
) {
    suspend operator fun invoke(taskId: UUID, minutesBefore: Long): AppResult<Unit> {
        val task = when (val result = taskRepository.getById(taskId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(ValidationError("not_found", "Task not found"))
        }
        val dueAtUtcMillis = task.dueAtUtcMillis
            ?: return AppResult.Failure(ValidationError("no_due_date", "Set a due date before adding a reminder"))
        return reminderRepository.create(
            Reminder(
                id = UUID.randomUUID(),
                taskId = taskId,
                triggerAtUtcMillis = dueAtUtcMillis - minutesBefore * 60_000L,
                type = ReminderType.RELATIVE,
                enabled = true,
            )
        )
    }
}

class UpdateReminderUseCase(private val reminderRepository: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder): AppResult<Unit> = reminderRepository.update(reminder)
}

class DeleteReminderUseCase(private val reminderRepository: ReminderRepository) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> = reminderRepository.delete(id)
}
