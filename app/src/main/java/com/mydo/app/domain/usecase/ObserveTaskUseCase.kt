package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveTaskUseCase(private val taskRepository: TaskRepository) {
    operator fun invoke(taskId: UUID): Flow<AppResult<Task?>> = taskRepository.observeById(taskId)
}

class UpdateTaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(task: Task): AppResult<Unit> = taskRepository.update(task)
}

class DeleteTaskUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> = taskRepository.delete(id)
}
