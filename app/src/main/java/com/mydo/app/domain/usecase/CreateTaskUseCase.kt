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
    suspend operator fun invoke(
        title: String,
        projectId: UUID? = null,
        sectionId: UUID? = null,
        dueAtUtcMillis: Long? = null,
        priority: Priority = Priority.P4,
    ): AppResult<Unit> {
        val now = timeProvider.nowUtcMillis()
        val task = Task(
            id = UUID.randomUUID(),
            projectId = projectId,
            sectionId = sectionId,
            parentTaskId = null,
            title = title,
            description = "",
            completed = false,
            priority = priority,
            dueAtUtcMillis = dueAtUtcMillis,
            recurringRule = null,
            recurrenceAnchorUtcMillis = null,
            occurrenceNumber = 1,
            previousOccurrenceTaskId = null,
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
