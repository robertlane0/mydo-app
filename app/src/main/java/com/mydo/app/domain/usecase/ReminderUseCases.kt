package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.domain.model.Reminder
import com.mydo.app.domain.model.ReminderType
import com.mydo.app.domain.repository.ReminderRepository
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Observes a task's reminders, newest trigger first. */
class ObserveRemindersUseCase(private val reminderRepository: ReminderRepository) {
    operator fun invoke(taskId: UUID): Flow<AppResult<List<Reminder>>> = reminderRepository.observeByTask(taskId)
}

/** Creates a reminder at an explicit absolute time, then arms its OS alarm. */
class CreateAbsoluteReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(taskId: UUID, triggerAtUtcMillis: Long): AppResult<Unit> {
        val result = reminderRepository.create(
            Reminder(id = UUID.randomUUID(), taskId = taskId, triggerAtUtcMillis = triggerAtUtcMillis, type = ReminderType.ABSOLUTE, enabled = true)
        )
        if (result is AppResult.Success) reminderAlarmCoordinator?.sync(taskId)
        return result
    }
}

/** Creates a reminder a fixed offset before the task's current due date, then arms its OS alarm. */
class CreateRelativeReminderUseCase(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(taskId: UUID, minutesBefore: Long): AppResult<Unit> {
        val task = when (val result = taskRepository.getById(taskId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(ValidationError("not_found", "Task not found"))
        }
        val dueAtUtcMillis = task.dueAtUtcMillis
            ?: return AppResult.Failure(ValidationError("no_due_date", "Set a due date before adding a reminder"))
        val result = reminderRepository.create(
            Reminder(
                id = UUID.randomUUID(),
                taskId = taskId,
                triggerAtUtcMillis = dueAtUtcMillis - minutesBefore * 60_000L,
                type = ReminderType.RELATIVE,
                enabled = true,
            )
        )
        if (result is AppResult.Success) reminderAlarmCoordinator?.sync(taskId)
        return result
    }
}

/** Updates a reminder, then re-syncs its task's OS alarms to match the new trigger/enabled state. */
class UpdateReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(reminder: Reminder): AppResult<Unit> {
        val result = reminderRepository.update(reminder)
        if (result is AppResult.Success) reminderAlarmCoordinator?.sync(reminder.taskId)
        return result
    }
}

/** Deletes a reminder and cancels its OS alarm, if one was armed. */
class DeleteReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> {
        val result = reminderRepository.delete(id)
        if (result is AppResult.Success) reminderAlarmCoordinator?.cancelReminder(id)
        return result
    }
}
