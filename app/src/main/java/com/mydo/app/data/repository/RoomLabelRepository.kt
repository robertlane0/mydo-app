package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.LabelEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.repository.LabelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomLabelRepository(private val db: MydoDatabase) : LabelRepository {
    private val dao = db.labelDao()

    override fun observeAll(): Flow<AppResult<List<Label>>> =
        dao.observeAll().map<List<LabelEntity>, AppResult<List<Label>>> { list ->
            AppResult.Success(list.map { it.toDomain() })
        }.catch { e ->
            emit(AppResult.Failure(DatabaseError("db_error", "Failed to load labels", e)))
        }

    override suspend fun getById(id: UUID): AppResult<Label?> = try {
        AppResult.Success(dao.getById(id.toUUIDString())?.toDomain())
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load label", e))
    }

    override suspend fun create(label: Label): AppResult<Unit> = try {
        dao.insert(label.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create label", e))
    }

    override suspend fun update(label: Label): AppResult<Unit> = try {
        dao.update(label.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update label", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete label", e))
    }

    override suspend fun search(query: String): AppResult<List<Label>> = try {
        AppResult.Success(dao.search(query).map { it.toDomain() })
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to search labels", e))
    }
}