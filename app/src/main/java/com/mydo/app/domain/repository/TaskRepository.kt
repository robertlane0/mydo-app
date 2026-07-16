package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.search.TaskFilterContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TaskRepository {
    fun observeInboxTasks(): Flow<AppResult<List<TaskSummary>>>
    fun observeTodayTasks(endOfDayUtcMillis: Long): Flow<AppResult<List<TaskSummary>>>
    fun observeProjectTasks(projectId: UUID): Flow<AppResult<List<TaskSummary>>>
    fun observeSectionTasks(sectionId: UUID): Flow<AppResult<List<TaskSummary>>>
    fun observeById(id: UUID): Flow<AppResult<Task?>>

    /** Overdue: not completed, due before [nowUtcMillis] (specs07-upcoming.md, "Overdue"). */
    fun observeOverdue(nowUtcMillis: Long): Flow<AppResult<List<TaskSummary>>>

    /** Scheduled tasks in `[sinceUtcMillis, untilUtcMillis)`, for the Upcoming timeline's lazily-widened window. */
    fun observeScheduledWindow(sinceUtcMillis: Long, untilUtcMillis: Long): Flow<AppResult<List<TaskSummary>>>

    suspend fun getById(id: UUID): AppResult<Task?>
    suspend fun getByIds(ids: List<UUID>): AppResult<List<Task>>
    suspend fun create(task: Task): AppResult<Unit>
    suspend fun update(task: Task): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>

    suspend fun updateCompletion(id: UUID, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun updateDueDate(id: UUID, dueAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun updatePriority(id: UUID, priority: Priority, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun updateRecurrence(id: UUID, recurringRule: String?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun moveToProject(id: UUID, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun clearSection(sectionId: UUID, updatedAtUtcMillis: Long): AppResult<Unit>
    suspend fun search(query: String): AppResult<List<TaskSummary>>

    /** Atomically assigns new `sortOrder` values matching [orderedIds]' order (drag reorder / bulk move). */
    suspend fun reorder(orderedIds: List<UUID>): AppResult<Unit>

    /** One flat, pre-joined snapshot of every task for the search/filter query engine (specs08/specs14). */
    suspend fun getFilterContexts(): AppResult<List<TaskFilterContext>>
}
