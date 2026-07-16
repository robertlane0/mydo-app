package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // -- Observe queries --

    @Query("SELECT * FROM tasks WHERE projectId IS NULL AND parentTaskId IS NULL AND completed = 0 ORDER BY sortOrder ASC")
    fun observeInbox(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAtUtcMillis IS NOT NULL AND dueAtUtcMillis < :endOfDayUtcMillis AND parentTaskId IS NULL AND completed = 0 ORDER BY dueAtUtcMillis ASC, sortOrder ASC")
    fun observeToday(endOfDayUtcMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAtUtcMillis IS NOT NULL AND dueAtUtcMillis >= :startUtcMillis AND dueAtUtcMillis < :endUtcMillis AND parentTaskId IS NULL AND completed = 0 ORDER BY dueAtUtcMillis ASC, sortOrder ASC")
    fun observeByDateRange(startUtcMillis: Long, endUtcMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAtUtcMillis IS NOT NULL AND dueAtUtcMillis < :nowUtcMillis AND parentTaskId IS NULL AND completed = 0 ORDER BY dueAtUtcMillis ASC")
    fun observeOverdue(nowUtcMillis: Long): Flow<List<TaskEntity>>

    // Upcoming timeline: every scheduled (non-subtask) task from a rolling start point
    // forward. The ViewModel widens `sinceUtcMillis`'s window as the user scrolls,
    // which keeps this a single indexed query instead of a full table scan.
    @Query("SELECT * FROM tasks WHERE dueAtUtcMillis IS NOT NULL AND dueAtUtcMillis >= :sinceUtcMillis AND dueAtUtcMillis < :untilUtcMillis AND parentTaskId IS NULL AND completed = 0 ORDER BY dueAtUtcMillis ASC, sortOrder ASC")
    fun observeScheduledWindow(sinceUtcMillis: Long, untilUtcMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND parentTaskId IS NULL AND completed = 0 ORDER BY sortOrder ASC")
    fun observeByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE sectionId = :sectionId AND parentTaskId IS NULL AND completed = 0 ORDER BY sortOrder ASC")
    fun observeBySection(sectionId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId ORDER BY sortOrder ASC")
    fun observeSubtasks(parentTaskId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY completedAtUtcMillis DESC")
    fun observeCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    // -- Get queries (suspend) --

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE parentTaskId = :parentTaskId")
    suspend fun countSubtasks(parentTaskId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE parentTaskId = :parentTaskId AND completed = 1")
    suspend fun countCompletedSubtasks(parentTaskId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND completed = 0")
    suspend fun countActiveByProject(projectId: String): Int

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<TaskEntity>

    // Full local snapshot used by the search/filter query engine, which evaluates the
    // parsed query in-memory (see domain/search) rather than expressing every operator
    // — including OR/NOT/grouping across joined labels — as raw SQL.
    @Query("SELECT * FROM tasks")
    suspend fun getAllSnapshot(): List<TaskEntity>

    @Query("SELECT DISTINCT parentTaskId FROM tasks WHERE parentTaskId IS NOT NULL")
    suspend fun getParentIdsWithSubtasks(): List<String>

    // -- Search --

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND parentTaskId IS NULL ORDER BY updatedAtUtcMillis DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 50): List<TaskEntity>

    // -- Mutations --

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE tasks SET completed = :completed, completedAtUtcMillis = :completedAtUtcMillis, updatedAtUtcMillis = :updatedAtUtcMillis WHERE id = :id")
    suspend fun updateCompletion(id: String, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long)

    @Query("UPDATE tasks SET projectId = :projectId, sectionId = :sectionId, updatedAtUtcMillis = :updatedAtUtcMillis WHERE id = :id")
    suspend fun moveToProject(id: String, projectId: String?, sectionId: String?, updatedAtUtcMillis: Long)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Query("UPDATE tasks SET dueAtUtcMillis = :dueAtUtcMillis, updatedAtUtcMillis = :updatedAtUtcMillis WHERE id = :id")
    suspend fun updateDueDate(id: String, dueAtUtcMillis: Long?, updatedAtUtcMillis: Long)

    @Query("UPDATE tasks SET priority = :priority, updatedAtUtcMillis = :updatedAtUtcMillis WHERE id = :id")
    suspend fun updatePriority(id: String, priority: String, updatedAtUtcMillis: Long)

    @Query("UPDATE tasks SET recurringRule = :recurringRule, updatedAtUtcMillis = :updatedAtUtcMillis WHERE id = :id")
    suspend fun updateRecurrence(id: String, recurringRule: String?, updatedAtUtcMillis: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE projectId IS NULL AND parentTaskId IS NULL")
    suspend fun nextInboxSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE projectId = :projectId AND sectionId IS NULL AND parentTaskId IS NULL")
    suspend fun nextProjectSortOrder(projectId: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE sectionId = :sectionId AND parentTaskId IS NULL")
    suspend fun nextSectionSortOrder(sectionId: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE parentTaskId = :parentTaskId")
    suspend fun nextSubtaskSortOrder(parentTaskId: String): Int

    @Query("UPDATE tasks SET sectionId = NULL, updatedAtUtcMillis = :updatedAtUtcMillis WHERE sectionId = :sectionId")
    suspend fun clearSection(sectionId: String, updatedAtUtcMillis: Long)
}
