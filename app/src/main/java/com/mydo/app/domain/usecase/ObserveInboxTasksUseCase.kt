package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class ObserveInboxTasksUseCase(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(): Flow<AppResult<List<TaskSummary>>> {
        return taskRepository.observeInboxTasks()
    }
}
