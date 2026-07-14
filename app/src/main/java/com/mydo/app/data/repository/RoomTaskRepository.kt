package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.TaskEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toSummary
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomTaskRepository(private val db: MydoDatabase) : TaskRepository {
    private val dao = db.taskDao()
    private val labelDao = db.labelDao()
    private val projectDao = db.projectDao()

    private suspend fun mapToSummary(entity: TaskEntity): TaskSummary {
        val projectName = entity.projectId?.let { projectDao.getById(it)?.name }
        return entity.toSummary(projectName)
    }

    private suspend fun mapToDomain(entity: TaskEntity): Task {
        val labels = labelDao.getByTask(entity.id)
        val subtaskCount = dao.countSubtasks(entity.id)
        val completedSubtaskCount = dao.countCompletedSubtasks(entity.id)
        return entity.toDomain(labels, subtaskCount, completedSubtaskCount)
    }

    override fun observeInboxTasks(): Flow<AppResult<List<TaskSummary>>> =
        dao.observeInbox().map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load tasks", e))) }

    override fun observeTodayTasks(endOfDayUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> =
        dao.observeToday(endOfDayUtcMillis).map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load tasks", e))) }

    override fun observeProjectTasks(projectId: UUID): Flow<AppResult<List<TaskSummary>>> =
        dao.observeByProject(projectId.toUUIDString()).map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load tasks", e))) }

    override fun observeSectionTasks(sectionId: UUID): Flow<AppResult<List<TaskSummary>>> =
        dao.observeBySection(sectionId.toUUIDString()).map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load tasks", e))) }

    override fun observeById(id: UUID): Flow<AppResult<Task?>> =
        dao.observeById(id.toUUIDString()).map<TaskEntity?, AppResult<Task?>> { entity -> AppResult.Success(entity?.let { mapToDomain(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load task", e))) }

    override fun observeAllScheduledTasks(): Flow<AppResult<List<TaskSummary>>> =
        dao.observeAllScheduled().map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load scheduled tasks", e))) }

    override fun observeScheduledTasksFrom(startUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> =
        dao.observeScheduledFrom(startUtcMillis).map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load scheduled tasks", e))) }

    override fun observeOverdueTasks(nowUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> =
        dao.observeOverdueTasks(nowUtcMillis).map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load overdue tasks", e))) }

    override fun observeRecurringTasks(): Flow<AppResult<List<TaskSummary>>> =
        dao.observeRecurringTasks().map<List<TaskEntity>, AppResult<List<TaskSummary>>> { list -> AppResult.Success(list.map { mapToSummary(it) }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load recurring tasks", e))) }

    override suspend fun getById(id: UUID): AppResult<Task?> = try {
        AppResult.Success(dao.getById(id.toUUIDString())?.let { mapToDomain(it) })
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load task", e))
    }

    override suspend fun create(task: Task): AppResult<Unit> = try {
        val sortOrder = if (task.parentTaskId != null) {
            dao.nextSubtaskSortOrder(task.parentTaskId.toUUIDString())
        } else if (task.sectionId != null) {
            dao.nextSectionSortOrder(task.sectionId.toUUIDString())
        } else if (task.projectId != null) {
            dao.nextProjectSortOrder(task.projectId.toUUIDString())
        } else {
            dao.nextInboxSortOrder()
        }
        dao.insert(task.toEntity().copy(sortOrder = sortOrder))
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create task", e))
    }

    override suspend fun update(task: Task): AppResult<Unit> = try {
        dao.update(task.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update task", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete task", e))
    }

    override suspend fun updateCompletion(id: UUID, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = try {
        dao.updateCompletion(id.toUUIDString(), completed, completedAtUtcMillis, updatedAtUtcMillis)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update task completion", e))
    }

    override suspend fun moveToProject(id: UUID, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit> = try {
        dao.moveToProject(id.toUUIDString(), projectId?.toUUIDString(), sectionId?.toUUIDString(), updatedAtUtcMillis)
        val sortOrder = if (sectionId != null) {
            dao.nextSectionSortOrder(sectionId.toUUIDString())
        } else if (projectId != null) {
            dao.nextProjectSortOrder(projectId.toUUIDString())
        } else {
            dao.nextInboxSortOrder()
        }
        dao.updateSortOrder(id.toUUIDString(), sortOrder)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to move task", e))
    }

    override suspend fun clearSection(sectionId: UUID, updatedAtUtcMillis: Long): AppResult<Unit> = try {
        dao.clearSection(sectionId.toUUIDString(), updatedAtUtcMillis)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to clear section", e))
    }

    override suspend fun search(query: String): AppResult<List<TaskSummary>> = try {
        val results = dao.search(query).map { mapToSummary(it) }
        AppResult.Success(results)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to search tasks", e))
    }

    override suspend fun searchWithCompletion(query: String, completed: Boolean): AppResult<List<TaskSummary>> = try {
        val results = dao.searchWithCompletion(query, completed).map { mapToSummary(it) }
        AppResult.Success(results)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to search tasks", e))
    }

    override suspend fun bulkMoveToProject(ids: List<UUID>, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit> = try {
        dao.bulkMoveToProject(ids.map { it.toUUIDString() }, projectId?.toUUIDString(), sectionId?.toUUIDString(), updatedAtUtcMillis)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to bulk move tasks", e))
    }

    override suspend fun bulkSetPriority(ids: List<UUID>, priority: String, updatedAtUtcMillis: Long): AppResult<Unit> = try {
        dao.bulkSetPriority(ids.map { it.toUUIDString() }, priority, updatedAtUtcMillis)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to bulk set priority", e))
    }

    override suspend fun bulkSetDueDate(ids: List<UUID>, dueAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = try {
        dao.bulkSetDueDate(ids.map { it.toUUIDString() }, dueAtUtcMillis, updatedAtUtcMillis)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to bulk set due date", e))
    }

    override suspend fun bulkComplete(ids: List<UUID>, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = try {
        dao.bulkComplete(ids.map { it.toUUIDString() }, completed, completedAtUtcMillis, updatedAtUtcMillis)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to bulk complete tasks", e))
    }

    override suspend fun bulkDelete(ids: List<UUID>): AppResult<Unit> = try {
        dao.bulkDelete(ids.map { it.toUUIDString() })
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to bulk delete tasks", e))
    }

    override suspend fun getRecurringSeries(parentTaskId: UUID): AppResult<List<TaskSummary>> = try {
        val results = dao.getRecurringSeries(parentTaskId.toUUIDString()).map { mapToSummary(it) }
        AppResult.Success(results)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load recurring series", e))
    }

    override suspend fun completeRecurringTask(task: Task, completedAtUtcMillis: Long, updatedAtUtcMillis: Long): AppResult<Task> = try {
        // Mark current task as complete
        val completedTask = task.copy(
            completed = true,
            completedAtUtcMillis = completedAtUtcMillis,
            updatedAtUtcMillis = updatedAtUtcMillis
        )
        dao.update(completedTask.toEntity())
        
        // Generate next occurrence
        val nextDue = calculateNextDueDate(task.recurringRule!!, task.dueAtUtcMillis!!)
        if (nextDue != null) {
            val nextSortOrder = if (task.sectionId != null) {
                dao.nextSectionSortOrder(task.sectionId.toUUIDString())
            } else if (task.projectId != null) {
                dao.nextProjectSortOrder(task.projectId.toUUIDString())
            } else {
                dao.nextInboxSortOrder()
            }
            
            val nextTask = task.copy(
                id = UUID.randomUUID(),
                completed = false,
                dueAtUtcMillis = nextDue,
                completedAtUtcMillis = null,
                createdAtUtcMillis = updatedAtUtcMillis,
                updatedAtUtcMillis = updatedAtUtcMillis,
                parentTaskId = task.id,
                sortOrder = nextSortOrder
            )
            dao.insert(nextTask.toEntity())
            
            // Also copy reminders, labels, subtasks? For now just create the next occurrence
            AppResult.Success(nextTask)
        } else {
            // Recurrence ended (COUNT/UNTIL reached)
            AppResult.Success(completedTask)
        }
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to complete recurring task", e))
    }

    private fun calculateNextDueDate(rrule: String, currentDueUtcMillis: Long): Long? {
        // Simplified RRULE parser for next occurrence
        // Format: FREQ=DAILY|WEEKLY|MONTHLY|YEARLY[;INTERVAL=N][;BYDAY=MO,TU...][;BYMONTHDAY=1,15,-1][;BYMONTH=1,6...][;COUNT=N][;UNTIL=YYYYMMDD]
        val parts = rrule.split(";").associate { it.split("=").let { (k, v) -> k to v } }
        val freq = parts["FREQ"] ?: return null
        val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
        
        // This is a simplified implementation - a full RRULE parser would use a library
        // For now, implement basic daily/weekly/monthly/yearly with interval
        val currentMs = currentDueUtcMillis
        val dayMs = 24L * 60 * 60 * 1000
        
        return when (freq) {
            "DAILY" -> currentMs + interval * dayMs
            "WEEKLY" -> currentMs + interval * 7 * dayMs
            "MONTHLY" -> addMonths(currentMs, interval)
            "YEARLY" -> addYears(currentMs, interval)
            else -> null
        }
    }

    private fun addMonths(utcMillis: Long, months: Int): Long {
        // Simple month addition - in production use java.time
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = utcMillis
        calendar.add(java.util.Calendar.MONTH, months)
        return calendar.timeInMillis
    }

    private fun addYears(utcMillis: Long, years: Int): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = utcMillis
        calendar.add(java.util.Calendar.YEAR, years)
        return calendar.timeInMillis
    }
}