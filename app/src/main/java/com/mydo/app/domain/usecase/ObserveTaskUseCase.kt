package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveTaskUseCase(private val taskRepository: TaskRepository) {
    operator fun invoke(taskId: UUID): Flow<AppResult<Task?>> = taskRepository.observeById(taskId)
}

/** Updates a task, then re-syncs its reminders' OS alarms (a retitle, for example, changes
 *  the text a not-yet-fired reminder notification will show). */
class UpdateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(task: Task): AppResult<Unit> {
        val result = taskRepository.update(task)
        if (result is AppResult.Success) reminderAlarmCoordinator?.sync(task.id)
        return result
    }
}

/** Deletes a task. Any scheduled reminder alarms for it are cancelled first — the
 *  `reminders` table's `ON DELETE CASCADE` from tasks removes the reminder rows as a side
 *  effect of this delete, so they must be read and cancelled *before* the task is gone. */
class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> {
        reminderAlarmCoordinator?.cancelAll(id)
        return taskRepository.delete(id)
    }
}
