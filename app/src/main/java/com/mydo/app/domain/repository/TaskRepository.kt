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
    fun observeAllScheduledTasks(): Flow<AppResult<List<TaskSummary>>>
    fun observeScheduledTasksFrom(startUtcMillis: Long): Flow<AppResult<List<TaskSummary>>>
    fun observeOverdueTasks(nowUtcMillis: Long): Flow<AppResult<List<TaskSummary>>>
    fun observeRecurringTasks(): Flow<AppResult<List<TaskSummary>>>
    
    suspend fun getById(id: UUID): AppResult<Task?>
    suspend fun create(task: Task): AppResult<Unit>
    suspend fun update(task: Task): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    
    suspend fun updateCompletion(id: UUID, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun moveToProject(id: UUID, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun clearSection(sectionId: UUID, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun search(query: String): AppResult<List<TaskSummary>>
    suspend fun searchWithCompletion(query: String, completed: Boolean): AppResult<List<TaskSummary>>
    
    // Bulk operations
    suspend fun bulkMoveToProject(ids: List<UUID>, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun bulkSetPriority(ids: List<UUID>, priority: String, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun bulkSetDueDate(ids: List<UUID>, dueAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun bulkComplete(ids: List<UUID>, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun bulkDelete(ids: List<UUID>): AppResult<Unit>
    
    // Recurring tasks
    suspend fun getRecurringSeries(parentTaskId: UUID): AppResult<List<TaskSummary>>
    suspend fun completeRecurringTask(task: Task, completedAtUtcMillis: Long, updatedAtUtcMillis: Long): AppResult<Task>
}
