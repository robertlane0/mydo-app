package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.FilterEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Filter
import com.mydo.app.domain.repository.FilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomFilterRepository(private val db: MydoDatabase) : FilterRepository {
    private val dao = db.filterDao()

    override fun observeAll(): Flow<AppResult<List<Filter>>> =
        dao.observeAll().map<List<FilterEntity>, AppResult<List<Filter>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load filters", e))) }

    override suspend fun getById(id: UUID): AppResult<Filter?> = try {
        AppResult.Success(dao.getById(id.toUUIDString())?.toDomain())
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load filter", e))
    }

    override suspend fun findByName(name: String): AppResult<Filter?> = try {
        AppResult.Success(dao.findByName(name)?.toDomain())
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to look up filter", e))
    }

    override suspend fun create(filter: Filter): AppResult<Unit> = try {
        dao.insert(filter.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create filter", e))
    }

    override suspend fun update(filter: Filter): AppResult<Unit> = try {
        dao.update(filter.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update filter", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete filter", e))
    }

    override suspend fun search(query: String): AppResult<List<Filter>> = try {
        AppResult.Success(dao.search(query).map { it.toDomain() })
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to search filters", e))
    }

    override suspend fun count(): AppResult<Int> = try {
        AppResult.Success(dao.count())
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to count filters", e))
    }
}
