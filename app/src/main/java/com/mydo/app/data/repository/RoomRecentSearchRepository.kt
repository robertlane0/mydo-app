package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.RecentSearchEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.domain.model.RecentSearch
import com.mydo.app.domain.repository.RecentSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Caps stored history well above what the UI ever shows, per specs08-search.md ("Recent Searches"). */
private const val MAX_STORED_RECENT_SEARCHES = 25

class RoomRecentSearchRepository(
    private val db: MydoDatabase,
    private val timeProvider: TimeProvider,
) : RecentSearchRepository {
    private val dao = db.recentSearchDao()

    override fun observeRecent(limit: Int): Flow<AppResult<List<RecentSearch>>> =
        dao.observeRecent(limit)
            .map<List<RecentSearchEntity>, AppResult<List<RecentSearch>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load recent searches", e))) }

    override suspend fun record(query: String): AppResult<Unit> = try {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            dao.upsert(
                RecentSearchEntity(
                    id = UUID.randomUUID().toString(),
                    query = trimmed,
                    searchedAtUtcMillis = timeProvider.nowUtcMillis(),
                )
            )
            dao.trimTo(MAX_STORED_RECENT_SEARCHES)
        }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to save recent search", e))
    }

    override suspend fun remove(query: String): AppResult<Unit> = try {
        dao.deleteByQuery(query)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to remove recent search", e))
    }

    override suspend fun clear(): AppResult<Unit> = try {
        dao.clear()
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to clear recent searches", e))
    }
}
