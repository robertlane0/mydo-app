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
}
