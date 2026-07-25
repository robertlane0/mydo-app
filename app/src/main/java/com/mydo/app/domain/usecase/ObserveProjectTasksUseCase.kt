package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** All incomplete tasks in a project, across every section (specs06-projects.md). */
class ObserveProjectTasksUseCase(private val taskRepository: TaskRepository) {
    operator fun invoke(projectId: UUID): Flow<AppResult<List<TaskSummary>>> = taskRepository.observeProjectTasks(projectId)
}
