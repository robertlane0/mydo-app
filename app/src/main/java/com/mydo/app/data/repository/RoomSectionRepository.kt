package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.SectionEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Section
import com.mydo.app.domain.repository.SectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomSectionRepository(private val db: MydoDatabase) : SectionRepository {
    private val dao = db.sectionDao()

    override fun observeByProject(projectId: UUID): Flow<AppResult<List<Section>>> =
        dao.observeByProject(projectId.toUUIDString())
            .map<List<SectionEntity>, AppResult<List<Section>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load sections", e))) }

    override suspend fun getById(id: UUID): AppResult<Section?> = try {
        AppResult.Success(dao.getById(id.toUUIDString())?.toDomain())
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load section", e))
    }

    override suspend fun create(section: Section): AppResult<Unit> = try {
        val sortOrder = dao.nextSortOrder(section.projectId.toUUIDString())
        dao.insert(section.toEntity().copy(sortOrder = sortOrder))
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create section", e))
    }

    override suspend fun update(section: Section): AppResult<Unit> = try {
        dao.update(section.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update section", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete section", e))
    }

    override suspend fun reorder(id: UUID, sortOrder: Int): AppResult<Unit> = try {
        dao.updateSortOrder(id.toUUIDString(), sortOrder)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to reorder section", e))
    }

    override suspend fun search(query: String): AppResult<List<Section>> = try {
        AppResult.Success(dao.search(query).map { it.toDomain() })
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to search sections", e))
    }
}
