package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Filter
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface FilterRepository {
    fun observeAll(): Flow<AppResult<List<Filter>>>
    fun observeFavorites(): Flow<AppResult<List<Filter>>>
    suspend fun getById(id: UUID): AppResult<Filter?>
    suspend fun create(filter: Filter): AppResult<Unit>
    suspend fun update(filter: Filter): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
}