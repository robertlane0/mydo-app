package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SearchTasksUseCase(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(query: String): Flow<AppResult<List<TaskSummary>>> {
        if (query.isBlank()) {
            return flow { emit(AppResult.Success(emptyList())) }
        }
        return flow { emit(taskRepository.search(query)) }
    }
    
    operator fun invoke(query: String, completed: Boolean): Flow<AppResult<List<TaskSummary>>> {
        if (query.isBlank()) {
            return flow { emit(AppResult.Success(emptyList())) }
        }
        return flow { emit(taskRepository.searchWithCompletion(query, completed)) }
    }
}