package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.ProjectEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomProjectRepository(private val db: MydoDatabase) : ProjectRepository {
    private val dao = db.projectDao()

    override fun observeActive(): Flow<AppResult<List<Project>>> =
        dao.observeActive().map<List<ProjectEntity>, AppResult<List<Project>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load projects", e))) }

    override fun observeArchived(): Flow<AppResult<List<Project>>> =
        dao.observeArchived().map<List<ProjectEntity>, AppResult<List<Project>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load projects", e))) }

    override fun observeFavorites(): Flow<AppResult<List<Project>>> =
        dao.observeFavorites().map<List<ProjectEntity>, AppResult<List<Project>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load projects", e))) }

    override fun observeById(id: UUID): Flow<AppResult<Project?>> =
        dao.observeById(id.toUUIDString()).map<ProjectEntity?, AppResult<Project?>> { entity -> AppResult.Success(entity?.toDomain()) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load project", e))) }

    override suspend fun getById(id: UUID): AppResult<Project?> = try {
        AppResult.Success(dao.getById(id.toUUIDString())?.toDomain())
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load project", e))
    }

    override suspend fun create(project: Project): AppResult<Unit> = try {
        dao.insert(project.toEntity().copy(sortOrder = dao.nextSortOrder()))
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create project", e))
    }

    override suspend fun update(project: Project): AppResult<Unit> = try {
        dao.update(project.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update project", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete project", e))
    }

    override suspend fun reorder(id: UUID, sortOrder: Int): AppResult<Unit> = try {
        dao.updateSortOrder(id.toUUIDString(), sortOrder)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to reorder project", e))
    }
}
