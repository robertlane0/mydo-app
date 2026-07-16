package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.RecentSearch
import kotlinx.coroutines.flow.Flow

interface RecentSearchRepository {
    fun observeRecent(limit: Int = 10): Flow<AppResult<List<RecentSearch>>>
    suspend fun record(query: String): AppResult<Unit>
    suspend fun remove(query: String): AppResult<Unit>
    suspend fun clear(): AppResult<Unit>
}
