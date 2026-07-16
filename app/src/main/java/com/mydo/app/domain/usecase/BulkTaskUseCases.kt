package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.repository.ReminderRepository
import com.mydo.app.domain.repository.TaskRepository
import java.util.UUID

/** specs17-bulk-operations.md, "Maximum Selection": prevents accidental mass operations. */
const val MAX_BULK_SELECTION = 500

/**
 * Full pre-operation state of the affected tasks, enough to reverse a Move/Priority/Due
 * Date/Complete bulk action with a single write per task (specs17, "Undo System").
 * [generatedOccurrenceIds] additionally tracks any next-occurrence rows a bulk Complete
 * created, which undo must delete on top of restoring the snapshot.
 */
data class BulkActionOutcome(
    val affectedCount: Int,
    val snapshot: List<Task>,
    val generatedOccurrenceIds: List<UUID> = emptyList(),
)

class BulkSetPriorityUseCase(private val taskRepository: TaskRepository, private val timeProvider: TimeProvider) {
    suspend operator fun invoke(taskIds: List<UUID>, priority: Priority): AppResult<BulkActionOutcome> {
        val before = when (val result = taskRepository.getByIds(taskIds)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        val now = timeProvider.nowUtcMillis()
        before.forEach { task -> taskRepository.updatePriority(task.id, priority, now) }
        return AppResult.Success(BulkActionOutcome(before.size, before))
    }
}

class BulkSetDueDateUseCase(private val taskRepository: TaskRepository, private val timeProvider: TimeProvider) {
    suspend operator fun invoke(taskIds: List<UUID>, dueAtUtcMillis: Long?): AppResult<BulkActionOutcome> {
        val before = when (val result = taskRepository.getByIds(taskIds)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        val now = timeProvider.nowUtcMillis()
        before.forEach { task -> taskRepository.updateDueDate(task.id, dueAtUtcMillis, now) }
        return AppResult.Success(BulkActionOutcome(before.size, before))
    }
}

class BulkMoveTasksUseCase(private val taskRepository: TaskRepository, private val timeProvider: TimeProvider) {
    suspend operator fun invoke(taskIds: List<UUID>, projectId: UUID?, sectionId: UUID?): AppResult<BulkActionOutcome> {
        val before = when (val result = taskRepository.getByIds(taskIds)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        val now = timeProvider.nowUtcMillis()
        before.forEach { task -> taskRepository.moveToProject(task.id, projectId, sectionId, now) }
        return AppResult.Success(BulkActionOutcome(before.size, before))
    }
}

/** specs17, "Complete": each recurring task in the batch generates its next occurrence, same as a single completion. */
class BulkCompleteTasksUseCase(
    taskRepository: TaskRepository,
    reminderRepository: ReminderRepository,
    timeProvider: TimeProvider,
) {
    private val completeTaskUseCase = CompleteTaskUseCase(taskRepository, reminderRepository, timeProvider)

    suspend operator fun invoke(taskIds: List<UUID>): AppResult<BulkActionOutcome> {
        val snapshot = mutableListOf<Task>()
        val generated = mutableListOf<UUID>()
        for (id in taskIds) {
            when (val result = completeTaskUseCase(id)) {
                is AppResult.Failure -> return result
                is AppResult.Success -> {
                    snapshot += result.value.completedTask.copy(completed = false, completedAtUtcMillis = null)
                    result.value.generatedTask?.let { generated += it.id }
                }
            }
        }
        return AppResult.Success(BulkActionOutcome(snapshot.size, snapshot, generated))
    }
}

/** specs17, "Delete": destructive, no automated undo — the confirmation dialog *is* the safeguard. */
class BulkDeleteTasksUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(taskIds: List<UUID>): AppResult<Int> {
        var deleted = 0
        for (id in taskIds) {
            when (taskRepository.delete(id)) {
                is AppResult.Failure -> Unit // best-effort: continue deleting the rest
                is AppResult.Success -> deleted++
            }
        }
        return AppResult.Success(deleted)
    }
}

/** Reverses Move/Priority/Due Date/Complete bulk actions (specs17, "Undo Scope"). */
class UndoBulkTaskOperationUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(outcome: BulkActionOutcome): AppResult<Unit> {
        outcome.generatedOccurrenceIds.forEach { taskRepository.delete(it) }
        outcome.snapshot.forEach { taskRepository.update(it) }
        return AppResult.Success(Unit)
    }
}
