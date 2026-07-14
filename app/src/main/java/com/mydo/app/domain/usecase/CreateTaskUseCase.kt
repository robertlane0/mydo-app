package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.repository.TaskRepository
import java.util.UUID

class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(title: String, projectId: UUID? = null): AppResult<Unit> {
        val now = timeProvider.nowUtcMillis()
        val task = Task(
            id = UUID.randomUUID(),
            projectId = projectId,
            sectionId = null,
            parentTaskId = null,
            title = title,
            description = "",
            completed = false,
            priority = Priority.P4,
            dueAtUtcMillis = null,
            recurringRule = null,
            sortOrder = 0,
            createdAtUtcMillis = now,
            updatedAtUtcMillis = now,
            completedAtUtcMillis = null,
            labels = emptyList(),
            subtaskCount = 0,
            completedSubtaskCount = 0
        )
        return taskRepository.create(task)
    }
}
