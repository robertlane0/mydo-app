package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.model.TaskSummary
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TaskRepository {
    fun observeInboxTasks(): Flow<AppResult<List<TaskSummary>>>
    fun observeTodayTasks(endOfDayUtcMillis: Long): Flow<AppResult<List<TaskSummary>>>
    fun observeProjectTasks(projectId: UUID): Flow<AppResult<List<TaskSummary>>>
    fun observeSectionTasks(sectionId: UUID): Flow<AppResult<List<TaskSummary>>>
    fun observeById(id: UUID): Flow<AppResult<Task?>>
    
    suspend fun getById(id: UUID): AppResult<Task?>
    suspend fun create(task: Task): AppResult<Unit>
    suspend fun update(task: Task): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    
    suspend fun updateCompletion(id: UUID, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun moveToProject(id: UUID, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun clearSection(sectionId: UUID, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun search(query: String): AppResult<List<TaskSummary>>
}
