package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.LabelRepository
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveLabelsUseCase(private val labelRepository: LabelRepository) {
    operator fun invoke(): Flow<AppResult<List<Label>>> = labelRepository.observeAll()
}

class ObserveTaskLabelsUseCase(private val labelRepository: LabelRepository) {
    operator fun invoke(taskId: UUID): Flow<AppResult<List<Label>>> = labelRepository.observeByTask(taskId)
}

class CreateLabelUseCase(
    private val labelRepository: LabelRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(name: String, color: String): AppResult<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AppResult.Failure(ValidationError("blank_name", "Give this label a name"))
        when (val existing = labelRepository.findByName(trimmed)) {
            is AppResult.Failure -> return existing
            is AppResult.Success -> if (existing.value != null) {
                return AppResult.Failure(ValidationError("duplicate_name", "A label named \"$trimmed\" already exists"))
            }
        }
        return labelRepository.create(Label(id = UUID.randomUUID(), name = trimmed, color = color, createdAtUtcMillis = timeProvider.nowUtcMillis()))
    }
}

class UpdateLabelUseCase(private val labelRepository: LabelRepository) {
    suspend operator fun invoke(label: Label): AppResult<Unit> {
        val trimmed = label.name.trim()
        if (trimmed.isEmpty()) return AppResult.Failure(ValidationError("blank_name", "Give this label a name"))
        when (val existing = labelRepository.findByName(trimmed)) {
            is AppResult.Failure -> return existing
            is AppResult.Success -> if (existing.value != null && existing.value.id != label.id) {
                return AppResult.Failure(ValidationError("duplicate_name", "A label named \"$trimmed\" already exists"))
            }
        }
        return labelRepository.update(label.copy(name = trimmed))
    }
}

class DeleteLabelUseCase(private val labelRepository: LabelRepository) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> = labelRepository.delete(id)
}

class AssignLabelUseCase(private val labelRepository: LabelRepository) {
    suspend operator fun invoke(taskId: UUID, labelId: UUID): AppResult<Unit> = labelRepository.assignToTask(taskId, labelId)
}

class UnassignLabelUseCase(private val labelRepository: LabelRepository) {
    suspend operator fun invoke(taskId: UUID, labelId: UUID): AppResult<Unit> = labelRepository.unassignFromTask(taskId, labelId)
}

/** Every task carrying a given label, for the label's task-list screen (specs13, "Label Detail"). */
class ObserveTasksForLabelUseCase(
    private val taskRepository: TaskRepository,
    private val labelRepository: LabelRepository,
) {
    suspend operator fun invoke(labelId: UUID): AppResult<List<TaskSummary>> {
        val label = when (val result = labelRepository.getById(labelId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(ValidationError("not_found", "Label not found"))
        }
        val contexts = when (val result = taskRepository.getFilterContexts()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        val labelNameLower = label.name.lowercase()
        val tasks = contexts
            .filter { it.labelNames.contains(labelNameLower) }
            .sortedWith(compareBy({ it.completed }, { it.dueAtUtcMillis ?: Long.MAX_VALUE }))
            .map {
                TaskSummary(
                    id = it.taskId,
                    title = it.title,
                    completed = it.completed,
                    priority = it.priority,
                    dueAtUtcMillis = it.dueAtUtcMillis,
                    projectPath = it.projectName,
                    recurring = it.recurring,
                )
            }
        return AppResult.Success(tasks)
    }
}
